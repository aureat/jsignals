package jsignals.tests;

import jsignals.async.ResourceRef;

import java.util.concurrent.*;
import java.util.function.Supplier;

public class ResourceRaceTest {

    public static void main(String[] args) throws InterruptedException {
        // Simulate a fetcher with delay
        Supplier<CompletableFuture<Integer>> fetcher = () -> CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(100); // Simulate work
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            var num = (int) (Math.random() * 1000);
            System.out.println("[" + Thread.currentThread().getName() + "] Fetched: " + num);
            return num;
        });

        ResourceRef<Integer> resource = new ResourceRef<>(fetcher, false);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                var future = resource.fetch();
                try {
                    future.get();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }
                latch.countDown();
            });
        }

        latch.await();
        var tasks = executor.shutdownNow();
        System.out.println("All fetch tasks completed. Remaining tasks: " + tasks.size());

        // Wait for the last fetch to complete
        System.out.println("Waiting for final fetch to complete...");

        System.out.println("Final state: " + resource.get());
        System.out.println("Final data: " + resource.getData());
        System.out.println("Final error: " + resource.getError());
    }

}
