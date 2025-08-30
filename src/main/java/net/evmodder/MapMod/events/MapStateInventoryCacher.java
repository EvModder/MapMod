package net.evmodder.MapMod.events;

import java.util.List;
import java.util.stream.Stream;
import net.evmodder.MapMod.Main;
import net.evmodder.MapMod.MapRelationUtils;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;

public class MapStateInventoryCacher{

	List<MapState> mapStatesInInv = null;
	private final void saveMapStates(){
		MinecraftClient client = MinecraftClient.getInstance();
		mapStatesInInv = MapRelationUtils.getAllNestedItems(
				Stream.concat(client.player.getInventory().main.stream(), client.player.getEnderChestInventory().heldStacks.stream())
			)
			.filter(s -> s.getItem() == Items.FILLED_MAP).map(s -> FilledMapItem.getMapState(s, client.world)).toList();
	}

	private final boolean loadMapStates(){
		MinecraftClient client = MinecraftClient.getInstance();
		List<ItemStack> mapItems = MapRelationUtils.getAllNestedItems(
				Stream.concat(client.player.getInventory().main.stream(), client.player.getEnderChestInventory().heldStacks.stream())
			)
			.filter(s -> s.getItem() == Items.FILLED_MAP).toList();

		if(mapItems.size() != mapStatesInInv.size()){
			Main.LOGGER.warn("MapStateInventoryCacher: inventory on join does not match inventory when disconnected");
			return false;
		}
		int statesLoaded = 0;
		for(int i=0; i<mapItems.size(); ++i){
			if(FilledMapItem.getMapState(mapItems.get(i), client.world) != null) continue; // Already loaded
			if(mapStatesInInv.get(i) == null) continue; // Loaded state wasn't cached
			MapIdComponent mapIdComponent = mapItems.get(i).get(DataComponentTypes.MAP_ID);
			client.world.putMapState(mapIdComponent, mapStatesInInv.get(i));
			client.world.putClientsideMapState(mapIdComponent, mapStatesInInv.get(i));
			++statesLoaded;
		}
		return statesLoaded > 0;
	}

	public MapStateInventoryCacher(){
		ClientPlayConnectionEvents.DISCONNECT.register(
				//ClientPlayNetworkHandler handler, MinecraftClient client
				(_, _) -> saveMapStates()
			);
		ClientPlayConnectionEvents.JOIN.register(
			//ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server
			(_, _, _) -> loadMapStates()
		);
	}
}