package dev.mauricioandrade.shisho.infrastructure.report;

import dev.mauricioandrade.shisho.domain.model.Analysis;
import dev.mauricioandrade.shisho.domain.model.ArchitectureMatch;
import dev.mauricioandrade.shisho.domain.model.KataGuide;
import dev.mauricioandrade.shisho.domain.model.KataGuide.Snippet;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.SourceFile;
import dev.mauricioandrade.shisho.domain.model.Verdict;
import dev.mauricioandrade.shisho.domain.port.out.ReportWriter;
import dev.mauricioandrade.shisho.infrastructure.i18n.Messages;
import dev.mauricioandrade.shisho.infrastructure.report.ReportTexts.VerdictText;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * One self-contained HTML page: inline CSS, no script, no external resource, light and dark
 * themes. No timestamp, so the same analysis always produces the same file.
 */
public final class HtmlReportWriter implements ReportWriter {

    /** {@code `code`} in the catalog's prose, as in Markdown. */
    private static final Pattern INLINE_CODE = Pattern.compile("`([^`]+)`");

    private final PrintStream out;
    private final ReportTexts texts;
    private final String language;
    private final String version;

    public HtmlReportWriter(PrintStream out, Messages messages, List<KataGuide> guides, String version) {
        this.out = out;
        this.texts = new ReportTexts(messages, guides);
        Locale locale = messages.bundle().getLocale();
        this.language = locale.toLanguageTag().equals("und") ? "en" : locale.toLanguageTag();
        this.version = version;
    }

    @Override
    public void write(Analysis analysis) {
        StringBuilder html = new StringBuilder();
        html.append("""
                <!doctype html>
                <html lang="%s">
                <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>%s</title>
                <style>%s</style>
                </head>
                <body>
                <main>
                """.formatted(language, escape(texts.text("html.title", analysis.projectName())), STYLE));
        header(analysis, html);
        architecture(analysis, html);
        lessons(analysis, html);
        guides(analysis, html);
        html.append("<footer>").append(escape(texts.text("html.generated", version))).append("</footer>\n");
        html.append("</main>\n</body>\n</html>\n");
        out.print(html);
        out.flush();
    }

    private void header(Analysis analysis, StringBuilder html) {
        html.append("<header>\n")
                .append("<p class=\"brand\"><span lang=\"ja\">師匠</span> Shishō</p>\n")
                .append("<h1>").append(escape(analysis.projectName())).append("</h1>\n")
                .append("<p class=\"subtitle\">").append(escape(texts.header(analysis))).append("</p>\n")
                .append("<div class=\"rank rank-").append(analysis.rank().name().toLowerCase(Locale.ROOT)).append("\">")
                .append("<span class=\"rank-label\">").append(escape(texts.text("html.rank"))).append("</span>")
                .append("<span class=\"rank-value\">").append(escape(texts.rank(analysis))).append("</span>")
                .append("<span class=\"rank-score\">").append(escape(texts.text("html.score", analysis.score()))).append("</span>")
                .append("</div>\n")
                .append("<p class=\"counts\">").append(escape(texts.counts(analysis))).append("</p>\n")
                .append("</header>\n");
    }

