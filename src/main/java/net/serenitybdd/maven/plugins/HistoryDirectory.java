package net.serenitybdd.maven.plugins;

import com.google.common.base.Optional;
import net.thucydides.core.ThucydidesSystemProperty;
import net.thucydides.core.util.EnvironmentVariables;

public class HistoryDirectory {

    private final static String DEFAULT_HISTORY_DIRECTORY = "history";

    public static String configuredIn(EnvironmentVariables environmentVariables, String configuredHistoryPath) {
        return ThucydidesSystemProperty.SERENITY_HISTORY_DIRECTORY.from(environmentVariables,
                Optional.fromNullable(configuredHistoryPath).or(DEFAULT_HISTORY_DIRECTORY));

    }
}
