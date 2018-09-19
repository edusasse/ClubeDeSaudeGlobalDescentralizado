package com.csgd.constantes;

public enum Parametros {

	PERIODO_DE_CARENCIA_MINIMO((byte)3),
	PERIODOS_DE_ENTRADA((byte)24),
	CAPITAL_MAXIMO_ACESSIVEL_POR_CLIENTE((byte)50),
	FATOR_DE_VARIACAO_MAXIMO((byte)5); // nao utilizado na implementacao
	
	byte value;
	
	Parametros(byte value) {
		this.value = value;
	}
	
	public byte getValorParametro() {
		return value;
	}
	
}
