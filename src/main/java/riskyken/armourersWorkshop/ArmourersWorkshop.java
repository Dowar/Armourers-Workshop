package riskyken.armourersWorkshop;

import java.io.File;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLInterModComms.IMCMessage;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import riskyken.armourersWorkshop.common.ApiRegistrar;
import riskyken.armourersWorkshop.common.addons.Addons;
import riskyken.armourersWorkshop.common.blocks.BlockSkinnable.Seat;
import riskyken.armourersWorkshop.common.blocks.ModBlocks;
import riskyken.armourersWorkshop.common.capability.IWardrobeCapability;
import riskyken.armourersWorkshop.common.capability.WardrobeProvider;
import riskyken.armourersWorkshop.common.capability.WardrobeStorage;
import riskyken.armourersWorkshop.common.command.CommandArmourers;
import riskyken.armourersWorkshop.common.config.ConfigHandler;
import riskyken.armourersWorkshop.common.config.ConfigHandlerClient;
import riskyken.armourersWorkshop.common.config.ConfigSynchronizeHandler;
import riskyken.armourersWorkshop.common.crafting.CraftingManager;
import riskyken.armourersWorkshop.common.creativetab.CreativeTabArmourersWorkshop;
import riskyken.armourersWorkshop.common.items.ModItems;
import riskyken.armourersWorkshop.common.lib.LibModInfo;
import riskyken.armourersWorkshop.common.network.GuiHandler;
import riskyken.armourersWorkshop.common.network.PacketHandler;
import riskyken.armourersWorkshop.common.skin.EntityEquipmentDataManager;
import riskyken.armourersWorkshop.common.skin.SkinExtractor;
import riskyken.armourersWorkshop.common.skin.cache.CommonSkinCache;
import riskyken.armourersWorkshop.common.skin.cubes.CubeRegistry;
import riskyken.armourersWorkshop.common.skin.type.SkinTypeRegistry;
import riskyken.armourersWorkshop.common.update.UpdateCheck;
import riskyken.armourersWorkshop.proxies.CommonProxy;
import riskyken.armourersWorkshop.utils.ModLogger;
import riskyken.armourersWorkshop.utils.SkinIOUtils;
import riskyken.plushieWrapper.common.creativetab.ModCreativeTab;

@Mod(modid = LibModInfo.ID, name = LibModInfo.NAME, version = LibModInfo.VERSION, guiFactory = LibModInfo.GUI_FACTORY_CLASS)
public class ArmourersWorkshop {
    
    /* 
     * Hello and welcome to the Armourer's Workshop source code.
     * 
     * Please read this to familiarise yourself with the different terms used in the code.
     * 
     * Important - Any time the texture that is used on the player model is referred to,
     * (normal called the players skin) it will be called the player texture or entity
     * texture to prevent confusion with AW skins.
     * 
     * Skin - A custom 3D model that can be created by a player. Skins are stored in SkinDataCache
     * server side and ClientSkinCache on the client side.
     * 
     * SkinType - Each skin has a skin type, examples; head, chest, bow and block. All
     * skin types can be found in the SkinTypeRegistry.
     * 
     * SkinPart - Each skin type will have at least 1 skin part. For example the chest skin type has
     * base, left arm and right arm skin parts.
     * 
     * SkinPartType - Each skin part has a part type examples; left arm, left leg and right arm.
     * 
     * TODO Finish this!
     * 
     * SkinTexture
     * EntityTexture
     * SkinPartModel
     * 
     * SkinPointer
     * SkinDye
     * 
     * SkinTextureKey
     * SkinPartModelKey
     */
    
    @Mod.Instance(LibModInfo.ID)
    public static ArmourersWorkshop instance;

    @SidedProxy(clientSide = LibModInfo.PROXY_CLIENT_CLASS, serverSide = LibModInfo.PROXY_COMMNON_CLASS)
    public static CommonProxy proxy;

    public static ModCreativeTab creativeTabArmorersWorkshop = new ModCreativeTab(LibModInfo.ID);
    public static CreativeTabArmourersWorkshop tabArmorersWorkshop = new CreativeTabArmourersWorkshop(CreativeTabs.getNextID(), LibModInfo.ID.toLowerCase());

    public static ModItems modItems;
    public static ModBlocks modBlocks;
    
    @Mod.EventHandler
    public void perInit(FMLPreInitializationEvent event) {
        ModLogger.log("Loading " + LibModInfo.NAME + " " + LibModInfo.VERSION);
        creativeTabArmorersWorkshop.setMinecraftCreativeTab(tabArmorersWorkshop);
        
        File configDir = event.getSuggestedConfigurationFile().getParentFile();
        configDir = new File(configDir, LibModInfo.ID);
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        ConfigHandler.init(new File(configDir, "common.cfg"));
        ConfigHandlerClient.init(new File(configDir, "client.cfg"));
        
        SkinTypeRegistry.init();
        CapabilityManager.INSTANCE.register(IWardrobeCapability.class, new WardrobeStorage(), WardrobeProvider.class);

        EntityRegistry.registerModEntity(Seat.class, "seat", 1, instance, 10, 20, false);
        
        Addons.preInit();
        proxy.preInit();
        
        SkinIOUtils.makeLibraryDirectory();
        UpdateCheck.checkForUpdates();
        SkinExtractor.extractSkins();
        
        modItems = new ModItems();
        modBlocks = new ModBlocks();
        
        CubeRegistry.init();
        proxy.initLibraryManager();
    }

    @Mod.EventHandler
    public void load(FMLInitializationEvent event) {
        CraftingManager.init();

        modBlocks.registerTileEntities();

        new GuiHandler();
        new ConfigSynchronizeHandler();
        
        PacketHandler.init();
        EntityEquipmentDataManager.init();
        
        proxy.init();
        proxy.registerKeyBindings();
        proxy.initRenderers();
        
        Addons.init();
    }
    
    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit();
        Addons.postInit();
        proxy.libraryManager.reloadLibrary();
    }
    
    @Mod.EventHandler
    public void serverStart(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandArmourers());
        CommonSkinCache.INSTANCE.serverStarted();
    }
    
    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        CommonSkinCache.INSTANCE.serverStopped();
    }
    
    @Mod.EventHandler
    public void processIMC(FMLInterModComms.IMCEvent event) {
        for (IMCMessage imcMessage : event.getMessages()) {
            if (!imcMessage.isStringMessage()) continue;
            if (imcMessage.key.equalsIgnoreCase("register")) {
                ModLogger.log(String.format("Receiving registration request from %s for class %s", imcMessage.getSender(), imcMessage.getStringValue()));
                ApiRegistrar.INSTANCE.addApiRequest(imcMessage.getSender(), imcMessage.getStringValue());
            }
        }
    }
    
    public static boolean isDedicated() {
        return proxy.getClass() == CommonProxy.class;
    }
}
