package com.bisnode.opa;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nullable;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class StartOpaTaskTest {

    private Project project;

    @Before
    public void before() {
        project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("com.bisnode.opa");
    }

    @After
    public void after() {
        OpaPluginUtils.stopOpaProcess(project);
    }

    @Test
    public void canAddTaskToProject() {
        project = ProjectBuilder.builder().build();
        Task task = project.task("startOpa");
        assertThat(task instanceof StartOpaTask, is(true));
    }

    @Test
    public void taskIsInOpaGroup() {
        project = ProjectBuilder.builder().build();
        StartOpaTask task = (StartOpaTask) project.task("startOpa");
        assertThat(task.getGroup(), is("opa"));
    }

    @Test
    public void opaPluginStartTaskSavesProcessInExtProperties() {
        StartOpaTask startOpaTask = (StartOpaTask) project.getTasks().getByName("startOpa");
        startOpaTask.startOpa();

        @Nullable Object object = project.getExtensions().getExtraProperties().get("opaProcess");
        assertThat(object instanceof Process, is(true));
    }

}
