package com.example.pinpad_app.enums;


public enum Transaction {
        CAMPO_COMPROVANTE_CLIENTE(121),
        CAMPO_COMPROVANTE_ESTAB(122),
        RETORNO_CONFIGURE(0),
        RETORNO_START_TRANSACTION(10000),
        CANCELAR_TRANSACAO(0),
        CONFIRMAR_TRANSACAO(1);

        private final int valor;

        Transaction(int valor) {
            this.valor = valor;
        }

        public int getValor() {
            return valor;
        }
}
