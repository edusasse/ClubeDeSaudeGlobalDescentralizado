package com.csgd.calc;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import com.csgd.domain.IPlanoItem;
import com.csgd.domain.IRegiao;
import com.csgd.domain.PlanoItemDecorator;
import com.csgd.domain.Premio;

public class Capital {

	private static Map<IRegiao, Map<IPlanoItem, Map<Premio, BigDecimal>>> mapaDeCapitalPorRegiao = new HashMap<IRegiao, Map<IPlanoItem, Map<Premio, BigDecimal>>>();
	private static Map<IRegiao, Map<IPlanoItem, BigDecimal>> mapaDeCapitalPorRegiaoSaldo = new HashMap<IRegiao, Map<IPlanoItem, BigDecimal>>();
	
	/**
	 * Obtem o saldo apenas.
	 * 
	 * @param regiao
	 * @param planoItem
	 * @return
	 */
	public static BigDecimal getCapitalPorRegiao(IRegiao regiao, IPlanoItem planoItem) {
		if (planoItem instanceof PlanoItemDecorator) {
			planoItem = ((PlanoItemDecorator) planoItem).getPlanoItem();
		}
		
		Map<IPlanoItem, BigDecimal> mapPlanoItemSaldo = mapaDeCapitalPorRegiaoSaldo.get(regiao);
		if (mapPlanoItemSaldo == null) {
			synchronized (Capital.class) {
				mapPlanoItemSaldo = mapaDeCapitalPorRegiaoSaldo.get(regiao);
				if (mapPlanoItemSaldo == null) {
					mapPlanoItemSaldo = new HashMap<>();
					mapaDeCapitalPorRegiaoSaldo.put(regiao, mapPlanoItemSaldo);
				}
			}
		}
		
		final BigDecimal result = mapPlanoItemSaldo.get(planoItem);
		return result == null ? new BigDecimal(0) : result;
	}
	
	/** 
	 * Faaz todo o calculo baseado no premio pago.
	 * 
	 * @param regiao
	 * @param planoItem
	 * @param premio
	 */
	public static void adicionarPremioCapital(IRegiao regiao, IPlanoItem planoItem, Premio premio) {
		if (planoItem instanceof PlanoItemDecorator) {
			planoItem = ((PlanoItemDecorator) planoItem).getPlanoItem();
		}
		Map<IPlanoItem, Map<Premio, BigDecimal>> mapPlanoItem = mapaDeCapitalPorRegiao.get(regiao);
		if (mapPlanoItem == null) {
			synchronized (Capital.class) {
				mapPlanoItem = mapaDeCapitalPorRegiao.get(regiao);
				if (mapPlanoItem == null) {
					mapPlanoItem = new HashMap<>();
					mapaDeCapitalPorRegiao.put(regiao, mapPlanoItem);
				}
			}
		}
		Map<Premio, BigDecimal> mapPremios = mapPlanoItem.get(planoItem);
		if (mapPremios == null) {
			synchronized (Capital.class) {
				mapPremios = mapPlanoItem.get(planoItem);
				if (mapPremios == null) {
					mapPremios = new HashMap<Premio, BigDecimal>();
					mapPlanoItem.put(planoItem, mapPremios);
				}
			}
		}
		
		BigDecimal valor = premio.getPercentualAlocacao(planoItem).multiply(premio.getValor());
		mapPremios.put(premio, valor);
		
		final BigDecimal valorAcumuladoSaldo = mapPremios.values().stream()
			.reduce(BigDecimal::add)
			.orElse(new BigDecimal(0));
		
		Map<IPlanoItem, BigDecimal> mapPlanoItemSaldo = mapaDeCapitalPorRegiaoSaldo.get(regiao);
		if (mapPlanoItemSaldo == null) {
			synchronized (Capital.class) {
				mapPlanoItemSaldo = mapaDeCapitalPorRegiaoSaldo.get(regiao);
				if (mapPlanoItemSaldo == null) {
					mapPlanoItemSaldo = new HashMap<>();
					mapaDeCapitalPorRegiaoSaldo.put(regiao, mapPlanoItemSaldo);
				}
			}
		}
		mapPlanoItemSaldo.put(planoItem, valorAcumuladoSaldo);
	}
	
}
