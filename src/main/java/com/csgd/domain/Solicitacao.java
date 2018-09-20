package com.csgd.domain;

import java.math.BigDecimal;

public class Solicitacao {

	private Fornecedor fornecedor;
	private Cliente cliente;
	private IPlanoItem planoItem;
	private BigDecimal valor;
 

	public Solicitacao(Fornecedor fornecedor, Cliente cliente, IPlanoItem planoItem, BigDecimal valor) {
		super();
		this.fornecedor = fornecedor;
		this.cliente = cliente;
		this.planoItem = planoItem;
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

	public IPlanoItem getPlanoItem() {
		return planoItem;
	}
}
