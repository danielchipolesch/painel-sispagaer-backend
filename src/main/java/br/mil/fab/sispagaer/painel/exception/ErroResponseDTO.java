package br.mil.fab.sispagaer.painel.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;

/**
 * DTO padrão de resposta de erro, retornado em todas as situações de falha.
 *
 * @param status    Código HTTP (ex: 400, 401, 404, 500)
 * @param mensagem  Mensagem descritiva do erro (segura para exibição ao cliente)
 * @param detalhe   Detalhe técnico opcional (omitido se nulo — não exibir stack trace)
 * @param timestamp Momento do erro em UTC
 */
@Schema(description = "Resposta padronizada de erro")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponseDTO(

    @Schema(description = "Código HTTP do erro", example = "400")
    int status,

    @Schema(description = "Mensagem do erro", example = "Parâmetro 'competencia' é obrigatório.")
    String mensagem,

    @Schema(description = "Detalhe técnico (opcional, omitido em produção)")
    String detalhe,

    @Schema(description = "Momento do erro em UTC")
    Instant timestamp

) {
    /** Construtor conveniente sem detalhe técnico. */
    public ErroResponseDTO(int status, String mensagem) {
        this(status, mensagem, null, Instant.now());
    }

    /** Construtor com detalhe técnico (usar apenas em perfis não-produção). */
    public ErroResponseDTO(int status, String mensagem, String detalhe) {
        this(status, mensagem, detalhe, Instant.now());
    }
}
