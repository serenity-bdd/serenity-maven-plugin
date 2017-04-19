package net.serenitybdd.maven.plugins;

import com.google.common.base.Optional;
import net.serenitybdd.core.history.FileSystemTestOutcomeSummaryRecorder;
import net.serenitybdd.core.history.TestOutcomeSummaryRecorder;
import net.thucydides.core.ThucydidesSystemProperty;
import net.thucydides.core.guice.Injectors;
import net.thucydides.core.util.EnvironmentVariables;
import net.thucydides.core.webdriver.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This plugin records a summary of test results in the target directory
 */
@Mojo(name = "history")
public class SerenityHistoryMojo extends AbstractMojo {

    private final static String DEFAULT_HISTORY_DIRECTORY = "history";
    /**
     * Test outcome summaries are stored here
     */
    @Parameter(property = "serenity.outputDirectory")
    public String outcomesDirectoryPath;

    @Parameter(property = "serenity.historyDirectory")
    public String historyDirectoryPath;

    @Parameter(property = "serenity.deletePreviousHistory")
    public Boolean deletePreviousHistory = false;

    @Parameter(defaultValue = "${session}")
    private MavenSession session;


    protected TestOutcomeSummaryRecorder getTestOutcomeSummaryRecorder() {

        MavenProjectHelper.propagateBuildDir(session);

        EnvironmentVariables environmentVariables = Injectors.getInjector().getInstance(EnvironmentVariables.class);

        System.out.println("historyDirectoryPath = " + historyDirectoryPath);
        System.out.println("SERENITY_HISTORY_DIRECTORY = " + ThucydidesSystemProperty.SERENITY_HISTORY_DIRECTORY.from(environmentVariables));

        String configuredHistoryDirectoryPath
                = ThucydidesSystemProperty.SERENITY_HISTORY_DIRECTORY.from(environmentVariables,
                                                                           Optional.fromNullable(historyDirectoryPath).or(DEFAULT_HISTORY_DIRECTORY));

        System.out.println("configuredHistoryDirectoryPath = " + configuredHistoryDirectoryPath);

        Path historyDirectory = Paths.get(configuredHistoryDirectoryPath);

        return new FileSystemTestOutcomeSummaryRecorder(historyDirectory, deletePreviousHistory);
    }

    private Path outputDirectory() {
        return (!StringUtils.isEmpty(outcomesDirectoryPath)) ?
                session.getCurrentProject().getBasedir().toPath().resolve(outcomesDirectoryPath).toAbsolutePath() :
                getConfiguration().getOutputDirectory().toPath();
    }

    private Configuration getConfiguration() {
        return Injectors.getInjector().getProvider(Configuration.class).get();
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Storing Serenity test result summaries");
        getTestOutcomeSummaryRecorder().recordOutcomeSummariesFrom(outputDirectory());
    }
}
