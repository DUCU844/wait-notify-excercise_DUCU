package edu.eci.arsw.primefinder;

import java.util.LinkedList;
import java.util.List;

public class PrimeFinderThread extends Thread {

    private final int a;
    private final int b;
    private final List<Integer> primes;
    private final Control control;

    public PrimeFinderThread(int a, int b, Control control) {
        super();
        this.a       = a;
        this.b       = b;
        this.primes  = new LinkedList<>();
        this.control = control;
    }

    @Override
    public void run() {
        try {
            for (int i = a; i < b; i++) {
                // Cooperative suspension point — no busy-wait.
                // If Control signaled a pause, this thread enters wait()
                // until notifyAll() wakes it.
                control.awaitIfPaused();

                if (isPrime(i)) {
                    primes.add(i);
                    System.out.println(i);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    boolean isPrime(int n) {
        boolean ans;
        if (n > 2) {
            ans = n % 2 != 0;
            for (int i = 3; ans && i * i <= n; i += 2) {
                ans = n % i != 0;
            }
        } else {
            ans = n == 2;
        }
        return ans;
    }

    public List<Integer> getPrimes() { return primes; }
    public int getA() { return a; }
    public int getB() { return b; }
}
