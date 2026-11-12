package fr.vt.simulation.threadclassique.producteurconsommateur.etape1_waitnotify;

/**
 * Thread consommateur de la démo wait/notify.
 *
 * But : vider le tampon plus lentement que le producteur, pour provoquer des attentes côté producteur.
 *
 * Entrée : le tampon partagé et le nombre d'éléments à consommer.
 *
 * Sortie : aucune directe : retire {@code count} valeurs, une toutes les 1000 ms.
 */
class Consumer extends Thread {

    private final BoundedBuffer buffer;
    private final int count;

    Consumer(BoundedBuffer buffer, int count) {
        this.buffer = buffer;
        this.count = count;
    }

    @Override
    public void run() {
        try {
            for (int i = 1; i <= count; i++) {
                buffer.consume();
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
