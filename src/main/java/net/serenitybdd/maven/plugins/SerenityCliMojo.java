package net.serenitybdd.maven.plugins;


import com.beust.jcommander.internal.Lists;
import com.google.common.base.Strings;
import net.serenitybdd.cli.Serenity;
import net.thucydides.core.ThucydidesSystemProperty;
import net.thucydides.core.guice.Injectors;
import net.thucydides.core.reports.UserStoryTestReporter;
import net.thucydides.core.reports.html.HtmlAggregateStoryReporter;
import net.thucydides.core.util.EnvironmentVariables;
import net.thucydides.core.webdriver.Configuration;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

@Mojo(name = "cli", requiresProject = false, requiresDependencyResolution = ResolutionScope.COMPILE)
public class SerenityCliMojo extends AbstractMojo {

    private final static Logger LOGGER = LoggerFactory.getLogger(SerenityCliMojo.class);

    /**
     * Destination directory to contain the generated Serenity report
     */
    @Parameter(property = "serenity.cli.destinationDirectory")

    public File outputDirectory;

    /**
     * Source directory containing the Serenity JSON output files
     */
    @Parameter(property = "serenity.cli.sourceDirectory")
    public File sourceDirectory;

    /**
     * Project name to appear in the Serenity reports (defaults to the directory name)
     */
    @Parameter
    public String project;

    /**
     * URL of the issue tracking system to be used to generate links for issue numbers.
     */
    @Parameter
    public String issueTrackerUrl;

    /**
     * Base URL for JIRA, if you are using JIRA as your issue tracking system.
     * If you specify this property, you don't need to specify the issueTrackerUrl.
     */
    @Parameter
    public String jiraUrl;

    /**
     * JIRA project key, which will be prepended to the JIRA issue numbers.
     */
    @Parameter
    public String jiraProjectKey;

    @Parameter
    public String jiraUsername;

    @Parameter
    public String jiraPassword;

    @Parameter
    public String jiraWorkflow;

    @Parameter
    public String jiraWorkflowActive;

    /**
     * Base directory for requirements.
     */
    @Parameter
    public String requirementsBaseDir;

    EnvironmentVariables environmentVariables;

    Configuration configuration;

    @Parameter(defaultValue = "${session}")
    protected MavenSession session;

    /**
     * Serenity project key
     */
    @Parameter(property = "thucydides.project.key", defaultValue = "default")
    public String projectKey;

    protected void setOutputDirectory(final File outputDirectory) {
        this.outputDirectory = outputDirectory;
        getConfiguration().setOutputDirectory(this.outputDirectory);
    }

    protected void setSourceDirectory(final File sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
    }

    public void prepareExecution() {
        MavenProjectHelper.propagateBuildDir(session);
        configureOutputDirectorySettings();
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
        configureEnvironmentVariables();
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

    private EnvironmentVariables getEnvironmentVariables() {
        if (environmentVariables == null) {
            environmentVariables = Injectors.getInjector().getProvider(EnvironmentVariables.class).get() ;
        }
        return environmentVariables;
    }

    private Configuration getConfiguration() {
        if (configuration == null) {
            configuration = Injectors.getInjector().getProvider(Configuration.class).get();
        }
        return configuration;
    }

    private void configureEnvironmentVariables() {
        Locale.setDefault(Locale.ENGLISH);
        updateSystemProperty(ThucydidesSystemProperty.THUCYDIDES_PROJECT_KEY.getPropertyName(), projectKey, net.serenitybdd.core.Serenity.getDefaultProjectKey());
        updateSystemProperty(ThucydidesSystemProperty.THUCYDIDES_TEST_REQUIREMENTS_BASEDIR.toString(),
                requirementsBaseDir);
    }

    private void updateSystemProperty(String key, String value, String defaultValue) {
        if (value != null) {
            getEnvironmentVariables().setProperty(key, value);
        } else {
            getEnvironmentVariables().setProperty(key, defaultValue);
        }
    }

    private void updateSystemProperty(String key, String value) {
        if (value != null) {
            getEnvironmentVariables().setProperty(key, value);
        }
    }

    private void addNotEmptyArgument(List argList, String argKey, String argValue) {
        if(!Strings.isNullOrEmpty(argValue)) {
            argList.add(argKey);
            argList.add(argValue);
        }
    }

    public void execute() throws MojoExecutionException {
        prepareExecution();
        try {
            List<String> args = new ArrayList<>();

            if(sourceDirectory != null) {
                args.add("-source");
                args.add(sourceDirectory.getAbsolutePath());
            }
            if(outputDirectory != null) {
                args.add("-destination");
                args.add(outputDirectory.getAbsolutePath());
            }
            addNotEmptyArgument(args,"-project",project);
            addNotEmptyArgument(args,"-issueTrackerUrl",issueTrackerUrl);
            addNotEmptyArgument(args,"-jiraUrl",jiraUrl);
            addNotEmptyArgument(args,"-jiraProject",jiraProjectKey);
            addNotEmptyArgument(args,"-jiraUsername",jiraUsername);
            addNotEmptyArgument(args,"-jiraPassword",jiraPassword);
            addNotEmptyArgument(args,"-jiraWorkflowActive",jiraPassword);
            addNotEmptyArgument(args,"-jiraWorkflow",jiraWorkflow);
            String[] argsArray = args.toArray(new String[args.size()]);
            new Serenity().executeWith(argsArray);
        } catch (Exception e) {
            throw new MojoExecutionException("Error generating aggregate serenity reports", e);
        }
    }
}
