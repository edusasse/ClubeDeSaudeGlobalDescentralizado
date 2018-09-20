package com.csgd.domain;

public class PlanoItem implements IPlanoItem {

	private String nome;

	public PlanoItem(String nome) {
		super();
		this.nome = nome;
	}

	public PlanoItem() {
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}
	
	@Override
	public IPlanoItem getPlanoItem() {
		return this;
	}
}
