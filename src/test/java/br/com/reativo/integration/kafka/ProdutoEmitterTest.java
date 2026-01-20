package br.com.reativo.integration.kafka;

import br.com.reativo.modelo.Produto;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProdutoEmitterTest {

    @InjectMocks
    ProdutoEmitter emitterService;

    @Mock
    Emitter<String> emitter;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    // ===================== sendKafkaData =====================

    @Test
    void deveEnviarMensagemSimplesParaKafka() {

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        when(emitter.send(anyString()))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));

        Uni<Void> result = emitterService.sendKafkaData("ProdutoX");

        result.await().indefinitely();

        verify(emitter, times(1)).send(captor.capture());

        assertEquals("ProdutoX", captor.getValue());
    }

    // ===================== sendMsgToKafkaAboutProduct =====================

    @Test
    void deveEnviarProdutoComoJson_paraKafka() {

        Produto produto = new Produto(10L, "Teclado");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        emitterService.sendMsgToKafkaAboutProduct(produto);

        verify(emitter, times(1)).send(captor.capture());

        String jsonEnviado = captor.getValue();

        assertTrue(jsonEnviado.contains("\"id\":10"));
        assertTrue(jsonEnviado.contains("\"nome\":\"Teclado\""));
    }

    // ===================== sendRequestTocreateNewProductNotificationKafka =====================

    @Test
    void deveEnviarNotificacaoDeCriacao_quandoProdutoNaoNulo() {

        Produto produto = new Produto(5L, "Mesa");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        emitterService.sendRequestTocreateNewProductNotificationKafka(produto);

        verify(emitter, times(1)).send(captor.capture());

        String json = captor.getValue();

        assertTrue(json.contains("\"Mesa\""));
    }

    @Test
    void naoDeveEnviarNada_quandoProdutoNulo() {

        emitterService.sendRequestTocreateNewProductNotificationKafka(null);

        verifyNoInteractions(emitter);
    }

    // ===================== sendCreateProdutoNotificationKafka =====================

    @Test
    void deveEnviarNotificacaoAposCriarProduto() {

        Produto produto = new Produto(99L, "Notebook");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        emitterService.sendCreateProdutoNotificationKafka(produto);

        verify(emitter, times(1)).send(captor.capture());

        String json = captor.getValue();

        assertTrue(json.contains("Notebook"));
        assertTrue(json.contains("99"));
    }
}
