package com.csgd.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import com.csgd.constantes.Parametros;

public class ValorCapital {
		private BigDecimal capitalAcessivel;
		private BigDecimal capitalMaximoAcessivelComRestricaoPorCliente;
		private BigDecimal capitalTotal;
		
		public ValorCapital(Cliente cliente, BigDecimal capitalAcessivel, BigDecimal capitalTotal) {
			this.capitalAcessivel = capitalAcessivel;
			this.capitalMaximoAcessivelComRestricaoPorCliente = capitalAcessivel.multiply(new BigDecimal(Parametros.CAPITAL_MAXIMO_ACESSIVEL_POR_CLIENTE.getValorParametro()), new MathContext(16, RoundingMode.HALF_UP)).divide(new BigDecimal(100), new MathContext(16, RoundingMode.HALF_UP));
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