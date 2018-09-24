package com.csgd.simulador;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import com.csgd.calc.Calculadora;
import com.csgd.calc.Capital;
import com.csgd.domain.Cliente;
import com.csgd.domain.Fornecedor;
import com.csgd.domain.IPlanoItem;
import com.csgd.domain.Plano;
import com.csgd.domain.PlanoItem;
import com.csgd.domain.PlanoItemParametro;
import com.csgd.domain.Premio;
import com.csgd.domain.Regiao;
import com.csgd.domain.Solicitacao;
import com.csgd.domain.ValorCapital;
import com.csgd.fake.Calendario;

public class Simulador {
	
	private static final int NUMERO_DE_CLIENTES_INICIAL_SIMULAR = 10; // quantidade inicial de clients
	private static final int PERCENTUAL_DE_INCREMENTO_DE_CLIENTES_MES_A_MES_SIMULAR = 5; // gerar novos clientes
	private static final int PERCENTUAL_DE_CLIENTES_QUE_PARAM_DE_PAGAR = 2; // clientes que param de pagar para sempre
	private static final int VALOR_PARCELA_MEDIA_SIMULAR = 20; // valor base da parcela 
	private static final int VALOR_PARCELA_VARIACAO_SIMULAR = 1; // variacao do valor da parcela
	private static final int VALOR_SOLICITACAO_SIMULAR = 180; // 90% 
	private static final int PERCENTUAL_DE_CLIENTES_COM_SINISTROS = 10; // 10% clientes vão gastar 90% do capital dos premios
	private static final int NUMERO_DE_MESES_SIMULAR = 36; // numero de periodos
	
	private SimpleDateFormat sdf = new SimpleDateFormat("MM-yyyy");
	
	private Map<Date, Map<IPlanoItem, ReportCapital>> mapSaldoCapitalNoPeriodo = new HashMap<Date, Map<IPlanoItem, ReportCapital>>();
	private Map<Cliente, Map<IPlanoItem, Map<Date, Report>>> map = new HashMap<Cliente, Map<IPlanoItem, Map<Date, Report>>>();
	private Set<Date> setDate = new HashSet<>();

	public static void main(String[] args) {
		new Simulador().simular();
	}
	
