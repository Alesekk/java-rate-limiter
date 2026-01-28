void main() throws Exception {
    testSingleThreadLogic();
    testMultiThreadOver();
}

static class TestTimeProvider implements TimeProvider {
    long time = 0;

    public long getNanoTime() {
        return time;
    }

    public void moveTime(double s) {
        time += (long) (s * 1_000_000_000);
    }
}

static void testSingleThreadLogic() {
    System.out.println("Testing the correctness of logic, two requests per second for a TestUser");

    TestTimeProvider time = new TestTimeProvider();
    APIRateLimiter limiter = new APIRateLimiter(2, 1, time);

    System.out.println("First request for the user, should be allowed: " + limiter.allowAccess("TestUser"));
    System.out.println("Second request for the user, should be allowed: " + limiter.allowAccess("TestUser"));
    System.out.println("Third request for the user, should be blocked: " + limiter.allowAccess("TestUser"));

    System.out.println("Now, we fake move time by 1.5 seconds, this should refill the bucket for the TestUser");
    time.moveTime(1.5);

    System.out.println("Fourth request for the user, after shift, should be allowed: " + limiter.allowAccess("TestUser"));
}

static void task(APIRateLimiter limiter,
                 CountDownLatch start,
                 CountDownLatch done,
                 AtomicInteger success) {
    try {
        start.await();
        if (limiter.allowAccess("User")) {
            success.incrementAndGet();
        }
    } catch (InterruptedException ignored) {
    } finally {
        done.countDown();
    }
}

static void testMultiThreadOver() throws Exception {
    System.out.println("Testing the multi-threaded concurrency");
    TimeProvider time = System::nanoTime;
    APIRateLimiter limiter = new APIRateLimiter(10, 0, time);
    ExecutorService executor = Executors.newFixedThreadPool(50);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(1000);
    AtomicInteger success = new AtomicInteger();
    for (int i = 0; i < 1000; i++) {
        executor.submit(() -> task(limiter, startLatch, endLatch, success));
    }
    startLatch.countDown();
    endLatch.await();
    executor.shutdown();
    System.out.println("Success = " + success.get());
    }