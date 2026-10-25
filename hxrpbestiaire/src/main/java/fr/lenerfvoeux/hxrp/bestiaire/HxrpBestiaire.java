package fr.lenerfvoeux.hxrp.bestiaire;

import fr.lenerfvoeux.hxrp.bestiaire.command.CommandBestiaire;
import fr.lenerfvoeux.hxrp.bestiaire.command.CommandBoss;
import fr.lenerfvoeux.hxrp.bestiaire.entite.Effets;
import fr.lenerfvoeux.hxrp.bestiaire.stats.Stats;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Le bestiaire de HxRP : les boss animés avec GeckoLib (modèles Blockbench convertis), leurs appâts, leurs phases,
 * leurs sbires et leurs butins. Réglages : config/bestiaire_stats.xlsx, relu par /bestiaire recharger.
 */
@Mod(modid = HxrpBestiaire.MODID, name = "HxRP Bestiaire", version = HxrpBestiaire.VERSION, acceptedMinecraftVersions = "[1.12.2]",
        dependencies = "required-after:geckolib3;after:hxrpmetiers;after:hxrpphenix")
public class HxrpBestiaire {
    public static final String MODID = "hxrpbestiaire";
    public static final String VERSION = "0.2.0";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @Mod.Instance(MODID)
    public static HxrpBestiaire instance;

    @SidedProxy(clientSide = "fr.lenerfvoeux.hxrp.bestiaire.client.ClientProxy", serverSide = "fr.lenerfvoeux.hxrp.bestiaire.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        proxy.init();
        Registre.feu();
        LOG.info("Bestiaire : prêt, {} boss, {} espèces, {} animations (maladies du Hunter Virus : {})", Especes.BOSS.size(), Especes.TOUTES.size(),
                Durees.nombre(), Effets.virusPresent() ? "oui" : "non");
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent e) {
        Stats.charger(Loader.instance().getConfigDir());
        e.registerServerCommand(new CommandBestiaire());
        for (Espece s : Especes.BOSS) e.registerServerCommand(new CommandBoss(s));
    }
}
