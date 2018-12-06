package com.example.elifguler.skatemate;

public class BinarySemaphore { // used for mutual exclusion
    private boolean value;

    BinarySemaphore(boolean initValue) {
        value = initValue;
    }

    public synchronized void P() { // atomic operation // blocking
        while (!value) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        value = false;
    }

    public synchronized void V() { // atomic operation // non-blocking
        value = true;
        notify(); // wake up a process from the queue
    }
}
