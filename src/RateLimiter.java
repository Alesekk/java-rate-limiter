import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;


interface TimeProvider {
    long getNanoTime();
}
interface RateLimiter {
    boolean allowAccess(String clientID);
}

class APIRateLimiter implements RateLimiter {

    class Bucket{
        final double tokens;
        final long refillTime;
        Bucket(int tokens, long refillTime) {
            this.tokens = tokens;
            this.refillTime = refillTime;
        }
    }
    private final int capacity;
    private final double tokensPerSecond;
    private final TimeProvider timeProvider;

    private final Map<String, AtomicReference<Bucket>> buckets = new ConcurrentHashMap<>();
    APIRateLimiter(int capacity, double tokensPerSecond, TimeProvider timeProvider){
        this.capacity = capacity;
        this.tokensPerSecond = tokensPerSecond;
        this.timeProvider = timeProvider;
    }
    @Override
    public boolean allowAccess(String clientID) {
        AtomicReference<Bucket> bucket = buckets.computeIfAbsent(clientID, k ->
                new AtomicReference<>(new Bucket(capacity, timeProvider.getNanoTime()))
        );
        while(true){
            Bucket currentBucket = bucket.get();
            long time = timeProvider.getNanoTime();
            double timePassed = (time - currentBucket.refillTime) / 1e9;
            int currentPresentTokens = Math.min(capacity,(int) (currentBucket.tokens + (timePassed * tokensPerSecond)));
            if (currentPresentTokens > 0){
                Bucket newBucket = new Bucket(currentPresentTokens-1, time);
                if(bucket.compareAndSet(currentBucket, newBucket)){
                    return true;
                }
            } else {
                return false;
            }
        }
    }
}
