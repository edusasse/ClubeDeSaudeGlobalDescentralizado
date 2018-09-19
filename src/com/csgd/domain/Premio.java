package com.csgd.domain;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.csgd.fake.Calendario;

public class Premio {

	private IRegiao regiao;
	
	private Cliente cliente;
	
	private Date dataDoPagamento; 

	private BigDecimal valor;
	
	private boolean closed;

	private Map<IPlanoItem, IPlanoItem> mapPlanoItem = new HashMap<IPlanoItem, IPlanoItem>();
	
	public Premio(IRegiao regiao, Cliente cliente, BigDecimal valor) {
		super();
		this.regiao = regiao;
		this.cliente = cliente;
		this.dataDoPagamento = Calendario.getInstance().getDataAtual();
		this.valor = valor;
	}
	
	/**
	 * Verifica se o premio esta fechado.
	 * 
	 * @return
	 */
	public boolean isClosed() {
		return closed;
	}
	
	/**
	 * Fecha o pagamento registrando uma fotografia das configurações do momento.
	 * 
	 */
	public void close() {
		checkClose();
		this.mapPlanoItem = cliente.getMapPlanoItemCopy(getRegiao());
		this.closed = true;
	}
	
	/**
	 * Verifica se o premio ja foi fechado.
	 */
	protected void checkClose() {
		if (isClosed()) {
			throw new IllegalArgumentException("Premio fechado!");
		}
	}
	
	/**
	 * Retorna a fotografia das configurações no momento do fechamento.
	 * 
	 * @return
	 */
	public Map<IPlanoItem, IPlanoItem> getMapPlanoItem() {
		return Collections.unmodifiableMap(mapPlanoItem);
	}
	
	public BigDecimal getPercentualAlocacao(IPlanoItem pi) {
		BigDecimal result = null;
		if (getMapPlanoItem() != null) {
			final IPlanoItem pid = getMapPlanoItem().get(pi);
			if (pid instanceof PlanoItemParametro) {
				result = ((PlanoItemParametro) pid).getPercentualAlocacao();
			}
		}
		
		return result;
	}

	public IRegiao getRegiao() {
		return regiao;
	}
	
	public Cliente getCliente() {
		return cliente;
	}
	
	public Date getDataDoPagamento() {
		return dataDoPagamento;
	}
	
	public BigDecimal getValor() {
		return valor;
	}
	
}
