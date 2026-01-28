# Java Rate Limiter Assignment

A thread-safe, lock-free implementation of the **Token Bucket algorithm** in Java.

## Features
* **Core Logic:** Per-client rate limiting using `AtomicReference` and CAS (non-blocking).
* **Testing:** Includes a `TimeProvider` abstraction to simulate time travel in unit tests.
* **Concurrency:** tested with `ExecutorService` and `CountDownLatch`.

## How to Run
1. Navigate to the `src` folder.
2. Compile the files:
   ```bash
   javac RateLimiterTest.java APIRateLimiter.java
3. Run the tests:
   ```bash
   java RateLimiterTest