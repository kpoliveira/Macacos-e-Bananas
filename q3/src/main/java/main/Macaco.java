package main;

public class Macaco extends Thread implements Comparable<Macaco> {
    private int id;
    private long ultimoTempoQueComeu;
    private Bananeira bananeira;

    public Macaco(int id, Bananeira bananeira) {
        this.id = id;
        this.bananeira = bananeira;
        ultimoTempoQueComeu = System.currentTimeMillis();
    }

    public int getIdMacaco() { return id; }

    @Override
    public void run() {
        try {
            loop();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void loop() throws InterruptedException {
        while (!bananeira.isDone()) {
            if (getTempoSemComer() >= bananeira.TEMPO_MAXIMO_SEM_COMER) {
                morrer();
            }
            boolean subiu = subir();
            if (!subiu) {
                System.out.println("O macaco " + id + " nao conseguiu subir na bananeira");
                Thread.sleep(bananeira.TEMPO_DE_ESPERA_AO_NAO_SUBIR);
                continue;
            }
            Thread.sleep(bananeira.TEMPO_PARA_SUBIR);
            if (comerBanana()) {
                System.out.println("O macaco " + id + " comeu uma banana");
                ultimoTempoQueComeu = System.currentTimeMillis();
                descer();
                continue;
            }
            if (bananeira.isDone()) {
                descer();
                break;
            }
            System.out.println("Nao havia bananas para o macaco " + id + " comer");
            descer();
        }
    }

    private void morrer() {
        System.out.println("O macaco " + id + " morreu de fome");
        System.exit(1);
    }

    private boolean subir() throws InterruptedException {
        return bananeira.subir(this);
    }

    private boolean descer() {
        return bananeira.descer(this);
    }

    private boolean comerBanana() {
        return bananeira.comerBanana(this);
    }

    public long getTempoSemComer() {
        return System.currentTimeMillis() - ultimoTempoQueComeu;
    }

    @Override
    public int compareTo(Macaco m) {
        return Integer.compare(id, m.id);
    }
}
