package com.csgd.domain;

public class Regiao implements IRegiao {

	private String nome;
	private Plano plano = null;
	
	public Regiao(String nome, Plano plano) {
		super();
		this.nome = nome;
		this.plano = plano;
	}

	@Override
	public String getNome() {
		return nome;
	}
	
	@Override
	public Plano getPlano() {
		return plano;
	}

}
