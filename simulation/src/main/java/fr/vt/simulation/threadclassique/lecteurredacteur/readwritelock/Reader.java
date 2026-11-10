package fr.vt.simulation.threadclassique.lecteurredacteur.readwritelock;

/**
 * Thread lecteur de la démo ReadWriteLock.
 *
 * But : lire la donnée partagée plusieurs fois.
 *
 * Entrée : la donnée partagée et le nom du thread.
 *
 * Sortie : 3 lignes "<nom> lit : <valeur>".
 */
class Reader extends Thread {

    private final SharedData sharedData;

    Reader(SharedData sharedData, String name) {
        super(name);
        this.sharedData = sharedData;
    }

    @Override
    public void run() {
        for (int i = 0; i < 3; i++) {
            sharedData.read();
        }
    }
}