    private void architecture(Analysis analysis, StringBuilder html) {
        html.append("<section>\n<h2>").append(escape(texts.text("html.architecture"))).append("</h2>\n");
        if (analysis.architecture().isEmpty()) {
            html.append("<p>").append(escape(texts.architecture(analysis))).append("</p>\n</section>\n");
            return;
        }
        ArchitectureMatch match = analysis.architecture().orElseThrow();
        html.append("<p class=\"architecture\"><strong>").append(escape(match.architecture().name())).append("</strong> ")
                .append("<span class=\"muted\">").append(escape(texts.text("html.confidence", match.confidence()))).append("</span></p>\n")
                .append("<div class=\"meter\" role=\"presentation\"><span style=\"width: ").append(match.confidence()).append("%\"></span></div>\n")
                .append("<ul class=\"chips\" aria-label=\"").append(escape(texts.text("html.signals"))).append("\">");
        match.signals().forEach(signal -> html.append("<li><code>").append(escape(signal)).append("</code></li>"));
        html.append("</ul>\n");

        VerdictText verdict = texts.verdict(match);
        html.append("<div class=\"verdict verdict-").append(kind(match.verdict())).append("\">\n")
                .append("<p>").append(escape(verdict.headline())).append("</p>\n");
        if (!verdict.evidence().isEmpty()) {
            html.append("<ul>\n");
            verdict.evidence().forEach(evidence -> html.append("<li>").append(escape(evidence)).append("</li>\n"));
            html.append("</ul>\n");
        }
        verdict.next().ifPresent(next -> html.append("<p class=\"next\">").append(escape(next)).append("</p>\n"));
        html.append("</div>\n</section>\n");
    }

    private void lessons(Analysis analysis, StringBuilder html) {
        html.append("<section>\n<h2>").append(escape(texts.text("html.lessons"))).append("</h2>\n");
        if (analysis.lessons().isEmpty()) {
            String empty = analysis.baseline().isPresent() ? "html.no-new-lessons" : "html.no-lessons";
            html.append("<p class=\"empty\">").append(escape(texts.text(empty))).append("</p>\n");
        }
        byFile(analysis.lessons(), html);
        analysis.baseline().filter(baseline -> !baseline.known().isEmpty()).ifPresent(baseline -> {
            html.append("<details class=\"known\">\n<summary>")
                    .append(escape(texts.text("report.baseline.known", baseline.known().size()))).append("</summary>\n");
            byFile(baseline.known(), html);
            html.append("</details>\n");
        });
        analysis.baseline().filter(baseline -> baseline.fixed() > 0).ifPresent(baseline -> html.append("<p class=\"muted\">")
                .append(escape(texts.text("report.baseline.fixed", baseline.fixed()))).append("</p>\n"));
        if (!analysis.unparseableFiles().isEmpty()) {
            html.append("<h3>").append(escape(texts.text("html.unparseable"))).append("</h3>\n<ul class=\"unparseable\">\n");
            for (SourceFile.Unparseable file : analysis.unparseableFiles()) {
                html.append("<li>").append(escape(texts.unparseable(file))).append("</li>\n");
            }
            html.append("</ul>\n");
        }
        html.append("</section>\n");
    }

    private void byFile(List<Lesson> lessons, StringBuilder html) {
        Map<Path, List<Lesson>> byFile = lessons.stream()
                .collect(Collectors.groupingBy(Lesson::file, LinkedHashMap::new, Collectors.toList()));
        byFile.forEach((file, inFile) -> {
            html.append("<article>\n<h3><code>").append(escape(slashes(file))).append("</code></h3>\n<ol>\n");
            inFile.forEach(lesson -> lesson(lesson, html));
            html.append("</ol>\n</article>\n");
        });
    }

    private void lesson(Lesson lesson, StringBuilder html) {
        String severity = lesson.severity().name().toLowerCase(Locale.ROOT);
        String where = lesson.line() > 0 ? texts.text("html.line", lesson.line()) : texts.text("html.whole-file");
        html.append("<li class=\"lesson\">\n")
                .append("<p class=\"lesson-head\"><span class=\"badge badge-").append(severity).append("\">")
                .append(escape(texts.severity(lesson.severity()))).append("</span> ")
                .append("<strong>").append(escape(texts.text("kata." + lesson.kataId() + ".title"))).append("</strong> ")
                .append("<code class=\"muted\">").append(escape(lesson.kataId())).append("</code> ")
                .append("<span class=\"muted\">").append(escape(where)).append("</span></p>\n")
                .append("<p>").append(escape(texts.explanation(lesson))).append("</p>\n");
        texts.guide(lesson.kataId()).ifPresent(_ -> html.append("<p><a href=\"#kata-").append(escape(lesson.kataId()))
                .append("\">").append(escape(texts.text("html.how-to-fix"))).append("</a></p>\n"));
        html.append("</li>\n");
    }