	public void simular() {
		Random rand = new Random();

		// Calendario
		final Calendario cal = Calendario.getInstance();		
		cal.definirDataInicial(new Date());
		
		// Itens do Plano
		IPlanoItem planoItemConsultas = new PlanoItem("Consultas");

		// Montagem do Plano
		Plano planoBrasil = new Plano();
		planoBrasil.addPlanoItem(planoItemConsultas);
		
		// Regiao
		Regiao regiaoBrasil = new Regiao("Brasil", planoBrasil);		 

		// Clientes iniciais
		List<Cliente> listaDeClientes = criarClienteIniciais(planoItemConsultas, planoBrasil, regiaoBrasil);
		
		// Definir data inicial
		Calendario.getInstance().definirDataInicial(new Date());

		// Premios
		int meses = NUMERO_DE_MESES_SIMULAR;
		while (meses > 0) {
			System.out.println("Data Atual: " + sdf.format(Calendario.getInstance().getDataAtual()) + " - Num. de Clientes [" + listaDeClientes.size() + "]" );
			setDate.add(Calendario.getInstance().getDataAtual());

			// Criar novos clientes
			criarNovosClientes(planoItemConsultas, planoBrasil, regiaoBrasil, listaDeClientes, meses);
			
			// Pagamento de premios
			pagamentoDePremios(rand, regiaoBrasil, listaDeClientes);
			
			// Solicitacoes
			boolean aborted = false;
			// espera alguns meses antes de iniciar a geracao de solicitacoes
//			if (meses < NUMERO_DE_MESES_SIMULAR-12) { 
//				aborted = gerarSolicitacoes2(planoBrasil, regiaoBrasil, listaDeClientes);
//			}
			
			planoBrasil.getListaDeItensDoPlano().stream()
			.forEach(planoItem -> planoBrasil.getListaDeClientes().stream()
					.forEach(cli -> {
						final byte controleDeCarencia = cli.getControleDeCarencia(regiaoBrasil, planoItem);
						final byte controleDeEntrada = cli.getControleDeEntrada(regiaoBrasil, planoItem);
						final BigDecimal variacao = cli.getVariacao(regiaoBrasil, planoItem);
						final BigDecimal mediaPremios = cli.getMediaPremios(regiaoBrasil, planoItem);
						final ValorCapital valorCapital = cli.getCapitalPorRegiaoEPlanoItem(regiaoBrasil, planoItem);
						final BigDecimal premiosPagos = cli.getPremioAcumulado(regiaoBrasil, planoItem);
						
						Map<IPlanoItem, Map<Date, Report>> mapPlanoItem = map.get(cli);
						if (mapPlanoItem == null) {
							map.put(cli, mapPlanoItem = new HashMap<IPlanoItem, Map<Date, Report>>());
						}
						Map<Date, Report> mapDate = mapPlanoItem.get(planoItem);
						if (mapDate == null) {
							mapPlanoItem.put(planoItem, mapDate = new HashMap<Date, Report>());
						}
						Report report = mapDate.get(Calendario.getInstance().getDataAtual());
						if (report == null) {
							mapDate.put(Calendario.getInstance().getDataAtual(), report = new Report());
						}
					 
						report.controleDeCarencia = controleDeCarencia;
						report.controleDeEntrada = controleDeEntrada;
						report.variacao = variacao;
						report.mediaPremios = mediaPremios;
						report.valorCapital = valorCapital;
						report.premiosPagos = premiosPagos;
						final Report this_report = report;
						
						cli.getListaDeSolicitacoes().stream()
						.filter(soli -> soli.getDataSolicitacao().equals(Calendario.getInstance().getDataAtual()))
						.forEach(soli -> this_report.solicitacao = soli.getValor());
					}));

			planoBrasil.getListaDeItensDoPlano().stream()
				.forEach(planoItem -> { 
					 Map<IPlanoItem, ReportCapital> m = mapSaldoCapitalNoPeriodo.get(Calendario.getInstance().getDataAtual());
					 if (m == null) {
						 mapSaldoCapitalNoPeriodo.put(Calendario.getInstance().getDataAtual(), m = new HashMap<IPlanoItem, ReportCapital>());
					 }
					m.put(planoItem, new ReportCapital(Capital.getCapitalPorRegiaoSaldo(regiaoBrasil, planoItem), Capital.getCapitalPorRegiaoTotal(regiaoBrasil, planoItem)));
				});
			
			if (aborted) {
				break;
			}
			
			// controle laço
			meses--;
			Calendario.getInstance().incrementarEm1Mes();
		}
 
		// REPORT 1 
		System.out.println("REPORT 1 - Capital Acessivel Por Cliente");
		StringBuilder csv = new StringBuilder();
		csv.append("Cliente;");
		
		// ordena as datas
		List<Date> tempList = new ArrayList<Date>(setDate);
		Collections.sort(tempList);
		// imprime as datas
		tempList.stream().forEachOrdered(data -> csv.append(sdf.format(data)+";"));
		csv.append("\n");
		// ordena os clientes
		final Set<Entry<Cliente, Map<IPlanoItem, Map<Date, Report>>>> entrySet = map.entrySet();
		List<Entry<Cliente, Map<IPlanoItem, Map<Date, Report>>>> entrySetList = new ArrayList<Entry<Cliente, Map<IPlanoItem, Map<Date, Report>>>>(entrySet);
		Collections.sort(entrySetList, new Comparator<Entry<Cliente, Map<IPlanoItem, Map<Date, Report>>>>() {
			public int compare(Map.Entry<Cliente,Map<IPlanoItem, java.util.Map<Date,Report>>> o1, Map.Entry<Cliente,Map<IPlanoItem, java.util.Map<Date,Report>>> o2) {
				return new Integer(o1.getKey().getNome().split(" ")[1]).compareTo(new Integer(o2.getKey().getNome().split(" ")[1]));
			};
		});
		// imprime os clientes
		entrySetList.stream().forEach(es -> {
			csv.append(es.getKey().getNome()+";");
			tempList.stream().forEach(data -> {
				Report r = es.getValue().get(planoItemConsultas).get(data);
				if (r == null) {
					csv.append(";");
				} else {
					csv.append(r.valorCapital.getCapitalMaximoAcessivelComRestricaoPorCliente().toString().replaceAll("\\.", ",")+";");
				}
			});
			csv.append("\n");
		});		 
		
		System.out.println(csv);
		System.out.println("Premios pagos");
		StringBuilder csvPremiosPagos = new StringBuilder();
		entrySetList.stream().forEach(es -> {
			csvPremiosPagos.append(es.getKey().getNome()+";");
			tempList.stream().forEach(data -> {
				Report r = es.getValue().get(planoItemConsultas).get(data);
				if (r == null) {
					csvPremiosPagos.append(";");
				} else {
					csvPremiosPagos.append(r.premiosPagos.toString().replaceAll("\\.", ",")+";");
				}
			});
			csvPremiosPagos.append("\n");
		});		 
		
		System.out.println(csvPremiosPagos);
		
		// REPORT 2
		System.out.println("REPORT 2");
		StringBuilder csv2 = new StringBuilder();
		csv2.append("\nPlano Item;");
		tempList.stream().forEachOrdered(data -> csv2.append(sdf.format(data) + ";"));
		planoBrasil.getListaDeItensDoPlano().stream().forEach(planoItem -> {
			csv2.append("\n" + planoItem.getNome() + " Saldo;");
			tempList.stream().forEachOrdered(data -> {
				csv2.append(mapSaldoCapitalNoPeriodo.get(data).get(planoItem).getCapitalPorRegiaoSaldo().setScale(4,  BigDecimal.ROUND_HALF_UP).toString().replaceAll("\\.", ",") + ";");
			});
		});
		System.out.println(csv2);
		
		
		
		// REPORT 3
//		System.out.println("REPORT 3");
		StringBuilder csv3 = new StringBuilder();
//		csv3.append("\nPlano Item;");
//		tempList.stream().forEachOrdered(data -> csv3.append(sdf.format(data) + ";"));
		planoBrasil.getListaDeItensDoPlano().stream().forEach(planoItem -> {
			csv3.append("\n" + planoItem.getNome() + " Total;");
			tempList.stream().forEachOrdered(data -> {
				csv3.append(mapSaldoCapitalNoPeriodo.get(data).get(planoItem).getCapitalPorRegiaoTotal().setScale(4,  BigDecimal.ROUND_HALF_UP).toString().replaceAll("\\.", ",") + ";");
			});
		});
		System.out.println(csv3);
		 
	}

