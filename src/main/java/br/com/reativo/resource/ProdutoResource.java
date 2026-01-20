package br.com.reativo.resource;

import br.com.reativo.integration.kafka.ProdutoEmitter;
import br.com.reativo.modelo.Produto;
import br.com.reativo.repository.ProdutoRepository;
import br.com.reativo.service.ProdutoService;
import io.quarkus.runtime.util.StringUtil;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/produto")
public class ProdutoResource {

    @Inject
    ProdutoRepository produtoRepository;

    @Inject
    ProdutoEmitter produtoEmitter;

    @Inject
    ProdutoService produtoService;

    //Apenas msg via Kafka
    @POST
    @Path("message")
    public Uni<Response> sendMessageKafkaJustName(String nomeProduto){
        return produtoService.sendMessageKafkaInfoNameProduct((v) -> !StringUtil.isNullOrEmpty(nomeProduto), nomeProduto);
    }

    @POST
    @Path("message/produto")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> sendMessageKafkaAboutProduct(Produto produto){
        return produtoService.sendMessageKafkaAboutProduct((v) -> !StringUtil.isNullOrEmpty(produto.nome()), produto);
    }

    //CRUD
    @GET
    public Multi<Produto> findAll(){
        return produtoService.findAll();
    }

    @GET
    @Path("{id}")
    public Uni<Response> findById(@PathParam("id") Long id){
        return produtoService.findById(id);
    }

    @POST
    @Path("/create")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> createProdutoByKafka(Produto produto){
        return produtoService.createProdutoByKafka((v) -> !StringUtil.isNullOrEmpty(produto.nome()), produto);
    }

    @PATCH
    @Path("/update/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response>  updateById(@PathParam("id") Long id, @Nonnull Produto produto){
        return produtoService.update(id, produto);
    }

    @DELETE
    @Path("/remove/{id}")
    public Uni<Response> deleteById(@PathParam("id") Long id){
        return produtoService.deleteById(id);
    }

}
