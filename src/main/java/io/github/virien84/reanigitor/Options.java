package io.github.virien84.reanigitor;

import io.github.virien84.reanigitor.github.GitHubClient;

/** Parsed command-line options. */
public record Options(String user, int months, int max) {

    static final int DEFAULT_MONTHS = 12;
    static final int DEFAULT_MAX = 12;

    public static Options parse(String... args) {
        String user = null;
        int months = DEFAULT_MONTHS;
        int max = DEFAULT_MAX;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--months" -> months = positiveInt(arg, args, ++i);
                case "--max" -> max = positiveInt(arg, args, ++i);
                default -> {
                    if (arg.startsWith("-")) {
                        throw new UsageException("Unknown option: " + arg);
                    }
                    if (user != null) {
                        throw new UsageException("Only one GitHub user at a time, got " + user + " and " + arg);
                    }
                    if (!GitHubClient.isValidUsername(arg)) {
                        throw new UsageException("Not a valid GitHub username: " + arg);
                    }
                    user = arg;
                }
            }
        }
        if (user == null) {
            throw new UsageException("Missing the GitHub username");
        }
        return new Options(user, months, max);
    }

    private static int positiveInt(String option, String[] args, int index) {
        if (index >= args.length) {
            throw new UsageException(option + " needs a number");
        }
        try {
            int value = Integer.parseInt(args[index]);
            if (value > 0) {
                return value;
            }
        } catch (NumberFormatException e) {
            // Reported below.
        }
        throw new UsageException(option + " must be a positive whole number, got " + args[index]);
    }
}
