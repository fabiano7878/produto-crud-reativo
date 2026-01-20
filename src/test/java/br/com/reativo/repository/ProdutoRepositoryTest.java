package br.com.reativo.repository;

import br.com.reativo.modelo.Produto;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ProdutoRepositoryTest {

    ProdutoRepository repository;

    @Mock
    PgPool pgPool;

    @Mock
    PreparedQuery<RowSet<Row>> preparedQuery;

    @Mock
    RowSet<Row> rowSet;

    @Mock
    Row row;

    @Mock
    RowIterator<Row> rowIterator;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        repository = new ProdutoRepository();
    }

    // ===================== FIND ALL =====================

    @Test
    void deveRetornarListaDeProdutos_noFindAll() {

        when(pgPool.query(any(String.class)))
                .thenReturn(preparedQuery);

        when(preparedQuery.execute())
                .thenReturn(Uni.createFrom().item(rowSet));

        when(rowSet.iterator())
                .thenReturn(rowIterator);

        when(rowIterator.hasNext())
                .thenReturn(true, true, false);

        when(rowIterator.next())
                .thenReturn(row, row);

        when(row.getLong("id"))
                .thenReturn(1L, 2L);

        when(row.getString("nome"))
                .thenReturn("Notebook", "Mouse");

        Multi<Produto> resultado = repository.findAll(pgPool);

        var lista = resultado.collect().asList().await().indefinitely();

        assertEquals(2, lista.size());
        assertEquals("Notebook", lista.get(0).nome());
        assertEquals("Mouse", lista.get(1).nome());
    }

    // ===================== FIND BY ID =====================

    @Test
    void deveRetornarProduto_quandoFindByIdEncontrar() {

        when(pgPool.preparedQuery(any(String.class)))
                .thenReturn(preparedQuery);

        when(preparedQuery.execute(any(Tuple.class)))
                .thenReturn(Uni.createFrom().item(rowSet));

        when(rowSet.iterator())
                .thenReturn(rowIterator);

        when(rowIterator.hasNext())
                .thenReturn(true);

        when(rowIterator.next())
                .thenReturn(row);

        when(row.getLong("id")).thenReturn(10L);
        when(row.getString("nome")).thenReturn("Teclado");

        Produto produto = repository.findById(pgPool, 10L)
                .await().indefinitely();

        assertNotNull(produto);
        assertEquals(10L, produto.id());
        assertEquals("Teclado", produto.nome());
    }

    @Test
    void deveRetornarNull_quandoFindByIdNaoEncontrar() {

        when(pgPool.preparedQuery(any(String.class)))
                .thenReturn(preparedQuery);

        when(preparedQuery.execute(any(Tuple.class)))
                .thenReturn(Uni.createFrom().item(rowSet));

        when(rowSet.iterator())
                .thenReturn(rowIterator);

        when(rowIterator.hasNext())
                .thenReturn(false);

        Produto produto = repository.findById(pgPool, 99L)
                .await().indefinitely();

        assertNull(produto);
    }

    // ===================== ADD (com ArgumentCaptor) =====================

    @Test
    void deveAdicionarProdutoComSucesso_validandoTuplaEnviada() {

        Produto entrada = new Produto(0L, "Mesa");

        when(pgPool.preparedQuery(any(String.class)))
                .thenReturn(preparedQuery);

        ArgumentCaptor<Tuple> captor = ArgumentCaptor.forClass(Tuple.class);

        when(preparedQuery.execute(captor.capture()))
                .thenReturn(Uni.createFrom().item(rowSet));

        when(rowSet.rowCount()).thenReturn(1);
        when(rowSet.iterator()).thenReturn(rowIterator);
        when(rowIterator.next()).thenReturn(row);
        when(row.getLong("id")).thenReturn(100L);

        Produto salvo = repository.add(pgPool, entrada)
                .await().indefinitely();

        // Valida retorno
        assertNotNull(salvo);
        assertEquals(100L, salvo.id());
        assertEquals("Mesa", salvo.nome());

        // 🔥 Valida TUPlA enviada ao banco
        Tuple sent = captor.getValue();
        assertEquals("Mesa", sent.getString(0));
    }

    @Test
    void deveRetornarNull_quandoInsertNaoAfetarLinhas() {

        Produto entrada = new Produto(0L, "Mesa");

        when(pgPool.preparedQuery(any(String.class)))
                .thenReturn(preparedQuery);

        when(preparedQuery.execute(any(Tuple.class)))
                .thenReturn(Uni.createFrom().item(rowSet));

        when(rowSet.rowCount()).thenReturn(0);

        Produto salvo = repository.add(pgPool, entrada)
                .await().indefinitely();

        assertNull(salvo);
    }

    // ===================== DELETE =====================

    @Test
    void deveDeletarComSucesso() {

        when(pgPool.preparedQuery(any(String.class)))
                .thenReturn(preparedQuery);

        when(preparedQuery.execute(any(Tuple.class)))
                .thenReturn(Uni.createFrom().item(rowSet));

        when(rowSet.rowCount()).thenReturn(1);

        Boolean deleted = repository.deleteById(pgPool, 5L)
                .await().indefinitely();

        assertTrue(deleted);
    }

    @Test
    void naoDeveDeletar_quandoRegistroNaoExistir() {

        when(pgPool.preparedQuery(any(String.class)))
                .thenReturn(preparedQuery);

        when(preparedQuery.execute(any(Tuple.class)))
                .thenReturn(Uni.createFrom().item(rowSet));

        when(rowSet.rowCount()).thenReturn(0);

        Boolean deleted = repository.deleteById(pgPool, 5L)
                .await().indefinitely();

        assertFalse(deleted);
    }

    // ===================== UPDATE (com ArgumentCaptor) =====================

    @Test
    void deveAtualizarComSucesso_validandoTuplaEnviada() {

        when(pgPool.preparedQuery(any(String.class)))
                .thenReturn(preparedQuery);

        ArgumentCaptor<Tuple> captor = ArgumentCaptor.forClass(Tuple.class);

        when(preparedQuery.execute(captor.capture()))
                .thenReturn(Uni.createFrom().item(rowSet));

        when(rowSet.rowCount()).thenReturn(1);

        Boolean updated = repository.updateById(
                pgPool,
                7L,
                new Produto(7L, "NovoNome")
        ).await().indefinitely();

        assertTrue(updated);

        // 🔥 Valida tupla enviada ao banco
        Tuple sent = captor.getValue();
        assertEquals("NovoNome", sent.getString(0));
        assertEquals(7L, sent.getLong(1));
    }

    @Test
    void naoDeveAtualizar_quandoRegistroNaoExistir() {

        when(pgPool.preparedQuery(any(String.class)))
                .thenReturn(preparedQuery);

        when(preparedQuery.execute(any(Tuple.class)))
                .thenReturn(Uni.createFrom().item(rowSet));

        when(rowSet.rowCount()).thenReturn(0);

        Boolean updated = repository.updateById(
                pgPool,
                7L,
                new Produto(7L, "NovoNome")
        ).await().indefinitely();

        assertFalse(updated);
    }
}
