package com.csgd.calc;

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.csgd.domain.Cliente;
import com.csgd.domain.IPlanoItem;
import com.csgd.domain.IRegiao;
import com.csgd.domain.Premio;
import com.csgd.domain.Solicitacao;
import com.csgd.domain.ValorCapital;
import com.csgd.fake.Calendario;

public class Calculadora {

	private static volatile Calculadora instance;
	protected final SimpleDateFormat sdfAgrupadorPeriodo = new SimpleDateFormat("MM-yyyy");

	private Calculadora() {
	}

	public static Calculadora getInstance() {
		if (instance == null) {
			synchronized (Calculadora.class) {
				if (instance == null) {
					instance = new Calculadora();
				}
			}
		}
		return instance;
	}

	/**
	 * Para efeito de demonstracao vamos assumir que todas as solicita��es s�o
	 * aprovadas e o capital � descontado.
	 * 
	 * @param solicitacao
	 */
	public synchronized void processarSolicitacao(Solicitacao solicitacao) {
		ValorCapital capitalPorRegiaoEPlanoItem = solicitacao.getCliente().getCapitalPorRegiaoEPlanoItem(solicitacao.getFornecedor().getRegiao(), solicitacao.getPlanoItem());
		if (capitalPorRegiaoEPlanoItem.getCapitalMaximoAcessivelComRestricaoPorCliente().compareTo(solicitacao.getValor()) <= 0) {
			throw new IllegalArgumentException("Capital disponivel [" + capitalPorRegiaoEPlanoItem.getCapitalMaximoAcessivelComRestricaoPorCliente() + "] não é sufuciente para atender a solicitação [" + solicitacao.getValor() + "]");
		}
		// calcula o capital total decrementando o valor da solicitacao
		Capital.processarSoliciataoDeValorDoCapital(solicitacao.getFornecedor().getRegiao(), solicitacao.getPlanoItem(), solicitacao);
		// calcula o capital de cada cliente apos a solicitacao para o plano item
		calcularCapitalIndividual(solicitacao.getFornecedor().getRegiao(), solicitacao.getPlanoItem());
	} 
	
	/**
	 * Processa o premio pago para cada plano item.
	 * 
	 * @param premio
	 */
	public synchronized void processarPremio(Premio premio) {
		// fecha o premio
		premio.close();
		// atualiza o capital total
		premio.getMapPlanoItem().values().stream()
			.forEach(pi -> {
				Capital.adicionarPremioCapital(premio.getRegiao(), pi, premio);
			});
		// calcula o capital de cada cliente apos o premio para os plano itens contidos no premio
		calcularCapitalIndividual(premio);
	}

	/**
	 * Calcula o novo capital individual por cliente para os plano itens do premio. 
	 * @param premio
	 */
	protected void calcularCapitalIndividual(Premio premio) {
		premio.getMapPlanoItem().values().stream().forEach(planoItem -> {
			calcularCapitalIndividual(premio.getRegiao(), planoItem.getPlanoItem());
		});

	}

