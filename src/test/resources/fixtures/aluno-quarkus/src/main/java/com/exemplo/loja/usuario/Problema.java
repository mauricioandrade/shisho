package com.exemplo.loja.usuario;

/** O formato de erro da RFC 9457 (application/problem+json). */
public record Problema(String title, int status, String detail) {
}
