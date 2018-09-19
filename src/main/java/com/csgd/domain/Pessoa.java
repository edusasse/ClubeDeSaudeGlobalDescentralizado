package com.csgd.domain;

public abstract class Pessoa {

	private String nome;

	public Pessoa(String nome) {
		super();
		this.nome = nome;
	}
	
	public String getNome() {
		return nome;
	}
}
