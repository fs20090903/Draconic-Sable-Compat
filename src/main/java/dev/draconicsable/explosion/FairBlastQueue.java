package dev.draconicsable.explosion;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;

/** Server-thread-only admission queue. Time is supplied by the caller for deterministic tests. */
final class FairBlastQueue {
    private static final class Turn {
        final WeakReference<Object> owner;
        long requested, granted = Long.MIN_VALUE;
        Turn(Object owner, long tick) { this.owner = new WeakReference<>(owner); requested = tick; }
    }
    private final ArrayDeque<Turn> turns = new ArrayDeque<>();
    private long tick = Long.MIN_VALUE, used;

    long allowance(Object owner, long nowTick, long slice, long total) {
        if (tick != nowTick) {
            tick = nowTick;
            used = 0;
            // Abandoned processes must not block the queue or retain their world.
            turns.removeIf(t -> t.owner.get() == null || t.requested < nowTick - 1);
        }
        Turn turn = null;
        for (var candidate : turns) if (candidate.owner.get() == owner) { turn = candidate; break; }
        if (turn == null) { turn = new Turn(owner, nowTick); turns.addLast(turn); }
        turn.requested = nowTick;
        if (turn != turns.peekFirst() || turn.granted == nowTick) return 0;
        long allowance = Math.max(0, Math.min(slice, total - used));
        if (allowance > 0) {
            turn.granted = nowTick;
            turns.removeFirst();
            turns.addLast(turn);
        }
        return allowance;
    }

    void charge(long elapsed) { used += Math.max(0, elapsed); }
    void forget(Object owner) { turns.removeIf(t -> t.owner.get() == null || t.owner.get() == owner); }
}