	protected void criarNovosClientes(IPlanoItem planoItemConsultas, Plano planoBrasil, Regiao regiaoBrasil,
			List<Cliente> listaDeClientes, int meses) {
		final int qtdClientesAtuais = listaDeClientes.size();
		final int qtdIncrementoDeClientes = Math.round(qtdClientesAtuais*PERCENTUAL_DE_INCREMENTO_DE_CLIENTES_MES_A_MES_SIMULAR/100f);
		for (int i=0; meses > 0 && i < qtdIncrementoDeClientes;i++) {
			final int id = qtdClientesAtuais+i;
			Cliente cli = new Cliente("Cliente " + id );
			int  n = 100;// rand.nextInt(50) + 20;
			cli.addPlanoItem(regiaoBrasil, planoBrasil, new PlanoItemParametro(planoItemConsultas, new BigDecimal(n)));
//				cli.addPlanoItem(regiaoBrasil, planoBrasil, new PlanoItemParametro(planoItemExames, new BigDecimal(100-n)));
			
			listaDeClientes.add(cli);
		}
	}

	protected void pagamentoDePremios(Random rand, Regiao regiaoBrasil, List<Cliente> listaDeClientes) {
		for (Cliente cli: listaDeClientes) {
			final int pct = rand.nextInt(100) + 1;
			if (cli.isClienteParouDePagar()) {
				cli.retrocederEntradaPorFaltaDePagamentoDoPremio(regiaoBrasil);
			} else if (pct <= PERCENTUAL_DE_CLIENTES_QUE_PARAM_DE_PAGAR) {
				cli.retrocederEntradaPorFaltaDePagamentoDoPremio(regiaoBrasil);
			} else {
				final BigDecimal valorPremio = VALOR_PARCELA_VARIACAO_SIMULAR == 0 ? 
						new BigDecimal(VALOR_PARCELA_MEDIA_SIMULAR) 
						: new BigDecimal(rand.nextInt(VALOR_PARCELA_VARIACAO_SIMULAR + 1) + VALOR_PARCELA_MEDIA_SIMULAR);
				Premio premio = new Premio(regiaoBrasil, cli, valorPremio);
				cli.adicionarPremio(premio);
				Calculadora.getInstance().processarPremio(premio);
			}
		}
	}

