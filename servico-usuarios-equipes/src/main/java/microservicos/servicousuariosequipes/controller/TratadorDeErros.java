package microservicos.servicousuariosequipes.controller;

import microservicos.servicousuariosequipes.service.excecoes.ConflitoException;
import microservicos.servicousuariosequipes.service.excecoes.RecursoNaoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

/** Converte erros em respostas HTTP, no mesmo formato usado pelo servico-notificacoes. */
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> camposInvalidos(MethodArgumentNotValidException e) {
        List<String> detalhes = e.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getDefaultMessage())
                .toList();
        return Map.of("erro", "Requisicao invalida", "detalhes", detalhes);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> jsonInvalido(HttpMessageNotReadableException e) {
        return Map.of("erro", "Requisicao invalida",
                "detalhes", List.of("JSON malformado ou valor de enum inexistente."));
    }

    /** Regras de negocio: elenco invalido (Strategy), senha atual incorreta, perfil errado. */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> regraDeNegocio(IllegalArgumentException e) {
        return Map.of("erro", "Requisicao invalida", "detalhes", List.of(e.getMessage()));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> naoEncontrado(RecursoNaoEncontradoException e) {
        return Map.of("erro", "Nao encontrado", "detalhes", List.of(e.getMessage()));
    }

    @ExceptionHandler(ConflitoException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> conflito(ConflitoException e) {
        return Map.of("erro", "Conflito", "detalhes", List.of(e.getMessage()));
    }
}
