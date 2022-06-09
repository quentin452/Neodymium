package makamys.neodymium;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import makamys.neodymium.renderer.NeoRenderer;
import makamys.neodymium.util.SpriteUtil;

@Mod(modid = Neodymium.MODID, version = Neodymium.VERSION)
public class Neodymium
{
    public static final String MODID = "neodymium";
    public static final String VERSION = "0.0";
    
    public static final Logger LOGGER = LogManager.getLogger("neodymium");
    
    public static NeoRenderer renderer;
    
    public static boolean enabled;
    public static int chunkLoadsPerTick;
    public static List<Class> blockClassBlacklist;
    public static double fogStart;
    public static double fogEnd;
    public static double farPlaneDistanceMultiplier;
	public static float maxSimpleMeshHeight;
	public static boolean forceVanillaBiomeTemperature;
	public static boolean hideUnderVanillaChunks;
	public static boolean disableChunkMeshes;
	public static boolean disableSimpleMeshes;
	public static boolean saveMeshes;
	public static boolean optimizeChunkMeshes;
	public static int maxMeshesPerFrame;
	public static int sortFrequency;
	public static int gcRate;
	public static int VRAMSize;
	public static int debugPrefix;
	public static int debugInfoStartY;
    
    private File configFile;
    
    public static boolean fogEventWasPosted;
    
    public static boolean ofFastRender;
    public static boolean enableFog;
    
    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        configFile = event.getSuggestedConfigurationFile();
        reloadConfig();
    }
    
    private void reloadConfig() {
        Configuration config = new Configuration(configFile);
        
        config.load();
        enabled = config.get("General", "enabled", true).getBoolean();
        chunkLoadsPerTick = config.get("General", "chunkLoadsPerTick", 64).getInt();
        blockClassBlacklist = Arrays.stream(config.get("General", "blockClassBlacklist", "net.minecraft.block.BlockRotatedPillar;biomesoplenty.common.blocks.BlockBOPLog;gregapi.block.multitileentity.MultiTileEntityBlock").getString().split(";"))
                .map(className -> {
                    try {
                        return Class.forName(className);
                    } catch (ClassNotFoundException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        fogStart = config.get("Fog", "fogStart", "0.4").getDouble();
        fogEnd = config.get("Fog", "fogEnd", "0.8").getDouble();
        farPlaneDistanceMultiplier = config.get("Fog", "farPlaneDistanceMultiplier", "1.0").getDouble();
        
		maxSimpleMeshHeight = (float)config.get("Debug", "maxSimpleMeshHeight", 1000.0).getDouble();
		
		forceVanillaBiomeTemperature = config.get("Simple mesh generation", "forceVanillaBiomeTemperature", true).getBoolean();
        
		hideUnderVanillaChunks = config.getBoolean("hideUnderVanillaChunks", "render", true, "");
		disableChunkMeshes = config.getBoolean("disableChunkMeshes", "render", true, "");
		disableSimpleMeshes = config.getBoolean("disableSimpleMeshes", "render", false, "");
		optimizeChunkMeshes = config.getBoolean("optimizeChunkMeshes", "render", true, "");
		saveMeshes = config.getBoolean("saveMeshes", "render", false, "");
		maxMeshesPerFrame = config.getInt("maxMeshesPerFrame", "render", -1, -1, Integer.MAX_VALUE, "");
		sortFrequency = config.getInt("sortFrequency", "render", 1, 1, Integer.MAX_VALUE, "");
		gcRate = config.getInt("gcRate", "render", 1, 1, Integer.MAX_VALUE, "Maximum number of meshes to relocate each frame.");
		VRAMSize = config.getInt("VRAMSize", "render", 1024, 1, Integer.MAX_VALUE, "VRAM buffer size (MB).");
		enableFog = config.getBoolean("enableFog", "render", true, "");
		debugPrefix = config.getInt("debugPrefix", "debug", Keyboard.KEY_F4, -1, Integer.MAX_VALUE, "This key has to be held down while pressing the debug keybinds. LWJGL keycode. Setting this to 0 will make the keybinds usable without holding anything else down. Setting this to -1 will disable debug keybinds entirely.");
		debugInfoStartY = config.getInt("debugInfoStartY", "debug", 80, -1, Integer.MAX_VALUE, "The Y position of the first line of the debug info in the F3 overlay. Set this to -1 to disable showing that info.");
		
        if(config.hasChanged()) {
            config.save();
        }
    }
    
    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    private void onPlayerWorldChanged(World newWorld) {
    	if(getRendererWorld() == null && newWorld != null) {
    		reloadConfig();
    		if(enabled) {
    			SpriteUtil.init();
    		}
    	}
    	if(renderer != null) {
            renderer.destroy();
            renderer = null;
        }
    	if(enabled && newWorld != null) {
            renderer = new NeoRenderer(newWorld);
        }
    }
    
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onWorldUnload(WorldEvent.Unload event) {
        if(event.world == getRendererWorld()) {
        	onPlayerWorldChanged(null);
        }
    }
    
    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if(!event.world.isRemote) return;
        
        if(isActive()) {
            renderer.onChunkLoad(event);
        }
    }
    
    public static boolean isActive() {
        return renderer != null && renderer.hasInited && !renderer.destroyPending;
    }
    
    private World getRendererWorld() {
    	return renderer != null ? renderer.world : null;
    }
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
    	if(event.phase == TickEvent.Phase.START) {
    		EntityPlayer player = Minecraft.getMinecraft().thePlayer;
    		World world = player != null ? player.worldObj : null;
        	if(world != getRendererWorld()) {
        		onPlayerWorldChanged(world);
        	}
        	
        	if(MixinConfigPlugin.isOptiFinePresent()) {
    	        try {
    	            ofFastRender = (boolean)Class.forName("Config").getMethod("isFastRender").invoke(null);
    	        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
    	                | SecurityException | ClassNotFoundException e) {
    	            // oops
    	        }
        	}
    	}
    }
    
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if(event.phase == TickEvent.Phase.START) {
            if(isActive()) {
                renderer.serverTick();
            }
        }
    }
    
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if(event.phase == TickEvent.Phase.END) {
            if(isActive()) {
                renderer.onRenderTickEnd();
            }
        }
    }
    
    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent event) {
        FontRenderer fontRenderer = RenderManager.instance.getFontRenderer();
        if(isActive() && event.type == ElementType.TEXT && fontRenderer != null && Minecraft.getMinecraft().gameSettings.showDebugInfo && (Neodymium.debugInfoStartY != -1))
        {
            Minecraft mc = Minecraft.getMinecraft();
            ScaledResolution scaledresolution = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
            int w = scaledresolution.getScaledWidth();
            int h = scaledresolution.getScaledHeight();
            
            int yOffset = 0;
            for(String s : renderer.getDebugText()) {
                fontRenderer.drawStringWithShadow(s, w - fontRenderer.getStringWidth(s) - 10, Neodymium.debugInfoStartY + yOffset, 0xFFFFFF);
                yOffset += 10;
            }
        }
    }

    
    @SubscribeEvent
    public void onRenderFog(EntityViewRenderEvent.RenderFogEvent event) {
        fogEventWasPosted = true;
    }

    public static boolean shouldRenderVanillaWorld() {
        return !isActive() || (isActive() && renderer.renderWorld && !renderer.rendererActive);
    }

}