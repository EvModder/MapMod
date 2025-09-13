package net.evmodder.MapMod;

import java.util.HashMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.evmodder.EvLib.FileIO;
import net.evmodder.MapMod.commands.*;
import net.evmodder.MapMod.events.*;
import net.evmodder.MapMod.keybinds.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;

// gradle genSources/eclipse/cleanloom/--stop
//MC source will be in ~/.gradle/caches/fabric-loom or ./.gradle/loom-cache
// gradle build --refresh-dependencies
// gradle migrateMappings --mappings "1.21.4+build.8"
// Fix broken eclipse build paths after updating loom,fabric-api,version in configs: gradle eclipse
public class Main implements ClientModInitializer{
	// Reference variables
	public static final String MOD_ID = "keybound";
	public static final String configFilename = MOD_ID+".txt";
	//public static final String MOD_NAME = "KeyBound";
	//public static final String MOD_VERSION = "@MOD_VERSION@";
	public static final String KEYBIND_CATEGORY = "key.categories."+MOD_ID;
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static HashMap<String, String> config;

	public static ClickUtils clickUtils;
	public static boolean mapHighlightHUD, mapHighlightIFrame, mapHighlightHandledScreen;
	public static boolean invisItemFramesWithMaps=true, invisItemFramesWithMapsSemiTransparentOnly=false;
	public static boolean mapartDb, mapartDbContact, skipTransparentMaps, skipMonoColorMaps;

	public static int MAP_COLOR_UNLOADED = 13150930;
	public static int MAP_COLOR_UNLOCKED = 14692709;
	public static int MAP_COLOR_UNNAMED = 15652823;
	public static int MAP_COLOR_NOT_IN_GROUP = 706660;
	public static int MAP_COLOR_IN_INV = 11862015, MAP_COLOR_IN_IFRAME = 5614310;//TODO: MAP_COLOR_IN_CONTAINER=11862015
	public static int MAP_COLOR_MULTI_IFRAME = 11817190, MAP_COLOR_MULTI_INV = 11817190;

	public static double MAX_IFRAME_TRACKING_DIST_SQ;

	private void loadConfig(){
		//=================================== Parsing config into a map
		config = new HashMap<>();
		final String configContents = FileIO.loadFile(configFilename, getClass().getResourceAsStream("/"+configFilename));
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
		if(listKey != null) LOGGER.error("Unterminated list in ./config/"+configFilename+"!\nkey: "+listKey);
	}

