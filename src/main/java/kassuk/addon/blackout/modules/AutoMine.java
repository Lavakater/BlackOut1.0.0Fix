package kassuk.addon.blackout.modules;

import kassuk.addon.blackout.BlackOut;
import kassuk.addon.blackout.BlackOutModule;
import kassuk.addon.blackout.enums.RotationType;
import kassuk.addon.blackout.enums.SwingState;
import kassuk.addon.blackout.enums.SwingType;
import kassuk.addon.blackout.globalsettings.SwingSettings;
import kassuk.addon.blackout.managers.Managers;
import kassuk.addon.blackout.utils.BOInvUtils;
import kassuk.addon.blackout.utils.OLEPOSSUtils;
import kassuk.addon.blackout.utils.SettingUtils;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.*;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author OLEPOSSU
 */

public class AutoMine extends BlackOutModule {
    public AutoMine() {
        super(BlackOut.BLACKOUT, "Auto Mine", "Automatically mines blocks to destroy your enemies.");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSpeed = settings.createGroup("Speed");
    private final SettingGroup sgExplode = settings.createGroup("Explode");

    private final SettingGroup sgCev = settings.createGroup("Cev");
    private final SettingGroup sgAntiSurround = settings.createGroup("Anti Surround");
    private final SettingGroup sgAntiBurrow = settings.createGroup("Anti Burrow");

    private final SettingGroup sgRender = settings.createGroup("Render");

    //--------------------General--------------------//
    private final Setting<Boolean> pauseEat = addPauseEat(sgGeneral);
    private final Setting<Boolean> pauseSword = sgGeneral.add(new BoolSetting.Builder()
        .name("Pause Sword")
        .description("Doesn't mine while holding sword.")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> newVer = sgGeneral.add(new BoolSetting.Builder()
        .name("1.12.2 Crystals")
        .description("Uses 1.12.2 crystal mechanics.")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> cc = sgGeneral.add(new BoolSetting.Builder()
        .name("CC Crystals")
        .description("Uses crystalpvp.cc crystal mechanics.")
        .defaultValue(false)
        .build()
    );
    private final Setting<SwitchMode> pickAxeSwitchMode = sgGeneral.add(new EnumSetting.Builder<SwitchMode>()
        .name("Pickaxe Switch Mode")
        .description("Method of switching. InvSwitch is used in most clients.")
        .defaultValue(SwitchMode.Silent)
        .build()
    );
    private final Setting<SwitchMode> crystalSwitchMode = sgGeneral.add(new EnumSetting.Builder<SwitchMode>()
        .name("Crystal Switch Mode")
        .description("Method of switching. InvSwitch is used in most clients.")
        .defaultValue(SwitchMode.Silent)
        .build()
    );
    private final Setting<Boolean> autoMine = sgGeneral.add(new BoolSetting.Builder()
        .name("Auto Mine")
        .description("Sets target block to the block you clicked.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> manualMine = sgGeneral.add(new BoolSetting.Builder()
        .name("Manual Mine")
        .description("Sets target block to the block you clicked.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> manualInsta = sgGeneral.add(new BoolSetting.Builder()
        .name("Manual Instant")
        .description("Uses civ mine when mining manually.")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> manualRangeReset = sgGeneral.add(new BoolSetting.Builder()
        .name("Manual Range Reset")
        .description("Resets manual mining if out of range.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> resetOnSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("Reset On Switch")
        .description("Resets mining when switched held item.")
        .defaultValue(false)
        .build()
    );
    /*
    private final Setting<Boolean> mineBeds = sgGeneral.add(new BoolSetting.Builder()
        .name("Mine Beds")
        .description("Allows the automine to mine beds.")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> mineAnchors = sgGeneral.add(new BoolSetting.Builder()
        .name("Mine Anchors")
        .description("Allows the automine to mine respawn anchors.")
        .defaultValue(false)
        .build()
    );*/

    //--------------------Speed--------------------//
    private final Setting<Double> speed = sgSpeed.add(new DoubleSetting.Builder()
        .name("Speed")
        .description("Vanilla speed multiplier.")
        .defaultValue(1)
        .min(0)
        .sliderRange(0, 2)
        .build()
    );
    private final Setting<Double> instaDelay = sgSpeed.add(new DoubleSetting.Builder()
        .name("Instant Delay")
        .description("Delay between civ mines.")
        .defaultValue(0.5)
        .min(0)
        .sliderRange(0, 1)
        .build()
    );
    private final Setting<Boolean> onGroundCheck = sgSpeed.add(new BoolSetting.Builder()
        .name("On Ground Check")
        .description("Mines 5x slower when not on ground.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> effectCheck = sgSpeed.add(new BoolSetting.Builder()
        .name("Effect Check")
        .description("Modifies mining speed depending on haste and mining fatigue.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> waterCheck = sgSpeed.add(new BoolSetting.Builder()
        .name("Water Check")
        .description("Mines 5x slower while submerged in water.")
        .defaultValue(true)
        .build()
    );

    //--------------------Explode--------------------//
    private final Setting<Double> explodeSpeed = sgExplode.add(new DoubleSetting.Builder()
        .name("Explode Speed")
        .description("How many times to attack a crystal every second.")
        .defaultValue(2)
        .min(0)
        .sliderRange(0, 2)
        .build()
    );
    private final Setting<Double> explodeTime = sgExplode.add(new DoubleSetting.Builder()
        .name("Explode Time")
        .description("Tries to attack a crystal for this many seconds.")
        .defaultValue(2)
        .min(0)
        .sliderRange(0, 10)
        .build()
    );

    //--------------------Cev--------------------//
    private final Setting<Priority> cevPriority = sgCev.add(new EnumSetting.Builder<Priority>()
        .name("Cev Priority")
        .description("Priority of cev.")
        .defaultValue(Priority.Normal)
        .build()
    );
    private final Setting<Boolean> instaCev = sgCev.add(new BoolSetting.Builder()
        .name("Instant Cev")
        .description("Only sends 1 mine start packet for each block.")
        .defaultValue(false)
        .build()
    );
    private final Setting<Priority> trapCevPriority = sgCev.add(new EnumSetting.Builder<Priority>()
        .name("Trap Cev Priority")
        .description("Priority of trap cev.")
        .defaultValue(Priority.Normal)
        .build()
    );
    private final Setting<Boolean> instaTrapCev = sgCev.add(new BoolSetting.Builder()
        .name("Instant Trap Cev")
        .description("Only sends 1 mine start packet for each block.")
        .defaultValue(false)
        .build()
    );
    private final Setting<Priority> surroundCevPriority = sgCev.add(new EnumSetting.Builder<Priority>()
        .name("Surround Cev Priority")
        .description("Priority of trap cev.")
        .defaultValue(Priority.Normal)
        .build()
    );
    private final Setting<Boolean> instaSurroundCev = sgCev.add(new BoolSetting.Builder()
        .name("Instant Surround Cev")
        .description("Only sends 1 mine start packet for each block.")
        .defaultValue(false)
        .build()
    );

    //--------------------Anti-Surround--------------------//
    private final Setting<Priority> surroundMinerPriority = sgAntiSurround.add(new EnumSetting.Builder<Priority>()
        .name("Surround Miner Priority")
        .description("Priority of surround miner.")
        .defaultValue(Priority.Normal)
        .build()
    );
    private final Setting<Boolean> instaSurroundMiner = sgAntiSurround.add(new BoolSetting.Builder()
        .name("Instant Surround Miner")
        .description("Only sends 1 mine start packet for each block.")
        .defaultValue(false)
        .build()
    );
    private final Setting<Priority> autoCityPriority = sgAntiSurround.add(new EnumSetting.Builder<Priority>()
        .name("Auto City Priority")
        .description("Priority of anti surround. Places crystal next to enemy's surround block.")
        .defaultValue(Priority.Normal)
        .build()
    );
    private final Setting<Boolean> instaAutoCity = sgAntiSurround.add(new BoolSetting.Builder()
        .name("Instant Auto City")
        .description("Only sends 1 mine start packet for each block.")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> explodeCrystal = sgAntiSurround.add(new BoolSetting.Builder()
        .name("Explode Crystal")
        .description("Attacks the crystal we placed.")
        .defaultValue(false)
        .build()
    );

    //--------------------Anti-Burrow--------------------//
    private final Setting<Priority> antiBurrowPriority = sgAntiBurrow.add(new EnumSetting.Builder<Priority>()
        .name("Anti Burrow Priority")
        .description("Priority of anti burrow.")
        .defaultValue(Priority.Normal)
        .build()
    );

    //--------------------Render--------------------//
    private final Setting<Double> animationExp = sgRender.add(new DoubleSetting.Builder()
        .name("Animation Exponent")
        .description("3 - 4 look cool.")
        .defaultValue(3)
        .range(0, 10)
        .sliderRange(0, 10)
        .build()
    );
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("Shape Mode")
        .description("Which parts of render should be rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );
    private final Setting<SettingColor> lineStartColor = sgRender.add(new ColorSetting.Builder()
        .name("Line Start Color")
        .description(BlackOut.COLOR)
        .defaultValue(new SettingColor(255, 0, 0, 0))
        .build()
    );
    private final Setting<SettingColor> lineEndColor = sgRender.add(new ColorSetting.Builder()
        .name("Line End Color")
        .description(BlackOut.COLOR)
        .defaultValue(new SettingColor(255, 0, 0, 255))
        .build()
    );
    private final Setting<SettingColor> startColor = sgRender.add(new ColorSetting.Builder()
        .name("Side Start Color")
        .description(BlackOut.COLOR)
        .defaultValue(new SettingColor(255, 0, 0, 0))
        .build()
    );
    private final Setting<SettingColor> endColor = sgRender.add(new ColorSetting.Builder()
        .name("Side End Color")
        .description(BlackOut.COLOR)
        .defaultValue(new SettingColor(255, 0, 0, 50))
        .build()
    );

    private double minedFor = 0;
    private Target target = null;
    private boolean started = false;
    private BlockPos civPos = null;

    private List<AbstractClientPlayerEntity> enemies = new ArrayList<>();

    private long lastTime = 0;
    private long lastPlace = 0;
    private long lastExplode = 0;
    private long lastCiv = 0;

    private double render = 1;

    private double delta = 0;
    private boolean ignore = false;

    private final Map<BlockPos, Long> explodeAt = new HashMap<>();

    private boolean reset = false;

    @Override
    public void onActivate() {
        target = null;
        minedFor = 0;
        started = false;
        lastTime = System.currentTimeMillis();
        civPos = null;
        reset = false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onSend(PacketEvent.Send event) {
        if (event.packet instanceof PlayerActionC2SPacket packet) {
            if (ignore) return;


            switch (packet.getAction()) {
                case START_DESTROY_BLOCK -> handleStart(event);
                case ABORT_DESTROY_BLOCK -> handleAbort(event);
                case STOP_DESTROY_BLOCK -> handleStop(event);
            }
        }
        if (event.packet instanceof UpdateSelectedSlotC2SPacket && resetOnSwitch.get()) {
            reset = true;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onRender(Render3DEvent event) {
        delta = (System.currentTimeMillis() - lastTime) / 1000d;
        lastTime = System.currentTimeMillis();

        update();
        explodeUpdate();
        render(event.renderer);
    }


    private void explodeUpdate() {
        Entity targetCrystal = null;

        List<BlockPos> toRemove = new ArrayList<>();

        for (Map.Entry<BlockPos, Long> entry : explodeAt.entrySet()) {
            if (System.currentTimeMillis() - entry.getValue() > explodeTime.get() * 1000) {
                toRemove.add(entry.getKey());
            }

            EndCrystalEntity crystal = crystalAt(entry.getKey());

            if (crystal != null) {
                targetCrystal = crystal;
                break;
            }
        }
        toRemove.forEach(explodeAt::remove);

        if (targetCrystal != null && !isPaused() && System.currentTimeMillis() - lastExplode > (1000 / explodeSpeed.get())) {
            if (!SettingUtils.shouldRotate(RotationType.Attacking) || Managers.ROTATION.start(targetCrystal.getBoundingBox(), priority, RotationType.Breaking)) {

                SettingUtils.swing(SwingState.Pre, SwingType.Attacking, Hand.MAIN_HAND);
                sendPacket(PlayerInteractEntityC2SPacket.attack(targetCrystal, mc.player.isSneaking()));
                SettingUtils.swing(SwingState.Post, SwingType.Attacking, Hand.MAIN_HAND);

                lastExplode = System.currentTimeMillis();

                if (SettingUtils.shouldRotate(RotationType.Attacking))
                    Managers.ROTATION.end(targetCrystal.getBoundingBox());
            }
        }
    }

    private void render(Renderer3D r) {
        if (target == null) return;

        int slot = fastestSlot();

        render = MathHelper.clamp(getMineTicks(slot, true) == getMineTicks(slot, false) ? render + delta * 2 : render - delta * 2, -2, 2);

        // Normal Speed
        double p = 1 - MathHelper.clamp(minedFor / getMineTicks(slot, false), 0, 1);
        p = Math.pow(p, animationExp.get());
        p = 1 - p;

        r.box(getRenderBox(p / 2), getColor(startColor.get(), endColor.get(), p, MathHelper.clamp(render, 0, 1)), getColor(lineStartColor.get(), lineEndColor.get(), p, MathHelper.clamp(render, 0, 1)), shapeMode.get(), 0);

        // Modified Speed
        p = 1 - MathHelper.clamp(minedFor / getMineTicks(slot, true), 0, 1);
        p = Math.pow(p, animationExp.get());
        p = 1 - p;

        r.box(getRenderBox(p / 2), getColor(startColor.get(), endColor.get(), p, MathHelper.clamp(-render, 0, 1)), getColor(lineStartColor.get(), lineEndColor.get(), p, MathHelper.clamp(-render, 0, 1)), shapeMode.get(), 0);
    }

    private void update() {
        if (mc.world == null) {
            return;
        }

        if (reset) {
            if (target != null && !target.manual) {
                target = null;
            }
            started = false;
            reset = false;
        }

        enemies = mc.world.getPlayers().stream().filter(player -> player != mc.player && !Friends.get().isFriend(player) && player.distanceTo(mc.player) < 10).toList();

        BlockPos lastPos = target == null || target.pos == null ? null : target.pos;

        if (target != null && target.manual && manualRangeReset.get() && !SettingUtils.inMineRange(target.pos)) {
            target = null;
            started = false;
        }

        if (target == null || !target.manual) {
            target = getTarget();
        }

        if (target == null) {
            return;
        }

        if (target.pos != null && !target.pos.equals(lastPos)) {
            if (started) {
                Direction dir = SettingUtils.getPlaceOnDirection(target.pos);
                send(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, target.pos, dir == null ? Direction.UP : dir, 0));
            }
            started = false;
        }

        if (!started) {
            boolean rotated = !SettingUtils.startMineRot() || Managers.ROTATION.start(target.pos, priority, RotationType.Breaking);

            if (rotated) {
                started = true;
                minedFor = 0;
                civPos = null;

                if (getMineTicks(fastestSlot(), true) == getMineTicks(fastestSlot(), false)) {
                    render = 2;
                } else {
                    render = -2;
                }

                Direction dir = SettingUtils.getPlaceOnDirection(target.pos);

                send(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, target.pos, dir == null ? Direction.UP : dir, 0));
                SettingUtils.mineSwing(SwingSettings.MiningSwingState.Start);

                if (SettingUtils.startMineRot()) {
                    Managers.ROTATION.end(target.pos);
                }
            }
        }

        if (!started) {
            return;
        }

        minedFor += delta * 20;

        if (isPaused()) {
            return;
        }

        if (!miningCheck(fastestSlot())) {
            return;
        }

        if (!civCheck()) {
            return;
        }

        if (!crystalCheck()) {
            return;
        }

        boolean rotated = !SettingUtils.endMineRot() || Managers.ROTATION.start(target.pos, priority, RotationType.Breaking);

        if (!rotated) {
            return;
        }

        endMine();
    }

    private boolean isPaused() {
        if (pauseEat.get() && mc.player.isUsingItem()) {
            return true;
        }
        if (pauseSword.get() && mc.player.getMainHandStack().getItem() instanceof SwordItem) {
            return true;
        }
        return false;
    }

    private boolean civCheck() {
        if (civPos == null) {
            return true;
        }
        if (System.currentTimeMillis() - lastCiv < instaDelay.get() * 1000) {
            return false;
        }
        return OLEPOSSUtils.solid2(civPos);
    }

    private void endMine() {
        int slot = fastestSlot();

        boolean switched = miningCheck(Managers.HOLDING.slot);
        boolean swapBack = false;

        Direction dir = SettingUtils.getPlaceOnDirection(target.pos);

        if (dir == null) {
            return;
        }

        if (SettingUtils.shouldRotate(RotationType.Breaking) && !Managers.ROTATION.start(target.pos, priority, RotationType.Breaking)) {
            return;
        }

        if (!switched) {
            switch (pickAxeSwitchMode.get()) {
                case Silent -> {
                    switched = true;
                    InvUtils.swap(slot, true);
                }
                case PickSilent -> {
                    switched = true;
                    BOInvUtils.pickSwitch(slot);
                }
                case InvSwitch -> switched = BOInvUtils.invSwitch(slot);
            }
            swapBack = switched;
        }

        if (!switched) {
            return;
        }

        send(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, target.pos, dir, 0));
        SettingUtils.mineSwing(SwingSettings.MiningSwingState.End);

        if (target.civ) {
            civPos = target.pos;
        }

        if (SettingUtils.endMineRot()) {
            Managers.ROTATION.end(target.pos);
        }

        if (swapBack) {
            switch (pickAxeSwitchMode.get()) {
                case Silent -> InvUtils.swapBack();
                case PickSilent -> BOInvUtils.pickSwapBack();
                case InvSwitch -> BOInvUtils.swapBack();
            }
        }

        if (!target.civ) {
            target = null;
            minedFor = 0;
        } else {
            civPos = target.pos;
            lastCiv = System.currentTimeMillis();
        }
    }

    private boolean crystalCheck() {
        switch (target.type) {
            case Cev, TrapCev, SurroundCev -> {
                if (crystalAt(target.crystalPos) != null) {
                    return true;
                }
                if (!EntityUtils.intersectsWithEntity(Box.from(new BlockBox(target.crystalPos)).withMaxY(target.crystalPos.getY() + 2), entity -> !entity.isSpectator())) {
                    placeCrystal();
                    return false;
                }
            }
            case AutoCity -> {
                if (crystalAt(target.crystalPos) != null) {
                    return true;
                }
                if (!EntityUtils.intersectsWithEntity(Box.from(new BlockBox(target.crystalPos)).withMaxY(target.crystalPos.getY() + 2), entity -> !entity.isSpectator())) {
                    return placeCrystal();
                }
            }
            default -> {
                return true;
            }
        }
        return false;
    }

    private EndCrystalEntity crystalAt(BlockPos pos) {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity crystal && entity.getBlockPos().equals(pos)) {
                return crystal;
            }
        }
        return null;
    }

    private boolean placeCrystal() {
        if (System.currentTimeMillis() - lastPlace < 250) {
            return false;
        }

        Hand hand = getHand();

        int crystalSlot = InvUtils.find(Items.END_CRYSTAL).slot();
        if (hand == null && crystalSlot < 0) {
            return false;
        }

        Direction dir = SettingUtils.getPlaceOnDirection(target.crystalPos.down());

        if (dir == null) {
            return false;
        }

        boolean rotated = !SettingUtils.shouldRotate(RotationType.Placing) || Managers.ROTATION.start(target.crystalPos.down(), priority, RotationType.Placing);

        if (!rotated) {
            return false;
        }

        boolean switched = hand != null;

        if (!switched) {
            switch (crystalSwitchMode.get()) {
                case Silent -> {
                    switched = true;
                    InvUtils.swap(crystalSlot, true);
                }
                case PickSilent -> switched = BOInvUtils.pickSwitch(crystalSlot);
                case InvSwitch -> switched = BOInvUtils.invSwitch(crystalSlot);
            }
        }

        if (!switched) {
            return false;
        }

        SettingUtils.swing(SwingState.Pre, SwingType.Placing, hand == null ? Hand.MAIN_HAND : hand);
        sendPacket(new PlayerInteractBlockC2SPacket(hand == null ? Hand.MAIN_HAND : hand, new BlockHitResult(Vec3d.ofCenter(target.crystalPos.down()), dir, target.crystalPos.down(), false), 0));
        SettingUtils.swing(SwingState.Post, SwingType.Placing, hand == null ? Hand.MAIN_HAND : hand);

        lastPlace = System.currentTimeMillis();

        if (shouldExplode()) {
            addExplode();
        }

        Managers.ROTATION.end(target.crystalPos.down());

        if (hand == null) {
            switch (crystalSwitchMode.get()) {
                case Silent -> InvUtils.swapBack();
                case PickSilent -> BOInvUtils.pickSwapBack();
                case InvSwitch -> BOInvUtils.swapBack();
            }
        }

        return true;
    }

    private void addExplode() {
        explodeAt.remove(target.crystalPos);
        explodeAt.put(target.crystalPos, System.currentTimeMillis());
    }

    private boolean shouldExplode() {
        return switch (target.type) {
            case Cev, SurroundCev, TrapCev -> true;
            case SurroundMiner, AntiBurrow, Manual -> false;
            case AutoCity -> explodeCrystal.get();
        };
    }

    private Target getTarget() {
        Target target = null;

        if (!autoMine.get()) {
            return target;
        }

        if (priorityCheck(target, cevPriority.get())) {
            Target t = getCev();
            if (t != null) {
                target = t;
            }
        }
        if (priorityCheck(target, trapCevPriority.get())) {
            Target t = getTrapCev();
            if (t != null) {
                target = t;
            }
        }
        if (priorityCheck(target, surroundCevPriority.get())) {
            Target t = getSurroundCev();
            if (t != null) {
                target = t;
            }
        }
        if (priorityCheck(target, surroundMinerPriority.get())) {
            Target t = getSurroundMiner();
            if (t != null) {
                target = t;
            }
        }
        if (priorityCheck(target, autoCityPriority.get())) {
            Target t = getAutoCity();
            if (t != null) {
                target = t;
            }
        }
        if (priorityCheck(target, antiBurrowPriority.get())) {
            Target t = getAntiBurrow();
            if (t != null) {
                target = t;
            }
        }
        return target;
    }

    private Target getCev() {
        boolean civ = instaCev.get();
        Target best = null;
        double distance = 1000;
        for (AbstractClientPlayerEntity player : enemies) {
            BlockPos pos = new BlockPos(player.getBlockX(), (int) Math.floor(player.getBoundingBox().maxY) + 1, player.getBlockZ());

            if (!(civ && pos.equals(civPos)) && getBlock(pos) != Blocks.OBSIDIAN) {
                continue;
            }
            if ((civ && pos.equals(civPos)) && !(getBlock(pos) instanceof AirBlock) && getBlock(pos) != Blocks.OBSIDIAN) {
                continue;
            }

            if (getBlock(pos.up()) != Blocks.AIR) {
                continue;
            }
            if (newVer.get() && getBlock(pos.up(2)) != Blocks.AIR) {
                continue;
            }

            if (!SettingUtils.inMineRange(pos)) {
                continue;
            }
            if (!SettingUtils.inPlaceRange(pos)) {
                continue;
            }
            if (!SettingUtils.inAttackRange(OLEPOSSUtils.getCrystalBox(pos.up()))) {
                continue;
            }

            if (blocked(pos.up())) {
                continue;
            }

            double d = mc.player.getEyePos().distanceTo(Vec3d.ofCenter(pos));

            if (distanceCheck(civ, pos, distance, d)) {
                best = new Target(pos, pos.up(), MineType.Cev, cevPriority.get().priority + (civ && pos.equals(civPos) ? 0.1 : 0), civ, false);
                distance = d;
            }
        }
        return best;
    }

    Target getTrapCev() {
        boolean civ = instaTrapCev.get();
        Target best = null;
        double distance = 1000;
        for (AbstractClientPlayerEntity player : enemies) {
            for (CardinalDirection cd : CardinalDirection.values()) {
                Direction dir = cd.toDirection();
                BlockPos pos = new BlockPos(player.getBlockX(), (int) Math.floor(player.getBoundingBox().maxY), player.getBlockZ()).offset(dir);

                if (!(civ && pos.equals(civPos)) && getBlock(pos) != Blocks.OBSIDIAN) {
                    continue;
                }
                if ((civ && pos.equals(civPos)) && !(getBlock(pos) instanceof AirBlock) && getBlock(pos) != Blocks.OBSIDIAN) {
                    continue;
                }

                if (getBlock(pos.up()) != Blocks.AIR) {
                    continue;
                }
                if (newVer.get() && getBlock(pos.up(2)) != Blocks.AIR) {
                    continue;
                }

                if (!SettingUtils.inMineRange(pos)) {
                    continue;
                }
                if (!SettingUtils.inPlaceRange(pos)) {
                    continue;
                }
                if (!SettingUtils.inAttackRange(OLEPOSSUtils.getCrystalBox(pos.up()))) {
                    continue;
                }

                if (blocked(pos.up())) {
                    continue;
                }

                double d = mc.player.getEyePos().distanceTo(Vec3d.ofCenter(pos));

                if (distanceCheck(civ, pos, distance, d)) {
                    best = new Target(pos, pos.up(), MineType.TrapCev, trapCevPriority.get().priority + (civ && pos.equals(civPos) ? 0.1 : 0), civ, false);
                    distance = d;
                }
            }
        }
        return best;
    }

    private Target getSurroundCev() {
        boolean civ = instaSurroundCev.get();
        Target best = null;
        double distance = 1000;
        for (AbstractClientPlayerEntity player : enemies) {
            for (CardinalDirection cd : CardinalDirection.values()) {
                Direction dir = cd.toDirection();
                BlockPos pos = player.getBlockPos().offset(dir);

                if (!(civ && pos.equals(civPos)) && getBlock(pos) != Blocks.OBSIDIAN) {
                    continue;
                }
                if ((civ && pos.equals(civPos)) && !(getBlock(pos) instanceof AirBlock) && getBlock(pos) != Blocks.OBSIDIAN) {
                    continue;
                }

                if (getBlock(pos.up()) != Blocks.AIR) {
                    continue;
                }
                if (newVer.get() && getBlock(pos.up(2)) != Blocks.AIR) {
                    continue;
                }

                if (!SettingUtils.inMineRange(pos)) {
                    continue;
                }
                if (!SettingUtils.inPlaceRange(pos)) {
                    continue;
                }
                if (!SettingUtils.inAttackRange(OLEPOSSUtils.getCrystalBox(pos.up()))) {
                    continue;
                }

                if (blocked(pos.up())) {
                    continue;
                }

                double d = mc.player.getEyePos().distanceTo(Vec3d.ofCenter(pos));

                if (distanceCheck(civ, pos, distance, d)) {
                    best = new Target(pos, pos.up(), MineType.SurroundCev, surroundCevPriority.get().priority + (civ && pos.equals(civPos) ? 0.1 : 0), civ, false);
                    distance = d;
                }
            }
        }
        return best;
    }

    private Target getSurroundMiner() {
        boolean civ = instaSurroundMiner.get();
        Target best = null;
        double distance = 1000;
        for (AbstractClientPlayerEntity player : enemies) {
            for (CardinalDirection cd : CardinalDirection.values()) {
                Direction dir = cd.toDirection();
                BlockPos pos = player.getBlockPos().offset(dir);

                if (((!civ || !pos.equals(civPos)) && !OLEPOSSUtils.solid2(pos)) || getBlock(pos) == Blocks.BEDROCK) {
                    continue;
                }

                if (!SettingUtils.inMineRange(pos)) {
                    continue;
                }

                double d = mc.player.getEyePos().distanceTo(Vec3d.ofCenter(pos));

                if (distanceCheck(civ, pos, distance, d)) {
                    best = new Target(pos, null, MineType.SurroundMiner, surroundMinerPriority.get().priority + (civ && pos.equals(civPos) ? 0.1 : 0), civ, false);
                    distance = d;
                }
            }
        }
        return best;
    }

    private Target getAutoCity() {
        boolean civ = instaAutoCity.get();
        Target best = null;
        double distance = 1000;
        for (AbstractClientPlayerEntity player : enemies) {
            for (CardinalDirection cd : CardinalDirection.values()) {
                Direction dir = cd.toDirection();
                BlockPos pos = player.getBlockPos().offset(dir);

                if (((!civ || !pos.equals(civPos)) && !OLEPOSSUtils.solid2(pos)) || getBlock(pos) == Blocks.BEDROCK) {
                    continue;
                }

                if (getBlock(pos.offset(dir)) != Blocks.AIR) {
                    continue;
                }
                if (newVer.get() && getBlock(pos.offset(dir).up()) != Blocks.AIR) {
                    continue;
                }
                if (!crystalBlock(pos.offset(dir).down())) {
                    continue;
                }

                if (!SettingUtils.inMineRange(pos)) {
                    continue;
                }
                if (!SettingUtils.inPlaceRange(pos.offset(dir).down())) {
                    continue;
                }

                if (blocked(pos.offset(dir))) {
                    continue;
                }

                double d = mc.player.getEyePos().distanceTo(Vec3d.ofCenter(pos));

                if (distanceCheck(civ, pos, distance, d)) {
                    best = new Target(pos, pos.offset(dir), MineType.AutoCity, autoCityPriority.get().priority + (civ && pos.equals(civPos) ? 0.1 : 0), civ, false);
                    distance = d;
                }
            }
        }
        return best;
    }

    private Target getAntiBurrow() {
        Target best = null;
        double distance = 1000;
        for (AbstractClientPlayerEntity player : enemies) {
            BlockPos pos = player.getBlockPos();

            if (!OLEPOSSUtils.solid2(pos) || getBlock(pos) == Blocks.BEDROCK) {
                continue;
            }

            if (!SettingUtils.inMineRange(pos)) {
                continue;
            }

            double d = mc.player.getEyePos().distanceTo(Vec3d.ofCenter(pos));

            if (d < distance) {
                best = new Target(pos, null, MineType.AntiBurrow, antiBurrowPriority.get().priority, false, false);
                distance = d;
            }
        }
        return best;
    }

    private boolean distanceCheck(boolean civ, BlockPos pos, double closest, double distance) {
        if (civ && pos.equals(civPos)) {
            return true;
        }
        if (target != null && pos.equals(target.pos)) {
            return true;
        }

        return distance < closest;
    }

    private boolean priorityCheck(Target current, Priority priority) {
        if (priority.priority < 0) {
            return false;
        }
        if (current == null) {
            return true;
        }

        return priority.priority >= current.priority;
    }

    private void send(Packet<?> packet) {
        ignore = true;
        sendPacket(packet);
        ignore = false;
    }

    private void handleStart(PacketEvent.Send event) {
        event.cancel();

        BlockPos pos = ((PlayerActionC2SPacket) event.packet).getPos();
        if (target != null && pos.equals(target.pos)) {
            return;
        }
        if (manualMine.get() && getBlock(pos) != Blocks.BEDROCK) {
            started = false;

            target = new Target(pos, null, MineType.Manual, 0, manualInsta.get(), true);
        }
    }

    private void handleAbort(PacketEvent.Send event) {
        event.cancel();
    }

    private void handleStop(PacketEvent.Send event) {
        event.cancel();
    }

    private Block getBlock(BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock();
    }

    private Hand getHand() {
        if (mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL) {
            return Hand.OFF_HAND;
        }
        if (Managers.HOLDING.isHolding(Items.END_CRYSTAL)) {
            return Hand.MAIN_HAND;
        }
        return null;
    }

    private boolean miningCheck(int slot) {
        if (target == null || target.pos == null) {
            return false;
        }
        return minedFor * speed.get() >= getMineTicks(slot, true);
    }

    private float getTime(BlockPos pos, int slot, boolean speedMod) {
        BlockState state = mc.world.getBlockState(pos);
        float f = state.getHardness(mc.world, pos);
        if (f == -1.0F) {
            return 0.0F;
        } else {
            float i = !state.isToolRequired() || mc.player.getInventory().getStack(slot).isSuitableFor(state) ? 30 : 100;
            return getSpeed(state, slot, speedMod) / f / i;
        }
    }

    private float getMineTicks(int slot, boolean speedMod) {
        return slot == -1 ? slot : (float) (1 / (getTime(target.pos, slot, speedMod) * speed.get()));
    }

    private float getSpeed(BlockState state, int slot, boolean speedMod) {
        ItemStack stack = mc.player.getInventory().getStack(slot);
        float f = mc.player.getInventory().getStack(slot).getMiningSpeedMultiplier(state);
        if (f > 1.0) {
            int i = EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, stack);
            if (i > 0 && !stack.isEmpty()) f += (float) (i * i + 1);
        }

        if (!speedMod) return f;


        if (effectCheck.get()) {
            if (StatusEffectUtil.hasHaste(mc.player)) {
                f *= 1.0 + (float) (StatusEffectUtil.getHasteAmplifier(mc.player) + 1) * 0.2F;
            }
            if (mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
                f *= Math.pow(0.3, mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier() + 1);
            }
        }

        if (waterCheck.get() && mc.player.isSubmergedInWater() && !EnchantmentHelper.hasAquaAffinity(mc.player)) {
            f /= 5.0;
        }

        if (onGroundCheck.get() && !mc.player.isOnGround()) {
            f /= 5.0;
        }

        return f;
    }

    private int fastestSlot() {
        int slot = -1;
        if (mc.player == null || mc.world == null) {
            return -1;
        }
        for (int i = 0; i < (pickAxeSwitchMode.get() == SwitchMode.Silent ? 9 : 35); i++) {
            if (slot == -1 || (mc.player.getInventory().getStack(i).getMiningSpeedMultiplier(mc.world.getBlockState(target.pos)) > mc.player.getInventory().getStack(slot).getMiningSpeedMultiplier(mc.world.getBlockState(target.pos)))) {
                slot = i;
            }
        }
        return slot;
    }

    private Color getColor(Color start, Color end, double progress, double alphaMulti) {
        return new Color(
            lerp(start.r, end.r, progress, 1),
            lerp(start.g, end.g, progress, 1),
            lerp(start.b, end.b, progress, 1),
            lerp(start.a, end.a, progress, alphaMulti));
    }

    private int lerp(double start, double end, double d, double multi) {
        return (int) Math.round((start + (end - start) * d) * multi);
    }

    private boolean crystalBlock(BlockPos pos) {
        return getBlock(pos) == Blocks.OBSIDIAN || getBlock(pos) == Blocks.BEDROCK;
    }

    private Box getRenderBox(double progress) {
        return new Box(target.pos.getX() + 0.5 - progress, target.pos.getY() + 0.5 - progress, target.pos.getZ() + 0.5 - progress, target.pos.getX() + 0.5 + progress, target.pos.getY() + 0.5 + progress, target.pos.getZ() + 0.5 + progress);
    }

    private boolean blocked(BlockPos pos) {
        Box box = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + (cc.get() ? 1 : 2), pos.getZ() + 1);

        return EntityUtils.intersectsWithEntity(box, entity -> entity instanceof PlayerEntity && !entity.isSpectator());
    }

    public BlockPos targetPos() {
        return target == null ? null : target.pos;
    }

    public enum SwitchMode {
        Silent,
        PickSilent,
        InvSwitch
    }

    public enum Priority {
        Highest(6),
        Higher(5),
        High(4),
        Normal(3),
        Low(2),
        Lower(1),
        Lowest(0),
        Disabled(-1);

        public final int priority;

        Priority(int priority) {
            this.priority = priority;
        }
    }

    public enum MineType {
        Cev,
        TrapCev,
        SurroundCev,
        SurroundMiner,
        AutoCity,
        AntiBurrow,
        Manual
    }

    private record Target(BlockPos pos, BlockPos crystalPos, MineType type, double priority, boolean civ,
                          boolean manual) {
    }
}
