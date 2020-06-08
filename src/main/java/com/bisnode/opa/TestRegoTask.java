package com.bisnode.opa;

import com.bisnode.opa.configuration.OpaPluginConvention;
import com.bisnode.opa.junit.OpaToJunitConverter;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.tooling.TestExecutionException;
import org.w3c.dom.Document;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class TestRegoTask extends DefaultTask {

    public TestRegoTask() {
        setGroup("opa");
        setDescription("Run OPA tests in testDir of any policies provided in srcDir");
    }

    @Nullable
    private String srcDir;
    @Nullable
    private String testDir;

    @TaskAction
    public void testRego() {
        Project project = getProject();
        Logger logger = getLogger();
        OpaPluginConvention convention = project.getConvention().getPlugin(OpaPluginConvention.class);

        String src = Optional.ofNullable(srcDir).orElse(convention.getSrcDir());
        String testSrc = Optional.ofNullable(testDir).orElse(convention.getTestDir());

        String location = convention.getLocation();
        List<String> command = Arrays.asList(location, "test", "--format=json", src, testSrc);

        Process process;
        try {
            logger.debug("Running command {}", String.join(" ", command));
            process = new ProcessBuilder()
                    .directory(project.getRootDir())
                    .command(command)
                    .start();

            int exitCode = process.waitFor();

            try (BufferedReader reader = bufferedInputReader(process.getInputStream())) {
                String output = reader.lines().collect(Collectors.joining("\n"));
                Document document = OpaToJunitConverter.fromOpaTestJson(output);

                String targetReport = getProject().getBuildDir().getAbsolutePath() + "/test-results/opa/";
                if (!new File(targetReport).mkdirs()) {
                    throw new IOException("Could not create test results directory " + targetReport);
                }

                try (OutputStream out = new FileOutputStream(targetReport + "TEST-opa-tests.xml")) {
                    OpaToJunitConverter.write(document, out);
                }

                if (exitCode != 0) {
                    throw new TestExecutionException(output);
                }
            } catch (IOException e) {
                throw new TestExecutionException("Failed to read input stream", e);
            }

        } catch (IOException | InterruptedException e) {
            throw new TestExecutionException("Failed to start OPA process for tests", e);
        }
    }

    @InputDirectory
    public String getSrcDir() {
        return Optional.ofNullable(srcDir)
                .orElse(getProject().getConvention().getPlugin(OpaPluginConvention.class).getSrcDir());
    }

    @InputDirectory
    public String getTestDir() {
        return Optional.ofNullable(testDir)
                .orElse(getProject().getConvention().getPlugin(OpaPluginConvention.class).getTestDir());
    }

    public void setSrcDir(String srcDir) {
        this.srcDir = srcDir;
    }

    public void setTestDir(String testDir) {
        this.testDir = testDir;
    }

    private static BufferedReader bufferedInputReader(InputStream inputStream) {
        return new BufferedReader(new InputStreamReader(inputStream, UTF_8));
    }

}
