package com.csgd.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.csgd.calc.Capital;

public class Cliente extends Pessoa {

	private LinkedList<Premio> listaDePremiosPagos = new LinkedList<Premio>();
	private List<Solicitacao> listaDeSolicitacoes = new ArrayList<Solicitacao>();

	private Map<IRegiao, Map<IPlanoItem, IPlanoItem>> mapPlanoItem = new HashMap<IRegiao, Map<IPlanoItem, IPlanoItem>>();
	private Map<IRegiao, Map<IPlanoItem, ValorCapital>> mapPlanoItemValor = new HashMap<IRegiao, Map<IPlanoItem, ValorCapital>>();

	private boolean clienteParouDePagar = false;
	private byte diaLimiteDePagamentoDoPremio = 15;

	public Cliente(String nome) {
		super(nome);
	}

	public boolean isClienteParouDePagar() {
		return clienteParouDePagar;
	}
	
	/**
	 * Valor em percentual da variacao dos premios pagos nos periodos anteriores.
	 * 
	 * @return
	 */
	public BigDecimal getVariacao(IRegiao regiao, IPlanoItem planoItem) {
		BigDecimal result = new BigDecimal(0);
		if (!listaDePremiosPagos.isEmpty()) {
			final BigDecimal mediaPremiosPagos = getMediaPremiosPagos(regiao, planoItem);
			if (mediaPremiosPagos.compareTo(new BigDecimal(0)) > 0) {
				final BigDecimal mediaPremios = getMediaPremios(regiao, planoItem);
				if (mediaPremios.compareTo(mediaPremiosPagos) > 0) {
					result = mediaPremiosPagos.divide(mediaPremios, new MathContext(16, RoundingMode.HALF_UP));
				}
			}
		}

		return result;
	}

	public void guardarCalculoDaDistribuicaoDeAcessoProporcionalAoCapitalPorRegiaoEPlanoItem(IRegiao regiao,
			IPlanoItem planoItem, BigDecimal valor) {
		BigDecimal valorCapitalTotal = valor;

		Map<IPlanoItem, ValorCapital> mapRegiao = mapPlanoItemValor.get(regiao);
		if (mapRegiao == null) {
			mapPlanoItemValor.put(regiao, mapRegiao = new HashMap<IPlanoItem, ValorCapital>());
		}
		BigDecimal variacao = getVariacao(regiao, planoItem);
		if (variacao.compareTo(new BigDecimal(0)) != 0) {
			valor = valor.multiply(variacao, new MathContext(16, RoundingMode.HALF_UP));
		}
		PlanoItemParametro planoItemParametro = null;
		if (!(planoItem instanceof PlanoItemParametro)) {
			planoItemParametro = (PlanoItemParametro) mapPlanoItem.get(regiao).get(planoItem);
		} else {
			planoItemParametro = (PlanoItemParametro) planoItem;
		}
		if (planoItemParametro.getControleDeCarencia() > 0) {
			valor = new BigDecimal(0);
		} else if (planoItemParametro.getControleDeEntrada() < planoItemParametro
				.getControleDeEntradaDefinicaoInicial()) {
			final BigDecimal pctEntrada = new BigDecimal(
					Math.pow(new Double(planoItemParametro.getControleDeEntrada()), 2))
							.divide(new BigDecimal(
									Math.pow(new Double(planoItemParametro.getControleDeEntradaDefinicaoInicial()), 2)),
									new MathContext(16, RoundingMode.HALF_UP));
			valor = valor.multiply(pctEntrada, new MathContext(16, RoundingMode.HALF_UP));
		}
		mapRegiao.put(planoItem, new ValorCapital(this, valor, valorCapitalTotal));
	}

	public PlanoItemParametro getPlanoItemParametro(IRegiao regiao, IPlanoItem planoItem) {
		return (PlanoItemParametro) mapPlanoItem.get(regiao).get(planoItem);
	}

	/**
	 * Valor em percentual da variacao dos premios pagos nos periodos anteriores.
	 * 
	 * @return
	 */
	public ValorCapital getCapitalPorRegiaoEPlanoItem(IRegiao regiao, IPlanoItem planoItem) {
		ValorCapital result = null;
		Map<IPlanoItem, ValorCapital> mapRegiao = mapPlanoItemValor.get(regiao);
		if (mapRegiao == null) {
			result = new ValorCapital(this, new BigDecimal(0), new BigDecimal(0));
		} else {
			result = mapRegiao.get(planoItem);
			if (result == null) {
				result = new ValorCapital(this, new BigDecimal(0), new BigDecimal(0));
			}
		}

		return result;
	}

