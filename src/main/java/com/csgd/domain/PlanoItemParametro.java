package com.csgd.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import com.csgd.constantes.Parametros;

public class PlanoItemParametro extends PlanoItemDecorator {

	private BigDecimal percentualAlocacao;

	private byte controleDeEntrada = 0;	
	private byte controleDeCarencia = 0;
	private final byte controleDeEntradaDefinicaoInicial;
	
	public PlanoItemParametro(IPlanoItem planoItem, BigDecimal percentualAlocacao) {
		this(planoItem);		
		setPercentualAlocacao(percentualAlocacao);
	}

	public PlanoItemParametro(IPlanoItem planoItem) {
		super(planoItem);
		this.controleDeEntradaDefinicaoInicial = Parametros.PERIODOS_DE_ENTRADA.getValorParametro();
		this.controleDeCarencia = Parametros.PERIODO_DE_CARENCIA_MINIMO.getValorParametro();
	}
	
	/**
	 * Define o percentual.
	 * 
	 * @param percentualAlocacao
	 */
	public void setPercentualAlocacao(BigDecimal percentualAlocacao) {
		if (percentualAlocacao.compareTo(new BigDecimal(0)) < 0) {
			throw new IllegalArgumentException("O percentual alocado para o plano item [" + getPlanoItem().getNome() + "] n�o pode ser menor que zero!");
		}
		if (percentualAlocacao.compareTo(new BigDecimal(100)) > 0) {
			throw new IllegalArgumentException("O percentual alocado para o plano item [" + getPlanoItem().getNome() + "] n�o pode ser maior que cem!");
		}
		this.percentualAlocacao = percentualAlocacao.divide(new BigDecimal(100, new MathContext(16, RoundingMode.HALF_UP)));
	}
	 	
	/**
	 * Incrementa o periodo de entrada.
	 */
	public void processarAvancoDePagamentoEPeriodo() {
		if (controleDeEntrada < controleDeEntradaDefinicaoInicial && controleDeCarencia == 0) {
			this.controleDeEntrada++;
		}
		if (controleDeCarencia > 0) {
			this.controleDeCarencia--;
		}
	}
	
	/**
	 * Decrementa o periodo de entrada.
	 */
	public void retrocederEntrada() {
		if (controleDeEntrada > 0) {
			this.controleDeEntrada--;
		}
	}
	
	public byte getControleDeEntrada() {
		return controleDeEntrada;
	}

	public byte getControleDeCarencia() {
		return controleDeCarencia;
	}
	
	public byte getControleDeEntradaDefinicaoInicial() {
		return controleDeEntradaDefinicaoInicial;
	}

	public BigDecimal getPercentualAlocacao() {
		return percentualAlocacao;
	}
	
}