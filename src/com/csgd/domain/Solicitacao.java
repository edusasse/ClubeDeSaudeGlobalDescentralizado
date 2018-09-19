package com.csgd.domain;

import java.math.BigDecimal;

public class Solicitacao {

	private Fornecedor fornecedor;
	private Cliente cliente;
	private BigDecimal valor;

	public Solicitacao(Fornecedor fornecedor, Cliente cliente, BigDecimal valor) {
		super();
		this.fornecedor = fornecedor;
		this.cliente = cliente;
		this.valor = valor;
	}

	public Fornecedor getFornecedor() {
		return fornecedor;
	}

	public Cliente getCliente() {
		return cliente;
	}

	public BigDecimal getValor() {
		return valor;
	}

	
}
