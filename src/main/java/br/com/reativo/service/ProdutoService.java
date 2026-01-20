package br.com.reativo.service;

import br.com.reativo.exception.ProdutoException;
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
            return produtoRepository.findAll(pgPool)
                    .onFailure(ProdutoException.class)
                    .invoke(ex -> LOGGER.error("Erro ao tentar consultar os Produtos", ex));
    }

    public Uni<Response> sendMessageKafkaInfoNameProduct(ValidacaoKafka validacaoKafka, String nomeProduto){

            if (!validacaoKafka.isTextFiledNullOrEmpty(nomeProduto)) {
                return Uni.createFrom()
                        .item(Response.status(Response.Status.BAD_REQUEST)
                                .entity("Nome do produto inválido")
                                .build());
            }

        // 2) Envio ao Kafka (void) encapsulado em Uni
        return Uni.createFrom().item(() -> {
            try {
                produtoEmitter.sendKafkaData(nomeProduto);

                return Response.ok(
                        "Mensagem enviada para Kafka referente ao produto: " + nomeProduto
                ).build();

            } catch (ProdutoException ex) {
                LOGGER.error("Erro ao enviar mensagem para Kafka: {}", nomeProduto, ex);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        });
    }

    public Uni<Response> sendMessageKafkaAboutProduct(ValidacaoKafka validacaoKafka, Produto produto) {

        if (produto == null || !validacaoKafka.isTextFiledNullOrEmpty(produto.nome())) {
            return Uni.createFrom()
                    .item(Response.status(Response.Status.BAD_REQUEST)
                            .entity("Nome do produto inválido")
                            .build());
        }

        return Uni.createFrom().item(() -> {
            try {
                produtoEmitter.sendMsgToKafkaAboutProduct(produto);
                return Response.ok().entity("A mensagem tem o produto: %s , %s".formatted(produto.id(), produto.nome())).build();
            } catch (ProdutoException ex) {
                LOGGER.info("Erro com a mensagem recebida de Produto: {}", produto);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        });
    }

    public Uni<Response> findById(long id){

        return produtoRepository.findById(pgPool, id)
                .onItem().transform(produto ->
                                produto != null
                                ? Response.ok(produto).build()
                                : Response.status(Response.Status.NOT_FOUND).build()
                )
                .onFailure(ProdutoException.class)
                .recoverWithItem(ex -> {
                    LOGGER.error("Erro ao tentar consultar o Produto {}", id, ex);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                });
    }

    public Uni<Response> createProdutoByKafka(ValidacaoKafka validacaoKafka, Produto produto) {
        LOGGER.info("Payload create: {}", produto);

        if (!Objects.nonNull(produto) || !validacaoKafka.isTextFiledNullOrEmpty(produto.nome())) {
            LOGGER.error("Erro ao tentar criar Produto: {} \n", produto);
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST).build());
        }

        produtoEmitter.sendRequestTocreateNewProductNotificationKafka(produto);
        return produtoRepository.add(pgPool, produto)
                .onItem()
                .transform(p -> {
                            produtoEmitter.sendCreateProdutoNotificationKafka(p);
                            return Response.status(Response.Status.CREATED).entity(p).build();
                        }
                ).onFailure(ProdutoException.class)
                .recoverWithItem(ex -> {
                    LOGGER.error("Erro ao tentar criar Produto: {} \n", produto, ex);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                });
    }

    public Uni<Response> update(Long id, Produto produto) {
        return produtoRepository.findById(pgPool, id)
                .onItem().transformToUni(produtoEncontrado -> {
                    LOGGER.info("Produto encontrado: {}, id: {}", produtoEncontrado, id);
                    if (produtoEncontrado == null) {
                        return Uni.createFrom()
                                .item(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Produto de id: %s não encontrado!".formatted(id)).build());
                    }

                    return produtoRepository.updateById(pgPool, id, produto)
                            .onItem().transform(updated ->
                                    updated
                                    ? Response.ok().entity(String.format("Produto de id: %s update com sucesso!", id)).build()
                                    : Response.status(Response.Status.BAD_REQUEST).entity(String.format("Update não consistiu os dados em Produto!", id)).build()
                            );
                })
                .onFailure(ProdutoException.class)
                .recoverWithItem(ex -> {
                    LOGGER.error("Erro atualizar o Produto {}", id, ex);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                });
    }

    public Uni<Response> deleteById(Long id) {

        return produtoRepository.findById(pgPool, id)
                .onItem().transformToUni(produtoEncontrado -> {
                    if (produtoEncontrado == null) {
                        return Uni.createFrom()
                                .item(Response.status(Response.Status.NOT_FOUND).entity("Produto de id: &s não encontrado!".formatted(id)).build());
                    }

                    return produtoRepository.deleteById(pgPool, id)
                            .onItem()
                            .transform(deleted ->
                                    deleted
                                            ? Response.ok().entity(String.format("Produto de id:%s removido com sucesso!", id)).build()
                                            : Response.status(Response.Status.BAD_REQUEST).entity("Produto de id:%s não encontrado!".formatted(id)).build()
                            )
                            .onFailure(ProdutoException.class)
                            .recoverWithItem(ex -> {
                                LOGGER.error("Erro ao tentar deletar Produto: {} \n", id, ex);
                                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                            });
                });
    }
}
