package com.khaopun.holdshield.client.mixin;

import com.khaopun.holdshield.client.ShieldVisual;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Wraps the single call that draws the first-person hands, so mods that replace that drawing (Punchy) also see
 * the faked hands. Wrapping the call site (not the callee) guarantees the window is closed again afterwards,
 * even if another mod cancels the callee.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererVisualMixin {
	@WrapOperation(
			method = "renderItemInHand",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/FirstPersonHandsAndItemsRenderer;submitHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/PlayerRenderState;Lnet/minecraft/client/renderer/state/level/FirstPersonHandsAndItemsRenderState;)V"))
	private void holdshield$wrapSubmit(FirstPersonHandsAndItemsRenderer renderer, float partialTick, PoseStack poseStack,
			SubmitNodeCollector collector, PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState handsState,
			Operation<Void> original) {
		ShieldVisual.open("render.submit");
		try {
			original.call(renderer, partialTick, poseStack, collector, playerState, handsState);
		} finally {
			ShieldVisual.close();
		}
	}
}
