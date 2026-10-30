package dev.draconicsable.explosion;

/** Deterministic regression checks: fixed caller order, overshoots, cancellation and phase competition. */
public final class FairBlastQueueTest {
    private static void check(boolean condition,String message) { if(!condition)throw new AssertionError(message); }
    public static void main(String[] args) {
        var q=new FairBlastQueue();var a=new Object();var b=new Object();
        check(q.allowance(a,0,2,6)==2,"single preparation keeps its slice");q.charge(2);
        check(q.allowance(a,0,2,6)==0,"same owner cannot consume repeated slices in one tick");
        check(q.allowance(a,1,2,6)==2,"next tick restores allowance");q.charge(2);
        check(q.allowance(b,1,2,6)==0,"new arrival waits behind existing turn");
        q.forget(a);
        check(q.allowance(b,1,2,6)==2,"explicit cancellation releases turn immediately");

        q=new FairBlastQueue();Object[] owners=new Object[8];int[] grants=new int[8];
        for(int i=0;i<owners.length;i++)owners[i]=new Object();
        for(int tick=0;tick<80;tick++)for(int i=0;i<owners.length;i++) {
            long grant=q.allowance(owners[i],tick,2,6);
            if(grant>0){grants[i]++;q.charge(7);} // One uninterruptible call exceeded the whole budget.
        }
        for(int i=0;i<grants.length;i++)check(grants[i]>=9,"fixed-order overshoot starved owner "+i);

        q=new FairBlastQueue();int preparing=0,removing=0;
        for(int tick=0;tick<40;tick++) {
            long r=q.allowance(a,tick,12,12);if(r>0){removing++;q.charge(r);}
            long p=q.allowance(b,tick,2,6);if(p>0){preparing++;q.charge(p);}
        }
        check(preparing>=19 && removing>=19,"removal and preparation both make bounded progress");

        q=new FairBlastQueue();q.allowance(a,0,2,6);q.charge(2);q.allowance(b,0,2,6);
        check(q.allowance(b,1,2,6)==0,"missing head gets one tick grace");
        check(q.allowance(b,2,2,6)==2,"abandoned head cannot block subsequent ticks");

        q=new FairBlastQueue();q.allowance(a,0,2,6);q.charge(20);q.allowance(b,0,2,6);q.forget(a);
        check(q.allowance(b,0,2,6)==0,"cancellation does not refund consumed time");
        check(q.allowance(b,1,2,6)==2,"overshoot debt ends at tick boundary");
        check(new FairBlastQueue().allowance(a,0,2,6)==2,"dimensions have independent queues");
        System.out.println("PASS: fair blast queue bounds, fixed-order starvation, overshoot, phase competition, cancellation and expiry");
    }
}
