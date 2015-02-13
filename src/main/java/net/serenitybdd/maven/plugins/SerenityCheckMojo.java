package net.serenitybdd.maven.plugins;

import net.thucydides.core.reports.ResultChecker;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

/**
 * This plugin deletes existing history files for Serenity for this project.
 */
@Mojo(name = "check")
public class SerenityCheckMojo extends AbstractMojo {
    /**
     * Aggregate reports are generated here
     */
    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}/site/serenity", required=true)
    public File outputDirectory;

    protected ResultChecker getResultChecker() {
        return new ResultChecker(outputDirectory);
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Checking Serenity test results");
        getResultChecker().checkTestResults();
    }
}
