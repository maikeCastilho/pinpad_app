package com.example.pinpad_app.enums;

public enum TextsSharedPreferences {
    TEXT_CNPJ_OU_CPF("textCNPJouCPF"),
    TEXT_CNPJ_AUTOMACAO("textCnpjAutomocao"),
    TEXT_CNPJ_FACILITADOR("textCnpjFacilitador"),
    TEXT_CODIGO_LOJA("textCodigoLoja"),
    TEXT_COM_EXTERNA("textComExterna"),
    TEXT_CUPOM_FISCAL("textCupomFiscal"),
    TEXT_DATA_FISCAL("textDataFiscal"),
    TEXT_ENDERECO_SITEF("textEnderecoSitef"),
    TEXT_HORARIO("textHorario"),
    TEXT_MODALIDADE("textModalidade"),
    TEXT_NOME_SHARED("camposSiTef"),
    TEXT_NUMERO_TERMINAL("textNumeroTerminalSitef"),
    TEXT_PARAMETROS_ADICIONAIS("textParametrosAdicionais"),
    TEXT_VALOR("textValor"),

    TEXT_RESTRICOES("textRestricoes"),
    TEXT_OPERADOR("textOperador");

    private final String valor;

    TextsSharedPreferences(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }
}
