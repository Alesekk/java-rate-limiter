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
        final int tokens;
        final long refill_time;
        Bucket(int tokens, long refill_time) {
            this.tokens = tokens;
            this.refill_time = refill_time;
        }
    }
    private final int capacity;
    private final int tokens_per_second;
    private final TimeProvider timeProvider;

    private final Map<String, AtomicReference<Bucket>> buckets = new ConcurrentHashMap<>();
    APIRateLimiter(int capacity, int tokensPerSecond, TimeProvider timeProvider){
        this.capacity = capacity;
        this.tokens_per_second = tokensPerSecond;
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
            double timePassed = (time - currentBucket.refill_time) / 1e9;
            int currentPresentTokens = Math.min(capacity,(int) (currentBucket.tokens + (timePassed * tokens_per_second)));
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
