package kassuk.addon.blackout.modules;

import kassuk.addon.blackout.BlackOut;
import kassuk.addon.blackout.BlackOutModule;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Random;


/**
 * @author KassuK
 */

public class AnteroTaateli extends BlackOutModule {
    public AnteroTaateli() {
        super(BlackOut.BLACKOUT, "Auto Andrew Tate", "What colour is your bugatti?");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> iFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("Ignore Friends")
        .description("Doesn't send messages targeted to friends.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder()
        .name("Delay")
        .description("Tick delay between messages.")
        .defaultValue(50)
        .min(0)
        .sliderRange(0, 100)
        .build()
    );

    private double timer = 0;
    private final Random r = new Random();

    private final String[] messages = new String[]{
        "Hey brokies top G here",
        "Top G drinks sparkling water and breathes air",
        "I hate dead people all you do is fucking laying down like pussies",
        "Get up and do some push-ups",
        "Top G is never late time is just running ahead of schedule",
        "<NAME>, what color is your Bugatti?"
    };

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        timer++;
        if (mc.player != null && mc.world != null) {
            PlayerEntity bugatti = getClosest();
            if (timer >= delay.get() && bugatti != null) {
                timer = 0;
                ChatUtils.sendPlayerMsg(getMessage(bugatti));
            }
        }
    }

    private String getMessage(PlayerEntity pl) {
        int index = r.nextInt(0, messages.length);
        String msg = messages[index];
        return msg.replace("<NAME>", pl.getName().getString());
    }

    private PlayerEntity getClosest() {
        PlayerEntity closest = null;
        float distance = -1;
        if (!mc.world.getPlayers().isEmpty()) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player != mc.player && (!iFriends.get() || !Friends.get().isFriend(player))) {
                    if (closest == null || mc.player.getPos().distanceTo(player.getPos()) < distance) {
                        closest = player;
                        assert mc.player != null;
                        distance = (float) mc.player.getPos().distanceTo(player.getPos());
                    }
                }
            }
        }
        return closest;
    }
}
