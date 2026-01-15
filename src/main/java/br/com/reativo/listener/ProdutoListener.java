package br.com.reativo.listener;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ProdutoListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProdutoListener.class);

    @Incoming("produto-nome-in")
    public void recieveMessageFromKafka(String message){
        LOGGER.info("Kafka,a mensagem recebida e: {}",message);
    }
}
