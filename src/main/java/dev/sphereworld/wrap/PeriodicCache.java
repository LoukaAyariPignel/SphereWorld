package dev.sphereworld.wrap;

public interface PeriodicCache {
    ThreadLocal<Integer> CONSTRUCTING = ThreadLocal.withInitial(() -> 0);

    int sphereworld$period();

    void sphereworld$setPeriod(int period);
}
