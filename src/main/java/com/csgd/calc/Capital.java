package com.csgd.calc;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.Map;

import com.csgd.constantes.Parametros;
import com.csgd.domain.IPlanoItem;
import com.csgd.domain.IRegiao;
import com.csgd.domain.PlanoItemDecorator;
import com.csgd.domain.Premio;
import com.csgd.domain.Solicitacao;

public class Capital {

	private static Map<IRegiao, Map<IPlanoItem, Map<Solicitacao, BigDecimal>>> mapaDeCapitalPorRegiaoSolicitacao = new HashMap<IRegiao, Map<IPlanoItem, Map<Solicitacao, BigDecimal>>>();
	private static Map<IRegiao, Map<IPlanoItem, Map<Premio, BigDecimal>>> mapaDeCapitalPorRegiaoPremio = new HashMap<IRegiao, Map<IPlanoItem, Map<Premio, BigDecimal>>>();
	private static Map<IRegiao, Map<IPlanoItem, BigDecimal>> mapaDeCapitalPorRegiaoTotal = new HashMap<IRegiao, Map<IPlanoItem, BigDecimal>>();
	private static Map<IRegiao, Map<IPlanoItem, BigDecimal>> mapaDeCapitalPorRegiaoSaldo = new HashMap<IRegiao, Map<IPlanoItem, BigDecimal>>();
	
	/**
	 * Obtem o saldo apenas.
	 * 
	 * @param regiao
	 * @param planoItem
	 * @return
	 */
	public static BigDecimal getCapitalPorRegiaoSaldo(IRegiao regiao, IPlanoItem planoItem) {
		if (planoItem instanceof PlanoItemDecorator) {
			planoItem = ((PlanoItemDecorator) planoItem).getPlanoItem();
		}
		return getCapitalPorRegiaoDosMapasDeControle(regiao, planoItem, mapaDeCapitalPorRegiaoSaldo);
	}

	protected static BigDecimal getCapitalPorRegiaoDosMapasDeControle(IRegiao regiao, IPlanoItem planoItem,
			Map<IRegiao, Map<IPlanoItem, BigDecimal>> map) {
		Map<IPlanoItem, BigDecimal> mapPlanoItemSaldo = map.get(regiao);
		if (mapPlanoItemSaldo == null) {
			synchronized (Capital.class) {
				mapPlanoItemSaldo = map.get(regiao);
				if (mapPlanoItemSaldo == null) {
					mapPlanoItemSaldo = new HashMap<>();
					map.put(regiao, mapPlanoItemSaldo);
				}
			}
		}
		
		final BigDecimal result = mapPlanoItemSaldo.get(planoItem);
		return result == null ? new BigDecimal(0) : result;
	}
	
	public static BigDecimal getCapitalPorRegiaoTotal(IRegiao regiao, IPlanoItem planoItem) {
		if (planoItem instanceof PlanoItemDecorator) {
			planoItem = ((PlanoItemDecorator) planoItem).getPlanoItem();
		}
		
		return getCapitalPorRegiaoDosMapasDeControle(regiao, planoItem, mapaDeCapitalPorRegiaoTotal);
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
		
		// Guarda o premio para registro historico
		Map<Premio, BigDecimal> mapPremios = processarPremioRegistroHistorico(regiao, planoItem, premio);
		
		final BigDecimal valorAcumuladoSaldo = mapPremios.values().stream()
			.reduce(BigDecimal::add)
			.orElse(new BigDecimal(0));
		
		
		adicionarPremioCapitalAosMapasDeControle(regiao, planoItem, valorAcumuladoSaldo, mapaDeCapitalPorRegiaoSaldo);
		adicionarPremioCapitalAosMapasDeControle(regiao, planoItem, valorAcumuladoSaldo, mapaDeCapitalPorRegiaoTotal);
	}

	protected static void adicionarPremioCapitalAosMapasDeControle(IRegiao regiao, IPlanoItem planoItem, final BigDecimal valorAcumuladoSaldo,
			Map<IRegiao, Map<IPlanoItem, BigDecimal>> map) {
		Map<IPlanoItem, BigDecimal> mapPlanoItemSaldo = map.get(regiao);
		if (mapPlanoItemSaldo == null) {
			synchronized (Capital.class) {
				mapPlanoItemSaldo = map.get(regiao);
				if (mapPlanoItemSaldo == null) {
					mapPlanoItemSaldo = new HashMap<>();
					map.put(regiao, mapPlanoItemSaldo);
				}
			}
		}
		mapPlanoItemSaldo.put(planoItem, valorAcumuladoSaldo);
	}

