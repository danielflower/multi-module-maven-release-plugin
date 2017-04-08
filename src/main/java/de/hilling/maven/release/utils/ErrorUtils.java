package de.hilling.maven.release.utils;

import static java.util.Arrays.asList;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.api.errors.GitAPIException;

public final class ErrorUtils {

    private ErrorUtils() {
    }

    public static void printBigErrorMessageAndThrow(Log log, String terseMessage, List<String> linesToLog) throws
                                                                                                           MojoExecutionException {
        log.error("");
        log.error("");
        log.error("");
        log.error("************************************");
        log.error("Could not execute the release plugin");
        log.error("************************************");
        log.error("");
        log.error("");
        for (String line : linesToLog) {
            log.error(line);
        }
        log.error("");
        log.error("");
        throw new MojoExecutionException(terseMessage);
    }

    public static void printBigGitErrorExceptionAndThrow(Log log, GitAPIException gae) throws
                                                                                          MojoExecutionException {
        StringWriter sw = new StringWriter();
        gae.printStackTrace(new PrintWriter(sw));
        String exceptionAsString = sw.toString();

        printBigErrorMessageAndThrow(log, "Could not release due to a Git error", asList(
            "There was an error while accessing the Git repository. The error returned from git was:",
            gae.getMessage(), "Stack trace:", exceptionAsString));
    }
}
