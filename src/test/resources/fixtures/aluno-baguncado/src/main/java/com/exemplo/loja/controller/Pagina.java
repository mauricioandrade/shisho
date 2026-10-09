package com.exemplo.loja.controller;

import java.util.List;

public record Pagina<T>(List<T> itens, long total) {
}
