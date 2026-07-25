package io.github.outakeshi.indexsmith.generator.util;

import io.github.outakeshi.indexsmith.generator.exception.ShellException;
import io.github.outakeshi.indexsmith.generator.model.GeneratorJavaFile;
import io.github.outakeshi.indexsmith.generator.model.WriteMode;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Safe writer for generated source files. */
@Slf4j
public final class FileUtil {
    private static final DefaultShellCallback DEFAULT_SHELL_CALLBACK = new DefaultShellCallback(true);

    private FileUtil() { }

    public static void writeFile(GeneratorJavaFile generatorJavaFile) throws IOException {
        writeFile(generatorJavaFile, WriteMode.FAIL);
    }

    public static void writeFile(GeneratorJavaFile generatorJavaFile, WriteMode writeMode) throws IOException {
        writeFiles(java.util.Collections.singletonList(generatorJavaFile), writeMode);
    }

    /** Preflights the full batch so FAIL mode never leaves a partially generated source set. */
    public static void writeFiles(Collection<GeneratorJavaFile> generatedFiles, WriteMode writeMode) throws IOException {
        if (generatedFiles == null) throw new IllegalArgumentException("generatedFiles must not be null");
        WriteMode effectiveMode = writeMode == null ? WriteMode.FAIL : writeMode;
        Map<Path, PreparedFile> uniqueFiles = new LinkedHashMap<>();
        for (GeneratorJavaFile generatedFile : generatedFiles) {
            PreparedFile prepared = prepare(generatedFile);
            PreparedFile previous = uniqueFiles.putIfAbsent(prepared.target, prepared);
            if (previous != null && !Arrays.equals(previous.content, prepared.content)) {
                throw new IOException("Multiple generated sources resolve to " + prepared.target
                        + " with different content");
            }
        }

        List<PreparedFile> writableFiles = new ArrayList<>();
        for (PreparedFile prepared : uniqueFiles.values()) {
            if (Files.exists(prepared.target)) {
                if (Arrays.equals(Files.readAllBytes(prepared.target), prepared.content)) {
                    log.debug("generated source unchanged: {}", prepared.target.toAbsolutePath());
                    continue;
                }
                if (effectiveMode == WriteMode.SKIP) {
                    log.warn("generated source differs; keeping existing file: {}", prepared.target.toAbsolutePath());
                    continue;
                }
                if (effectiveMode == WriteMode.FAIL) {
                    throw new IOException("Generated source differs from existing file "
                            + prepared.target.toAbsolutePath() + "; choose SKIP or OVERWRITE explicitly");
                }
            }
            writableFiles.add(prepared);
        }
        for (PreparedFile prepared : writableFiles) {
            writeAtomically(prepared.target, prepared.content);
            log.info("generated source written: {}", prepared.target.toAbsolutePath());
        }
    }

    private static PreparedFile prepare(GeneratorJavaFile generatedFile) throws IOException {
        if (generatedFile == null) throw new IllegalArgumentException("generatedFile must not be null");
        File targetFile;
        try {
            targetFile = getTargetFile(generatedFile);
        } catch (ShellException exception) {
            throw new IOException("Unable to resolve output path for " + generatedFile.getFiledName(), exception);
        }
        Charset charset = Charset.forName(generatedFile.getFileEncoding() == null
                ? Charset.defaultCharset().name() : generatedFile.getFileEncoding());
        byte[] content = generatedFile.getCompilationUnit().toString().getBytes(charset);
        return new PreparedFile(targetFile.toPath().toAbsolutePath().normalize(), content);
    }

    private static File getTargetFile(GeneratorJavaFile generatorJavaFile) throws ShellException {
        File directory = DEFAULT_SHELL_CALLBACK.getDirectory(
                generatorJavaFile.getTargetProject(), generatorJavaFile.getTargetPackage().asString());
        return new File(directory, generatorJavaFile.getFiledName());
    }

    private static void writeAtomically(Path target, byte[] content) throws IOException {
        Path parent = target.getParent();
        Path temporary = Files.createTempFile(parent, target.getFileName().toString(), ".tmp");
        boolean moved = false;
        try {
            Files.write(temporary, content);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
        } finally {
            if (!moved) Files.deleteIfExists(temporary);
        }
    }

    private static final class PreparedFile {
        private final Path target;
        private final byte[] content;

        private PreparedFile(Path target, byte[] content) {
            this.target = target;
            this.content = content;
        }
    }
}
