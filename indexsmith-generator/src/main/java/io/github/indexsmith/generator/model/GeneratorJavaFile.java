package io.github.indexsmith.generator.model;


import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Name;
import lombok.Getter;

import java.util.Optional;

/**
 */
@Getter
public class GeneratorJavaFile {
    private final String targetProject;
    private final Name targetPackage;
    private final String filedName;
    private final CompilationUnit compilationUnit;
    private final String fileEncoding;

    public GeneratorJavaFile(String targetProject, Name targetPackage, CompilationUnit compilationUnit) {
        this.targetProject = targetProject;
        this.targetPackage = targetPackage;
        this.filedName = compilationUnit.getTypes().get(0).getName().asString() + ".java";
        this.compilationUnit = compilationUnit;
        this.fileEncoding = "UTF-8";
    }

    public GeneratorJavaFile(String targetProject, String targetPackage, CompilationUnit compilationUnit) {
        this.targetProject = targetProject;
        this.targetPackage = new Name(targetPackage);
        this.filedName = compilationUnit.getTypes().get(0).getName().asString() + ".java";
        this.compilationUnit = compilationUnit;
        this.fileEncoding = "UTF-8";
    }

    public Optional<TypeDeclaration<?>> getType() {
        return compilationUnit.getPrimaryType();
    }
}
