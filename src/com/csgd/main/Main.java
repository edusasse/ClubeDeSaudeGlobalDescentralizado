package com.csgd.main;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import com.csgd.calc.Calculadora;
import com.csgd.calc.Capital;
import com.csgd.constantes.Parametros;
import com.csgd.domain.Cliente;
import com.csgd.domain.IPlanoItem;
import com.csgd.domain.Plano;
import com.csgd.domain.PlanoItem;
import com.csgd.domain.PlanoItemParametro;
import com.csgd.domain.Premio;
import com.csgd.domain.Regiao;
import com.csgd.domain.ValorCapital;
import com.csgd.fake.Calendario;

public class Main {
	
	public static final int NUMERO_DE_CLIENTES_SIMULAR = 100;
	private static final int NUMERO_DE_MESES_SIMULAR = 36;
	private static final int VALOR_PARCELA_VARIACAO_SIMULAR = 50;
	private static final int VALOR_PARCELA_MEDIA_SIMULAR = 200;

	public static void main(String[] args) {
		new Main().simular();
	}
	
	public void simular() {
		Random rand = new Random();

		// Calendario
		final Calendario cal = Calendario.getInstance();		
		cal.definirDataInicial(new Date());
		
		// Itens do Plano
		IPlanoItem planoItemConsultas = new PlanoItem("Consultas");
		IPlanoItem planoItemExames = new PlanoItem("Exames"); 

		// Montagem do Plano
		Plano planoBrasil = new Plano();
		planoBrasil.addPlanoItem(planoItemConsultas);
		planoBrasil.addPlanoItem(planoItemExames);
		
		// Regiao
		Regiao regiaoBrasil = new Regiao("Brasil", planoBrasil);		 

		List<Cliente> listaDeClientes = new ArrayList<>(); 
		for (int i=0; i<NUMERO_DE_CLIENTES_SIMULAR;i++) {
			Cliente cli = new Cliente("Cliente " + i);
			int  n = rand.nextInt(50) + 20;
			cli.addPlanoItem(regiaoBrasil, planoBrasil, new PlanoItemParametro(planoItemConsultas, new BigDecimal(n)));
			cli.addPlanoItem(regiaoBrasil, planoBrasil, new PlanoItemParametro(planoItemExames, new BigDecimal(100-n)));
		
			listaDeClientes.add(cli);
		}
		
		// Premios
		int meses = NUMERO_DE_MESES_SIMULAR;
		while (meses > 0) {
			Calendario.getInstance().incrementarEm1Mes();
			System.out.println("Data Atual: " + Calendario.getInstance().getDataAtual());
			for (Cliente cli: listaDeClientes) {
				Calendario.getInstance().definirDataInicial(new Date());
				Premio premio = new Premio(regiaoBrasil, cli, new BigDecimal(rand.nextInt(VALOR_PARCELA_MEDIA_SIMULAR + 1 - VALOR_PARCELA_VARIACAO_SIMULAR) + VALOR_PARCELA_VARIACAO_SIMULAR));
				cli.adicionarPremio(premio);
				Calculadora.getInstance().calcularCapital(premio);
			}
			meses--;
			
			planoBrasil.getListaDeItensDoPlano().stream()
			.forEach(planoItem -> planoBrasil.getListaDeClientes().stream()
					.forEach(cli -> {
						final byte controleDeCarencia = cli.getControleDeCarencia(regiaoBrasil, planoItem);
						final byte controleDeEntrada = cli.getControleDeEntrada(regiaoBrasil, planoItem);
						final BigDecimal variacao = cli.getVariacao(regiaoBrasil, planoItem);
						final BigDecimal mediaPremios = cli.getMediaPremios(regiaoBrasil, planoItem);
						final ValorCapital valorCapital = cli.getCapitalPorRegiaoEPlanoItem(regiaoBrasil, planoItem);
						final BigDecimal premiosPagos = cli.getPremioAcumulado(regiaoBrasil, planoItem);
						System.out.println("Cliente [" + cli.getNome() + "] "
								+ "Plano Item [" + planoItem.getNome() +"](" + cli.getPlanoItemParametro(regiaoBrasil, planoItem).getPercentualAlocacao().multiply(new BigDecimal(100), new MathContext(6)) + " %) "
								+ "Variacao [" + variacao + "] "
								+ "Entrada [" + controleDeEntrada + "] "
								+ "Carencia [" + controleDeCarencia + "]"
								+ "Media Premios [" + mediaPremios + "] "
								+ "Premios pagos [" + premiosPagos + "] "
								+ "Capital Total [" + valorCapital.getCapitalTotal() + "] "
								+ "Capital Acessivel [" + valorCapital.getCapitalAcessivel() + "] "
								+ "Capital Acessivel (max Cli) [" + valorCapital.getCapitalMaximoAcessivelComRestricaoPorCliente() + "](" + Parametros.CAPITAL_MAXIMO_ACESSIVEL_POR_CLIENTE.getValorParametro() + "%)");
					}));
		}

		System.out.println("\n == Total de capital acumulado por Plano Item ==");
		planoBrasil.getListaDeItensDoPlano().stream()
		.forEach(planoItem -> 
			System.out.println("Capital por Plano Item [" + planoItem.getNome() + "] Valor [" + Capital.getCapitalPorRegiao(regiaoBrasil, planoItem) + "]"));
		 
		
	}
	
	public static void mainStatic(String[] args) {
		
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
		Calculadora.getInstance().calcularCapital(premio1);		
		
		Premio premioMayara1 = new Premio(regiaoBrasil, clienteMayara, new BigDecimal(200));
		clienteMayara.adicionarPremio(premioMayara1);
		Calculadora.getInstance().calcularCapital(premioMayara1);
		
		// -- Periodo 2
		Calendario.getInstance().incrementarEm1Mes();
		Premio premio2 = new Premio(regiaoBrasil, clienteEduardo, new BigDecimal(150));
		clienteEduardo.adicionarPremio(premio2);
		Calculadora.getInstance().calcularCapital(premio2);
		
		Premio premioMayara2 = new Premio(regiaoBrasil, clienteMayara, new BigDecimal(200));
		clienteMayara.adicionarPremio(premioMayara2);
		Calculadora.getInstance().calcularCapital(premioMayara2);
		
		System.out.println("Capital - Região Brasil - planoItemConsultas: " + Capital.getCapitalPorRegiao(regiaoBrasil, planoItemConsultas));
		System.out.println("Capital - Região Brasil - planoItemConsultas: " + Capital.getCapitalPorRegiao(regiaoBrasil, planoItemExames));

		System.out.println("Eduardo - planoItemConsultas : " + clienteEduardo.getPremioAcumulado(regiaoBrasil, planoItemConsultas));
		System.out.println("Eduardo - planoItemExames : " + clienteEduardo.getPremioAcumulado(regiaoBrasil, planoItemExames));
		System.out.println("Mayara - planoItemConsultas : " + clienteMayara.getPremioAcumulado(regiaoBrasil, planoItemConsultas));
		System.out.println("Mayara - planoItemExames : " + clienteMayara.getPremioAcumulado(regiaoBrasil, planoItemExames));
		
		
	}
}
