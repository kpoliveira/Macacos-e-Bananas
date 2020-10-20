package main;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Bananeira implements Runnable {
    private final int NUM_DE_MACACOS = 3;
    private final int MAX_DE_MACACOS_NA_BANANEIRA = 2;

    public final long TEMPO_MAXIMO_SEM_COMER = 10000;
    public final long TEMPO_PARA_SUBIR = 1000;
    public final long TEMPO_DE_ESPERA_AO_NAO_SUBIR = 2000;

    private final int NUM_DE_BANANAS_INICIO = 50;
    private final int NUM_DE_BANANAS_MAXIMO = 50;
    private final int TAXA_PRODUCAO_DE_BANANAS_SEG = 5;
    private final long INTERVALO_PRODUCAO_DE_BANANAS = (long) 1000 / TAXA_PRODUCAO_DE_BANANAS_SEG;


    private long totalBananasAConsumir;
    private long bananasConsumidas;
    private Lock incrementoBananasConsumidas = new ReentrantLock();

    private Macaco[] macacos = new Macaco[NUM_DE_MACACOS];
    private boolean[] macacoSubiu = new boolean[NUM_DE_MACACOS];
    private long[] contadorBananasMacacos = new long[NUM_DE_MACACOS];
    private ConcurrentSkipListSet<Macaco> macacosEmEspera = new ConcurrentSkipListSet<>();
    private Semaphore controleMacacosEsperando = new Semaphore(NUM_DE_MACACOS);

    // Controla as subidas
    private Semaphore espacosDisponiveis = new Semaphore(MAX_DE_MACACOS_NA_BANANEIRA);
    // Controla o consumo e produção de bananas
    private Semaphore bananasDisponiveis = new Semaphore(NUM_DE_BANANAS_INICIO);
    private Semaphore produzirBananas = new Semaphore(0);

    private boolean done;

    public Bananeira(long totalBananasAConsumir) {
        for (int c = 0; c < macacos.length; c++) {
            macacos[c] = new Macaco(c, this);
        }
        this.totalBananasAConsumir = totalBananasAConsumir;
    }

    public void loop() throws InterruptedException {
        System.out.println("Iniciando threads dos macacos");
        for (Macaco m : macacos) {
            m.setPriority(Thread.MIN_PRIORITY);
            m.start();
        }

        int numBananasDisponiveis = NUM_DE_BANANAS_INICIO;
        loopExterno:
        while (!done) {
            // Só começa a produzir quando acabarem as bananas
            boolean comecarProducao = produzirBananas.tryAcquire(numBananasDisponiveis, TEMPO_PARA_SUBIR, TimeUnit.MILLISECONDS);
            if (!comecarProducao)
                continue;
            for (int c = 0; c < NUM_DE_BANANAS_MAXIMO; c++) {
                if (done)
                    break loopExterno;
                Thread.sleep(INTERVALO_PRODUCAO_DE_BANANAS);
                System.out.println("1 banana sera produzida");
                bananasDisponiveis.release();
            }
            numBananasDisponiveis = NUM_DE_BANANAS_MAXIMO;
        }
        for (Macaco m : macacos)
            m.join();
        System.out.println("Total de bananas para cada macaco:");
        for (int c = 0; c < contadorBananasMacacos.length; c++) {
            System.out.printf("Macaco %d: %d\n", c, contadorBananasMacacos[c]);
        }

    }

    public boolean isDone() { return done; }

    @Override
    public void run() {
        try {
            loop();
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }
/*
    public void mudarPrioridade(Macaco m) {
        long tempoSemComer = m.getTempoSemComer();
        double percentualFome = (double) tempoSemComer / TEMPO_MAXIMO_SEM_COMER;
        int novaPrioridade = Thread.MIN_PRIORITY + (int) Math.ceil(percentualFome * (Thread.MAX_PRIORITY - Thread.MIN_PRIORITY));
        System.out.printf("Mudando prioridade do macaco %d de %d para %d (tempo sem comer: %d, percentual de fome: %f)\n", m.getIdMacaco(), m.getPriority(), novaPrioridade, tempoSemComer, percentualFome);
        m.setPriority(novaPrioridade);
    }
*/
    public void adicionarMacacoEmEspera(Macaco m) {
        macacosEmEspera.add(m);
        controleMacacosEsperando.drainPermits();
    }

    public void removerMacacoEmEspera(Macaco m) {
        macacosEmEspera.remove(m);
        // Quando não há mais macacos em espera
        if (macacosEmEspera.isEmpty())
            controleMacacosEsperando.release(NUM_DE_MACACOS);
    }

    public boolean subir(Macaco m) throws InterruptedException {
        if (macacoSubiu[m.getIdMacaco()])
            return false;

        boolean emEspera = macacosEmEspera.contains(m);
        // Se esse macaco não falhou em subir alguma vez, deve dar vez a um que tenha falhado
        if (!emEspera)
            controleMacacosEsperando.acquire();

        // Tenta subir
        if ( ! espacosDisponiveis.tryAcquire() ) {
            //mudarPrioridade(m);
            // Se não conseguiu subir, adiciona à espera
            if (!emEspera)
                adicionarMacacoEmEspera(m);
            return false;
        }
        macacoSubiu[m.getIdMacaco()] = true;
        return true;
    }

    public boolean descer(Macaco m) {
        if (!macacoSubiu[m.getIdMacaco()])
            return false;
        macacoSubiu[m.getIdMacaco()] = false;
        espacosDisponiveis.release();
        // Alterar prioridade
        //mudarPrioridade(m);
        if (macacosEmEspera.contains(m))
            removerMacacoEmEspera(m);
        return true;
    }

    public boolean comerBanana(Macaco m) {
        if (!macacoSubiu[m.getIdMacaco()])
            return false;
        if (!bananasDisponiveis.tryAcquire()) {
            //mudarPrioridade(m);
            return false;
        }
        // TODO gerenciar bananas
        incrementoBananasConsumidas.lock();
        if (done) {
            incrementoBananasConsumidas.unlock();
            return false;
        }
        bananasConsumidas++;
        if (bananasConsumidas == totalBananasAConsumir)
            done = true;
        incrementoBananasConsumidas.unlock();
        produzirBananas.release();

        contadorBananasMacacos[m.getIdMacaco()]++;
        //mudarPrioridade(m);
        return true;
    }
}
