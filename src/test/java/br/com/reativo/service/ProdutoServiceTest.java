package br.com.reativo.service;

import br.com.reativo.exception.ProdutoException;
import br.com.reativo.integration.kafka.ProdutoEmitter;
import br.com.reativo.modelo.Produto;
import br.com.reativo.repository.ProdutoRepository;
import br.com.reativo.validacao.funcional.ValidacaoKafka;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProdutoServiceTest {

    @InjectMocks
    ProdutoService service;

    @Mock
    ProdutoRepository repository;

    @Mock
    ProdutoEmitter emitter;

    @Mock
    PgPool pgPool;

    @Mock
    ValidacaoKafka validacaoKafka;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    // ===================== FIND ALL =====================

    @Test
    void deveRetornarListaDeProdutos_quandoFindAllSucesso() {

        Produto p1 = new Produto(1L, "Notebook");
        Produto p2 = new Produto(2L, "Mouse");

        when(repository.findAll(pgPool))
                .thenReturn(Multi.createFrom().items(p1, p2));

        Multi<Produto> resultado = service.findAll();

        var lista = resultado.collect().asList().await().indefinitely();

        assertEquals(2, lista.size());
        assertEquals("Notebook", lista.get(0).nome());
        assertEquals("Mouse", lista.get(1).nome());

        verify(repository, times(1)).findAll(pgPool);
    }

    @Test
    void deveLogarErro_quandoFindAllFalhar() {

        when(repository.findAll(pgPool))
                .thenReturn(Multi.createFrom().failure(new ProdutoException("erro db")));

        Multi<Produto> resultado = service.findAll();

        assertThrows(ProdutoException.class, () ->
                resultado.collect().asList().await().indefinitely()
        );
    }

    // ===================== FIND BY ID =====================

    @Test
    void deveRetornar200_quandoProdutoEncontradoPorId() {

        Produto produto = new Produto(10L, "Teclado");

        when(repository.findById(pgPool, 10L))
                .thenReturn(Uni.createFrom().item(produto));

        Response resp = service.findById(10L)
                .await().indefinitely();

        assertEquals(200, resp.getStatus());
        assertEquals(produto, resp.getEntity());
    }

    @Test
    void deveRetornar404_quandoProdutoNaoEncontradoPorId() {

        when(repository.findById(pgPool, 99L))
                .thenReturn(Uni.createFrom().nullItem());

        Response resp = service.findById(99L)
                .await().indefinitely();

        assertEquals(404, resp.getStatus());
    }

    @Test
    void deveRetornar500_quandoFindByIdFalhar() {

        when(repository.findById(pgPool, 5L))
                .thenReturn(Uni.createFrom().failure(new ProdutoException("erro")));

        Response resp = service.findById(5L)
                .await().indefinitely();

        assertEquals(500, resp.getStatus());
    }

    // ===================== KAFKA - NOME =====================

    @Test
    void deveRetornar400_quandoNomeInvalidoAoEnviarKafka() {

        when(validacaoKafka.isTextFiledNullOrEmpty(anyString()))
                .thenReturn(false);

        Response resp = service
                .sendMessageKafkaInfoNameProduct(validacaoKafka, "")
                .await().indefinitely();

        assertEquals(400, resp.getStatus());
        verifyNoInteractions(emitter);
    }

    @Test
    void deveEnviarKafka_quandoNomeValido() {

        when(validacaoKafka.isTextFiledNullOrEmpty(anyString()))
                .thenReturn(true);

        Response resp = service
                .sendMessageKafkaInfoNameProduct(validacaoKafka, "ProdutoX")
                .await().indefinitely();

        assertEquals(200, resp.getStatus());
        verify(emitter, times(1)).sendKafkaData("ProdutoX");
    }

    // ===================== CREATE PRODUTO =====================

    @Test
    void deveRetornar400_quandoProdutoNuloAoCriar() {

        Response resp = service
                .createProdutoByKafka(validacaoKafka, null)
                .await().indefinitely();

        assertEquals(400, resp.getStatus());
        verifyNoInteractions(repository);
        verifyNoInteractions(emitter);
    }

    @Test
    void deveCriarProdutoComSucesso() {

        Produto entrada = new Produto(0L, "Cadeira");
        Produto salvo = new Produto(50L, "Cadeira");

        when(validacaoKafka.isTextFiledNullOrEmpty(anyString()))
                .thenReturn(true);

        when(repository.add(pgPool, entrada))
                .thenReturn(Uni.createFrom().item(salvo));

        Response resp = service
                .createProdutoByKafka(validacaoKafka, entrada)
                .await().indefinitely();

        assertEquals(201, resp.getStatus());
        assertEquals(salvo, resp.getEntity());

        verify(emitter, times(1))
                .sendRequestTocreateNewProductNotificationKafka(entrada);

        verify(emitter, times(1))
                .sendCreateProdutoNotificationKafka(salvo);
    }

    @Test
    void deveRetornar500_quandoErroAoSalvarProduto() {

        Produto entrada = new Produto(0L, "Mesa");

        when(validacaoKafka.isTextFiledNullOrEmpty(anyString()))
                .thenReturn(true);

        when(repository.add(pgPool, entrada))
                .thenReturn(Uni.createFrom().failure(new ProdutoException("erro")));

        Response resp = service
                .createProdutoByKafka(validacaoKafka, entrada)
                .await().indefinitely();

        assertEquals(500, resp.getStatus());
    }

    // ===================== UPDATE =====================

    @Test
    void deveRetornarErro_quandoProdutoNaoExisteParaUpdate() {

        when(repository.findById(pgPool, 1L))
                .thenReturn(Uni.createFrom().nullItem());

        Response resp = service
                .update(1L, new Produto(1L, "Novo"))
                .await().indefinitely();

        assertEquals(500, resp.getStatus());
    }

    @Test
    void deveAtualizarProdutoComSucesso() {

        Produto existente = new Produto(1L, "Antigo");

        when(repository.findById(pgPool, 1L))
                .thenReturn(Uni.createFrom().item(existente));

        when(repository.updateById(pgPool, 1L, existente))
                .thenReturn(Uni.createFrom().item(true));

        Response resp = service
                .update(1L, existente)
                .await().indefinitely();

        assertEquals(200, resp.getStatus());
        verify(repository, times(1)).updateById(pgPool, 1L, existente);
    }

    // ===================== DELETE =====================

    @Test
    void deveRetornar404_quandoProdutoNaoExisteParaDelete() {

        when(repository.findById(pgPool, 2L))
                .thenReturn(Uni.createFrom().nullItem());

        Response resp = service
                .deleteById(2L)
                .await().indefinitely();

        assertEquals(404, resp.getStatus());
    }

    @Test
    void deveDeletarProdutoComSucesso() {

        Produto existente = new Produto(3L, "Lixeira");

        when(repository.findById(pgPool, 3L))
                .thenReturn(Uni.createFrom().item(existente));

        when(repository.deleteById(pgPool, 3L))
                .thenReturn(Uni.createFrom().item(true));

        Response resp = service
                .deleteById(3L)
                .await().indefinitely();

        assertEquals(200, resp.getStatus());
        verify(repository, times(1)).deleteById(pgPool, 3L);
    }
}