    private void guides(Analysis analysis, StringBuilder html) {
        List<KataGuide> guides = texts.guides(analysis.allLessons());
        if (guides.isEmpty()) {
            return;
        }
        html.append("<section>\n<h2>").append(escape(texts.text("html.how-to-fix"))).append("</h2>\n");
        for (KataGuide guide : guides) {
            html.append("<article id=\"kata-").append(escape(guide.kataId())).append("\" class=\"guide\">\n")
                    .append("<h3>").append(escape(texts.title(guide.kataId()))).append(" <code class=\"muted\">")
                    .append(escape(guide.kataId())).append("</code></h3>\n");
            prose(texts.text("html.why"), guide.why(), html);
            prose(texts.text("html.acceptable"), guide.acceptableWhen(), html);
            html.append("<div class=\"before-after\">\n");
            snippet(texts.text("html.before"), "before", guide.before(), html);
            snippet(texts.text("html.after"), "after", guide.after(), html);
            html.append("</div>\n<h4>").append(escape(texts.text("html.references"))).append("</h4>\n<ul class=\"references\">\n");
            guide.references().forEach(url -> html.append("<li><a href=\"").append(escape(url)).append("\">")
                    .append(escape(url)).append("</a></li>\n"));
            html.append("</ul>\n</article>\n");
        }
        html.append("</section>\n");
    }

    private static void prose(String label, List<String> paragraphs, StringBuilder html) {
        html.append("<h4>").append(escape(label)).append("</h4>\n");
        paragraphs.forEach(paragraph -> html.append("<p>").append(inline(paragraph)).append("</p>\n"));
    }

    private static void snippet(String label, String kind, Snippet snippet, StringBuilder html) {
        html.append("<div class=\"snippet snippet-").append(kind).append("\">\n<h4>").append(escape(label)).append("</h4>\n");
        if (!snippet.explanation().isEmpty()) {
            html.append("<p>").append(inline(snippet.explanation())).append("</p>\n");
        }
        html.append("<pre><code class=\"language-").append(escape(snippet.language())).append("\">")
                .append(escape(snippet.code())).append("</code></pre>\n</div>\n");
    }

    /** Escaped, with the Markdown {@code `code`} spans as {@code <code>}. */
    private static String inline(String text) {
        return INLINE_CODE.matcher(escape(text)).replaceAll("<code>$1</code>");
    }

    private static String kind(Verdict verdict) {
        return switch (verdict) {
            case Verdict.Overengineering _ -> "over";
            case Verdict.Underengineering _ -> "under";
            case Verdict.Adequate _ -> "adequate";
        };
    }

    private static String slashes(Path file) {
        return file.toString().replace('\\', '/');
    }

    static String escape(String text) {
        StringBuilder escaped = new StringBuilder(text.length());
        for (char character : text.toCharArray()) {
            switch (character) {
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                case '"' -> escaped.append("&quot;");
                case '\'' -> escaped.append("&#39;");
                default -> escaped.append(character);
            }
        }
        return escaped.toString();
    }