	@Override public void onInitializeClient(){
		loadConfig();
		//=================================== Loading config features
		//int clicksInDuration = 190, durationTicks = 75;
		int clicksInDuration = 78, durationTicks = 90;
		boolean keybindMapArtLoad=false, keybindMapArtCopy=false, keybindMapArtMove=false, keybindMapArtBundleStow=false, keybindMapArtBundleStowReverse=false;
		int keybindMapArtBundleStowMax = 64;
		boolean mapMoveIgnoreAirPockets=true;
		boolean mapPlaceHelper=false, mapPlaceHelperAuto=false, mapPlaceHelperByName=true, mapPlaceHelperByImg=true, mapHighlightTooltip=false;
		boolean iFramePlacer=false, iFramePlacerMatchBlock=true, iFramePlacerMustConnect=true;
		boolean mapMetadataTooltip=false, mapMdStaircase=false, mapMdMaterial=false, mapMdNumColors=false, mapMdTransparency=false, mapMdNoobline=false,
				mapMdPercentCarpet=false, mapMdPercentStaircase=false;
		boolean mapWallCmd=false, mapWallBorder=false;
		int mapWallBorderColor1=-14236, mapWallBorderColor2=-8555656, mapWallUpscale=128;

		//config.forEach((key, value) -> {
		for(String key : config.keySet()){
			String value = config.get(key);
			switch(key){
//				case "mapart_database": mapartDb = !value.equalsIgnoreCase("false"); break;
//				case "mapart_database_share_contact": mapartDbContact = !value.equalsIgnoreCase("false"); break;

				case "limiter_clicks_in_duration": clicksInDuration = Integer.parseInt(value); break;
				case "limiter_duration_ticks": durationTicks = Integer.parseInt(value); break;
//				case "max_clicks_per_tick": clicks_per_gt = Integer.parseInt(value); break;
//				case "millis_between_clicks": millis_between_clicks = Integer.parseInt(value); break;

				case "map_state_cache": if(!value.equalsIgnoreCase("false")) new MapStateInventoryCacher(); break;
				case "invis_itemframes_with_maps": invisItemFramesWithMaps = !value.equalsIgnoreCase("false"); break;
				case "invis_itemframes_with_maps.semi_transparent_only": invisItemFramesWithMapsSemiTransparentOnly = !value.equalsIgnoreCase("false"); break;
				case "map_metadata_in_tooltip": mapMetadataTooltip = !value.equalsIgnoreCase("false"); break;
				case "map_metadata_in_tooltip.staircase": mapMdStaircase = !value.equalsIgnoreCase("false"); break;
				case "map_metadata_in_tooltip.material": mapMdMaterial = !value.equalsIgnoreCase("false"); break;
				case "map_metadata_in_tooltip.percent_carpet": mapMdPercentCarpet = !value.equalsIgnoreCase("false"); break;
				case "map_metadata_in_tooltip.percent_staircase": mapMdPercentStaircase = !value.equalsIgnoreCase("false"); break;
				case "map_metadata_in_tooltip.num_colors": mapMdNumColors = !value.equalsIgnoreCase("false"); break;
				case "map_metadata_in_tooltip.transparency": mapMdTransparency = !value.equalsIgnoreCase("false"); break;
				case "map_metadata_in_tooltip.noobline": mapMdNoobline = !value.equalsIgnoreCase("false"); break;
				case "map_highlight_in_tooltip": mapHighlightTooltip = !value.equalsIgnoreCase("false"); break;
				case "map_highlight_in_hotbarhud": mapHighlightHUD = !value.equalsIgnoreCase("false"); break;
				case "map_highlight_in_itemframe": mapHighlightIFrame = !value.equalsIgnoreCase("false"); break;
				case "map_highlight_in_container_name": mapHighlightHandledScreen = !value.equalsIgnoreCase("false"); break;
				case "map_highlight_color_unloaded": MAP_COLOR_UNLOADED = Integer.parseInt(value); break;
				case "map_highlight_color_unlocked": MAP_COLOR_UNLOCKED = Integer.parseInt(value); break;
				case "map_highlight_color_unnamed": MAP_COLOR_UNNAMED = Integer.parseInt(value); break;
				case "map_highlight_color_ungrouped": MAP_COLOR_NOT_IN_GROUP = Integer.parseInt(value); break;
				case "map_highlight_color_matches_inventory": MAP_COLOR_IN_INV = Integer.parseInt(value); break;
				case "map_highlight_color_matches_itemframe": MAP_COLOR_IN_IFRAME = Integer.parseInt(value); break;
				case "map_highlight_color_multi_itemframe": MAP_COLOR_MULTI_IFRAME = Integer.parseInt(value); break;
				case "map_highlight_color_multi_inventory": MAP_COLOR_MULTI_INV = Integer.parseInt(value); break;
				case "fully_transparent_map_is_filler_item": skipTransparentMaps = !value.equalsIgnoreCase("false"); break;
				case "highlight_duplicate_monocolor_maps": skipMonoColorMaps = value.equalsIgnoreCase("false"); break;
				case "itemframe_tracking_distance": MAX_IFRAME_TRACKING_DIST_SQ = Double.parseDouble(value)*Double.parseDouble(value); break;
				//case "mapart_notify_not_in_group": notifyIfLoadNewMapArt = !value.equalsIgnoreCase("false"); break;
				case "keybind.mapart.copy": keybindMapArtCopy = !value.equalsIgnoreCase("false"); break;
				case "keybind.mapart.load": keybindMapArtLoad = !value.equalsIgnoreCase("false"); break;
				case "keybind.mapart.move.bundle": keybindMapArtBundleStow = !value.equalsIgnoreCase("false"); break;
				case "keybind.mapart.move.bundle.reverse": keybindMapArtBundleStowReverse = !value.equalsIgnoreCase("false"); break;
				case "keybind.mapart.move.bundle.max": keybindMapArtBundleStowMax = Integer.parseInt(value); break;
				case "keybind.mapart.move.3x9": keybindMapArtMove = !value.equalsIgnoreCase("false"); break;
				case "keybind.mapart.move_3x9.ignore_air_pockets": mapMoveIgnoreAirPockets = !value.equalsIgnoreCase("false"); break;
				case "mapart_placement_helper": mapPlaceHelper=!value.equalsIgnoreCase("false"); break;
				case "mapart_placement_helper.use_name": mapPlaceHelperByName=!value.equalsIgnoreCase("false"); break;
				case "mapart_placement_helper.use_image": mapPlaceHelperByImg=!value.equalsIgnoreCase("false"); break;
				case "mapart_placement_helper.autoplace": mapPlaceHelperAuto=!value.equalsIgnoreCase("false"); break;
				case "iframe_placement_helper": iFramePlacer = !value.equalsIgnoreCase("false"); break;
				case "iframe_placement_helper.must_match_block": iFramePlacerMatchBlock = !value.equalsIgnoreCase("false"); break;
				case "iframe_placement_helper.must_connect": iFramePlacerMustConnect = !value.equalsIgnoreCase("false"); break;
				case "mapart_group_include_unlocked": MapGroupUtils.INCLUDE_UNLOCKED = !value.equalsIgnoreCase("false"); break;
				case "mapart_group_command": new CommandMapArtGroup(); break;
				case "mapart_generate_img_upscale_to": mapWallUpscale=Integer.parseInt(value); break;
				case "mapart_generate_img_border": mapWallBorder=!value.equalsIgnoreCase("false"); break;
				case "mapart_generate_img_command": mapWallCmd=!value.equalsIgnoreCase("false"); break;
				case "mapart_generate_img_border_color1": mapWallBorderColor1=Integer.parseInt(value); break;
				case "mapart_generate_img_border_color2": mapWallBorderColor2=Integer.parseInt(value); break;

				default:
					LOGGER.warn("Unrecognized config setting: "+key);
			}
		}
		clickUtils = new ClickUtils(clicksInDuration, durationTicks);

		if(keybindMapArtLoad) new KeybindMapLoad();
		if(keybindMapArtCopy) new KeybindMapCopy();
		if(keybindMapArtMove) new KeybindMapMove(mapMoveIgnoreAirPockets);
		if(keybindMapArtBundleStow || keybindMapArtBundleStowReverse)
			new KeybindMapMoveBundle(keybindMapArtBundleStow, keybindMapArtBundleStowReverse, keybindMapArtBundleStowMax);
		if(mapPlaceHelper) new MapHandRestock(mapPlaceHelperByName, mapPlaceHelperByImg, mapPlaceHelperAuto);
		if(iFramePlacer) new ItemFrameAutoPlacer(iFramePlacerMatchBlock, iFramePlacerMustConnect);

		if(mapWallCmd) new CommandExportMapImg(mapWallUpscale, mapWallBorder, mapWallBorderColor1, mapWallBorderColor2);

		if(mapHighlightTooltip) ItemTooltipCallback.EVENT.register(TooltipMapNameColor::tooltipColors);
		if(mapHighlightTooltip || mapHighlightHUD || mapHighlightIFrame || mapHighlightHandledScreen){
			ClientTickEvents.START_CLIENT_TICK.register(client -> {
				InventoryHighlightUpdater.onUpdateTick(client);
				ItemFrameHighlightUpdater.onUpdateTick(client);
				if(mapHighlightHandledScreen/* || mapHighlightTooltip*/) ContainerHighlightUpdater.onUpdateTick(client);
			});
		}

		if(mapMetadataTooltip){
			new TooltipMapLoreMetadata(mapMdStaircase, mapMdMaterial, mapMdNumColors, mapMdTransparency, mapMdNoobline, mapMdPercentCarpet, mapMdPercentStaircase);
		}
	}
}