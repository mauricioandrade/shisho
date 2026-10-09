package dev.mauricioandrade.shisho.infrastructure.fix;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import dev.mauricioandrade.shisho.domain.model.Analysis;
import dev.mauricioandrade.shisho.domain.model.FileFix;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.port.out.SourceFixer;
import dev.mauricioandrade.shisho.infrastructure.fix.LineDiff.Edit;
import dev.mauricioandrade.shisho.infrastructure.parser.JavaParserSourceParser;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@link SourceFixer} on JavaParser's layout-preserving printer (docs/adr/0003-autofix.md): only
 * the corrected lines change. A file is left alone if it cannot be read, parsed or printed, or if
 * the corrected code no longer parses.
 */
public final class JavaParserSourceFixer implements SourceFixer {

    private static final System.Logger LOG = System.getLogger(JavaParserSourceFixer.class.getName());

    private final Map<String, Fixer> fixers;

    public JavaParserSourceFixer() {
        this(List.of(
                new NonFinalDependencyFixer(),
                new FieldInjectionFixer(),
                new LombokConstructorFixer(),
                new RequestBodyWithoutValidFixer(),
                new TransactionalReadOnlyFixer()));
    }

    JavaParserSourceFixer(List<Fixer> fixers) {
        this.fixers = fixers.stream().collect(Collectors.toMap(Fixer::kataId, Function.identity()));
    }

    @Override
    public List<FileFix> fix(Analysis analysis) {
        Map<Path, List<Lesson>> byFile = analysis.lessons().stream()
                .filter(lesson -> fixers.containsKey(lesson.kataId()) && lesson.afterUpgradeTo().isEmpty())
                .collect(Collectors.groupingBy(Lesson::file, LinkedHashMap::new, Collectors.toList()));
        List<FileFix> fixes = new ArrayList<>();
        byFile.forEach((file, lessons) -> fix(analysis, file, lessons).ifPresent(fixes::add));
        return fixes;
    }

    private Optional<FileFix> fix(Analysis analysis, Path file, List<Lesson> lessons) {
        JavaParser parser = new JavaParser(new ParserConfiguration()
                .setLanguageLevel(JavaParserSourceParser.languageLevel(analysis.javaVersion())));
        String original;
        try {
            original = Files.readString(analysis.projectRoot().resolve(file));
        } catch (IOException e) {
            LOG.log(Level.DEBUG, "Not fixing {0}: {1}", file, e);
            return Optional.empty();
        }
        Optional<CompilationUnit> parsed = parse(parser, original);
        if (parsed.isEmpty()) {
            return Optional.empty();
        }
        CompilationUnit unit = parsed.get();
        String fixed;
        List<Lesson> fixedLessons = new ArrayList<>();
        try {
            LexicalPreservingPrinter.setup(unit);
            List<Runnable> changes = new ArrayList<>();
            for (Lesson lesson : lessons) {
                fixers.get(lesson.kataId()).prepare(unit, lesson, analysis).ifPresent(change -> {
                    changes.add(change);
                    fixedLessons.add(lesson);
                });
            }
            changes.forEach(Runnable::run);
            removeUnusedImports(unit, lessons);
            fixed = tidy(original, Constructors.indentMarked(LexicalPreservingPrinter.print(unit)));
        } catch (RuntimeException e) {
            // the layout-preserving printer has known gaps; a file it cannot handle is simply not fixed
            LOG.log(Level.DEBUG, "Not fixing {0}: {1}", file, e);
            return Optional.empty();
        }
        if (fixedLessons.isEmpty() || fixed.equals(original) || parse(parser, fixed).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new FileFix(file, original, fixed, fixedLessons));
    }

    private void removeUnusedImports(CompilationUnit unit, List<Lesson> lessons) {
        Set<String> removed = lessons.stream()
                .flatMap(lesson -> fixers.get(lesson.kataId()).removedAnnotations().stream())
                .collect(Collectors.toSet());
        Set<String> stillUsed = unit.findAll(AnnotationExpr.class).stream()
                .map(annotation -> annotation.getName().getIdentifier())
                .collect(Collectors.toSet());
        unit.getImports().stream()
                .filter(declaration -> !declaration.isStatic() && !declaration.isAsterisk())
                .filter(declaration -> removed.contains(declaration.getName().getIdentifier()))
                .filter(declaration -> !stillUsed.contains(declaration.getName().getIdentifier()))
                .toList()
                .forEach(ImportDeclaration::remove);
    }

    private static Optional<CompilationUnit> parse(JavaParser parser, String source) {
        ParseResult<CompilationUnit> result = parser.parse(source);
        return result.isSuccessful() ? result.getResult() : Optional.empty();
    }

    /** Keeps the file's line endings and strips trailing spaces from the lines the correction added. */
    static String tidy(String original, String printed) {
        String separator = original.contains("\r\n") ? "\r\n" : "\n";
        StringBuilder tidy = new StringBuilder();
        for (Edit edit : LineDiff.edits(LineDiff.lines(original), LineDiff.lines(printed))) {
            switch (edit) {
                case Edit.Delete _ -> {
                }
                case Edit.Insert insert -> tidy.append(insert.line().stripTrailing()).append(separator);
                case Edit.Keep keep -> tidy.append(keep.line()).append(separator);
            }
        }
        boolean endsWithNewline = printed.endsWith("\n");
        return endsWithNewline ? tidy.toString() : tidy.substring(0, Math.max(0, tidy.length() - separator.length()));
    }
}
