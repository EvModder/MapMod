package net.evmodder.MapMod;

import java.util.List;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class LockedMapTooltip{
	public static final int UNLOCKED_COLOR = 14692709;
	public static final int UNNAMED_COLOR = 15652823;

	private static final boolean isUnlockedMap(ItemStack item, TooltipContext context){
		MapIdComponent id = item.get(DataComponentTypes.MAP_ID);
		if(id == null) return false;

		MapState state = context.getMapState(id);
		if(state == null) return false;
		return state != null && !state.locked;
	}
	private static final boolean isUnnamedMap(ItemStack item){
		if(item.getCustomName() != null) return false;
		return item.get(DataComponentTypes.MAP_ID) != null;
	}

	public static final void redName(ItemStack item, TooltipContext context, TooltipType type, List<Text> lines){
		ContainerComponent container = item.get(DataComponentTypes.CONTAINER);
		if(container != null){
			if(container.stream().anyMatch(i -> isUnlockedMap(i, context))){
				lines.addFirst(lines.removeFirst().copy().append(Text.literal("*").withColor(UNLOCKED_COLOR).formatted(Formatting.BOLD)));
			}
			else if(container.stream().anyMatch(i -> isUnnamedMap(i))){
				lines.addFirst(lines.removeFirst().copy().append(Text.literal("*").withColor(UNNAMED_COLOR).formatted(Formatting.BOLD)));
			}
			return;
		}
//		if(type == TooltipType.BASIC) return;
		MapIdComponent id = item.get(DataComponentTypes.MAP_ID);
		if(id == null) return;

		MapState state = context.getMapState(id);
		if(state == null) return;

		if(!state.locked) lines.addFirst(lines.removeFirst().copy().withColor(UNLOCKED_COLOR));
		else if(item.getCustomName() == null) lines.addFirst(lines.removeFirst().copy().withColor(UNNAMED_COLOR));
	}
}