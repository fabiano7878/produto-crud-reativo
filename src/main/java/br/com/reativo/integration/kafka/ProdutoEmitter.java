package br.com.reativo.integration.kafka;


import br.com.reativo.modelo.Produto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

@ApplicationScoped
public class ProdutoEmitter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProdutoEmitter.class);

    @Inject @Channel("produto-nome-out")
    Emitter<String> emitter;

    public void sendKafkaData( String message){
            LOGGER.info("Enviando uma msg ao kafka: {}", message);
            emitter.send(message);
    }

    public void sendMsgToKafkaAboutProduct(Produto produto){
            try {
                String produtoJson = new ObjectMapper().writeValueAsString(produto);
                LOGGER.info("Enviando uma msg ao kafka: {}", produtoJson);
                emitter.send(produtoJson);
            } catch (JsonProcessingException e) {
                LOGGER.error("Erro ao tentar enviar uma mensgem ao kafka? ", e);
            }
    }

    public void


    sendRequestTocreateNewProductNotificationKafka(Produto produto){
        if(Objects.nonNull(produto)) {
            try {
                 String produtoJson = new ObjectMapper().writeValueAsString(produto);
                 LOGGER.info("Recebendo msg pelo kafka para gerar novo Produto:{} ", produtoJson);
                 emitter.send(produtoJson);
            } catch (JsonProcessingException e) {
                LOGGER.error("Erro no envio ao Kafka: ", e);
            }
        }
    }

    public void sendCreateProdutoNotificationKafka(Produto produto){
            try {
                String produtoJson = new ObjectMapper().writeValueAsString(produto);
                LOGGER.info("Notificanto o kafka após criar um novo Produto:{} ", produtoJson);
                emitter.send(produtoJson);
            } catch (JsonProcessingException e) {
                LOGGER.error("Erro no envio ao Kafka: ", e);
            }
    }
}
