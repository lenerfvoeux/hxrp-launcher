package fr.lenerfvoeux.hxrp.metiers;

import fr.lenerfvoeux.hxrp.metiers.block.GuiHandler;
import fr.lenerfvoeux.hxrp.metiers.block.TileFrigo;
import fr.lenerfvoeux.hxrp.metiers.capability.Nutrition;
import fr.lenerfvoeux.hxrp.metiers.command.CommandGourmet;
import fr.lenerfvoeux.hxrp.metiers.data.FoodDatabase;
import fr.lenerfvoeux.hxrp.metiers.network.Network;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = HxrpMetiers.MODID, name = "HxRP Metiers", version = HxrpMetiers.VERSION, acceptedMinecraftVersions = "[1.12.2]")
public class HxrpMetiers {
    public static final String MODID = "hxrpmetiers";
    public static final String VERSION = "0.5.0";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @Mod.Instance(MODID)
    public static HxrpMetiers instance;

    @SidedProxy(clientSide = "fr.lenerfvoeux.hxrp.metiers.client.ClientProxy", serverSide = "fr.lenerfvoeux.hxrp.metiers.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        FoodDatabase.load();
        Nutrition.register();
        Network.init();
        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        GameRegistry.registerTileEntity(TileFrigo.class, new ResourceLocation(MODID, "frigo"));
        NetworkRegistry.INSTANCE.registerGuiHandler(instance, new GuiHandler());
        GameRegistry.registerWorldGenerator(new fr.lenerfvoeux.hxrp.metiers.monde.GenMonde(), 5);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent e) {
        e.registerServerCommand(new CommandGourmet());
    }
}
