package dev.nebula.modules;

import dev.nebula.core.*;
import dev.nebula.mixin.ClientPlayerInteractionManagerAccessor;
import dev.nebula.mixin.MinecraftClientAccessor;
import dev.nebula.util.Util;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static dev.nebula.core.Mod.mc;

public final class PlayerMods {
    private PlayerMods() { }

    private static boolean isFood(ItemStack s) {
        if (s.isEmpty() || !s.contains(DataComponentTypes.FOOD)) return false;
        return !(s.isOf(Items.ROTTEN_FLESH) || s.isOf(Items.SPIDER_EYE) || s.isOf(Items.POISONOUS_POTATO)
                || s.isOf(Items.PUFFERFISH) || s.isOf(Items.CHORUS_FRUIT));
    }

    public static void init() {
        ModManager.register(new Mod("AutoRespawn", "Respawns instantly after dying", Category.PLAYER) {
            @Override public void onTick() {
                if (mc.currentScreen instanceof DeathScreen) mc.player.requestRespawn();
            }
        });

        ModManager.register(new Mod("AutoTool", "Selects the best tool while mining", Category.PLAYER) {
            int prev = -1;
            @Override public void onTick() {
                if (mc.currentScreen == null && mc.options.attackKey.isPressed()
                        && mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK
                        && mc.crosshairTarget instanceof BlockHitResult bhr) {
                    BlockState state = mc.world.getBlockState(bhr.getBlockPos());
                    int best = mc.player.getInventory().selectedSlot;
                    float bestSpeed = mc.player.getInventory().getStack(best).getMiningSpeedMultiplier(state);
                    for (int i = 0; i < 9; i++) {
                        float sp = mc.player.getInventory().getStack(i).getMiningSpeedMultiplier(state);
                        if (sp > bestSpeed) { bestSpeed = sp; best = i; }
                    }
                    if (best != mc.player.getInventory().selectedSlot) {
                        if (prev == -1) prev = mc.player.getInventory().selectedSlot;
                        Util.select(best);
                    }
                } else if (prev != -1) {
                    Util.select(prev);
                    prev = -1;
                }
            }
            @Override public void onDisable() { if (prev != -1 && mc.player != null) Util.select(prev); prev = -1; }
        });

        ModManager.register(new Mod("AutoEat", "Eats food when you are hungry", Category.PLAYER) {
            final Setting.Num hunger = add(new Setting.Num("Hunger", 14, 1, 19, 1));
            boolean eating;
            int prev = -1;

            private void stop() {
                eating = false;
                KeyBinding.updatePressedStates();
                if (prev != -1 && mc.player != null) Util.select(prev);
                prev = -1;
            }

            @Override public void onTick() {
                var p = mc.player;
                int food = p.getHungerManager().getFoodLevel();
                if (eating) {
                    if (food >= 20 || !isFood(p.getMainHandStack()) || mc.currentScreen != null) stop();
                    else mc.options.useKey.setPressed(true);
                    return;
                }
                if (mc.currentScreen != null || food > hunger.get() || p.isCreative()) return;
                int slot = Util.findHotbar(PlayerMods::isFood);
                if (slot == -1) return;
                prev = p.getInventory().selectedSlot;
                Util.select(slot);
                eating = true;
                mc.options.useKey.setPressed(true);
            }
            @Override public void onDisable() { if (eating) stop(); }
        });

        ModManager.register(new Mod("AntiAFK", "Moves randomly so you don't get kicked", Category.PLAYER) {
            final Random rng = new Random();
            int timer;
            @Override public void onTick() {
                if (++timer < 100) return;
                timer = 0;
                var p = mc.player;
                switch (rng.nextInt(3)) {
                    case 0 -> p.jump();
                    case 1 -> p.swingHand(Hand.MAIN_HAND);
                    default -> p.setYaw(p.getYaw() + rng.nextInt(60) - 30);
                }
            }
        });

        ModManager.register(new Mod("ChestStealer", "Takes all items from open chests", Category.PLAYER) {
            final Setting.Num delay = add(new Setting.Num("Delay", 2, 0, 10, 1));
            final Setting.Bool close = add(new Setting.Bool("AutoClose", true));
            int timer;
            @Override public void onTick() {
                if (!(mc.currentScreen instanceof GenericContainerScreen screen)) return;
                if (timer > 0) { timer--; return; }
                GenericContainerScreenHandler h = screen.getScreenHandler();
                int size = h.getRows() * 9;
                for (int i = 0; i < size; i++) {
                    if (!h.getSlot(i).getStack().isEmpty()) {
                        mc.interactionManager.clickSlot(h.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                        timer = delay.i();
                        return;
                    }
                }
                if (close.get()) mc.player.closeHandledScreen();
            }
        });

        ModManager.register(new Mod("Scaffold", "Places blocks under your feet", Category.PLAYER) {
            final Setting.Num delay = add(new Setting.Num("Delay", 1, 0, 6, 1));
            final Setting.Bool swapBack = add(new Setting.Bool("SwapBack", true));
            int timer;
            @Override public void onTick() {
                if (mc.currentScreen != null) return;
                if (timer > 0) { timer--; return; }
                var p = mc.player;
                BlockPos below = BlockPos.ofFloored(p.getX(), p.getY() - 1, p.getZ());
                if (!mc.world.getBlockState(below).isReplaceable()) return;
                int slot = Util.findHotbar(s -> s.getItem() instanceof BlockItem);
                if (slot == -1) return;
                for (Direction d : Direction.values()) {
                    BlockPos n = below.offset(d);
                    if (mc.world.getBlockState(n).isReplaceable()) continue;
                    int prev = p.getInventory().selectedSlot;
                    Util.select(slot);
                    Direction face = d.getOpposite();
                    Vec3d hitPos = Vec3d.ofCenter(n).add(face.getOffsetX() * 0.5, face.getOffsetY() * 0.5, face.getOffsetZ() * 0.5);
                    mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, new BlockHitResult(hitPos, face, n, false));
                    p.swingHand(Hand.MAIN_HAND);
                    if (swapBack.get()) Util.select(prev);
                    timer = delay.i();
                    return;
                }
            }
        });

        ModManager.register(new Mod("Nuker", "Breaks blocks around you", Category.PLAYER) {
            final Setting.Num radius = add(new Setting.Num("Radius", 3, 1, 5, 1));
            @Override public void onTick() {
                if (mc.currentScreen != null) return;
                var p = mc.player;
                int r = radius.i();
                BlockPos c = p.getBlockPos();
                List<BlockPos> list = new ArrayList<>();
                for (int x = -r; x <= r; x++) for (int y = -r; y <= r; y++) for (int z = -r; z <= r; z++) {
                    BlockPos pos = c.add(x, y, z);
                    BlockState st = mc.world.getBlockState(pos);
                    if (st.isAir() || st.getBlock() instanceof FluidBlock || st.getHardness(mc.world, pos) < 0) continue;
                    if (p.getEyePos().squaredDistanceTo(Vec3d.ofCenter(pos)) > 4.5 * 4.5) continue;
                    list.add(pos);
                }
                if (list.isEmpty()) return;
                list.sort(Comparator.comparingDouble((BlockPos b) -> Vec3d.ofCenter(b).squaredDistanceTo(p.getPos())));
                if (mc.interactionManager.getCurrentGameMode().isCreative()) {
                    for (int i = 0; i < Math.min(5, list.size()); i++) mc.interactionManager.attackBlock(list.get(i), Direction.UP);
                } else {
                    mc.interactionManager.updateBlockBreakingProgress(list.get(0), Direction.UP);
                    p.swingHand(Hand.MAIN_HAND);
                }
            }
        });

        ModManager.register(new Mod("FastPlace", "No delay between placing blocks/using items", Category.PLAYER) {
            @Override public void onTick() { ((MinecraftClientAccessor) mc).nebula$setItemUseCooldown(0); }
        });

        ModManager.register(new Mod("NoBreakDelay", "No delay between breaking blocks", Category.PLAYER) {
            @Override public void onTick() {
                if (mc.interactionManager != null)
                    ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).nebula$setBlockBreakingCooldown(0);
            }
        });
    }
}
