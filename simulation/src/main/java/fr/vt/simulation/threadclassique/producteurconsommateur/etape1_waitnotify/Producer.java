package fr.vt.simulation.threadclassique.producteurconsommateur.etape1_waitnotify;

/**
 * Thread producteur de la démo wait/notify.
 *
 * But : remplir le tampon à un rythme régulier.
 *
 * Entrée : le tampon partagé et le nombre d'éléments à produire.
 *
 * Sortie : aucune directe : les valeurs 1 à {@code count} sont déposées dans le tampon, une toutes les 500 ms.
 */
class Producer extends Thread {

    private final BoundedBuffer buffer;
    private final int count;

    Producer(BoundedBuffer buffer, int count) {
        this.buffer = buffer;
        this.count = count;
    }

    @Override
    public void run() {
        try {
            for (int i = 1; i <= count; i++) {
                buffer.produce(i);
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
