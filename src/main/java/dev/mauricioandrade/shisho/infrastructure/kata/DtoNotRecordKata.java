package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ClassKata;
import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Kind;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Severity;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A class that looks like a DTO (by package or by name suffix) but is not a record. A builder is
 * mutable by design, so it is never one; and in a DTO package, a class without instance state (a
 * mapper, a constants holder) holds no data to transfer.
 */
final class DtoNotRecordKata implements ClassKata {

    private static final Set<String> DTO_PACKAGES = Set.of("request", "response", "dto", "dtos", "requisicao", "resposta");
    private static final String BUILDER = "Builder";
    private static final List<String> DTO_SUFFIXES = List.of("Request", "Response", "Dto", "DTO", "Requisicao", "Resposta");

    @Override
    public String id() {
        return "dto-not-record";
    }

    @Override
    public Severity severity() {
        return Severity.LEVE;
    }

    @Override
    public JavaVersion minJavaVersion() {
        return JavaVersion.BASELINE;
    }

    @Override
    public List<Lesson> evaluate(ClassUnit unit) {
        if (unit.kind() != Kind.CLASS || !looksLikeDto(unit)) {
            return List.of();
        }
        return List.of(lesson(unit, unit.line(), unit.name()));
    }

    private static boolean looksLikeDto(ClassUnit unit) {
        if (unit.name().endsWith(BUILDER)) {
            return false;
        }
        boolean inDtoPackage = Arrays.stream(unit.packageName().split("\\."))
                .anyMatch(segment -> DTO_PACKAGES.contains(segment.toLowerCase(Locale.ROOT)));
        return inDtoPackage && !unit.stateNames().isEmpty() || DTO_SUFFIXES.stream().anyMatch(unit.name()::endsWith);
    }
}