	protected List<Cliente> criarClienteIniciais(IPlanoItem planoItemConsultas, Plano planoBrasil,
			Regiao regiaoBrasil) {
		List<Cliente> listaDeClientes = new ArrayList<>();
		for (int i=0; i<NUMERO_DE_CLIENTES_INICIAL_SIMULAR;i++) {
			Cliente cli = new Cliente("Cliente " + i);
			int  n = 100; //rand.nextInt(50) + 20;
			cli.addPlanoItem(regiaoBrasil, planoBrasil, new PlanoItemParametro(planoItemConsultas, new BigDecimal(n)));
//			cli.addPlanoItem(regiaoBrasil, planoBrasil, new PlanoItemParametro(planoItemExames, new BigDecimal(100-n)));
			
			listaDeClientes.add(cli);
		}
		return listaDeClientes;
	}

	protected void gerarSolicitacoes(Plano planoBrasil, Regiao regiaoBrasil, List<Cliente> listaDeClientes) {
		planoBrasil.getListaDeItensDoPlano().stream()
			.forEach(planoItem -> {
				// probabilidade de requerer uma solicitacao
				BigDecimal capitalPorRegiaoSaldoProbabilidade = Capital.getCapitalPorRegiaoSaldo(regiaoBrasil, planoItem)
						.multiply(new BigDecimal(PERCENTUAL_DE_CLIENTES_COM_SINISTROS).divide(new BigDecimal(100), new MathContext(16, RoundingMode.HALF_UP)), new MathContext(16, RoundingMode.HALF_UP));
				System.out.println(Capital.getCapitalPorRegiaoSaldo(regiaoBrasil, planoItem) + " 20% -" + capitalPorRegiaoSaldoProbabilidade);
				for (Cliente cli: listaDeClientes) {
					if (capitalPorRegiaoSaldoProbabilidade.compareTo(new BigDecimal(0)) <= 0) {
						break;
					}
					ValorCapital vc = cli.getCapitalPorRegiaoEPlanoItem(regiaoBrasil, planoItem);
	
					if (vc.getCapitalMaximoAcessivelComRestricaoPorCliente().compareTo(new BigDecimal(0)) > 0) {
						BigDecimal valorSolicitacao = null;
						if (capitalPorRegiaoSaldoProbabilidade.compareTo(vc.getCapitalMaximoAcessivelComRestricaoPorCliente()) <= 0) {
							valorSolicitacao = capitalPorRegiaoSaldoProbabilidade;
						} else {
							valorSolicitacao = vc.getCapitalMaximoAcessivelComRestricaoPorCliente();
						}
						capitalPorRegiaoSaldoProbabilidade = capitalPorRegiaoSaldoProbabilidade.subtract(valorSolicitacao);
						Solicitacao solicitacao = new Solicitacao(new Fornecedor("Medico", regiaoBrasil), cli, planoItem, Calendario.getInstance().getDataAtual(), valorSolicitacao);
						cli.adicionarSolicitacao(solicitacao);
						// processar a solicitacao
						try {
							Calculadora.getInstance().processarSolicitacao(solicitacao); 
						} catch (Exception e) {
							System.err.println(e.getMessage());
						}
					}
				}
			});
	}
	
