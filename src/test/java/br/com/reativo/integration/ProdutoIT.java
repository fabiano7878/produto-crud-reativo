package br.com.reativo.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestProfile(ProdutoIT.Profile.class)
class ProdutoIT {

    /**
     * Profile de teste que APONTA PARA O SEU DOCKER-COMPOSE
     * (sem Testcontainers).
     */
    public static class Profile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    // POSTGRES DO SEU DOCKER-COMPOSE
                    "quarkus.datasource.jdbc.url",
                    "jdbc:postgresql://localhost:5432/produto-kfka",

                    "quarkus.datasource.reactive.url",
                    "postgresql://localhost:5432/produto-kfka",

                    "quarkus.datasource.username", "kfka_produto",
                    "quarkus.datasource.password", "kfka-produto123",

                    // KAFKA DO SEU DOCKER-COMPOSE
                    "kafka.bootstrap.servers", "localhost:9092",

                    // Garante migração no início do teste
                    "quarkus.flyway.migrate-at-start", "true"
            );
        }
    }

    @Test
    void deveCriarEConsultarProduto_pontaAPonta() {

        // 1) Criar produto via API real
        given()
                .contentType(ContentType.JSON)
                .body("{\"id\":0, \"nome\":\"Cadeira\"}")
                .when()
                .post("/produto/create")
                .then()
                .statusCode(201)
                .body("nome", is("Cadeira"));

        // 2) Consultar todos e verificar se apareceu
        given()
                .when()
                .get("/produto")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("[0].nome", containsString("Cadeira"));
    }

    @Test
    void deveRetornar404_quandoProdutoNaoExistir() {

        given()
                .when()
                .get("/produto/99999")
                .then()
                .statusCode(404);
    }
}
