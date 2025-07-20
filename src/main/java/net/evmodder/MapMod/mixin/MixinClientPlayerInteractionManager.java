package net.evmodder.MapMod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.evmodder.MapMod.Main;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

@Mixin(ClientPlayerInteractionManager.class)
abstract class MixinClientPlayerInteractionManager{
	@Inject(method = "clickSlot", at = @At("HEAD"), cancellable = true)
	private void avoid_sending_too_many_clicks(int syncId, int slot, int button, SlotActionType action, PlayerEntity player, CallbackInfo ci){
		if(action == SlotActionType.THROW/* || action == SlotActionType.CLONE || action == SlotActionType.QUICK_CRAFT*/) return;
		if(Main.clickUtils.addClick(action) > Main.clickUtils.MAX_CLICKS){
			ci.cancel(); // Throw out clicks that exceed the limit!!
			Main.LOGGER.error("Discarding click in clickSlot() due to exceeding MAX_CLICKS limit!"
					+ " slot:"+slot+",button:"+button+",action:"+action.name()+",isShiftClick:"+Screen.hasShiftDown());
			MinecraftClient.getInstance().player.sendMessage(Text.literal("Discarding unsafe clicks!! > LIMIT").copy().withColor(/*&c=*/16733525), false);
		}
	}
}