    private static final String STYLE = """

            :root { color-scheme: light dark; --paper: #f7f4ee; --card: #ffffff; --ink: #1f1d1a; --muted: #6b665e;
              --line: #e2ddd3; --accent: #b3261e; --grave: #b3261e; --media: #a86400; --leve: #3d6a8a; --info: #6b665e;
              --over: #fbeae8; --under: #fdf2df; --adequate: #e8f3ea; }
            @media (prefers-color-scheme: dark) {
              :root { --paper: #161513; --card: #201e1b; --ink: #ece7de; --muted: #a39d92; --line: #36332e;
                --accent: #ef6a5f; --grave: #ef6a5f; --media: #e0a646; --leve: #7fb0d4; --info: #a39d92;
                --over: #3a2220; --under: #3a2f1c; --adequate: #1f3324; }
            }
            * { box-sizing: border-box; }
            body { margin: 0; background: var(--paper); color: var(--ink);
              font: 16px/1.55 system-ui, -apple-system, "Segoe UI", Roboto, sans-serif; }
            main { max-width: 960px; margin: 0 auto; padding: 32px 16px 48px; }
            header { border-bottom: 3px solid var(--accent); padding-bottom: 24px; margin-bottom: 32px; }
            .brand { margin: 0; color: var(--accent); font-weight: 700; letter-spacing: .04em; }
            h1 { margin: 4px 0; font-size: 2rem; overflow-wrap: anywhere; }
            h2 { font-size: 1.25rem; margin: 32px 0 12px; }
            h3 { font-size: 1rem; margin: 0 0 8px; overflow-wrap: anywhere; }
            .subtitle, .muted, footer { color: var(--muted); }
            .rank { display: inline-flex; flex-wrap: wrap; align-items: baseline; gap: 12px; margin-top: 12px;
              padding: 12px 16px; background: var(--card); border: 1px solid var(--line); border-left: 6px solid var(--ink); }
            .rank-dan_1 { border-left-color: var(--ink); }
            .rank-label { text-transform: uppercase; font-size: .75rem; letter-spacing: .08em; color: var(--muted); }
            .rank-value { font-size: 1.5rem; font-weight: 700; }
            .counts { margin: 12px 0 0; }
            .meter { height: 6px; background: var(--line); margin: 8px 0 12px; }
            .meter span { display: block; height: 100%; background: var(--accent); }
            .chips { list-style: none; padding: 0; margin: 0 0 16px; display: flex; flex-wrap: wrap; gap: 6px; }
            .chips li { background: var(--card); border: 1px solid var(--line); padding: 2px 8px; }
            .verdict { padding: 16px; border: 1px solid var(--line); }
            .verdict p { margin: 0 0 8px; }
            .verdict ul { margin: 0 0 8px; padding-left: 20px; }
            .verdict-over { background: var(--over); }
            .verdict-under { background: var(--under); }
            .verdict-adequate { background: var(--adequate); }
            .next { font-weight: 600; }
            article { background: var(--card); border: 1px solid var(--line); padding: 16px; margin-bottom: 12px; }
            ol { list-style: none; margin: 0; padding: 0; }
            .lesson + .lesson { border-top: 1px solid var(--line); margin-top: 12px; padding-top: 12px; }
            .lesson p { margin: 0 0 4px; }
            .badge { display: inline-block; min-width: 56px; text-align: center; font-size: .75rem; font-weight: 700;
              letter-spacing: .06em; color: #fff; padding: 2px 6px; }
            .badge-grave { background: var(--grave); }
            .badge-media { background: var(--media); }
            .badge-leve { background: var(--leve); }
            .badge-info { background: var(--info); }
            code { font: .875em ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; }
            .empty { font-size: 1.125rem; }
            .known { margin-top: 16px; }
            .known > summary { cursor: pointer; color: var(--muted); }
            .before-after { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 12px; }
            .snippet { min-width: 0; }
            .guide > h4 { margin: 16px 0 6px; font-size: .75rem; text-transform: uppercase; letter-spacing: .08em; color: var(--muted); }
            .guide > p { margin: 0 0 8px; }
            .references { margin: 0; padding-left: 20px; overflow-wrap: anywhere; }
            .snippet h4 { margin: 0 0 6px; font-size: .75rem; text-transform: uppercase; letter-spacing: .08em; }
            .snippet-before h4 { color: var(--grave); }
            .snippet-after h4 { color: var(--leve); }
            .snippet p { margin: 0 0 8px; }
            pre { margin: 0; padding: 12px; overflow-x: auto; background: var(--paper); border: 1px solid var(--line); }
            a { color: var(--accent); }
            footer { margin-top: 48px; font-size: .875rem; }
            """;
}
