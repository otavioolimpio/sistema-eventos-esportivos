package microservicos.serviconotificacoes.dominio;

/** Canais de envio suportados. Cada canal possui um adaptador próprio (RN02 do UC07). */
public enum Canal {
    EMAIL,
    DISCORD
}
