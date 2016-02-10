package net.serenitybdd.maven.plugins;

import net.thucydides.core.guice.Injectors;
import net.thucydides.core.reports.TestOutcomeAdaptorReporter;
import net.thucydides.core.reports.adaptors.AdaptorService;
import net.thucydides.core.util.EnvironmentVariables;
import net.thucydides.core.webdriver.Configuration;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;

/**
 * This plugin generates converts external (e.g. xUnit) files into Serenity reports.
 */
@Mojo( name = "import", requiresProject=false)
public class SerenityAdaptorMojo extends AbstractMojo {

    private final static Logger LOGGER = LoggerFactory.getLogger(SerenityAggregatorMojo.class);

    /**
     * Aggregate reports are generated here
     */
    @Parameter(property = "import.target", defaultValue = "${user.dir}/target/site/serenity", required=true)
    public File outputDirectory;

    /**
     * External test reports are read from here
     */
    @Parameter(property = "import.format", required=true)
    public String format;

    /**
     * External test reports are read from here if necessary.
     * This could be either a directory or a single file, depending on the adaptor used.
     * For some adaptors (e.g. database connectors), it will not be necessary.
     */
    @Parameter(property = "import.source")
    public File sourceDirectory;

    @Parameter(defaultValue = "${session}")
    protected MavenSession session;

    Configuration configuration;

    private final EnvironmentVariables environmentVariables;
    private final AdaptorService adaptorService;
    private final TestOutcomeAdaptorReporter reporter = new TestOutcomeAdaptorReporter();

    public SerenityAdaptorMojo(EnvironmentVariables environmentVariables) {
        MavenProjectHelper.propagateBuildDir(session);
        configureOutputDirectorySettings();
        this.environmentVariables = environmentVariables;
        this.adaptorService = new AdaptorService(environmentVariables);
    }

    public SerenityAdaptorMojo() {
        this(Injectors.getInjector().getProvider(EnvironmentVariables.class).get() );
    }

    protected File getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setSourceDirectory(File sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Importing external test reports");
        getLog().info("Source directory: " + sourceDirectory);
        getLog().info("Output directory: " + getOutputDirectory());

        try {
            getLog().info("Adaptor: " + adaptorService.getAdaptor(format));
            reporter.registerAdaptor(adaptorService.getAdaptor(format));
            reporter.setOutputDirectory(outputDirectory);
            reporter.generateReportsFrom(sourceDirectory);
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage());
        }
    }

    private void configureOutputDirectorySettings() {
        if (outputDirectory == null) {
            outputDirectory = getConfiguration().getOutputDirectory();
        }
        if (sourceDirectory == null) {
            sourceDirectory = getConfiguration().getOutputDirectory();
        }
        final Path projectDir = session.getCurrentProject().getBasedir().toPath();
        LOGGER.info("current_project.base.dir: " + projectDir.toAbsolutePath().toString());
        if (!outputDirectory.isAbsolute()) {
            outputDirectory = projectDir.resolve(outputDirectory.toPath()).toFile();
        }
        if (!sourceDirectory.isAbsolute()) {
            sourceDirectory = projectDir.resolve(sourceDirectory.toPath()).toFile();
        }
    }

    private Configuration getConfiguration() {
        if (configuration == null) {
            configuration = Injectors.getInjector().getProvider(Configuration.class).get();
        }
        return configuration;
    }
}
