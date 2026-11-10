package microservicos.serviconotificacoes.api;

import microservicos.serviconotificacoes.adapter.INotificador;
import microservicos.serviconotificacoes.singleton.NotificationClientManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração: sobem a aplicação com um banco SQLite temporário
 * e um servidor de e-mail simulado (mock do JavaMailSender).
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:target/teste-notificacoes-${random.uuid}.db",
        "notificacao.discord.webhook-url="
})
@AutoConfigureMockMvc
class NotificacaoControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private List<INotificador> adaptadores;

    @MockBean
    private JavaMailSender mailSender;

    @BeforeEach
    void registrarAdaptadoresReais() {
        // Garante que o Singleton use os adaptadores reais (os testes de unidade podem ter trocado).
        adaptadores.forEach(NotificationClientManager.getInstance()::registrar);
        reset(mailSender);
    }

    @Test
    void emailEnviadoComSucesso() throws Exception {
        mvc.perform(post("/notificacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"canal":"EMAIL","destinatario":"chefe@exemplo.com",
                                 "assunto":"Credenciais","mensagem":"Sua senha provisória é 1234."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ENVIADA"))
                .andExpect(jsonPath("$.canal").value("EMAIL"))
                .andExpect(jsonPath("$.id").isNotEmpty());

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void falhaNoServidorDeEmailEhRegistradaSemErroHttp() throws Exception {
        doThrow(new MailSendException("SMTP fora do ar")).when(mailSender).send(any(SimpleMailMessage.class));

        mvc.perform(post("/notificacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"canal":"EMAIL","destinatario":"chefe@exemplo.com","mensagem":"Olá"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FALHA"))
                .andExpect(jsonPath("$.erro").value(org.hamcrest.Matchers.containsString("SMTP fora do ar")));
    }

    @Test
    void discordSemWebhookConfiguradoResultaEmFalha() throws Exception {
        mvc.perform(post("/notificacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"canal":"DISCORD","destinatario":"Equipe Alpha","mensagem":"Inscrição aprovada"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FALHA"))
                .andExpect(jsonPath("$.erro").value(org.hamcrest.Matchers.containsString("Webhook do Discord")));
    }

    @Test
    void mensagemObrigatoria() throws Exception {
        mvc.perform(post("/notificacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"canal":"EMAIL","destinatario":"chefe@exemplo.com"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void canalInexistenteRetorna400() throws Exception {
        mvc.perform(post("/notificacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"canal":"SMS","destinatario":"81999999999","mensagem":"Olá"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void emailInvalidoRetorna400() throws Exception {
        mvc.perform(post("/notificacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"canal":"EMAIL","destinatario":"nao-e-email","mensagem":"Olá"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void notificacaoRegistradaPodeSerConsultada() throws Exception {
        String resposta = mvc.perform(post("/notificacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"canal":"EMAIL","destinatario":"jogador@exemplo.com","mensagem":"Bem-vindo"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(resposta);
        String id = json.get("id").asText();

        mvc.perform(get("/notificacoes/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destinatario").value("jogador@exemplo.com"));

        mvc.perform(get("/notificacoes/nao-existe"))
                .andExpect(status().isNotFound());
    }
}
