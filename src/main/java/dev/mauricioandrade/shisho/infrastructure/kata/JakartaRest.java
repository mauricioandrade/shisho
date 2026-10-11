package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Annotation;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Method;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Parameter;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Jakarta REST resources, the way Quarkus writes them: {@code @GET}, {@code @POST}... methods, whose
 * request body is the one parameter with no annotation saying where else it comes from.
 */
final class JakartaRest {

    private static final Set<String> HTTP_METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS");
    private static final Set<String> PATH = Set.of("Path");
    /** Parameters filled from somewhere other than the body, in Jakarta REST and in Quarkus REST. */
    private static final Set<String> NOT_BODY = Set.of(
            "PathParam", "QueryParam", "HeaderParam", "CookieParam", "MatrixParam", "FormParam", "BeanParam",
            "Context", "Suspended", "RestPath", "RestQuery", "RestHeader", "RestCookie", "RestMatrix", "RestForm");
    /** Types Quarkus REST injects with no annotation. */
    private static final Set<String> CONTEXT_TYPES = Set.of(
            "UriInfo", "HttpHeaders", "SecurityContext", "Request", "ResourceInfo", "SimpleResourceInfo", "Sse",
            "SseEventSink", "HttpServerRequest", "HttpServerResponse", "RoutingContext", "ServerRequestContext",
            "ContainerRequestContext");
    /** A template variable such as {@code id} in {@code /orders/{id}} or {@code /orders/{id: \d+}}. */
    private static final Pattern TEMPLATE = Pattern.compile("\\{\\s*(\\w+)");

    private JakartaRest() {
    }

    static boolean isResource(ClassUnit unit) {
        return Annotations.has(unit.annotations(), PATH) || unit.methods().stream().anyMatch(JakartaRest::isResourceMethod);
    }

    static boolean isResourceMethod(Method method) {
        return Annotations.has(method.annotations(), HTTP_METHODS);
    }

    /**
     * The body of a resource method. Quarkus REST also fills, with no annotation, a parameter named
     * after a path template variable and the context types, so neither is the body.
     */
    static Optional<Parameter> body(ClassUnit resource, Method method) {
        Set<String> templateVariables = Stream.of(resource.annotations(), method.annotations())
                .flatMap(annotations -> templateVariables(annotations).stream())
                .collect(Collectors.toSet());
        return method.parameters().stream()
                .filter(parameter -> !Annotations.has(parameter.annotations(), NOT_BODY))
                .filter(parameter -> !CONTEXT_TYPES.contains(parameter.type()))
                .filter(parameter -> !templateVariables.contains(parameter.name()))
                .findFirst();
    }

    private static List<String> templateVariables(List<Annotation> annotations) {
        return Annotations.find(annotations, PATH)
                .flatMap(path -> path.attribute("value"))
                .stream()
                .flatMap(value -> TEMPLATE.matcher(value).results().map(match -> match.group(1)))
                .toList();
    }
}
