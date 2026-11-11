package microservicos.servicousuariosequipes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import microservicos.servicousuariosequipes.domain.enums.Modalidade;

import java.util.List;

/** Corpo do POST /api/equipes (UC03). Chefe e jogadores sao referenciados pelo id do usuario. */
public record EquipeRequest(
        @NotBlank(message = "nome e obrigatorio") String nome,
        @NotNull(message = "modalidade e obrigatoria (FPS, MOBA ou LUTA)") Modalidade modalidade,
        @NotNull(message = "chefeId e obrigatorio") Long chefeId,
        @NotNull(message = "jogadoresIds e obrigatorio") List<Long> jogadoresIds
) {}
