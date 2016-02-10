package net.serenitybdd.maven.plugins;

import com.beust.jcommander.internal.Lists;
import net.serenitybdd.core.Serenity;
import net.thucydides.core.ThucydidesSystemProperty;
import net.thucydides.core.guice.Injectors;
import net.thucydides.core.reports.UserStoryTestReporter;
import net.thucydides.core.reports.html.HtmlAggregateStoryReporter;
import net.thucydides.core.util.EnvironmentVariables;
import net.thucydides.core.webdriver.Configuration;
import org.apache.commons.lang3.StringUtils;
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
import java.util.Collection;
import java.util.Locale;

/**
 * Generate aggregate XML acceptance test reports.
 */
@Mojo(name = "aggregate", requiresProject = false, requiresDependencyResolution = ResolutionScope.COMPILE)
public class SerenityAggregatorMojo extends AbstractMojo {

    private final static Logger LOGGER = LoggerFactory.getLogger(SerenityAggregatorMojo.class);

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
            generateCustomReports();
        } catch (IOException e) {
            throw new MojoExecutionException("Error generating aggregate serenity reports", e);
        }
    }

    private void generateCustomReports() throws IOException {
        System.out.println("GENERATE CUSTOM REPORTS");
        Collection<UserStoryTestReporter> customReporters = getCustomReportsFor(environmentVariables);
        for(UserStoryTestReporter reporter : customReporters) {
            System.out.println("GENERATE CUSTOM REPORT FOR " + reporter.getClass().getCanonicalName());
            reporter.generateReportsForTestResultsFrom(sourceOfTestResult());
        }
     }

    private Collection<UserStoryTestReporter> getCustomReportsFor(EnvironmentVariables environmentVariables) {

        Collection<UserStoryTestReporter> reports = Lists.newArrayList();

        for(String environmentVariable : environmentVariables.getKeys()) {
            if (environmentVariable.startsWith("serenity.custom.reporters.")) {
                String reportClass = environmentVariables.getProperty(environmentVariable);
                try {
                    UserStoryTestReporter reporter = (UserStoryTestReporter) Class.forName(reportClass).newInstance();
                    //String name = lastElementOf(Splitter.on(".").splitToList(environmentVariable));
                    reports.add(reporter);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return reports;
    }

//    private String lastElementOf(List<String> elements) {
//        return elements.isEmpty() ? "" : elements.get(elements.size() - 1);
//    }

    protected HtmlAggregateStoryReporter getReporter() {
        if (reporter == null) {
            reporter = new HtmlAggregateStoryReporter(projectKey);
        }
        return reporter;

    }

    private void generateHtmlStoryReports() throws IOException {
        System.out.println("Generating HTML Story Reports from "+sourceDirectory.getAbsolutePath());
        System.out.println("Generating HTML Story Reports to "+outputDirectory.getAbsolutePath());
        getReporter().setSourceDirectory(sourceDirectory);
        getReporter().setOutputDirectory(outputDirectory);
        getReporter().setIssueTrackerUrl(issueTrackerUrl);
        getReporter().setJiraUrl(jiraUrl);
        getReporter().setJiraProject(jiraProject);
        getReporter().setJiraUsername(jiraUsername);
        getReporter().setJiraPassword(jiraPassword);
        getReporter().generateReportsForTestResultsFrom(sourceDirectory);
    }

    private File sourceOfTestResult() {
        if ((sourceDirectory != null) && (sourceDirectory.exists())) {
            return sourceDirectory;
        } else {
            return outputDirectory;
        }

    }
}
