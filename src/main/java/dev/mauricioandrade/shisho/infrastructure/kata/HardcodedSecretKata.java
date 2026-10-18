package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.ProjectKata;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.model.Severity;
import dev.mauricioandrade.shisho.infrastructure.kata.ApplicationSettings.Found;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A password, secret, token or key written as a literal value in a setting production reads. The
 * key's last word decides ({@code login-password}, {@code jwt.secret}, {@code appPrivateKey}), so
 * {@code token-validity-in-seconds} is not a secret. A placeholder ({@code ${DB_PASSWORD}}) or an
 * empty value is not a literal. The lesson never repeats the value.
 */
final class HardcodedSecretKata implements ProjectKata {

    private static final Set<String> SECRET_WORDS = Set.of("password", "passwd", "pwd", "passphrase", "secret", "token", "apikey");
    private static final Set<String> SECRET_PAIRS = Set.of("api key", "private key", "secret key", "access key");
    private static final Set<String> NOT_SECRETS = Set.of("true", "false");
    private static final Pattern WORD_BREAK = Pattern.compile("[-_\\s]+|(?<=[a-z0-9])(?=[A-Z])");
    private static final Pattern INDEX = Pattern.compile("\\[\\d+]$");

    @Override
    public String id() {
        return "hardcoded-secret";
    }

    @Override
    public Severity severity() {
        return Severity.MEDIA;
    }

    @Override
    public JavaVersion minJavaVersion() {
        return JavaVersion.MINIMUM;
    }

    @Override
    public List<Lesson> evaluate(ProjectModel project) {
        return ApplicationSettings.outsideDevelopment(project)
                .filter(found -> namesASecret(found.key()) && isLiteral(found))
                .map(found -> lesson(found.file().file(), found.setting().line(), found.key()))
                .toList();
    }

    private static boolean namesASecret(String key) {
        String last = INDEX.matcher(key.substring(key.lastIndexOf('.') + 1)).replaceAll("").replace("\"", "");
        List<String> words = Arrays.stream(WORD_BREAK.split(last))
                .filter(word -> !word.isEmpty())
                .map(word -> word.toLowerCase(Locale.ROOT))
                .toList();
        if (words.isEmpty()) {
            return false;
        }
        int size = words.size();
        return SECRET_WORDS.contains(words.get(size - 1))
                || size > 1 && SECRET_PAIRS.contains(words.get(size - 2) + " " + words.get(size - 1));
    }

    private static boolean isLiteral(Found found) {
        String value = found.setting().value().trim();
        return !value.isEmpty() && !value.contains("${") && !NOT_SECRETS.contains(found.value());
    }
}
