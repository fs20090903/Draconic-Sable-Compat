package dev.draconicsable.explosion;

import dev.ryanhcode.sable.api.physics.constraint.*;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.joml.*;
import java.util.*;

/** Transient staff-style world anchors. Never changes user locks or persisted body state. */
public final class BlastHold implements AutoCloseable {
    private static final Map<ServerSubLevel,State> HELD=new IdentityHashMap<>();
    private static final class State {
        final String ticket="hold:"+UUID.randomUUID();
        final FixedConstraintHandle anchor;
        final Vector3d linear,angular;
        int users;
        long releaseAt=Long.MAX_VALUE;
        State(ServerSubLevel body) {
            var pipeline=SubLevelContainer.getContainer(body.getLevel()).physicsSystem().getPipeline();
            linear=pipeline.getLinearVelocity(body,new Vector3d());angular=pipeline.getAngularVelocity(body,new Vector3d());
            var pose=body.logicalPose();
            anchor=pipeline.addConstraint(null,body,new FixedConstraintConfiguration(
                new Vector3d(pose.position()),new Vector3d(pose.rotationPoint()),new Quaterniond(pose.orientation())));
            try {
                if(anchor!=null)pipeline.resetVelocity(body);
                else com.mojang.logging.LogUtils.getLogger().warn("[DE blast] Physics backend supplied no temporary anchor for {}",body.getUniqueId());
                SubLevelContainer.getContainer(body.getLevel()).addForceLoadTicket(body,WorldBlast.TICKET,ticket);
            }catch(RuntimeException|Error e){if(anchor!=null && anchor.isValid())anchor.remove();throw e;}
        }
    }
    private final List<ServerSubLevel> bodies=new ArrayList<>();
    private boolean closed;
    public BlastHold(Collection<ServerSubLevel> targets) {
        try {
            for(var body:targets)if(!body.isRemoved()) {
                var state=HELD.computeIfAbsent(body,State::new);
                state.users++;state.releaseAt=Long.MAX_VALUE;bodies.add(body);
            }
        }catch(RuntimeException|Error e){close();throw e;}
    }
    public static boolean held(ServerSubLevel body){return HELD.containsKey(body);}
    public static boolean anchored(ServerSubLevel body){var s=HELD.get(body);return s!=null && s.anchor!=null && s.anchor.isValid();}
    public static boolean ownsTicket(String key){return HELD.values().stream().anyMatch(s->s.ticket.equals(key));}
    @Override public void close() {
        if(closed)return;closed=true;
        for(var body:bodies){var state=HELD.get(body);if(state!=null && --state.users==0)state.releaseAt=body.getLevel().getGameTime()+2;}
        bodies.clear();
    }
    private static void release(ServerSubLevel body,State state) {
        try {
          if(state.anchor!=null && state.anchor.isValid())state.anchor.remove();
          if(!body.isRemoved() && state.anchor!=null) {
            var pipeline=SubLevelContainer.getContainer(body.getLevel()).physicsSystem().getPipeline();
            pipeline.resetVelocity(body);
            // Preserve pre-blast motion, never release accumulated constraint/engine impulses.
            pipeline.addLinearAndAngularVelocity(body,state.linear,state.angular);
          }
        } finally {
            SubLevelContainer.getContainer(body.getLevel()).removeForceLoadTicket(body,WorldBlast.TICKET,state.ticket);
        }
    }
    public static void tick(LevelTickEvent.Post event) {
        if(event.getLevel().isClientSide() || HELD.isEmpty())return;
        var iter=HELD.entrySet().iterator();
        while(iter.hasNext()) {
            var entry=iter.next();var body=entry.getKey();var state=entry.getValue();
            if(body.getLevel()!=event.getLevel())continue;
            if(body.isRemoved() || state.users==0 && body.getLevel().getGameTime()>=state.releaseAt){iter.remove();release(body,state);}
        }
    }
    public static void stopping(ServerStoppingEvent event) {
        var iter=HELD.entrySet().iterator();
        while(iter.hasNext()) {
            var e=iter.next();var body=e.getKey();var state=e.getValue();
            if(body.getLevel().getServer()!=event.getServer())continue;
            iter.remove();release(body,state);
        }
    }
}