	/**
	 * Calcula o novo capital individual por cliente para o plano item 
	 * 
	 * @param regiao
	 * @param planoItem
	 */
	protected void calcularCapitalIndividual(IRegiao regiao, IPlanoItem planoItem) {
		if (!regiao.getPlano().getListaDeClientes().isEmpty()) {
			// Mapeia todos os clientes para um objeto auxiliar sobre o qual sera realizado
			// o calculo
			final List<AuxCalc> listOfAuxCalc = regiao.getPlano().getListaDeClientes().stream()
					.map(cliente -> new AuxCalc(cliente, cliente.getPremioAcumulado(regiao, planoItem)))
					.collect(Collectors.toList());

			while (true) {
				// Obtem o menor valor acumulado
				final AuxCalc clienteComMenorValorAcumulado = listOfAuxCalc.stream()
						.filter(auxCalc -> auxCalc.getValor().compareTo(new BigDecimal(0)) > 0)
						.min(Comparator.comparing(auxCalc -> auxCalc.getValor())).orElse(null);
				// Se ja nao houver mais nenhum cliente com valor acima de zero o agrupamenteo
				// chegou ao fim
				if (clienteComMenorValorAcumulado == null) {
					break;
				}
				// Agrupa os clientes para o valor menor no momento
				final BigDecimal menorValorAuxParaOCalculo = clienteComMenorValorAcumulado.getValor();
				listOfAuxCalc.stream().filter(
						auxCalc -> auxCalc.getValor().compareTo(clienteComMenorValorAcumulado.getValor()) >= 0)
						.forEach(auxCalc -> auxCalc.agruparPeloValor(menorValorAuxParaOCalculo));
			}
			
			// Agrupa o valor representativo de cada cliente para que este seja distribuido igualmente entre outros clientes que contribuem dentro do mesmo grupo de valor
			final Map<Integer, List<ValorHistorico>> map = new HashMap<>();
			int n = 0;
			for (int i = 0; i < listOfAuxCalc.size(); i++) {
				while(true) {
					final AuxCalc aux = listOfAuxCalc.get(i);
					if (aux.getHistorico().size() > n) {
						List<ValorHistorico> list = map.get(n);
						if (list == null) {
							list = new ArrayList<>();
							map.put(new Integer(n), list);
						}
						list.add(aux.getHistorico().get(n));
					} else {
						break;
					}
					n++;
				}
				n = 0;
			}

			// processa o calculo baseado no agrupamento e guarda o valor do capital de cada cliente relativo ao plano item
			map.keySet().stream().forEach(index -> {
				final List<ValorHistorico> list = map.get(index);
				int multiplyer = list.size();
				// obtem o valor para o grupo de valor atual
				final BigDecimal protudo = list.get(0).getMenorValor().multiply(new BigDecimal(multiplyer), new MathContext(6));
				// saldo em capital para o plano item na regiao
				final BigDecimal valorCapitalSaldo = Capital.getCapitalPorRegiaoSaldo(regiao, planoItem);
				// total acumulado para determinar o percentual que a contribuicao representa
				final BigDecimal valorCapitalTotal = Capital.getCapitalPorRegiaoTotal(regiao, planoItem);
				// representacao das contribuicao em relacao ao capital total
				final BigDecimal percentualAcesso = protudo.divide(valorCapitalTotal, new MathContext(6));
				// aplica o percentual sobre o saldo do capital
				final BigDecimal valorCapitalAcessivel = valorCapitalSaldo.multiply(percentualAcesso, new MathContext(6));
				list.stream().forEach(c -> {
					if (c.getAuxCalc().listaHistorico.get(index) != null) {
						c.getAuxCalc().acumuladorCalculoDaDistribuicaoDeAcessoProporcionalAoCapitalPorPlanoItem(planoItem, valorCapitalAcessivel);
					}
				});
			});

			listOfAuxCalc.stream().forEach(aux -> aux.getCliente().guardarCalculoDaDistribuicaoDeAcessoProporcionalAoCapitalPorRegiaoEPlanoItem(regiao, planoItem, aux.valorAcumuladorCalculoDaDistribuicaoDeAcessoProporcionalAoCapitalPorPlanoItem(planoItem)));
		}
	}
 
	private class AuxCalc {
		private Cliente cliente;
		private Map<IPlanoItem, BigDecimal> mapDistribuicaoDeAcessoProporcionalAoCapital = new HashMap<IPlanoItem, BigDecimal>();
		private BigDecimal valor;
		private List<ValorHistorico> listaHistorico = new ArrayList<ValorHistorico>();

		AuxCalc(Cliente cliente, BigDecimal valor) {
			this.cliente = cliente;
			this.valor = valor;
		}

		public AuxCalc agruparPeloValor(BigDecimal menorValor) {
			if (new BigDecimal(0).compareTo(getValor()) != 0) {
				listaHistorico.add(new ValorHistorico(this, Calendario.getInstance().getDataAtual(), menorValor, getValor()));
				this.valor = getValor().subtract(menorValor);
			}

			return this;
		}

		public List<ValorHistorico> getHistorico() {
			return listaHistorico;
		}

		public BigDecimal getValor() {
			return valor;
		}

		public Cliente getCliente() {
			return cliente;
		}
		
		public void acumuladorCalculoDaDistribuicaoDeAcessoProporcionalAoCapitalPorPlanoItem(IPlanoItem planoItem, BigDecimal valor) {
			BigDecimal valorAcumulado = mapDistribuicaoDeAcessoProporcionalAoCapital.get(planoItem);
			if (valorAcumulado == null) {
				valorAcumulado = new BigDecimal(0);
			}
			valorAcumulado = valorAcumulado.add(valor);
			mapDistribuicaoDeAcessoProporcionalAoCapital.put(planoItem, valorAcumulado);
		}
		
		public BigDecimal valorAcumuladorCalculoDaDistribuicaoDeAcessoProporcionalAoCapitalPorPlanoItem(IPlanoItem planoItem) {
			return mapDistribuicaoDeAcessoProporcionalAoCapital.get(planoItem);
		}
	}

	private class ValorHistorico {
		private AuxCalc auxCalc;
		private Date dataPeriodo;
		private BigDecimal menorValor;
		private BigDecimal valorAnterior;

		ValorHistorico(AuxCalc auxCalc, Date dataPeriodo, BigDecimal menorValor, BigDecimal valorAnterior) {
			this.auxCalc = auxCalc;
			this.dataPeriodo = dataPeriodo;
			this.menorValor = menorValor;
			this.valorAnterior = valorAnterior;
		}
		
		public AuxCalc getAuxCalc() {
			return auxCalc;
		}

		public BigDecimal getMenorValor() {
			return menorValor;
		}

		public BigDecimal getValorAnterior() {
			return valorAnterior;
		}

		public Date getDataPeriodo() {
			return dataPeriodo;
		}
	}

}
