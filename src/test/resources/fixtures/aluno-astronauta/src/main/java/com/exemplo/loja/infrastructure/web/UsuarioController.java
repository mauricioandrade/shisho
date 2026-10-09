package com.exemplo.loja.infrastructure.web;

import com.exemplo.loja.application.usecase.BuscarUsuarioUseCase;
import com.exemplo.loja.application.usecase.CriarUsuarioUseCase;
import com.exemplo.loja.application.usecase.ListarUsuariosUseCase;
import com.exemplo.loja.application.usecase.RemoverUsuarioUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
class UsuarioController {

    private final CriarUsuarioUseCase criar;
    private final BuscarUsuarioUseCase buscar;
    private final ListarUsuariosUseCase listar;
    private final RemoverUsuarioUseCase remover;

    UsuarioController(
            CriarUsuarioUseCase criar, BuscarUsuarioUseCase buscar, ListarUsuariosUseCase listar, RemoverUsuarioUseCase remover) {
        this.criar = criar;
        this.buscar = buscar;
        this.listar = listar;
        this.remover = remover;
    }

    @PostMapping
    UsuarioResponse criar(@Valid @RequestBody CriarUsuarioRequest request) {
        return UsuarioResponse.de(criar.executar(request.nome()));
    }

    @GetMapping("/{id}")
    ResponseEntity<UsuarioResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.of(buscar.executar(id).map(UsuarioResponse::de));
    }

    @GetMapping
    List<UsuarioResponse> listar() {
        return listar.executar().stream().map(UsuarioResponse::de).toList();
    }

    @DeleteMapping("/{id}")
    void remover(@PathVariable Long id) {
        remover.executar(id);
    }
}
