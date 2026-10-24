package fr.lenerfvoeux.hxrp.phenix;

import fr.lenerfvoeux.hxrp.phenix.command.CommandPhenix;
import fr.lenerfvoeux.hxrp.phenix.stats.StatsPhenix;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Le Phénix de feu de HxRP : un boss volant animé avec GeckoLib, qu'on invoque avec un œuf de phénix, son appât
 * (le nid de braises) ou /phenix invoquer. Il ne casse aucun bloc et n'allume aucun feu ; il laisse une ou deux plumes
 * de phénix, l'ingrédient qui manque au Remède du Second Souffle du Hunter Virus. Ses réglages (vie, armure, dégâts,
 * portées, recharges, appât, butin) sont lus dans config/bestiaire_stats.xlsx, le classeur des boss de HxRP.
 */
@Mod(modid = HxrpPhenix.MODID, name = "HxRP Phenix", version = HxrpPhenix.VERSION, acceptedMinecraftVersions = "[1.12.2]",
        dependencies = "required-after:geckolib3;after:hxrpmetiers")
public class HxrpPhenix {
    public static final String MODID = "hxrpphenix";
    public static final String VERSION = "1.1.0";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @Mod.Instance(MODID)
    public static HxrpPhenix instance;

    @SidedProxy(clientSide = "fr.lenerfvoeux.hxrp.phenix.client.ClientProxy", serverSide = "fr.lenerfvoeux.hxrp.phenix.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        proxy.init();
        LOG.info("Phénix : prêt (dépendance au Hunter Virus : {})", Feu.virusPresent() ? "oui" : "non");
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent e) {
        StatsPhenix.charger(Loader.instance().getConfigDir());
        e.registerServerCommand(new CommandPhenix());
    }
}
