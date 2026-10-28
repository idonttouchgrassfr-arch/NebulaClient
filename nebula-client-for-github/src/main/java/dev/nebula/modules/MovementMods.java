package dev.nebula.modules;

import dev.nebula.core.*;
import dev.nebula.util.Util;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

import static dev.nebula.core.Mod.mc;

public final class MovementMods {
    private MovementMods() { }

    public static void init() {
        ModManager.register(new Mod("Sprint", "Always sprints while moving", Category.MOVEMENT) {
            final Setting.Bool omni = add(new Setting.Bool("Omni", false));
            @Override public void onTick() {
                var p = mc.player;
                if (p.isSneaking() || p.horizontalCollision || p.getHungerManager().getFoodLevel() <= 6) return;
                if (omni.get() ? Util.moving() : p.input.movementForward > 0) p.setSprinting(true);
            }
        });

        ModManager.register(new Mod("Flight", "Lets you fly", Category.MOVEMENT) {
            final Setting.Mode mode = add(new Setting.Mode("Mode", "Creative", "Motion"));
            final Setting.Num speed = add(new Setting.Num("Speed", 2, 0.5, 10, 0.5));
            final Setting.Bool antiKick = add(new Setting.Bool("AntiKick", true));
            int tick;

            @Override public void onTick() {
                var p = mc.player;
                if (mode.is("Creative")) {
                    p.getAbilities().allowFlying = true;
                    p.getAbilities().flying = true;
                    p.getAbilities().setFlySpeed((float) (0.05 * speed.get()));
                } else {
                    double vy = 0;
                    if (mc.options.jumpKey.isPressed()) vy = speed.get() * 0.5;
                    if (mc.options.sneakKey.isPressed()) vy = -speed.get() * 0.5;
                    if (antiKick.get() && vy == 0 && ++tick % 20 == 0) vy = -0.04;
                    Util.setSpeed(speed.get() * 0.4);
                    Vec3d v = p.getVelocity();
                    p.setVelocity(v.x, vy, v.z);
                    p.fallDistance = 0;
                }
            }
            @Override public void onDisable() {
                var p = mc.player;
                if (p == null) return;
                p.getAbilities().setFlySpeed(0.05f);
                if (!p.isCreative() && !p.isSpectator()) {
                    p.getAbilities().allowFlying = false;
                    p.getAbilities().flying = false;
                }
            }
            @Override public String info() { return mode.get(); }
        });

        ModManager.register(new Mod("Speed", "Moves faster on the ground", Category.MOVEMENT) {
            final Setting.Num speed = add(new Setting.Num("Speed", 1.5, 1, 5, 0.1));
            final Setting.Bool hop = add(new Setting.Bool("BunnyHop", true));
            @Override public void onTick() {
                var p = mc.player;
                if (!Util.moving() || p.isSneaking() || p.isTouchingWater() || p.isFallFlying() || p.getAbilities().flying) return;
                if (hop.get() && p.isOnGround()) p.jump();
                Util.setSpeed(0.2873 * speed.get());
            }
            @Override public String info() { return speed.display(); }
        });

        ModManager.register(new Mod("Step", "Step up blocks instantly", Category.MOVEMENT) {
            final Setting.Num height = add(new Setting.Num("Height", 2, 1, 5, 0.5));
            @Override public void onTick() {
                var attr = mc.player.getAttributeInstance(EntityAttributes.GENERIC_STEP_HEIGHT);
                if (attr != null) attr.setBaseValue(height.get());
            }
            @Override public void onDisable() {
                if (mc.player == null) return;
                var attr = mc.player.getAttributeInstance(EntityAttributes.GENERIC_STEP_HEIGHT);
                if (attr != null) attr.setBaseValue(0.6);
            }
        });

        ModManager.register(new Mod("NoFall", "Prevents fall damage", Category.MOVEMENT) {
            @Override public void onTick() {
                var p = mc.player;
                if (p.fallDistance > 2f && !p.isFallFlying())
                    p.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
            }
        });

        ModManager.register(new Mod("Spider", "Climb walls like a spider", Category.MOVEMENT) {
            @Override public void onTick() {
                var p = mc.player;
                if (p.horizontalCollision) { Vec3d v = p.getVelocity(); p.setVelocity(v.x, 0.2, v.z); }
            }
        });

        ModManager.register(new Mod("WaterFloat", "Float up and walk on water", Category.MOVEMENT) {
            @Override public void onTick() {
                var p = mc.player;
                if (p.isSneaking() || mc.options.jumpKey.isPressed() || !p.isTouchingWater()) return;
                Vec3d v = p.getVelocity();
                if (v.y < 0.11) p.setVelocity(v.x, 0.11, v.z);
            }
        });

        ModManager.register(new Mod("HighJump", "Jump higher", Category.MOVEMENT) {
            final Setting.Num power = add(new Setting.Num("Power", 0.8, 0.45, 3, 0.05));
            @Override public void onTick() {
                var p = mc.player;
                if (p.isOnGround() && mc.options.jumpKey.isPressed()) {
                    Vec3d v = p.getVelocity();
                    p.setVelocity(v.x, power.get(), v.z);
                }
            }
        });

        ModManager.register(new Mod("AirJump", "Jump in mid-air", Category.MOVEMENT) {
            boolean last;
            @Override public void onTick() {
                var p = mc.player;
                boolean now = mc.options.jumpKey.isPressed();
                if (now && !last && !p.isOnGround()) { Vec3d v = p.getVelocity(); p.setVelocity(v.x, 0.42, v.z); p.fallDistance = 0; }
                last = now;
            }
        });

        ModManager.register(new Mod("Glide", "Slows your fall", Category.MOVEMENT) {
            final Setting.Num fall = add(new Setting.Num("FallSpeed", 0.05, 0.01, 0.3, 0.01));
            @Override public void onTick() {
                var p = mc.player;
                Vec3d v = p.getVelocity();
                if (!p.isOnGround() && v.y < -fall.get() && !p.isTouchingWater()) p.setVelocity(v.x, -fall.get(), v.z);
            }
        });

        ModManager.register(new Mod("AutoJump", "Jumps automatically while moving", Category.MOVEMENT) {
            @Override public void onTick() {
                var p = mc.player;
                if (p.isOnGround() && Util.moving() && !p.isSneaking()) p.jump();
            }
        });

        ModManager.register(new Mod("Parkour", "Jumps at the edge of blocks", Category.MOVEMENT) {
            @Override public void onTick() {
                var p = mc.player;
                if (!p.isOnGround() || p.isSneaking() || !Util.moving()) return;
                if (mc.world.isSpaceEmpty(p, p.getBoundingBox().offset(0, -0.5, 0).expand(-0.25, 0, -0.25))) p.jump();
            }
        });

        ModManager.register(new Mod("ElytraBoost", "Accelerate while gliding (hold forward)", Category.MOVEMENT) {
            final Setting.Num power = add(new Setting.Num("Power", 1.0, 0.1, 3, 0.1));
            @Override public void onTick() {
                var p = mc.player;
                if (p.isFallFlying() && mc.options.forwardKey.isPressed()) {
                    Vec3d look = p.getRotationVector();
                    p.addVelocity(look.x * 0.04 * power.get(), look.y * 0.04 * power.get(), look.z * 0.04 * power.get());
                }
            }
        });

        ModManager.register(new Mod("Sneak", "Always sneak", Category.MOVEMENT) {
            @Override public void onTick() { mc.options.sneakKey.setPressed(true); }
            @Override public void onDisable() { KeyBinding.updatePressedStates(); }
        });

        ModManager.register(new Mod("AutoWalk", "Walk forward automatically", Category.MOVEMENT) {
            @Override public void onTick() { mc.options.forwardKey.setPressed(true); }
            @Override public void onDisable() { KeyBinding.updatePressedStates(); }
        });
    }
}
