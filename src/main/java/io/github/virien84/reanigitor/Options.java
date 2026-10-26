package io.github.virien84.reanigitor;

import io.github.virien84.reanigitor.github.GitHubClient;
import io.github.virien84.reanigitor.ollama.OllamaClient;
import io.github.virien84.reanigitor.wake.Lang;
import java.nio.file.Path;
import java.util.regex.Pattern;

/** Parsed command-line options. */
public record Options(String user, int months, int max, Lang lang, String model, Path out) {

    static final int DEFAULT_MONTHS = 12;
    static final int DEFAULT_MAX = 12;
    static final Path DEFAULT_OUT = Path.of("output");

    // Ollama model names: "name:tag", "namespace/name:tag".
    private static final Pattern MODEL = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._/-]*(:[A-Za-z0-9._-]+)?");

    public static Options parse(String... args) {
        String user = null;
        int months = DEFAULT_MONTHS;
        int max = DEFAULT_MAX;
        Lang lang = Lang.EN;
        String model = OllamaClient.DEFAULT_MODEL;
        Path out = DEFAULT_OUT;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--months" -> months = positiveInt(arg, args, ++i);
                case "--max" -> max = positiveInt(arg, args, ++i);
                case "--lang" -> {
                    String code = value(arg, args, ++i);
                    lang = Lang.fromCode(code);
                    if (lang == null) {
                        throw new UsageException("--lang must be en or es, got " + code);
                    }
                }
                case "--model" -> {
                    model = value(arg, args, ++i);
                    if (!MODEL.matcher(model).matches()) {
                        throw new UsageException("Not a valid Ollama model name: " + model);
                    }
                }
                case "--out" -> out = Path.of(value(arg, args, ++i));
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
        return new Options(user, months, max, lang, model, out);
    }

    private static String value(String option, String[] args, int index) {
        if (index >= args.length) {
            throw new UsageException(option + " needs a value");
        }
        return args[index];
    }

    private static int positiveInt(String option, String[] args, int index) {
        String value = value(option, args, index);
        try {
            int number = Integer.parseInt(value);
            if (number > 0) {
                return number;
            }
        } catch (NumberFormatException e) {
            // Reported below.
        }
        throw new UsageException(option + " must be a positive whole number, got " + value);
    }
}
