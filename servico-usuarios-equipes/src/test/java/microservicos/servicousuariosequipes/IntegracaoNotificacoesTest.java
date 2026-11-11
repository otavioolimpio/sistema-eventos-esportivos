package microservicos.servicousuariosequipes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de ponta a ponta do lado do servico-usuarios-equipes.
 *
 * Sobe a aplicacao inteira (controller -> service -> Feign) e coloca um servidor
 * HTTP falso no lugar do servico-notificacoes. Assim o teste confere a URL, o
 * caminho (/notificacoes) e o JSON que realmente saem pela rede, sem precisar
 * do outro servico rodando.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegracaoNotificacoesTest {

    record RequisicaoRecebida(String metodo, String caminho, String corpo) {}

    private static final BlockingQueue<RequisicaoRecebida> RECEBIDAS = new LinkedBlockingQueue<>();
    private static volatile String statusDevolvido = "ENVIADA";

    private static final HttpServer NOTIFICACOES_FALSO;
    private static final Path BANCO_TEMPORARIO;

    static {
        try {
            BANCO_TEMPORARIO = Files.createTempFile("usuarios-equipes-teste", ".db");
            BANCO_TEMPORARIO.toFile().deleteOnExit();

            NOTIFICACOES_FALSO = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            NOTIFICACOES_FALSO.createContext("/", troca -> {
                String corpo = new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                RECEBIDAS.add(new RequisicaoRecebida(
                        troca.getRequestMethod(), troca.getRequestURI().getPath(), corpo));

                // Mesmo formato do servico-notificacoes: 201 sempre, com status ENVIADA ou FALHA
                String erro = "FALHA".equals(statusDevolvido) ? "\"SMTP fora do ar\"" : "null";
                byte[] resposta = ("{\"id\":\"" + UUID.randomUUID() + "\",\"status\":\"" + statusDevolvido
                        + "\",\"erro\":" + erro + "}").getBytes(StandardCharsets.UTF_8);
                troca.getResponseHeaders().add("Content-Type", "application/json");
                troca.sendResponseHeaders(201, resposta.length);
                try (OutputStream saida = troca.getResponseBody()) {
                    saida.write(resposta);
                }
            });
            NOTIFICACOES_FALSO.start();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registro) {
        registro.add("app.services.notificacoes.url",
                () -> "http://localhost:" + NOTIFICACOES_FALSO.getAddress().getPort());
        registro.add("spring.datasource.url", () -> "jdbc:sqlite:" + BANCO_TEMPORARIO.toAbsolutePath());
    }

    @AfterAll
    static void pararServidorFalso() {
        NOTIFICACOES_FALSO.stop(0);
    }

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private ObjectMapper json;

    @BeforeEach
    void limpar() {
        RECEBIDAS.clear();
        statusDevolvido = "ENVIADA";
    }

    @Test
    void criarUsuario_disparaNotificacaoPorEmailNoServicoDeNotificacoes() throws Exception {
        String email = emailUnico();

        ResponseEntity<JsonNode> resposta = http.postForEntity("/api/usuarios", novoUsuario(email), JsonNode.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getBody().path("credenciaisEnviadas").asBoolean()).isTrue();
        assertThat(resposta.getBody().path("usuario").path("email").asText()).isEqualTo(email);
        assertThat(resposta.getBody().path("usuario").has("senhaHash")).isFalse();

        RequisicaoRecebida chamada = RECEBIDAS.poll(5, TimeUnit.SECONDS);
        assertThat(chamada).as("o servico de notificacoes deveria ter sido chamado").isNotNull();
        assertThat(chamada.metodo()).isEqualTo("POST");
        assertThat(chamada.caminho()).isEqualTo("/notificacoes");

        JsonNode corpo = json.readTree(chamada.corpo());
        assertThat(corpo.path("canal").asText()).isEqualTo("EMAIL");
        assertThat(corpo.path("destinatario").asText()).isEqualTo(email);
        assertThat(corpo.path("assunto").asText()).isNotBlank();
        assertThat(corpo.path("mensagem").asText()).contains("Senha provisoria:");
    }

    @Test
    void senhaRecebidaPorEmail_permiteAlterarSenha() throws Exception {
        ResponseEntity<JsonNode> criado = http.postForEntity("/api/usuarios", novoUsuario(emailUnico()), JsonNode.class);
        long id = criado.getBody().path("usuario").path("id").asLong();

        RequisicaoRecebida chamada = RECEBIDAS.poll(5, TimeUnit.SECONDS);
        assertThat(chamada).isNotNull();
        String senhaProvisoria = extrairSenha(json.readTree(chamada.corpo()).path("mensagem").asText());

        ResponseEntity<Void> errada = http.exchange("/api/usuarios/" + id + "/senha", HttpMethod.PUT,
                new HttpEntity<>(Map.of("senhaAtual", "senha-errada", "novaSenha", "novaSenha123")), Void.class);
        assertThat(errada.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<Void> certa = http.exchange("/api/usuarios/" + id + "/senha", HttpMethod.PUT,
                new HttpEntity<>(Map.of("senhaAtual", senhaProvisoria, "novaSenha", "novaSenha123")), Void.class);
        assertThat(certa.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void criarUsuario_quandoEnvioFalha_usuarioContinuaCadastrado() {
        statusDevolvido = "FALHA";

        ResponseEntity<JsonNode> resposta = http.postForEntity("/api/usuarios", novoUsuario(emailUnico()), JsonNode.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getBody().path("credenciaisEnviadas").asBoolean()).isFalse();
        assertThat(resposta.getBody().path("aviso").asText()).isNotBlank();

        long id = resposta.getBody().path("usuario").path("id").asLong();
        assertThat(http.getForEntity("/api/usuarios/" + id, JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void criarUsuario_comDadosInvalidos_naoNotifica() {
        ResponseEntity<JsonNode> resposta = http.postForEntity("/api/usuarios",
                Map.of("nome", "", "email", "nao-e-email"), JsonNode.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(RECEBIDAS).isEmpty();
    }

    private static Map<String, Object> novoUsuario(String email) {
        return Map.of(
                "nome", "Jogador Teste",
                "email", email,
                "cpf", UUID.randomUUID().toString().substring(0, 14),
                "tipoPerfil", "JOGADOR");
    }

    private static String emailUnico() {
        return "teste-" + UUID.randomUUID().toString().substring(0, 8) + "@exemplo.com";
    }

    private static String extrairSenha(String mensagem) {
        Matcher m = Pattern.compile("Senha provisoria: (\\S+)").matcher(mensagem);
        assertThat(m.find()).isTrue();
        return m.group(1);
    }
}
