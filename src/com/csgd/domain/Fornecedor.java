package com.csgd.domain;

public class Fornecedor extends Pessoa {

	private IRegiao regiao;
	
	public Fornecedor(String nome, IRegiao regiao) {
		super(nome);
		this.regiao = regiao;
	}
	
	public IRegiao getRegiao() {
		return this.regiao;
	}
}