	/**
	 * Traz o valor acumulado dos pr�mios.
	 * 
	 * @return
	 */
	public BigDecimal getPremioAcumulado(IRegiao regiao, IPlanoItem planoItem) {
		BigDecimal result = new BigDecimal(0);
		if (!listaDePremiosPagos.isEmpty()) {
			result = listaDePremiosPagos.stream().filter(premio -> premio.getRegiao().equals(regiao))
					.filter(premio -> premio.getMapPlanoItem().containsKey(planoItem))
					.map(premio -> premio.getValor().multiply(
							((PlanoItemParametro) premio.getMapPlanoItem().get(planoItem)).getPercentualAlocacao()))
					.reduce(BigDecimal::add).orElse(new BigDecimal(0));
		}

		return result;
	}

	/**
	 * Retorna a m�dia de todos os premios na lista.
	 * 
	 * @return
	 */
	public BigDecimal getMediaPremios(IRegiao regiao, IPlanoItem planoItem) {
		BigDecimal result = new BigDecimal(0);

		if (!listaDePremiosPagos.isEmpty()) {
			final BigDecimal valorAumuladoNosPremiosFechados = listaDePremiosPagos.stream()
					.filter(premio -> premio.getRegiao().equals(regiao))
					.filter(premio -> premio.getMapPlanoItem().containsKey(planoItem)).map(premio -> premio.getValor())
					.reduce(BigDecimal::add).orElse(new BigDecimal(0));

			result = valorAumuladoNosPremiosFechados.divide(new BigDecimal(listaDePremiosPagos.size()),
					new MathContext(16, RoundingMode.HALF_UP));
		}

		return result;
	}

	/**
	 * Retorna a m�dia dos premios sem o ultimo.
	 * 
	 * @return
	 */
	public BigDecimal getMediaPremiosPagos(IRegiao regiao, IPlanoItem planoItem) {
		BigDecimal result = new BigDecimal(0);

		if (listaDePremiosPagos.size() > 1) {
			final BigDecimal valorAumuladoNosPremiosFechados = listaDePremiosPagos.stream()
					.filter(premio -> !listaDePremiosPagos.getLast().equals(premio))
					.filter(premio -> premio.getRegiao().equals(regiao))
					.filter(premio -> premio.getMapPlanoItem().containsKey(planoItem)).map(premio -> premio.getValor())
					.reduce(BigDecimal::add).orElse(new BigDecimal(0));

			result = valorAumuladoNosPremiosFechados.divide(new BigDecimal(listaDePremiosPagos.size() - 1),
					new MathContext(16, RoundingMode.HALF_UP));
		}

		return result;
	}

	/**
	 * Adiciona o premio como o ultimo da lista.
	 * 
	 * @param premio
	 */
	public void adicionarPremio(Premio premio) {
		if (premio.isClosed()) {
			throw new IllegalArgumentException("Premio n�o pode estar fechado no momento da adi��o a lista.");
		}
		final Map<IPlanoItem, IPlanoItem> mPlanoItem = this.mapPlanoItem.get(premio.getRegiao());
		mPlanoItem.values().stream().forEach(planoItem -> {
			if (planoItem instanceof PlanoItemParametro) {
				((PlanoItemParametro) planoItem).processarAvancoDePagamentoEPeriodo();
			}
		});
		this.listaDePremiosPagos.addLast(premio);
	}
	
	public void retrocederEntradaPorFaltaDePagamentoDoPremio(Regiao regiao) {
		 this.clienteParouDePagar= true;
		final Map<IPlanoItem, IPlanoItem> mPlanoItem = this.mapPlanoItem.get(regiao);
		mPlanoItem.values().stream().forEach(planoItem -> {
			if (planoItem instanceof PlanoItemParametro) {
				((PlanoItemParametro) planoItem).retrocederEntrada();
			}
		}); 
	}
	
	/**
	 * Adiciona a solicitacao a lista de solicitacoes do cliente.
	 * 
	 * @param solicitacao
	 * @return
	 */
	public boolean adicionarSolicitacao(Solicitacao solicitacao) {
		boolean result = true;
		
		if (getControleDeCarencia(solicitacao.getFornecedor().getRegiao(), solicitacao.getPlanoItem()) > 0) {
			result = false;
		} else {
			this.listaDeSolicitacoes.add(solicitacao);
		}
		
		return result;
	}

