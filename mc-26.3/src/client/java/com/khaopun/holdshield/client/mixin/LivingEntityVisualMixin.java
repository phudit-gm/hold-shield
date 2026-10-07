package com.khaopun.holdshield.client.mixin;

import com.khaopun.holdshield.client.ShieldVisual;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fake hand getters for the local player, but only while {@link ShieldVisual} has its window open. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityVisualMixin {
	@Inject(method = "getMainHandItem", at = @At("HEAD"), cancellable = true)
	private void holdshield$mainHand(CallbackInfoReturnable<ItemStack> cir) {
		if (ShieldVisual.applies((LivingEntity) (Object) this)) {
			cir.setReturnValue(ShieldVisual.mainHand());
		}
	}

	@Inject(method = "getOffhandItem", at = @At("HEAD"), cancellable = true)
	private void holdshield$offHand(CallbackInfoReturnable<ItemStack> cir) {
		if (ShieldVisual.applies((LivingEntity) (Object) this)) {
			cir.setReturnValue(ShieldVisual.offHand());
		}
	}

	@Inject(method = "getItemInHand", at = @At("HEAD"), cancellable = true)
	private void holdshield$inHand(InteractionHand hand, CallbackInfoReturnable<ItemStack> cir) {
		if (ShieldVisual.applies((LivingEntity) (Object) this)) {
			cir.setReturnValue(ShieldVisual.inHand(hand));
		}
	}

	@Inject(method = "getUsedItemHand", at = @At("HEAD"), cancellable = true)
	private void holdshield$usedHand(CallbackInfoReturnable<InteractionHand> cir) {
		if (ShieldVisual.applies((LivingEntity) (Object) this)) {
			cir.setReturnValue(InteractionHand.OFF_HAND);
		}
	}
}
