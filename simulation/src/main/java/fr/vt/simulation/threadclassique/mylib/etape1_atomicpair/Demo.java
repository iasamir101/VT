package fr.vt.simulation.threadclassique.mylib.etape1_atomicpair;

import fr.vt.simulation.threadclassique.mylib.MyAtomicPairInteger;

import java.util.concurrent.atomic.AtomicInteger;

public class Demo {

    private static final int PER_THREAD = 100_000;

    private static int counter = 0;

    private static final MyAtomicPairInteger atomicCounter = new MyAtomicPairInteger();

    public static void main(String[] args) throws InterruptedException {

        Thread t1 = new Thread(Demo::count);
        Thread t2 = new Thread(Demo::count);

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println("Attendu : " + 2 * PER_THREAD * 2 );
        System.out.println("int           : " + counter);
        System.out.println("AtomicInteger : " + atomicCounter.get());
    }

    private static void count() {
        for (int i = 0; i < PER_THREAD; i++) {
            counter=counter + 2 ;
            atomicCounter.incrementAndGet();
        }
    }
}
