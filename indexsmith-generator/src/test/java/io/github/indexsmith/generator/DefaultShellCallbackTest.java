package io.github.indexsmith.generator;

import io.github.indexsmith.generator.exception.ShellException;
import io.github.indexsmith.generator.util.DefaultShellCallback;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DefaultShellCallbackTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void createsDirectoryFromJavaPackage() throws Exception {
        DefaultShellCallback callback = new DefaultShellCallback(true);

        File directory = callback.getDirectory(
                temporaryFolder.getRoot().getAbsolutePath(), "example.generated.model");

        assertTrue(directory.isDirectory());
        assertTrue(directory.getPath().endsWith("example/generated/model"));
        assertTrue(callback.isOverwriteEnabled());
    }

    @Test(expected = ShellException.class)
    public void rejectsTargetProjectThatIsNotDirectory() throws Exception {
        File file = temporaryFolder.newFile("output.java");
        new DefaultShellCallback(false).getDirectory(file.getAbsolutePath(), "example.generated");
    }
}
