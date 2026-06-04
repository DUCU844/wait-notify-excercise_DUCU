package edu.eci.arsw.primefinder;

import java.io.IOException;

public class Control extends Thread {

    private static final int NTHREADS     = 3;
    private static final int MAXVALUE     = 30000000;
    private static final int TMILISECONDS = 5000;

    private final int NDATA = MAXVALUE / NTHREADS;

    private final PrimeFinderThread[] pft;

    // Shared monitor: all pause/resume operations use this lock.
    // paused is read/written only inside synchronized(pauseLock),
    // guaranteeing visibility and preventing lost wakeups.
    final Object pauseLock = new Object();
    volatile boolean paused = false;

    private Control() {
        super();
        this.pft = new PrimeFinderThread[NTHREADS];
        int i;
        for (i = 0; i < NTHREADS - 1; i++) {
            pft[i] = new PrimeFinderThread(i * NDATA, (i + 1) * NDATA, this);
        }
        pft[i] = new PrimeFinderThread(i * NDATA, MAXVALUE + 1, this);
    }

    public static Control newControl() {
        return new Control();
    }

    /**
     * Called by each PrimeFinderThread in its loop.
     * If paused == true, the thread suspends (wait) until resumeAll() wakes it.
     * The while loop prevents spurious wakeups from bypassing the condition check.
     */
    void awaitIfPaused() throws InterruptedException {
        synchronized (pauseLock) {
            while (paused) {
                pauseLock.wait();
            }
        }
    }

    /** Signals pause: threads will suspend on their next call to awaitIfPaused(). */
    private void pauseAll() {
        synchronized (pauseLock) {
            paused = true;
        }
    }

    /** Wakes all suspended threads. */
    private void resumeAll() {
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll();
        }
    }

    @Override
    public void run() {
        for (int i = 0; i < NTHREADS; i++) {
            pft[i].start();
        }

        try {
            while (true) {
                Thread.sleep(TMILISECONDS);

                pauseAll();
                // Short cooperative delay: threads may still be between checkpoints.
                // In production a CyclicBarrier would give a hard guarantee; sufficient for the lab.
                Thread.sleep(50);

                System.out.println("\n=== PAUSE ===");
                int total = 0;
                for (int i = 0; i < NTHREADS; i++) {
                    int count = pft[i].getPrimes().size();
                    total += count;
                    System.out.println("  Thread " + i + " [" + pft[i].getA() + "-" + pft[i].getB() + "]: " + count + " primes");
                }
                System.out.println("  TOTAL so far: " + total);
                System.out.println("Press ENTER to continue...");

                // True blocking wait for ENTER — no busy-wait
                //noinspection ResultOfMethodCallIgnored
                System.in.read();
                while (System.in.available() > 0) System.in.read();

                resumeAll();
                System.out.println("=== RESUMED ===\n");
            }
        } catch (InterruptedException | IOException e) {
            Thread.currentThread().interrupt();
        }
    }
}