	public byte getControleDeEntrada(IRegiao regiao, IPlanoItem pi) {
		byte result = -1;

		final Map<IPlanoItem, IPlanoItem> mapRegiao = this.mapPlanoItem.get(regiao);
		final IPlanoItem planoItem = mapRegiao.get(pi);
		if (planoItem instanceof PlanoItemParametro) {
			result = ((PlanoItemParametro) planoItem).getControleDeEntrada();
		}

		return result;
	}

	public byte getControleDeCarencia(IRegiao regiao, IPlanoItem pi) {
		byte result = -1;

		final Map<IPlanoItem, IPlanoItem> mapRegiao = this.mapPlanoItem.get(regiao);
		final IPlanoItem planoItem = mapRegiao.get(pi);
		if (planoItem instanceof PlanoItemParametro) {
			result = ((PlanoItemParametro) planoItem).getControleDeCarencia();
		}

		return result;
	}

	/**
	 * Adiciona o Plano-Item ao plano do cliente.
	 * 
	 * @param regiao
	 * @param plano
	 * @param planoItem
	 */
	public void addPlanoItem(IRegiao regiao, Plano plano, IPlanoItem planoItem) {
		// Utiliza o PlanoItem original como chave
		IPlanoItem key = planoItem;
		if (planoItem instanceof PlanoItemDecorator) {
			key = ((PlanoItemDecorator) planoItem).getPlanoItem();
		}

		// Verifica se o plano reconhece o plano item
		if (!plano.contemPlanoItem(key)) {
			throw new IllegalArgumentException(
					"O plano item [" + planoItem.getNome() + "] n�o � reconhecido pelo plano");
		}

		// Verifica se o percentual � valido
		if (planoItem instanceof PlanoItemParametro) {
			verificarPercentual(regiao, (PlanoItemParametro) planoItem);
		}

		// Adiciona o cliente ao plano
		plano.adicionarClienteAoPlano(this);

		// Adiciona ao PlanoItem
		Map<IPlanoItem, IPlanoItem> mapaDaRegiao = mapPlanoItem.get(regiao);
		if (mapaDaRegiao == null) {
			mapaDaRegiao = new HashMap<IPlanoItem, IPlanoItem>();
			this.mapPlanoItem.put(regiao, mapaDaRegiao);
		}

		mapaDaRegiao.put(key, planoItem);
	}

	/**
	 * Verifica se o percentual de alocacao esta coerente.
	 * 
	 * @param regiao
	 * @param planoItem
	 * @return
	 */
	private boolean verificarPercentual(IRegiao regiao, PlanoItemParametro planoItem) {
		boolean result = true;
		if (!mapPlanoItem.isEmpty()) {
			final Map<IPlanoItem, IPlanoItem> mapaDaRegiao = mapPlanoItem.get(regiao);

			if (!mapaDaRegiao.isEmpty()) {
				BigDecimal totalPresente = mapaDaRegiao.values().stream().filter(o -> o instanceof PlanoItemParametro)
						.map(pi -> ((PlanoItemParametro) pi).getPercentualAlocacao()).reduce(BigDecimal::add)
						.orElse(new BigDecimal(0));

				if (totalPresente.add(planoItem.getPercentualAlocacao()).compareTo(new BigDecimal(100)) > 0) {
					throw new IllegalArgumentException("Valor atual alocado nos itens do plano � de [" + totalPresente
							+ "], para alocar este plano � preciso reconfigurar os demais.");
				}
			}
		}

		return result;
	}

	/**
	 * Cria uma c�pia da configura��o dos itens do plano.
	 * 
	 * @param regiao
	 * @return
	 */
	public Map<IPlanoItem, IPlanoItem> getMapPlanoItemCopy(IRegiao regiao) {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		final Map<IPlanoItem, IPlanoItem> map = (Map<IPlanoItem, IPlanoItem>) ((HashMap) mapPlanoItem.get(regiao))
				.clone();

		return map;
	}

	public void setDiaLimiteDePagamentoDoPremio(byte diaLimiteDePagamentoDoPremio) {
		this.diaLimiteDePagamentoDoPremio = diaLimiteDePagamentoDoPremio;
	}

	public byte getDiaLimiteDePagamentoDoPremio() {
		return diaLimiteDePagamentoDoPremio;
	}

	public List<Solicitacao> getListaDeSolicitacoes() {
		return Collections.unmodifiableList(listaDeSolicitacoes);
	}

	@Override
	public String toString() {
		return getNome();
	}
}