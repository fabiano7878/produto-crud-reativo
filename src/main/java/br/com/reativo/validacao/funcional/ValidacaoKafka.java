package br.com.reativo.validacao.funcional;

@FunctionalInterface
public interface ValidacaoKafka {
    boolean isTextFiledNullOrEmpty(String nomeProduto);
}
