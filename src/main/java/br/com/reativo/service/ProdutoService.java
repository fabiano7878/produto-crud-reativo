package br.com.reativo.service;

import br.com.reativo.integration.kafka.ProdutoEmitter;
import br.com.reativo.modelo.Produto;
import br.com.reativo.repository.ProdutoRepository;
import br.com.reativo.validacao.funcional.ValidacaoKafka;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

@ApplicationScoped
public class ProdutoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProdutoService.class);

    @Inject
    PgPool pgPool;

    @Inject
    ProdutoRepository produtoRepository;

    @Inject
    ProdutoEmitter produtoEmitter;


    public Multi<Produto> findAll(){
        return produtoRepository.findAll(pgPool);
    }


    public Response sendMessageKafkaInfoNameProduct(ValidacaoKafka validacaoKafka, String nomeProduto){
        if(validacaoKafka.isTextFiledNullOrEmpty(nomeProduto)) {
            produtoEmitter.sendKafkaData(nomeProduto);
            return Response.ok("Mensagem enviada para o kafka, referente ao produto: " + nomeProduto).build();
        }
        LOGGER.info("Erro com a mensagem recebida: {}", nomeProduto);
        return Response.serverError().build();
    }

    public Response sendMessageKafkaAboutProduct(ValidacaoKafka validacaoKafka, Produto produto){
        if(Objects.nonNull(produto) && validacaoKafka.isTextFiledNullOrEmpty(produto.nome())) {
            produtoEmitter.sendMsgToKafkaAboutProduct(produto);
            return Response.ok().build();
        }
        LOGGER.info("Erro com a mensagem recebida: {}", produto);
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    public Uni<Response> findById(long id){
            return produtoRepository.findById(pgPool, id)
                    .onItem()
                    .transform(p -> Objects.nonNull(p) ? Response.ok(p).build() : Response.status(Response.Status.NOT_FOUND).build());
    }

    public Uni<Response> createProdutoByKafka(ValidacaoKafka validacaoKafka, Produto produto) {
        LOGGER.info("Payload create: {}", produto);
        produtoEmitter.sendRequestTocreateNewProductNotificationKafka(produto);
        if (Objects.nonNull(produto) && validacaoKafka.isTextFiledNullOrEmpty(produto.nome())) {
            return produtoRepository.add(pgPool, produto)
                    .onItem()
                    .transform(p -> {
                        if(Objects.nonNull(p)){
                         produtoEmitter.sendCreateProdutoNotificationKafka(p);
                         return Response.status(Response.Status.CREATED).entity(p).build() ;
                        }else{
                         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                        }
                    });
        }
        return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST).build());
    }

    public Uni<Response> update(ValidacaoKafka validacaoKafka, Long id, Produto produto) {
        return Uni.createFrom().item(Response.ok().build());
    }

    public Uni<Response> deleteById(Long id) {
            return produtoRepository.deleteById(pgPool, id)
                    .onItem()
                    .transform(deleted -> {
                        if(deleted) {
                            return Response.ok().entity(String.format("Produto de id:%s removido com sucesso!", id)).build();
                        }else {
                            return Response.status(Response.Status.NOT_FOUND).entity(String.format("Produto de id:%s não encontrado!", id)).build();
                        }
                    });
    }
}
