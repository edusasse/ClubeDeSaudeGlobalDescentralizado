package com.csgd.domain;

import java.math.BigDecimal;
import java.math.MathContext;

import com.csgd.constantes.Parametros;

public class ValorCapital {
		private BigDecimal capitalAcessivel;
		private BigDecimal capitalMaximoAcessivelComRestricaoPorCliente;
		private BigDecimal capitalTotal;
		
		public ValorCapital(BigDecimal capitalAcessivel, BigDecimal capitalTotal) {
			this.capitalAcessivel = capitalAcessivel;
			this.capitalMaximoAcessivelComRestricaoPorCliente = capitalAcessivel.multiply(new BigDecimal(Parametros.CAPITAL_MAXIMO_ACESSIVEL_POR_CLIENTE.getValorParametro()), new MathContext(6)).divide(new BigDecimal(100), new MathContext(6));
			this.capitalTotal = capitalTotal;
		}
		
		public BigDecimal getCapitalAcessivel() {
			return capitalAcessivel;
		}
		
		public BigDecimal getCapitalTotal() {
			return capitalTotal;
		}

		public BigDecimal getCapitalMaximoAcessivelComRestricaoPorCliente() {
			return capitalMaximoAcessivelComRestricaoPorCliente;
		}
}