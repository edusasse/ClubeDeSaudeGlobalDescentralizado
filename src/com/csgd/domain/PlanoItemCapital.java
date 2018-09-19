package com.csgd.domain;

import java.math.BigDecimal;

public class PlanoItemCapital extends PlanoItemDecorator {

	private BigDecimal percentualAlocacao;

	public PlanoItemCapital(IPlanoItem planoItem, BigDecimal percentualAlocacao) {
		super(planoItem);
		setPercentualAlocacao(percentualAlocacao);
	}

	public PlanoItemCapital(IPlanoItem planoItem) {
		super(planoItem);
	}
	
	public BigDecimal getPercentualAlocacao() {
		return percentualAlocacao;
	}

	public void setPercentualAlocacao(BigDecimal percentualAlocacao) {
		if (percentualAlocacao.compareTo(new BigDecimal(0)) < 0) {
			throw new IllegalArgumentException("O percentual alocado para o plano item [" + getPlanoItem().getNome() + "] não pode ser menor que zero!");
		}
		if (percentualAlocacao.compareTo(new BigDecimal(100)) > 0) {
			throw new IllegalArgumentException("O percentual alocado para o plano item [" + getPlanoItem().getNome() + "] não pode ser maior que cem!");
		}
		this.percentualAlocacao = percentualAlocacao;
	}
	
	
}
