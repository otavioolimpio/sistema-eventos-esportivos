package microservicos.serviconotificacoes.api;

import microservicos.serviconotificacoes.service.RequisicaoInvalidaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

/** Converte erros de entrada em respostas HTTP 400 com mensagens claras. */
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> camposInvalidos(MethodArgumentNotValidException e) {
        List<String> detalhes = e.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getDefaultMessage())
                .toList();
        return Map.of("erro", "Requisição inválida", "detalhes", detalhes);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> jsonInvalido(HttpMessageNotReadableException e) {
        return Map.of("erro", "Requisição inválida",
                "detalhes", List.of("JSON malformado ou canal inexistente (use EMAIL ou DISCORD)."));
    }

    @ExceptionHandler(RequisicaoInvalidaException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> regraInvalida(RequisicaoInvalidaException e) {
        return Map.of("erro", "Requisição inválida", "detalhes", List.of(e.getMessage()));
    }
}
