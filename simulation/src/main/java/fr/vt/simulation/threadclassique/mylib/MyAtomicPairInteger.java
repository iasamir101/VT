package fr.vt.simulation.threadclassique.mylib;


public class MyAtomicPairInteger {

    private volatile int value;

    public MyAtomicPairInteger() {
        this(0);
    }

    public MyAtomicPairInteger(int initialValue) {
        requireEven(initialValue);
        this.value = initialValue;
    }

    public int get() {
        return value;
    }

    public synchronized boolean compareAndSet(int expected, int newValue) {
        requireEven(newValue);
        if (value != expected) {
            return false;
        }
        value = newValue;
        return true;
    }

    public int incrementAndGet() {
        return addAndGet(2);
    }

    public synchronized int addAndGet(int delta) {
        requireEven(delta);
        value += delta;
        return value;
    }

    private static void requireEven(int v) {
        if (v % 2 != 0) {
            throw new IllegalArgumentException("valeur impaire refusée : " + v);
        }
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
