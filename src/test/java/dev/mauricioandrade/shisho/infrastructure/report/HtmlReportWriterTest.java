package dev.mauricioandrade.shisho.infrastructure.report;

import dev.mauricioandrade.shisho.domain.model.Analysis;
import dev.mauricioandrade.shisho.domain.model.ArchitectureMatch;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.ClassRange;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Complexity;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Cost;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Family;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Signature;
import dev.mauricioandrade.shisho.domain.model.Evidence;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.KataGuide;
import dev.mauricioandrade.shisho.domain.model.KataGuide.Snippet;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Rank;
import dev.mauricioandrade.shisho.domain.model.Severity;
import dev.mauricioandrade.shisho.domain.model.SourceFile;
import dev.mauricioandrade.shisho.domain.model.Verdict;
import dev.mauricioandrade.shisho.infrastructure.i18n.Messages;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlReportWriterTest {

    private static final ArchitectureProfile CLEAN = new ArchitectureProfile("clean", "Clean Architecture", Family.DOMAIN_ISOLATION,
            new Signature(List.of("domain", "application"), List.of(), 2), List.of(), new Cost(new ClassRange(7, 12), Complexity.HIGH),
            List.of("vida_util_longa"), List.of(), List.of(), List.of(), Optional.of("package-by-feature"), List.of(), "");

    @Test
    void writesASelfContainedPageWithRankArchitectureVerdictAndLessons() {
        String html = render(analysis(List.of(
                lesson("field-injection", Severity.GRAVE, "src/main/java/app/UsuarioController.java", 14, "service"),
                lesson("missing-actuator", Severity.LEVE, "pom.xml", 0),
                lesson("system-out", Severity.MEDIA, "src/main/java/app/UsuarioController.java", 20, "System.out.println")), 46),
                new Messages(Messages.DEFAULT_LOCALE));

        assertThat(html)
                .startsWith("<!doctype html>\n<html lang=\"pt-BR\">")
                .contains("<title>loja — Shishō</title>")
                .contains("<span class=\"rank-value\">5º kyū</span><span class=\"rank-score\">nota 46 de 100</span>")
                .contains("<p class=\"counts\">3 lições: 1 grave, 1 média, 1 leve</p>")
                .contains("<strong>Clean Architecture</strong> <span class=\"muted\">67% da assinatura</span>")
                .contains("<div class=\"meter\" role=\"presentation\"><span style=\"width: 67%\"></span></div>")
                .contains("<li><code>domain</code></li><li><code>application</code></li>")
                .contains("<div class=\"verdict verdict-over\">")
                .contains("<li>4 use cases só para operações CRUD de Usuario")
                .contains("<p class=\"next\">Considere package-by-feature.</p>")
                .contains("<h3><code>src/main/java/app/UsuarioController.java</code></h3>")
                .contains("<span class=\"badge badge-grave\">GRAVE</span> <strong>Injeção de dependência em campo</strong> "
                        + "<code class=\"muted\">field-injection</code> <span class=\"muted\">linha 14</span>")
                .contains("<span class=\"muted\">arquivo inteiro</span>")
                .contains("<li>⚠ src/main/java/Quebrado.java:4 não pôde ser analisado: &lt;Parse error&gt;</li>")
                .contains("<footer>Gerado pelo Shishō 1.2.3</footer>")
                .doesNotContain("<script", "http://", "https://");
        assertThat(html.indexOf("UsuarioController.java</code></h3>"))
                .as("lessons are grouped by file, each file once")
                .isEqualTo(html.lastIndexOf("UsuarioController.java</code></h3>"));
    }

    @Test
    void celebratesAProjectWithoutLessonsAndSpeaksEnglishAsFallback() {
        String html = render(analysis(List.of(), 100), new Messages(Locale.ENGLISH));

        assertThat(html)
                .startsWith("<!doctype html>\n<html lang=\"en\">")
                .contains("<p class=\"empty\">No lessons. The master has nothing to correct.</p>")
                .contains("<span class=\"rank-value\">1st dan</span>");
    }

    @Test
    void linksEachLessonToItsExampleShownOnce() {
        KataGuide guide = new KataGuide("field-injection",
                List.of("O objeto nasce `completo`.", "E o teste não precisa de Spring."),
                List.of("Em testes."),
                new Snippet("java", "@Autowired\nprivate Service service;", ""),
                new Snippet("java", "private final Service service;", "Use o `construtor` & final."),
                List.of("https://docs.spring.io/a", "https://example.com/b"));
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        new HtmlReportWriter(new PrintStream(buffer, true, StandardCharsets.UTF_8), new Messages(Messages.DEFAULT_LOCALE),
                List.of(guide), "1.2.3").write(analysis(List.of(
                lesson("field-injection", Severity.GRAVE, "A.java", 3, "a"),
                lesson("field-injection", Severity.GRAVE, "B.java", 5, "b"),
                lesson("system-out", Severity.MEDIA, "B.java", 9, "System.out.println")), 76));
        String html = buffer.toString(StandardCharsets.UTF_8);

        assertThat(html.split("<a href=\"#kata-field-injection\">Como corrigir</a>", -1)).hasSize(3);
        assertThat(html).doesNotContain("#kata-system-out")
                .contains("<h2>Como corrigir</h2>\n<article id=\"kata-field-injection\" class=\"guide\">\n"
                        + "<h3>Injeção de dependência em campo <code class=\"muted\">field-injection</code></h3>\n"
                        + "<h4>Por quê</h4>\n<p>O objeto nasce <code>completo</code>.</p>\n<p>E o teste não precisa de Spring.</p>\n"
                        + "<h4>Quando é aceitável</h4>\n<p>Em testes.</p>\n<div class=\"before-after\">")
                .contains("</div>\n<h4>Referências</h4>\n<ul class=\"references\">\n"
                        + "<li><a href=\"https://docs.spring.io/a\">https://docs.spring.io/a</a></li>\n"
                        + "<li><a href=\"https://example.com/b\">https://example.com/b</a></li>\n</ul>\n</article>")
                .contains("<div class=\"snippet snippet-before\">\n<h4>Antes</h4>\n"
                        + "<pre><code class=\"language-java\">@Autowired\nprivate Service service;</code></pre>")
                .contains("<h4>Depois</h4>\n<p>Use o <code>construtor</code> &amp; final.</p>");
    }

    @Test
    void escapesEverythingThatComesFromTheProject() {
        assertThat(HtmlReportWriter.escape("<script>alert('x' & \"y\")</script>"))
                .isEqualTo("&lt;script&gt;alert(&#39;x&#39; &amp; &quot;y&quot;)&lt;/script&gt;");
    }

    private static Analysis analysis(List<Lesson> lessons, int score) {
        ArchitectureMatch match = new ArchitectureMatch(CLEAN, List.of("domain", "application"), 67, List.of(),
                new Verdict.Overengineering(List.of(new Evidence("um_use_case_por_metodo_crud", List.of("Usuario", 4))),
                        false, Optional.of("package-by-feature")));
        return new Analysis(Path.of("loja"), "loja", new JavaVersion(21), true, Optional.empty(), Optional.of(match), lessons,
                List.of(new SourceFile.Unparseable(Path.of("src/main/java/Quebrado.java"), 4, "<Parse error>")), score, Rank.forScore(score));
    }

    private static Lesson lesson(String kata, Severity severity, String file, int line, Object... arguments) {
        return new Lesson(kata, severity, Path.of(file), line, "kata." + kata + ".lesson", List.of(arguments));
    }

    private static String render(Analysis analysis, Messages messages) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        new HtmlReportWriter(new PrintStream(buffer, true, StandardCharsets.UTF_8), messages, List.of(), "1.2.3").write(analysis);
        return buffer.toString(StandardCharsets.UTF_8);
    }
}
