package io.github.outakeshi.indexsmith.generator.plugin;

import io.github.outakeshi.indexsmith.generator.generate.EsGenerator;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;

import static io.github.outakeshi.indexsmith.generator.plugin.BeanConvertUtil.buildEsContext;

/** Generates typed repositories from configured Elasticsearch index mappings. */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresOnline = true, requiresDependencyResolution = ResolutionScope.NONE, threadSafe = true)
public final class EsGeneratorMojo extends AbstractMojo {

    /**
     * Maven Project.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * Location of the configuration file.
     */
    @Parameter(property = "es.generator.configurationFile",
            defaultValue = "${project.basedir}/indexsmith-generator.xml", required = true)
    private File configurationFile;


    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Generating IndexSmith repositories");
        try {
            getLog().debug("Configuration file: " + configurationFile.getAbsolutePath());
            if (!configurationFile.exists()) {
                String errorMsg = "Configuration file does not exist: " + configurationFile.getAbsolutePath();
                throw new MojoExecutionException(errorMsg);
            }
            GeneratorConfiguration generatorConfiguration = XmlUtil.parse(configurationFile, new GeneratorConfiguration());
            ConfigurationInterpolator.interpolate(generatorConfiguration);
            if (generatorConfiguration.getSourceDir() == null
                    || generatorConfiguration.getSourceDir().trim().isEmpty()) {
                generatorConfiguration.setSourceDir("target/generated-sources/indexsmith");
            }
            if (generatorConfiguration.getClusters() == null
                    || generatorConfiguration.getClusters().isEmpty()) {
                throw new MojoExecutionException("At least one Elasticsearch cluster must be configured");
            }

            for (Cluster cluster : generatorConfiguration.getClusters()) {
                try (io.github.outakeshi.indexsmith.generator.model.EsConfigContext context =
                             buildEsContext(project, generatorConfiguration, cluster)) {
                    EsGenerator.generator(context);
                    project.addCompileSourceRoot(context.getTargetProject());
                }
            }
            getLog().info("IndexSmith repository generation completed");
        } catch (Exception e) {
            if (e instanceof MojoExecutionException) {
                throw (MojoExecutionException) e;
            }
            throw new MojoExecutionException("Error generating Elasticsearch code", e);
        }
    }
}
