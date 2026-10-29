package dev.draconicsable.explosion;

import com.brandon3055.draconicevolution.blocks.reactor.ProcessExplosion;
import com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorCore;
import dev.draconicsable.LoadedAccess;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Prepare during DE's countdown, refresh moving snapshots, reuse a valid prepared plan. */
public final class AnchoredReactorExplosion extends ProcessExplosion {
    private final ServerLevel level;
    private final BlockPos anchor;
    private final TileReactorCore owner;
    private ProcessExplosion scheduled;
    private Vec3 planOrigin;
    private boolean requested;
    private boolean actuallyDetonated;
    private boolean countdownRefreshed;
    private long nextCheck;
    public AnchoredReactorExplosion(BlockPos anchor,int radius,ServerLevel level,int delay) {
        super(anchor,radius,level,delay);this.anchor=anchor.immutable();this.level=level;
        owner=LoadedAccess.tile(level,anchor) instanceof TileReactorCore core?core:null;
    }
    private Vec3 location(){
        var body=Sable.HELPER.getContaining(level,anchor);
        return body==null?Vec3.atCenterOf(anchor):body.logicalPose().transformPosition(Vec3.atCenterOf(anchor));
    }
    private boolean matches(){return scheduled!=null && planOrigin.distanceToSqr(location())<=1.0/256 && ((BlastPlan)scheduled).draconicSable$matches();}
    private void prepare(Vec3 position){
        BlastTrace previous=null;
        if(scheduled!=null){previous=((BlastPlan)scheduled).draconicSable$takeTerrain();((BlastPlan)scheduled).draconicSable$discard();}
        planOrigin=position;
        calculationComplete=false;
        scheduled=new ProcessExplosion(BlockPos.containing(position),maxRadius,level,-1);
        scheduled.origin.set(position.x,position.y,position.z);
        scheduled.enableEffect=enableEffect;scheduled.lava=lava;
        ((BlastPlan)scheduled).draconicSable$reuseTerrain(previous);
        ((BlastPlan)scheduled).draconicSable$prepare();
        nextCheck=level.getGameTime()+20;
    }
    @Override public void updateProcess(){
        if(isDead)return;
        if(!requested && owner!=null && (owner.isRemoved() || LoadedAccess.tile(level,anchor)!=owner || owner.reactorState.get()!=TileReactorCore.ReactorState.BEYOND_HOPE)) {
            if(scheduled!=null)((BlastPlan)scheduled).draconicSable$discard();isDead=true;return;
        }
        if(scheduled==null)prepare(location());
        // A real reactor has a long safety countdown. Rebuilding repeatedly throughout it
        // wastes entire plans as the vehicle moves. Refresh once near its end, then do a
        // final validity check when detonation is requested. Standalone users have no GUI
        // countdown and retain the regular refresh behavior.
        boolean refreshWindow=owner==null || (!countdownRefreshed && owner.explosionCountdown.get()>=0 && owner.explosionCountdown.get()<=200);
        if(!requested && refreshWindow && scheduled.isCalculationComplete() && level.getGameTime()>=nextCheck){
            nextCheck=level.getGameTime()+20;
            if(!matches()){if(owner!=null)countdownRefreshed=true;prepare(location());}
        }
        if(!scheduled.isCalculationComplete())scheduled.updateCalculation();
        if(scheduled.isCalculationComplete()) {
            calculationComplete=true;
            if(requested)finishDetonation();
        }
    }
    @Override public void updateCalculation(){updateProcess();}
    @Override public boolean detonate() {
        if(actuallyDetonated)return false;
        if(requested)return scheduled.isCalculationComplete() && finishDetonation();
        if(!matches())prepare(location());
        requested=true;
        // Commit the detonation position once. The calculation may finish over subsequent
        // ticks, but must not chase movement forever or remove the reactor prematurely.
        return scheduled.isCalculationComplete() && finishDetonation();
    }
    private boolean finishDetonation(){
        if(actuallyDetonated || !scheduled.detonate())return false;
        actuallyDetonated=true;detonated=true;isDead=true;
        if(owner!=null && LoadedAccess.tile(level,anchor)==owner &&
            owner.reactorState.get()==TileReactorCore.ReactorState.BEYOND_HOPE)level.removeBlock(anchor,false);
        return true;
    }
    public boolean actuallyDetonated(){return actuallyDetonated;}
    public ProcessExplosion scheduled(){return scheduled;}
}
