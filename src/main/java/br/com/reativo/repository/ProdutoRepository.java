package br.com.reativo.repository;


import br.com.reativo.modelo.Produto;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.concurrent.CompletableFuture;


@ApplicationScoped
public class ProdutoRepository {

    public Multi<Produto> findAll(PgPool pgPool){
        CompletableFuture<RowSet<Row>> future = pgPool.query("select id, nome from produto").execute().convert().toCompletableFuture();
       Uni<RowSet<Row>> uni = Uni.createFrom().completionStage(future.toCompletableFuture());

       /**
        * Abordagem funcional, opção da implementação
        *
       return uni.onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
                .onItem().transform(row -> this.fromDB(row));
        */

        return uni.onItem().transformToMulti(Multi.createFrom()::iterable)
                .onItem().transform(this::fromDB);

    }
    private Produto fromDB(Row row){
        return new Produto(row.getLong("id"), row.getString("nome"));
    }

    public Uni<Produto> findById(PgPool pgPool, Long id) {
        CompletableFuture<RowSet<Row>> future = pgPool.preparedQuery("select id, nome from produto where id = $1").execute(Tuple.of(id)).convert().toCompletableFuture();
        Uni<RowSet<Row>> uni = Uni.createFrom().completionStage(future.toCompletableFuture());
        return uni.onItem().transform( rows -> rows.iterator().hasNext() ? fromDB(rows.iterator().next()) : null);
    }

    /**
    public Uni<Produto> findById(PgPool pgPool, Long id, Produto produto) {
        Future<RowSet<Row>> future = (Future<RowSet<Row>>) pgPool.preparedQuery("select id, nome from produto where id = $1").execute(Tuple.of(id));
        Uni<RowSet<Row>> uni = Uni.createFrom().completionStage(future.toCompletionStage());
        return uni.onItem().transform( rows -> rows.iterator().hasNext() ? fromDB(rows.iterator().next()) : null);
    }
     */

    public Uni<Produto> add(PgPool pgPool, Produto produto) {
        String sql = "INSERT INTO produto (nome) VALUES ($1) RETURNING id";
        return pgPool.preparedQuery(sql)
                .execute(Tuple.of(produto.nome()))
                .onItem().transform(row -> {
                    if (row.rowCount() > 0) {
                        return new Produto(row.iterator().next().getLong("id"), produto.nome());
                    }else {
                        return null;
                    }
                });
    }

    public Uni<Boolean> deleteById(PgPool pgPool, long id){
       String sql = "DELETE FROM produto WHERE id = $1";
       return pgPool.preparedQuery(sql)
               .execute(Tuple.of(id))
               .onItem().transform(rows -> rows.rowCount() > 0);
    }

    public Uni<Boolean> updateById(PgPool pgPool, long id, Produto produto){
        String sql = "UPDATE produto SET nome = $1 WHERE id = $2";
        return pgPool.preparedQuery(sql)
                .execute(Tuple.of(produto.nome(), id))
                .onItem()
                .transform(rows -> rows.rowCount() > 0);
    }
}