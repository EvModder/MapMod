package net.evmodder.MapMod.mixin;

import net.evmodder.MapMod.Main;
import net.evmodder.MapMod.MapGroupUtils;
import net.evmodder.MapMod.Events.ItemFrameHighlightUpdater;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(InGameHud.class)
abstract class MixinInGameHud{
	@ModifyVariable(method = "renderHeldItemTooltip", at = @At("STORE"), ordinal = 0)
	private MutableText showRepairCostNextToItemName(MutableText originalText){
		if(Main.mapHighlightHUD == false) return originalText;
		MinecraftClient client = MinecraftClient.getInstance();
		ItemStack currentStack = client.player.getInventory().getMainHandStack();
		MapIdComponent id = currentStack.get(DataComponentTypes.MAP_ID);
		if(id == null) return originalText;

		MutableText text = originalText;
		MapState state = client.world.getMapState(id);
		if(state != null && MapGroupUtils.shouldHighlightNotInCurrentGroup(state)){
			text.withColor(Main.MAP_COLOR_NOT_IN_GROUP);
			if(!state.locked) text.append(Text.literal("*").withColor(Main.MAP_COLOR_UNLOCKED));
		}
		else if(state != null && !state.locked) text.withColor(Main.MAP_COLOR_UNLOCKED);
		else if(state != null && ItemFrameHighlightUpdater.isInItemFrame(MapGroupUtils.getIdForMapState(state))) text.withColor(Main.MAP_COLOR_IN_IFRAME);
		else if(currentStack.getCustomName() == null) text.withColor(Main.MAP_COLOR_UNNAMED);
		return text;
	}
}