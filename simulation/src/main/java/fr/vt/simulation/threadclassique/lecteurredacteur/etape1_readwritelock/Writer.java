package fr.vt.simulation.threadclassique.lecteurredacteur.etape1_readwritelock;

/**
 * Thread rédacteur de la démo ReadWriteLock.
 *
 * But : modifier la donnée partagée plusieurs fois.
 *
 * Entrée : la donnée partagée et le nom du thread.
 *
 * Sortie : 3 lignes "<nom> écrit : Nouvelle valeur i".
 */
class Writer extends Thread {

    private final SharedData sharedData;

    Writer(SharedData sharedData, String name) {
        super(name);
        this.sharedData = sharedData;
    }

    @Override
    public void run() {
        for (int i = 0; i < 3; i++) {
            sharedData.write("Nouvelle valeur " + i);
        }
    }
}
