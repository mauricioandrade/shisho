package dev.mauricioandrade.shisho.domain.model;

import java.util.List;

/** What a detector found, explained by the message key {@code detector.<id>}. */
public record Evidence(String detectorId, List<Object> arguments) {

    public Evidence {
        arguments = List.copyOf(arguments);
    }

    public String messageKey() {
        return "detector." + detectorId;
    }
}
