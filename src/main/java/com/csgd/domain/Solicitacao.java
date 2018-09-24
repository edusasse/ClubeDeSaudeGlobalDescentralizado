package com.csgd.domain;

import java.math.BigDecimal;
import java.util.Date;

public class Solicitacao {

	private Fornecedor fornecedor;
	private Cliente cliente;
	private IPlanoItem planoItem;
	private Date dataSolicitacao;
	private BigDecimal valor;

	public Solicitacao(Fornecedor fornecedor, Cliente cliente, IPlanoItem planoItem, Date dataSolicitacao,
			BigDecimal valor) {
		super();
		this.fornecedor = fornecedor;
		this.cliente = cliente;
		this.planoItem = planoItem;
		this.dataSolicitacao = dataSolicitacao;
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
	
	public Date getDataSolicitacao() {
		return dataSolicitacao;
	}
}
