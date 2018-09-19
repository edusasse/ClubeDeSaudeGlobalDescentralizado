package com.csgd.domain;

public abstract class PlanoItemDecorator implements IPlanoItem {

	private final IPlanoItem planoItem;
	
	public PlanoItemDecorator(IPlanoItem planoItem) {
		this.planoItem = planoItem;
	}
	
	public IPlanoItem getPlanoItem() {
		return planoItem;
	}
	
	@Override
	public String getNome() {
		return getPlanoItem().getNome();
	}
}
