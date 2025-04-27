package net.evmodder.MapCopier;

/*recommended order:
public / private / protected
abstract
static
final
transient
volatile
**default**
synchronized
native
strictfp

*/

import java.util.HashMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.evmodder.MapCopier.Keybinds.InventoryUtils;
import net.evmodder.MapCopier.Keybinds.KeybindMapCopy;
import net.evmodder.MapCopier.Keybinds.KeybindMapLoad;
import net.evmodder.MapCopier.Keybinds.KeybindMapMove;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
// gradle genSources/eclipse/cleanloom/--stop
//MC source will be in ~/.gradle/caches/fabric-loom or ./.gradle/loom-cache
// gradle build --refresh-dependencies
// Fix broken eclipse build paths after updating loom,fabric-api,version in configs: gradle eclipse
public class Main implements ClientModInitializer{
	// Reference variables
	public static final String MOD_ID = "mapcopier";
	public static final String KEYBIND_CATEGORY = "key.categories."+MOD_ID;
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static HashMap<String, String> config;

	public static boolean mapColorHUD, mapColorIFrame;
	public static InventoryUtils inventoryUtils;

	private void loadConfig(){
		//=================================== Parsing config into a map
		config = new HashMap<>();
		final String configContents = FileIO.loadFile("mapcopier.txt", getClass().getResourceAsStream("/mapcopier.txt"));
		String listKey = null, listValue = null;
		int listDepth = 0;
		for(String line : configContents.split("\\r?\\n")){
			if(listKey != null){
				line = line.trim();
				listValue += line;
				listDepth += StringUtils.countMatches(line, '[') - StringUtils.countMatches(line, ']');
				if(listDepth == 0 && line.charAt(line.length()-1) == ']'){
					config.put(listKey, listValue);
					listKey = null;
				}
				continue;
			}
			final int sep = line.indexOf(':');
			if(sep == -1) continue;
			final String key = line.substring(0, sep).trim();
			final String value = line.substring(sep+1).trim();
			if(key.isEmpty() || value.isEmpty()) continue;
			if(value.charAt(0) == '[' && value.charAt(value.length()-1) != ']'){
				listDepth = StringUtils.countMatches(value, '[') - StringUtils.countMatches(value, ']');
				listKey = key; listValue = value;
			}
			config.put(key, value);
		}
		if(listKey != null) LOGGER.error("Unterminated list in ./config/mapcopier.txt!\nkey: "+listKey);
	}

	@Override public void onInitializeClient(){
		loadConfig();
		//=================================== Loading config features
		boolean keybindMapArtLoad=false, keybindMapArtCopy=false, keybindMapArtMove=false;
//		int clicks_per_gt=36, millis_between_clicks=50;
		boolean mapPlaceHelper=false, mapPlaceHelperByName=false, mapPlaceHelperByImg=false;
		int clicksInDuration = 190, durationTicks = 75;

		//config.forEach((key, value) -> {
		for(String key : config.keySet()){
			String value = config.get(key);
			switch(key){
				case "limiter_clicks_in_duration": clicksInDuration = Integer.parseInt(value); break;
				case "limiter_duration_ticks": durationTicks = Integer.parseInt(value); break;

				case "unlocked_map_red_tooltip": if(!value.equalsIgnoreCase("false")) ItemTooltipCallback.EVENT.register(LockedMapTooltip::redName); break;
				case "unlocked_map_red_hotbarhud": mapColorHUD = !value.equalsIgnoreCase("false"); break;
				case "unlocked_map_red_itemframe": mapColorIFrame = !value.equalsIgnoreCase("false"); break;
				case "keybind_mapart_load_from_shulker": keybindMapArtLoad = !value.equalsIgnoreCase("false"); break;
				case "keybind_mapart_take_from_shulker": keybindMapArtMove = !value.equalsIgnoreCase("false"); break;
				case "keybind_mapart_copy_in_inventory": keybindMapArtCopy = !value.equalsIgnoreCase("false"); break;
				case "mapart_placement_helper": mapPlaceHelper=true; break;
				case "mapart_placement_helper_use_name": mapPlaceHelperByName=true; break;
				case "mapart_placement_helper_use_image": mapPlaceHelperByImg=true; break;
//				case "max_clicks_per_tick": clicks_per_gt = Integer.parseInt(value); break;
//				case "millis_between_clicks": millis_between_clicks = Integer.parseInt(value); break;
				default:
					LOGGER.warn("Unrecognized config setting: "+key);
			}
		}
		inventoryUtils = new InventoryUtils(clicksInDuration, durationTicks);
		if(keybindMapArtLoad) new KeybindMapLoad();
		if(keybindMapArtCopy) new KeybindMapCopy();
		if(keybindMapArtMove) new KeybindMapMove();
		if(mapPlaceHelper) new MapHandRestock(mapPlaceHelperByName, mapPlaceHelperByImg);
	}
}