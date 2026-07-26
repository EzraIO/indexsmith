package io.github.indexsmith.generator.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.lang.reflect.Field;

public class EsGeneratorMojoTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test(expected = MojoExecutionException.class)
    public void failsBuildWhenConfigurationFileIsMissing() throws Exception {
        EsGeneratorMojo mojo = new EsGeneratorMojo();
        MavenProject project = new MavenProject();
        project.setFile(new File(temporaryFolder.getRoot(), "pom.xml"));
        set(mojo, "project", project);
        set(mojo, "configurationFile", new File(temporaryFolder.getRoot(), "missing.xml"));

        mojo.execute();
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