	protected boolean gerarSolicitacoes2(Plano planoBrasil, Regiao regiaoBrasil, List<Cliente> listaDeClientes) {
		boolean result = false;
		try {
			Random rand = new Random();
			planoBrasil.getListaDeItensDoPlano().stream()
				.forEach(planoItem -> {
					int array = (int) new BigDecimal(listaDeClientes.size()).multiply(new BigDecimal(PERCENTUAL_DE_CLIENTES_COM_SINISTROS).divide(new BigDecimal(100), new MathContext(16, RoundingMode.HALF_UP)), new MathContext(16, RoundingMode.HALF_UP)).longValue();
					Cliente[] clientesGasto = new Cliente[array];
					while(hasEmpty(clientesGasto) >= 0) {
						boolean loop = true;
						Cliente clienteAtual = null;
						while(loop) {
							int ind = rand.nextInt(listaDeClientes.size());
							loop = containsCliente(clientesGasto, clienteAtual = listaDeClientes.get(ind));
						}
						ValorCapital vc = clienteAtual.getCapitalPorRegiaoEPlanoItem(regiaoBrasil, planoItem);
						final BigDecimal valSolic = new BigDecimal(VALOR_SOLICITACAO_SIMULAR);
						if (vc.getCapitalMaximoAcessivelComRestricaoPorCliente().compareTo(valSolic) > 0) {
							clientesGasto[hasEmpty(clientesGasto)] = clienteAtual;
							BigDecimal valorSolicitacao = valSolic;
							Solicitacao solicitacao = new Solicitacao(new Fornecedor("Medico", regiaoBrasil), clienteAtual, planoItem, Calendario.getInstance().getDataAtual(), valorSolicitacao);
							clienteAtual.adicionarSolicitacao(solicitacao);
							// processar a solicitacao
							try {
								Calculadora.getInstance().processarSolicitacao(solicitacao); 
							} catch (Exception e) {
								System.err.println(e.getMessage());
							}
						} else {
//							if (clienteAtual.getControleDeEntrada(regiaoBrasil, planoItem) >= 24) {
//								System.out.println(clienteAtual.getControleDeEntrada(regiaoBrasil, planoItem) + " " + vc.getCapitalMaximoAcessivelComRestricaoPorCliente());
//								throw new IllegalArgumentException();
//							} else {
//								System.out.println("Ignorando solicitacao" + " " + vc.getCapitalMaximoAcessivelComRestricaoPorCliente());
//								clientesGasto[hasEmpty(clientesGasto)] = clienteAtual;
//							}
							System.out.println(clienteAtual.getControleDeEntrada(regiaoBrasil, planoItem) + " " + vc.getCapitalMaximoAcessivelComRestricaoPorCliente());
							throw new IllegalArgumentException();
						}
					}
				});
		} catch (Exception e) {
			result = true;
		}
		
		return result;
	}
		
	private boolean containsCliente(Cliente[] arr, Cliente cliente) {
		boolean result = false;
	
		for (Cliente c : arr) {
			if(c != null && c.equals(cliente)) {
				result = true;
				break;
			}
		}
		
		return result;
	}
	
	private int hasEmpty(Cliente[] arr) {
		int result = -1;
	
		for (int i=0; i<arr.length; i++) {
			if(arr[i] == null) {
				result = i;
				break;
			}
		}
		
		return result;
	}
	
