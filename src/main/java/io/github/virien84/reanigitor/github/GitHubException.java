package io.github.virien84.reanigitor.github;

/** A GitHub API call failed in a way the user should hear about. */
public class GitHubException extends RuntimeException {

    public GitHubException(String message) {
        super(message);
    }

    public GitHubException(String message, Throwable cause) {
        super(message, cause);
    }
}
