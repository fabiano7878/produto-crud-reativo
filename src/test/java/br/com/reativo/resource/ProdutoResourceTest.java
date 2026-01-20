package br.com.reativo.resource;

import br.com.reativo.modelo.Produto;
import br.com.reativo.service.ProdutoService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(ProdutoResourceTest.Profile.class)
class ProdutoResourceTest {

    @InjectMock
    ProdutoService produtoService;


    public static class Profile implements io.quarkus.test.junit.QuarkusTestProfile {
        @Override
        public java.util.Map<String, String> getConfigOverrides() {
            return java.util.Map.of(
                    "quarkus.kafka.devservices.enabled", "false",
                    "quarkus.datasource.devservices.enabled", "false",
                    "mp.messaging.outgoing.produto-nome-out.enabled", "false",
                    "mp.messaging.incoming.produto-nome-in.enabled", "false"
            );
        }
    }

    // ==================== GET /produto ====================

    @Test
    void deveRetornar200_quandoFindAllSucesso() {

        when(produtoService.findAll())
                .thenReturn(Multi.createFrom()
                        .items(
                                new Produto(1L, "Notebook"),
                                new Produto(2L, "Mouse")
                        ));

        given()
                .when()
                .get("/produto")
                .then()
                .statusCode(200)
                .body("size()", is(2))
                .body("[0].nome", is("Notebook"))
                .body("[1].nome", is("Mouse"));
    }

    // ==================== GET /produto/{id} ====================

    @Test
    void deveRetornar200_quandoFindByIdEncontrado() {

        when(produtoService.findById(10L))
                .thenReturn(
                        Uni.createFrom().item(
                                Response.ok(new Produto(10L, "Teclado")).build()
                        )
                );

        given()
                .when()
                .get("/produto/10")
                .then()
                .statusCode(200)
                .body("id", is(10))
                .body("nome", is("Teclado"));
    }

    @Test
    void deveRetornar404_quandoFindByIdNaoEncontrado() {

        when(produtoService.findById(99L))
                .thenReturn(
                        Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND).build()
                        )
                );

        given()
                .when()
                .get("/produto/99")
                .then()
                .statusCode(404);
    }

    // ==================== POST /produto/create ====================

    @Test
    void deveCriarProdutoViaEndpoint() {

        Produto entrada = new Produto(0L, "Cadeira");
        Produto salvo = new Produto(50L, "Cadeira");

        when(produtoService.createProdutoByKafka(any(), any()))
                .thenReturn(
                        Uni.createFrom().item(
                                Response.status(201).entity(salvo).build()
                        )
                );

        given()
                .contentType("application/json")
                .body("""
                        {"id":0,"nome":"Cadeira"}
                        """)
                .when()
                .post("/produto/create")
                .then()
                .statusCode(201)
                .body("id", is(50))
                .body("nome", is("Cadeira"));
    }

    // ==================== PATCH /produto/update/{id} ====================

    @Test
    void deveAtualizarProdutoViaEndpoint() {

        when(produtoService.update(eq(1L), any()))
                .thenReturn(
                        Uni.createFrom().item(
                                Response.ok("Produto atualizado").build()
                        )
                );

        given()
                .contentType("application/json")
                .body("""
                        {"id":1,"nome":"NovoNome"}
                        """)
                .when()
                .patch("/produto/update/1")
                .then()
                .statusCode(200);
    }

    // ==================== DELETE /produto/remove/{id} ====================

    @Test
    void deveDeletarProdutoViaEndpoint() {

        when(produtoService.deleteById(3L))
                .thenReturn(
                        Uni.createFrom().item(
                                Response.ok("Removido").build()
                        )
                );

        given()
                .when()
                .delete("/produto/remove/3")
                .then()
                .statusCode(200);
    }
}