	protected static Map<Premio, BigDecimal> processarPremioRegistroHistorico(IRegiao regiao, IPlanoItem planoItem, Premio premio) {
		Map<IPlanoItem, Map<Premio, BigDecimal>> mapPlanoItem = mapaDeCapitalPorRegiaoPremio.get(regiao);
		if (mapPlanoItem == null) {
			synchronized (Capital.class) {
				mapPlanoItem = mapaDeCapitalPorRegiaoPremio.get(regiao);
				if (mapPlanoItem == null) {
					mapPlanoItem = new HashMap<>();
					mapaDeCapitalPorRegiaoPremio.put(regiao, mapPlanoItem);
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
		return mapPremios;
	}
	
	/** 
	 * Faaz todo o calculo baseado no premio pago.
	 * 
	 * @param regiao
	 * @param planoItem
	 * @param solicitacao
	 */
	public static void processarSoliciataoDeValorDoCapital(IRegiao regiao, IPlanoItem planoItem, Solicitacao solicitacao) {
		if (planoItem instanceof PlanoItemDecorator) {
			planoItem = ((PlanoItemDecorator) planoItem).getPlanoItem();
		}
		
		// Processa calculo do valor do capital
		Map<IPlanoItem, BigDecimal> mapPlanoItemSaldo = mapaDeCapitalPorRegiaoSaldo.get(regiao);
		if (mapPlanoItemSaldo == null) {
			synchronized (Capital.class) {
				mapPlanoItemSaldo = mapaDeCapitalPorRegiaoSaldo.get(regiao);
				if (mapPlanoItemSaldo == null) {
					mapPlanoItemSaldo = new HashMap<>();
					mapaDeCapitalPorRegiaoSaldo.put(regiao, mapPlanoItemSaldo);
					mapPlanoItemSaldo.put(planoItem, new BigDecimal(0));
				}
			}
		}
		
		final BigDecimal pctFatorLimitanteDoCapitalPorCliente = new BigDecimal(Parametros.CAPITAL_MAXIMO_ACESSIVEL_POR_CLIENTE.getValorParametro()).divide(new BigDecimal(100), new MathContext(6));
		final BigDecimal capitalTotal = mapPlanoItemSaldo.get(planoItem);
		final BigDecimal capitalAcessivelPorCliente = capitalTotal.multiply(pctFatorLimitanteDoCapitalPorCliente, new MathContext(6));
		
		if (solicitacao.getValor().compareTo(capitalAcessivelPorCliente) < 0) {
			mapPlanoItemSaldo.put(planoItem, capitalTotal.subtract(solicitacao.getValor()));
		} else {
			throw new IllegalArgumentException("O capital disponivel [" + capitalAcessivelPorCliente + "] não é suficiente para processar esta solicitação de [" + solicitacao.getValor() + "]. Limite do capital por cliente definido em [" + Parametros.CAPITAL_MAXIMO_ACESSIVEL_POR_CLIENTE.getValorParametro() + " %]");
		}

		// Guarda a solicitacao para regisro
		processarSoliciataoDeValorDoCapitalRegistroHistorico(regiao, planoItem, solicitacao);
		
	}

	protected static void processarSoliciataoDeValorDoCapitalRegistroHistorico(IRegiao regiao, IPlanoItem planoItem,
			Solicitacao solicitacao) {
		Map<IPlanoItem, Map<Solicitacao, BigDecimal>> mapPlanoItem = mapaDeCapitalPorRegiaoSolicitacao.get(regiao);
		if (mapPlanoItem == null) {
			synchronized (Capital.class) {
				mapPlanoItem = mapaDeCapitalPorRegiaoSolicitacao.get(regiao);
				if (mapPlanoItem == null) {
					mapPlanoItem = new HashMap<>();
					mapaDeCapitalPorRegiaoSolicitacao.put(regiao, mapPlanoItem);
				}
			}
		}
		Map<Solicitacao, BigDecimal> mapSolicitacoes = mapPlanoItem.get(planoItem);
		if (mapSolicitacoes == null) {
			synchronized (Capital.class) {
				mapSolicitacoes = mapPlanoItem.get(planoItem);
				if (mapSolicitacoes == null) {
					mapSolicitacoes = new HashMap<Solicitacao, BigDecimal>();
					mapPlanoItem.put(planoItem, mapSolicitacoes);
				}
			}
		}
		mapSolicitacoes.put(solicitacao, solicitacao.getValor());
	}
	
}
