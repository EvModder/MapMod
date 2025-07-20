package net.evmodder.MapMod.Events;

import java.util.ArrayDeque;
import net.evmodder.MapMod.Main;
import net.evmodder.MapMod.MapRelationUtils;
import net.evmodder.MapMod.Keybinds.ClickUtils.ClickEvent;
import net.evmodder.MapMod.MapRelationUtils.RelatedMapsData;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.world.World;

public abstract class MapClickMoveNeighbors{
	private static boolean ongoingClickMove;

	record Rectangle(int tl, int w, int h){}

	private static final byte[] getColors(World world, ItemStack stack){
		if(stack.getItem() != Items.FILLED_MAP) return null;
		MapIdComponent mapId = stack.get(DataComponentTypes.MAP_ID);
		if(mapId == null) return null;
		MapState state = world.getMapState(mapId);
		return state == null ? null : state.colors;
	}

	public static void moveNeighbors(PlayerEntity player, int destSlot, ItemStack mapMoved){
		if(ongoingClickMove){Main.LOGGER.warn("MapMoveClick: Already ongoing"); return;}

		Main.LOGGER.info("MapMoveClick: moveNeighbors() called");
		final ItemStack[] slots = player.currentScreenHandler.slots.stream().map(Slot::getStack).toArray(ItemStack[]::new);
		final String movedName = mapMoved.getCustomName().getLiteralString();
		final MapIdComponent mapId = mapMoved.get(DataComponentTypes.MAP_ID);
		final MapState state = mapId == null ? null : player.getWorld().getMapState(mapId);
		final Boolean locked = state == null ? null : state.locked;
		//Main.LOGGER.info("MapMoveClick: locked="+locked);
		final RelatedMapsData data =  MapRelationUtils.getRelatedMapsByName(slots, movedName, mapMoved.getCount(), locked, player.getWorld());
		if(data.prefixLen() == -1){
			Main.LOGGER.info("MapMoveClick: related-name maps not found");
			return;
		}
		data.slots().removeIf(i -> {
			if(i == destSlot) return true;
			if(ItemStack.areItemsAndComponentsEqual(slots[i], mapMoved)){
				Main.LOGGER.info("MapMoveClick: TODO:support multiple copies (i:"+i+",dest"+destSlot);
				return true;
			}
			return false;
		});
		if(data.slots().isEmpty()){
			Main.LOGGER.info("MapMoveClick: no connected moveable maps found");
			return;
		}

		int tl = data.slots().stream().mapToInt(i->i.intValue()).min().getAsInt();
		if(tl%9 != 0 && data.slots().contains(tl+8)) --tl;
		int br = data.slots().stream().mapToInt(i->i.intValue()).max().getAsInt();
		if(br%9 != 8 && data.slots().contains(br-8)) ++br;
		int h = (br/9)-(tl/9)+1;
		int w = (br%9)-(tl%9)+1;

		if((h == 1 || w == 1) && w*h == data.slots().size()){
			if(state == null){++w; br += 1;}
			else{
				final byte[] colors = state.colors;
				final byte[] tlColors = getColors(player.getWorld(), slots[tl]);
				final byte[] brColors = getColors(player.getWorld(), slots[br]);
				int scoreLeft = -2, scoreRight = -2, scoreTop = -2, scoreBottom = -2;
				if(h == 1){
					scoreLeft = (tl%9==0||destSlot%9+w>8) ? -2 : MapRelationUtils.adjacentEdgeScore(colors, tlColors, true);
					scoreRight = (br%9==8||destSlot%9-w<0) ? -2 : MapRelationUtils.adjacentEdgeScore(brColors, colors, true);
				}
				if(w == 1){
					scoreTop = (tl-9<0||destSlot+9*h>=slots.length) ? -2 : MapRelationUtils.adjacentEdgeScore(colors, tlColors, false);
					scoreBottom = (br+9>=slots.length||destSlot-9*h<0) ? -2 : MapRelationUtils.adjacentEdgeScore(brColors, colors, false);
				}
				if(Math.max(scoreLeft, scoreRight) >= Math.max(scoreTop, scoreBottom)){
					++w;
					if(scoreLeft > scoreRight) tl -= 1; else br += 1;
					Main.LOGGER.info("MapMoveClick: extending width of 1-tall map, scoreLeft:"+scoreLeft+", scoreRight:"+scoreRight);
				}
				else{
					++h;
					if(scoreTop > scoreBottom) tl -= 9; else br += 9;
					Main.LOGGER.info("MapMoveClick: extending height of 1-wide map, scoreTop:"+scoreTop+", scoreBottom:"+scoreBottom);
				}
			}//state != null
		}

		//Main.LOGGER.info("MapMoveClick: tl="+tl+",br="+br+" | h="+h+",w="+w+" | x>"+destSlot);
		if(h*w != data.slots().size()+1){Main.LOGGER.info("MapMoveClick: H*W not found (#maps:"+(data.slots().size()+1)+")");return;}

		int fromSlot = -1;
		for(int i=0; i<h; ++i) for(int j=0; j<w; ++j){
			int s = tl + i*9 + j;
			if(data.slots().contains(s)) continue;
			if(fromSlot != -1){Main.LOGGER.info("MapMoveClick: Maps not in a rectangle");return;}
			ItemStack stack = slots[i];
			if(!stack.isEmpty() && stack.getItem() != Items.FILLED_MAP) Main.LOGGER.warn("MapMoveClick: moveFrom slot:"+s+" contains junk item: "+stack.getItem());
			fromSlot = s;
		}
		Main.LOGGER.info("MapMoveClick: tl="+tl+",br="+br+" | h="+h+",w="+w+" | "+fromSlot+"->"+destSlot);
		//player.sendMessage(Text.literal("MapMoveClick: tl="+tl+",br="+br+" | h="+h+",w="+w+" | "+fromSlot+"->"+destSlot), false);////

		final int tlDest = destSlot-(fromSlot-tl);
		for(int i=0; i<h; ++i) for(int j=0; j<w; ++j){
			int d = tlDest + i*9 + j;
			if(d == destSlot) continue;
			if(!slots[d].isEmpty() && !data.slots().contains(d)){
				Main.LOGGER.info("MapMoveClick: Destination is not empty (dTL="+tlDest+",cur="+d+")");
				return;
			}
		}
		final int brDest = tlDest + (br-tl);//equivalent: destSlot+(br-fromSlot);

		final boolean isPlayerInv = player.currentScreenHandler instanceof PlayerScreenHandler;
		final int hbStart = slots.length-(isPlayerInv ? 10 : 9); // extra slot at end to account for offhand
		final boolean fromHotbar = br >= hbStart, toHotbar = brDest >= hbStart;
		//Main.LOGGER.warn("MapMoveClick: fromHotbar:"+fromHotbar+", toHotbar:"+toHotbar+", brDest:"+brDest+", last  hotbar if to: "+(brDest-hbStart));
		//if(PREFER_HOTBAR_SWAPS){
		int hotbarButton = 40;
		for(int i=0; i<9; ++i){
			if(fromHotbar && (tl-hbStart)%9 <= i && i <= (br-hbStart)%9) continue;		// Avoid hotbar slots the map might be moving from
			if(toHotbar && (tlDest-hbStart)%9 <= i && i <= (brDest-hbStart)%9) continue;// Avoid hotbar slots the map might be moving into
			if(player.getInventory().getStack(i).isEmpty()){hotbarButton = i; break;}
		}
		if(hotbarButton == 40) Main.LOGGER.warn("MapMoveClick: Using offhand for swaps");

		int tempSlot = -1;
		if(!player.getInventory().getStack(hotbarButton).isEmpty()){
			Main.LOGGER.warn("MapMoveClick: No available hotbar slot");
			if(tl > tlDest){
				for(int i=tlDest-1; i>=0; --i) if(slots[i].isEmpty()){tempSlot=i; break;}
				if(tempSlot==-1) for(int i=br; i<slots.length; ++i) if(slots[i].isEmpty()){tempSlot=i; break;}
			}
			else{
				for(int i=tl; i>=0; --i) if(slots[i].isEmpty()){tempSlot=i; break;}
				if(tempSlot==-1) for(int i=brDest; i<slots.length; ++i) if(slots[i].isEmpty()){tempSlot=i; break;}
			}
			if(tempSlot == -1) Main.LOGGER.warn("MapMoveClick: No available slot with which to free up offhand");
		}

//		final MinecraftClient client = MinecraftClient.getInstance();
		final ArrayDeque<ClickEvent> clicks = new ArrayDeque<>();
		if(tempSlot != -1) clicks.add(new ClickEvent(tempSlot, hotbarButton, SlotActionType.SWAP));

		if(tl > tlDest){
			Main.LOGGER.info("MapMoveClick: Moving all, starting from TL");
			for(int i=0; i<h; ++i) for(int j=0; j<w; ++j){
				int s = tl + i*9 + j, d = tlDest + i*9 + j;
				if(d == destSlot) continue;
				//Main.LOGGER.warn("MapMoveClick: adding 2 clicks: "+s+"->"+d+", hb:"+hotbarButton);
				clicks.add(new ClickEvent(s, hotbarButton, SlotActionType.SWAP));
				clicks.add(new ClickEvent(d, hotbarButton, SlotActionType.SWAP));
//				client.interactionManager.clickSlot(syncId, s, hotbarButton, SlotActionType.SWAP, player);
//				client.interactionManager.clickSlot(syncId, d, hotbarButton, SlotActionType.SWAP, player);
			}
		}
		else{
			Main.LOGGER.info("MapMoveClick: Moving all, starting from BR");
			for(int i=0; i<h; ++i) for(int j=0; j<w; ++j){
				int s = br - i*9 - j, d = brDest - i*9 - j;
				if(d == destSlot) continue;
				//Main.LOGGER.warn("MapMoveClick: adding 2 clicks: "+s+"->"+d+", hb:"+hotbarButton);
				clicks.add(new ClickEvent(s, hotbarButton, SlotActionType.SWAP));
				clicks.add(new ClickEvent(d, hotbarButton, SlotActionType.SWAP));
//				client.interactionManager.clickSlot(syncId, s, hotbarButton, SlotActionType.SWAP, player);
//				client.interactionManager.clickSlot(syncId, d, hotbarButton, SlotActionType.SWAP, player);
			}
		}
		if(tempSlot != -1) clicks.add(new ClickEvent(tempSlot, hotbarButton, SlotActionType.SWAP));

		final int numClicks = clicks.size();
		ongoingClickMove = true;
		Main.clickUtils.executeClicks(clicks, /*canProceed=*/_->true, ()->{
			ongoingClickMove = false;
			Main.LOGGER.info("MapMoveClick: DONE (clicks:"+numClicks+")");
			player.sendMessage(Text.literal("MapMoveClick: DONE (clicks:"+numClicks+")"), true);
		});
//		if(Main.inventoryUtils.addClick(null) >= Main.inventoryUtils.MAX_CLICKS){
//			Main.LOGGER.warn("Not enough clicks available to execute MapMoveNeighbors :(");
//			return;
//		}
//		final MinecraftClient client = MinecraftClient.getInstance();
//		for(ClickEvent c : clicks) client.interactionManager.clickSlot(syncId, c.slotId(), c.button(), c.actionType(), player);
	}
}