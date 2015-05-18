package net.serenitybdd.maven.plugins;

import net.serenitybdd.core.Serenity;
import net.thucydides.core.ThucydidesSystemProperty;
import net.thucydides.core.guice.Injectors;
import net.thucydides.core.reports.html.HtmlAggregateStoryReporter;
import net.thucydides.core.util.EnvironmentVariables;
import net.thucydides.core.webdriver.Configuration;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * Generate aggregate XML acceptance test reports.
x *
 */
@Mojo(name = "aggregate", requiresProject = false)
public class SerenityAggregatorMojo extends AbstractMojo {

    /**
     * Aggregate reports are generated here
     */
    @Parameter(property = "serenity.outputDirectory")
    public File outputDirectory;

    /**
     * Serenity test reports are read from here
     */
    @Parameter(property = "serenity.sourceDirectory")
    public File sourceDirectory;

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

    @Parameter
    public String jiraUsername;

    @Parameter
    public String jiraPassword;

    /**
     * JIRA project key, which will be prepended to the JIRA issue numbers.
     */
    @Parameter
    public String jiraProject;

    /**
     * Base directory for requirements.
     */
    @Parameter
    public String requirementsBaseDir;

    EnvironmentVariables environmentVariables;

    Configuration configuration;

    /**
     * Serenity project key
     */
    @Parameter(property = "thucydides.project.key", defaultValue = "default")
    public String projectKey;

    protected void setOutputDirectory(final File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    protected void setSourceDirectory(final File sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
    }

    public void prepareExecution() {
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
    }

    private EnvironmentVariables getEnvironmentVariables() {
        if (environmentVariables == null) {
            environmentVariables = Injectors.getInjector().getProvider(EnvironmentVariables.class).get() ;
        }
        return environmentVariables;
    }

    private Configuration getConfiguration() {
        if (configuration == null) {
            configuration = Injectors.getInjector().getProvider(Configuration.class).get() ;
        }
        return configuration;
    }

    private void configureEnvironmentVariables() {
        Locale.setDefault(Locale.ENGLISH);
        updateSystemProperty(ThucydidesSystemProperty.THUCYDIDES_PROJECT_KEY.getPropertyName(), projectKey, Serenity.getDefaultProjectKey());
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

    private HtmlAggregateStoryReporter reporter;

    protected void setReporter(final HtmlAggregateStoryReporter reporter) {
        this.reporter = reporter;
    }

    public void execute() throws MojoExecutionException {
        prepareExecution();

        try {
            generateHtmlStoryReports();
        } catch (IOException e) {
            throw new MojoExecutionException("Error generating aggregate serenity reports", e);
        }
    }

    protected HtmlAggregateStoryReporter getReporter() {
        if (reporter == null) {
            reporter = new HtmlAggregateStoryReporter(projectKey);
        }
        return reporter;

    }

    private void generateHtmlStoryReports() throws IOException {
        getReporter().setSourceDirectory(sourceOfTestResult());
        getReporter().setOutputDirectory(outputDirectory);
        getReporter().setIssueTrackerUrl(issueTrackerUrl);
        getReporter().setJiraUrl(jiraUrl);
        getReporter().setJiraProject(jiraProject);
        getReporter().setJiraUsername(jiraUsername);
        getReporter().setJiraPassword(jiraPassword);
        getReporter().generateReportsForTestResultsFrom(sourceOfTestResult());
    }

    private File sourceOfTestResult() {
        if ((sourceDirectory != null) && (sourceDirectory.exists())) {
            return sourceDirectory;
        } else {
            return outputDirectory;
        }

    }
}
