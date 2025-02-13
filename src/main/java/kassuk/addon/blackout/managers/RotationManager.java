package kassuk.addon.blackout.managers;

import kassuk.addon.blackout.enums.RotationType;
import kassuk.addon.blackout.globalsettings.RotationSettings;
import kassuk.addon.blackout.utils.RotationUtils;
import kassuk.addon.blackout.utils.SettingUtils;
import kassuk.addon.blackout.utils.OLEPOSSUtils;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * @author OLEPOSSU
 */

public class RotationManager {

    public Target target = null;
    public double timer = 0;

    public float[] prevDir = new float[2];
    public float[] currentDir = new float[2];

    public float[] lastDir = new float[2];

    public double priority = 1000;
    public RotationSettings settings = null;
    public boolean unsent = false;
    public static List<Rotation> history = new ArrayList<>();
    public Target lastTarget = null;
    boolean shouldRotate = false;
    float[] next;
    boolean rotated = false;

    public RotationManager() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onTick(TickEvent.Pre event) {
        prevDir[0] = currentDir[0];
        prevDir[1] = currentDir[1];

        currentDir[0] = lastDir[0];
        currentDir[1] = lastDir[1];
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onMovePre(SendMovementPacketsEvent.Pre event) {
        unsent = true;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onMovePost(SendMovementPacketsEvent.Post event) {
        if (unsent && updateShouldRotate()) {
            updateNextRotation();

            if (rotated) {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(next[0], next[1], Managers.ONGROUND.isOnGround()));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onRender(Render3DEvent event) {
        if (mc.player == null) {
            return;
        }

        if (settings == null) {
            settings = Modules.get().get(RotationSettings.class);
        }
        timer -= event.frameTime;
        if (timer > 0 && target != null && lastDir != null) {
            if (SettingUtils.shouldVanillaRotate()) {
                mc.player.setYaw(MathHelper.lerpAngleDegrees(mc.getTickDelta(), prevDir[0], currentDir[0]));
                mc.player.setPitch(MathHelper.lerp(mc.getTickDelta(), prevDir[1], currentDir[1]));
            }
        } else if (target != null) {
            target = null;
            priority = 1000;
        } else {
            priority = 1000;
        }
    }

    public PlayerMoveC2SPacket onFull(PlayerMoveC2SPacket.Full packet) {
        unsent = false;
        if (!updateShouldRotate()) {
            return packet;
        }
        updateNextRotation();

        if (rotated) {
            return new PlayerMoveC2SPacket.Full(packet.getX(0), packet.getY(0), packet.getZ(0), next[0], next[1], packet.isOnGround());
        }

        return new PlayerMoveC2SPacket.PositionAndOnGround(packet.getX(0), packet.getY(0), packet.getZ(0), packet.isOnGround());
    }

    public PlayerMoveC2SPacket onPositionOnGround(PlayerMoveC2SPacket.PositionAndOnGround packet) {
        unsent = false;
        if (!updateShouldRotate()) {
            return packet;
        }
        updateNextRotation();

        if (rotated) {
            return new PlayerMoveC2SPacket.Full(packet.getX(0), packet.getY(0), packet.getZ(0), next[0], next[1], packet.isOnGround());
        }

        return packet;
    }

    public PlayerMoveC2SPacket onLookAndOnGround(PlayerMoveC2SPacket.LookAndOnGround packet) {
        unsent = false;
        if (!updateShouldRotate()) {
            return packet;
        }
        updateNextRotation();

        if (rotated) {
            return new PlayerMoveC2SPacket.LookAndOnGround(next[0], next[1], packet.isOnGround());
        }
        if (packet.isOnGround() != Managers.ONGROUND.isOnGround()) {
            return new PlayerMoveC2SPacket.OnGroundOnly(packet.isOnGround());
        }

        return null;
    }

    public PlayerMoveC2SPacket onOnlyOnground(PlayerMoveC2SPacket.OnGroundOnly packet) {
        unsent = false;
        if (!updateShouldRotate()) {
            return packet;
        }
        updateNextRotation();

        if (rotated) {
            return new PlayerMoveC2SPacket.LookAndOnGround(next[0], next[1], packet.isOnGround());
        }

        return packet;
    }

    private boolean updateShouldRotate() {
        shouldRotate = target != null && timer > 0;
        return shouldRotate;
    }

    private void updateNextRotation() {
        if (shouldRotate) {
            if (target instanceof BoxTarget) {
                ((BoxTarget) target).vec = getTargetPos();
                next = new float[]{RotationUtils.nextYaw(lastDir[0], Rotations.getYaw(((BoxTarget) target).vec), settings.yawStep.get()), RotationUtils.nextPitch(lastDir[1], Rotations.getPitch(((BoxTarget) target).vec), settings.pitchStep.get())};
            } else {
                next = new float[]{RotationUtils.nextYaw(lastDir[0], ((AngleTarget) target).yaw, settings.yawStep.get()), RotationUtils.nextPitch(lastDir[1], ((AngleTarget) target).pitch, settings.pitchStep.get())};
            }

            rotated = Math.abs(RotationUtils.yawAngle(next[0], lastDir[0])) > 0 || Math.abs(next[1] - lastDir[1]) > 0;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST + 100)
    private void onSend(PacketEvent.Sent event) {
        if (event.packet instanceof PlayerMoveC2SPacket packet) {
            if (packet.changesLook()) {
                lastDir = new float[]{packet.getYaw(0), packet.getPitch(0)};
                addHistory(lastDir[0], lastDir[1]);
            }
        }
    }

    public boolean isTarget(Box box) {
        if (!(lastTarget instanceof BoxTarget)) {
            return false;
        }

        return box.minX == ((BoxTarget) lastTarget).box.minX && box.minY == ((BoxTarget) lastTarget).box.minY && box.minZ == ((BoxTarget) lastTarget).box.minZ && box.maxX == ((BoxTarget) lastTarget).box.maxX && box.maxY == ((BoxTarget) lastTarget).box.maxY && box.maxZ == ((BoxTarget) lastTarget).box.maxZ;
    }

    public boolean isTarget(double yaw, double pitch) {
        if (!(lastTarget instanceof AngleTarget)) {
            return false;
        }

        return yaw == ((AngleTarget) lastTarget).yaw && pitch == ((AngleTarget) lastTarget).pitch;
    }

    public void end(Box box) {
        if (isTarget(box)) {
            priority = 1000;
        }
    }

    public void end(BlockPos pos) {
        end(Box.from(new BlockBox(pos)));
    }

    public void endYaw(double yaw, boolean reset) {
        if (!(target instanceof AngleTarget)) {
            return;
        }

        if (yaw == ((AngleTarget) target).yaw) {
            priority = 1000;
            if (reset) {
                target = null;
            }
        }
    }

    public void endPitch(double pitch, boolean reset) {
        if (!(target instanceof AngleTarget)) {
            return;
        }

        if (pitch == ((AngleTarget) target).pitch) {
            priority = 1000;
            if (reset) {
                target = null;
            }
        }
    }

    public boolean startYaw(double yaw, double p, RotationType type) {
        return start(yaw, lastDir[1], p, type);
    }

    public boolean startPitch(double pitch, double p, RotationType type) {
        return start(lastDir[0], pitch, p, type);
    }

    public boolean start(double yaw, double pitch, double p, RotationType type) {
        if (settings == null) {
            return false;
        }
        boolean alreadyRotated = lastDir[0] == yaw && lastDir[1] == pitch;
        if (p <= priority) {
            priority = p;
            lastTarget = target;

            target = new AngleTarget(yaw, pitch);
            timer = settings.getTime(type);
        }
        return alreadyRotated;
    }

    public boolean start(BlockPos pos, Box box, Vec3d vec, double p, RotationType type) {
        if (settings == null) {
            return false;
        }
        boolean alreadyRotated = SettingUtils.rotationCheck(box);

        if (p < priority || (p == priority && (!(target instanceof BoxTarget) || SettingUtils.rotationCheck(((BoxTarget) target).box)))) {
            if (!alreadyRotated) {
                priority = p;
            }
            lastTarget = target;

            target = pos != null ? new BoxTarget(pos, vec != null ? vec : OLEPOSSUtils.getMiddle(box), p, type) : new BoxTarget(box, vec != null ? vec : OLEPOSSUtils.getMiddle(box), p, type);
            timer = settings.getTime(type);
        }
        return alreadyRotated;
    }

    public boolean start(Box box, Vec3d vec, double p, RotationType type) {
        return start(null, box, vec, p, type);
    }

    public boolean start(Box box, double p, RotationType type) {
        return start(box, OLEPOSSUtils.getMiddle(box), p, type);
    }

    public boolean start(BlockPos pos, double p, RotationType type) {
        return start(Box.from(new BlockBox(pos)), p, type);
    }

    public boolean start(BlockPos pos, Vec3d vec, double p, RotationType type) {
        return start(pos, new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1), vec, p, type);
    }

    public void addHistory(double yaw, double pitch) {
        history.add(0, new Rotation(yaw, pitch, mc.player.getEyePos()));
        history = history.subList(0, Math.min(history.size(), 20));
    }

    public record Rotation(double yaw, double pitch, Vec3d vec) { }

    public Vec3d getTargetPos() {
        return ((BoxTarget) target).vec;
    }

    public void setHeadYaw(Args args) {
        if (!shouldRotate) {return;}

        args.set(1, prevDir[0]);
        args.set(2, currentDir[0]);
    }
    public void setBodyYaw(Args args) {
        if (!shouldRotate) {return;}

        args.set(1, prevDir[0]);
        args.set(2, currentDir[0]);
    }
    public void setPitch(Args args) {
        if (!shouldRotate) {return;}

        args.set(1, prevDir[1]);
        args.set(2, currentDir[1]);
    }

    private static class Target {}

    private static class BoxTarget extends Target {
        public final BlockPos pos;
        public final Box box;
        public final Vec3d targetVec;
        public Vec3d vec;
        public final double priority;
        public final RotationType type;

        public BoxTarget(BlockPos pos, Vec3d vec, double priority, RotationType type) {
            this.pos = pos;
            this.box = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
            this.vec = vec;
            this.targetVec = vec;
            this.priority = priority;
            this.type = type;
        }

        public BoxTarget(Box box, Vec3d vec, double priority, RotationType type) {
            this.pos = null;
            this.box = box;
            this.vec = vec;
            this.targetVec = vec;
            this.priority = priority;
            this.type = type;
        }
    }

    private static class AngleTarget extends Target {
        public final double yaw;
        public final double pitch;
        public boolean ended;

        public AngleTarget(double yaw, double pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.ended = false;
        }
    }
}
