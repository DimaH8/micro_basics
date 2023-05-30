package org.micro_basics.common;

import java.util.concurrent.atomic.AtomicBoolean;

public class Utils {
    public static void waitDockerSignal() {
        System.out.println("Start wait docker signal");
        AtomicBoolean running = new AtomicBoolean(true);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Got signal from docker. Start shutdown");
            running.set(false);
        }));

        // wait docker signal
        while (running.get()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Finish wait docker signal");
    }
}
