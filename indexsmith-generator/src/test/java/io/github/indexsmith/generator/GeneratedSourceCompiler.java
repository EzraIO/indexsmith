package io.github.indexsmith.generator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Compiles generated sources in a real javac process so Lombok works on both JDK 8 and newer JDKs. */
final class GeneratedSourceCompiler {
    private GeneratedSourceCompiler() { }

    static void compile(Path sourceRoot, File outputDirectory) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(findJavac().getAbsolutePath());
        command.add("-classpath");
        command.add(System.getProperty("java.class.path"));
        command.add("-d");
        command.add(outputDirectory.getAbsolutePath());
        try (Stream<Path> generated = Files.walk(sourceRoot)) {
            generated.filter(path -> path.toString().endsWith(".java"))
                    .map(Path::toString)
                    .forEach(command::add);
        }
        Process process = new ProcessBuilder(command).inheritIO().start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new AssertionError("Generated source compilation failed with exit code " + exitCode);
        }
    }

    private static File findJavac() throws IOException {
        File javaHome = new File(System.getProperty("java.home")).getCanonicalFile();
        File[] homes = {javaHome, javaHome.getParentFile()};
        for (File home : homes) {
            if (home == null) continue;
            File executable = new File(home, "bin/javac");
            if (executable.isFile()) return executable;
            File windowsExecutable = new File(home, "bin/javac.exe");
            if (windowsExecutable.isFile()) return windowsExecutable;
        }
        throw new IOException("Unable to find javac beneath java.home " + javaHome);
    }
}
