package com.khaopun.holdshield.client.mixin;

import com.khaopun.holdshield.client.ShieldVisual;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Compatibility with the Punchy! first-person animation mod. Punchy drives its animation state machines (shield,
 * hand equip, off-hand lowering, ...) from one end-of-tick callback and reads the player's real hands there. Without
 * this, Punchy would play the shield pose on the item we show in the right hand and hide the left-hand shield.
 * The window is only opened around that single callback.
 *
 * Targets Punchy 2.8a (synthetic lambda name); if Punchy is missing or changes, this simply does nothing.
 */
@Pseudo
@Mixin(targets = "punchy.client.PunchyClient", remap = false)
public abstract class PunchyTickVisualMixin {
	@Inject(method = "lambda$init$0", at = @At("HEAD"), require = 0, remap = false)
	private static void holdshield$openPunchyTick(Minecraft client, CallbackInfo ci) {
		ShieldVisual.open();
	}

	@Inject(method = "lambda$init$0", at = @At("RETURN"), require = 0, remap = false)
	private static void holdshield$closePunchyTick(Minecraft client, CallbackInfo ci) {
		ShieldVisual.close();
	}
}
