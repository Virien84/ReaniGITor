package io.github.virien84.reanigitor;

/** The command line is wrong; the message says how. */
public class UsageException extends RuntimeException {

    public UsageException(String message) {
        super(message);
    }
}
