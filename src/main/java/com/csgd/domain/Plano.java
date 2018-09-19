package com.csgd.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Plano {

	private List<IPlanoItem> listaDeItensDoPlano = new ArrayList<IPlanoItem>();
	private List<Cliente> listaDeClientes = new ArrayList<Cliente>();
	
	public void addPlanoItem(IPlanoItem planoItem) {
		this.listaDeItensDoPlano.add(planoItem);
	}
	
	public void adicionarClienteAoPlano(Cliente cliente) {
		if (!this.listaDeClientes.contains(cliente)) {
			this.listaDeClientes.add(cliente);
		}
	}
	
	public boolean contemPlanoItem(IPlanoItem planoItem) {
		return this.listaDeItensDoPlano.contains(planoItem);
	}
	
	public List<IPlanoItem> getListaDeItensDoPlano() {
		return Collections.unmodifiableList(listaDeItensDoPlano);
	}
	
	public List<Cliente> getListaDeClientes() {
		 return Collections.unmodifiableList(listaDeClientes);
	}
}