	public void simular2() {
		
		// Calendario
		final Calendario cal = Calendario.getInstance();		
		cal.definirDataInicial(new Date());

		// Itens do Plano
		IPlanoItem planoItemConsultas = new PlanoItem("Consultas");
		IPlanoItem planoItemExames = new PlanoItem("Exames");
		IPlanoItem planoItemProcedimentosClinicos = new PlanoItem("Procedimentos Clinicos");

		// Montagem do Plano
		Plano planoBrasil = new Plano();
		planoBrasil.addPlanoItem(planoItemConsultas);
		planoBrasil.addPlanoItem(planoItemExames);
		Plano planoEuropa = new Plano();
		planoEuropa.addPlanoItem(planoItemProcedimentosClinicos);
		planoEuropa.addPlanoItem(planoItemExames);
		
		// Regiao
		Regiao regiaoBrasil = new Regiao("Brasil", planoBrasil);
//		Regiao regiaoEuropa = new Regiao("Europa", planoBrasil);

		// Clientes
		Cliente clienteEduardo = new Cliente("Eduardo");
		clienteEduardo.addPlanoItem(regiaoBrasil, planoBrasil, new PlanoItemParametro(planoItemConsultas, new BigDecimal(60)));
		clienteEduardo.addPlanoItem(regiaoBrasil, planoBrasil, new PlanoItemParametro(planoItemExames, new BigDecimal(40)));
		
		Cliente clienteMayara = new Cliente("Mayara");
		clienteMayara.addPlanoItem(regiaoBrasil, planoBrasil, new PlanoItemParametro(planoItemConsultas, new BigDecimal(50)));
		clienteMayara.addPlanoItem(regiaoBrasil, planoBrasil, new PlanoItemParametro(planoItemExames, new BigDecimal(50)));

		// Premios
		// -- Periodo 1
		Calendario.getInstance().definirDataInicial(new Date());
		Premio premio1 = new Premio(regiaoBrasil, clienteEduardo, new BigDecimal(150));
		clienteEduardo.adicionarPremio(premio1);
		Calculadora.getInstance().processarPremio(premio1);		
		
		Premio premioMayara1 = new Premio(regiaoBrasil, clienteMayara, new BigDecimal(200));
		clienteMayara.adicionarPremio(premioMayara1);
		Calculadora.getInstance().processarPremio(premioMayara1);
		
		// -- Periodo 2
		Calendario.getInstance().incrementarEm1Mes();
		Premio premio2 = new Premio(regiaoBrasil, clienteEduardo, new BigDecimal(150));
		clienteEduardo.adicionarPremio(premio2);
		Calculadora.getInstance().processarPremio(premio2);
		
		Premio premioMayara2 = new Premio(regiaoBrasil, clienteMayara, new BigDecimal(200));
		clienteMayara.adicionarPremio(premioMayara2);
		Calculadora.getInstance().processarPremio(premioMayara2);
		
		System.out.println("Capital - Regi�o Brasil - planoItemConsultas: " + Capital.getCapitalPorRegiaoSaldo(regiaoBrasil, planoItemConsultas));
		System.out.println("Capital - Regi�o Brasil - planoItemConsultas: " + Capital.getCapitalPorRegiaoSaldo(regiaoBrasil, planoItemExames));

		System.out.println("Eduardo - planoItemConsultas : " + clienteEduardo.getPremioAcumulado(regiaoBrasil, planoItemConsultas));
		System.out.println("Eduardo - planoItemExames : " + clienteEduardo.getPremioAcumulado(regiaoBrasil, planoItemExames));
		System.out.println("Mayara - planoItemConsultas : " + clienteMayara.getPremioAcumulado(regiaoBrasil, planoItemConsultas));
		System.out.println("Mayara - planoItemExames : " + clienteMayara.getPremioAcumulado(regiaoBrasil, planoItemExames));
		
		
	}
	
	private class Report {
		byte controleDeCarencia;
		byte controleDeEntrada;
		BigDecimal variacao;
		BigDecimal mediaPremios;
		ValorCapital valorCapital;
		BigDecimal premiosPagos;
		
		public BigDecimal solicitacao;
	}
	
	private class ReportCapital { 
		BigDecimal capitalPorRegiaoSaldo;
		BigDecimal capitalPorRegiaoTotal;
		public ReportCapital(BigDecimal capitalPorRegiaoSaldo, BigDecimal capitalPorRegiaoTotal) {
			super();
			this.capitalPorRegiaoSaldo = capitalPorRegiaoSaldo;
			this.capitalPorRegiaoTotal = capitalPorRegiaoTotal;
		} 
		
		public BigDecimal getCapitalPorRegiaoSaldo() {
			return capitalPorRegiaoSaldo;
		}
		
		public BigDecimal getCapitalPorRegiaoTotal() {
			return capitalPorRegiaoTotal;
		}
	}
	
}
