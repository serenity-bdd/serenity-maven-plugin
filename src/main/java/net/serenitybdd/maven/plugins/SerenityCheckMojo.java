package net.serenitybdd.maven.plugins;

import net.thucydides.core.guice.Injectors;
import net.thucydides.core.reports.ResultChecker;
import net.thucydides.core.webdriver.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

/**
 * This plugin checks for the presence of failing tests in the target directory
 */
@Mojo(name = "check")
public class SerenityCheckMojo extends AbstractMojo {
    /**
     * Aggregate reports are generated here
     */
    @Parameter(property = "serenity.outputDirectory", defaultValue = "")//, required=true)
    public String outputDirectoryPath;

    @Parameter(defaultValue = "${session}")
    private MavenSession session;

    protected ResultChecker getResultChecker() {

        MavenProjectHelper.propagateBuildDir(session);
        File outputDirectory;
        if(!StringUtils.isEmpty(outputDirectoryPath)){
            outputDirectory = session.getCurrentProject().getBasedir().toPath().resolve(outputDirectoryPath).toFile();
        }else{
            outputDirectory = session.getCurrentProject().getBasedir().
                    toPath().resolve(getConfiguration().getOutputDirectory().toPath()).toFile();
        }
        return new ResultChecker(outputDirectory);
    }

    private Configuration getConfiguration() {
        return Injectors.getInjector().getProvider(Configuration.class).get();
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Checking Serenity test results");
        getResultChecker().checkTestResults();
    }
}
