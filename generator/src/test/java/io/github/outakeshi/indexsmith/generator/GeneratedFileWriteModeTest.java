package io.github.outakeshi.indexsmith.generator;

import io.github.outakeshi.indexsmith.generator.model.GeneratorJavaFile;
import io.github.outakeshi.indexsmith.generator.model.WriteMode;
import io.github.outakeshi.indexsmith.generator.util.FileUtil;
import com.github.javaparser.ast.CompilationUnit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GeneratedFileWriteModeTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void treatsIdenticalContentAsSuccessInFailMode() throws Exception {
        GeneratorJavaFile source = source("int", "value");
        FileUtil.writeFile(source, WriteMode.FAIL);
        byte[] before = Files.readAllBytes(output().toPath());

        FileUtil.writeFile(source, WriteMode.FAIL);

        assertEquals(new String(before, StandardCharsets.UTF_8),
                new String(Files.readAllBytes(output().toPath()), StandardCharsets.UTF_8));
    }

    @Test
    public void failAndSkipPreserveExistingSourceWhileOverwriteReplacesIt() throws Exception {
        FileUtil.writeFile(source("int", "value"), WriteMode.FAIL);
        String original = readOutput();
        try {
            FileUtil.writeFile(source("String", "name"), WriteMode.FAIL);
            fail("Expected source conflict");
        } catch (IOException exception) {
            assertTrue(exception.getMessage().contains("differs"));
        }
        assertEquals(original, readOutput());

        FileUtil.writeFile(source("String", "name"), WriteMode.SKIP);
        assertEquals(original, readOutput());

        FileUtil.writeFile(source("String", "name"), WriteMode.OVERWRITE);
        assertTrue(readOutput().contains("String name"));
    }

    @Test
    public void parsesWriteModesAndRejectsUnknownValue() {
        assertEquals(WriteMode.FAIL, WriteMode.parse(null));
        assertEquals(WriteMode.SKIP, WriteMode.parse("skip"));
        assertEquals(WriteMode.OVERWRITE, WriteMode.parse(" OVERWRITE "));
        try {
            WriteMode.parse("merge");
            fail("Expected invalid mode");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().contains("FAIL, SKIP, or OVERWRITE"));
        }
    }

    @Test
    public void failModePreflightsBatchBeforeWritingAnySource() throws Exception {
        FileUtil.writeFile(source("int", "value"), WriteMode.FAIL);
        GeneratorJavaFile newSource = source("Added", "String", "name");
        GeneratorJavaFile conflictingSource = source("String", "name");

        try {
            FileUtil.writeFiles(Arrays.asList(newSource, conflictingSource), WriteMode.FAIL);
            fail("Expected source conflict");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("differs"));
        }

        assertFalse(new File(temporaryFolder.getRoot(), "example/generated/Added.java").exists());
    }

    private GeneratorJavaFile source(String type, String name) {
        return source("Sample", type, name);
    }

    private GeneratorJavaFile source(String className, String type, String name) {
        CompilationUnit unit = new CompilationUnit("example.generated");
        unit.addClass(className).setPublic(true).addField(type, name);
        return new GeneratorJavaFile(temporaryFolder.getRoot().getAbsolutePath(), "example.generated", unit);
    }

    private File output() {
        return new File(temporaryFolder.getRoot(), "example/generated/Sample.java");
    }

    private String readOutput() throws IOException {
        return new String(Files.readAllBytes(output().toPath()), StandardCharsets.UTF_8);
    }
}
