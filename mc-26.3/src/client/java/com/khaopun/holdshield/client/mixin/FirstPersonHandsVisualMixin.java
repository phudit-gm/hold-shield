package com.khaopun.holdshield.client.mixin;

import com.khaopun.holdshield.client.ShieldVisual;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Opens the fake-hands window around the vanilla hand animation tick and the render state extraction, so the
 * first-person state (items, use hand, equip animation) is built from the faked hands.
 */
@Mixin(FirstPersonHandsAndItems.class)
public abstract class FirstPersonHandsVisualMixin {
	@Inject(method = "tick", at = @At("HEAD"))
	private void holdshield$openTick(CallbackInfo ci) {
		ShieldVisual.open();
	}

	@Inject(method = "tick", at = @At("RETURN"))
	private void holdshield$closeTick(CallbackInfo ci) {
		ShieldVisual.close();
	}

	@Inject(method = "extractRenderState", at = @At("HEAD"))
	private void holdshield$openExtract(CallbackInfo ci) {
		ShieldVisual.open();
	}

	@Inject(method = "extractRenderState", at = @At("RETURN"))
	private void holdshield$closeExtract(CallbackInfo ci) {
		ShieldVisual.close();
	}
}
