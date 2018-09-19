package com.csgd.fake;

import java.time.ZoneId;
import java.util.Date;

public class Calendario {
	
	private static volatile Calendario instance;
	private Date dataAtual;

    private Calendario() {
    	this.dataAtual = null;
    }

    public static Calendario getInstance() {
        if (instance == null) {
            synchronized (Calendario.class) {
                if (instance == null) {
                    instance = new Calendario();
                }
            }
        }
        return instance;
    }
    
    public synchronized void definirDataInicial(Date date) {
    	if (dataAtual == null) {
    		this.dataAtual = date;
    	}
    }
    
    public synchronized void incrementarEm1Dia() {
    	this.dataAtual = Date.from(getDataAtual()
    			.toInstant()
    			.atZone(ZoneId.systemDefault())
    			.toLocalDateTime()
    			.plusDays(1)
    			.atZone(ZoneId.systemDefault())
    			.toInstant());	
    }
    
    public synchronized void incrementarEm1Mes() {
    	this.dataAtual = Date.from(getDataAtual()
    			.toInstant()
    			.atZone(ZoneId.systemDefault())
    			.toLocalDateTime()
    			.plusMonths(1)
    			.atZone(ZoneId.systemDefault())
    			.toInstant());	
    }
    
	public Date getDataAtual() {
		return dataAtual;
	}
	
}
