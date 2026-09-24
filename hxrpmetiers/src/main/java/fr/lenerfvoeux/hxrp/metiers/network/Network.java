package fr.lenerfvoeux.hxrp.metiers.network;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.capability.Nutrition;
import fr.lenerfvoeux.hxrp.metiers.capability.NutritionData;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class Network {
    public static final SimpleNetworkWrapper NET = NetworkRegistry.INSTANCE.newSimpleChannel(HxrpMetiers.MODID);

    private Network() {}

    public static void init() {
        NET.registerMessage(MsgSync.Handler.class, MsgSync.class, 0, Side.CLIENT);
        NET.registerMessage(MsgRecettes.Handler.class, MsgRecettes.class, 1, Side.CLIENT);
        NET.registerMessage(MsgMiniJeu.Handler.class, MsgMiniJeu.class, 2, Side.CLIENT);
        NET.registerMessage(MsgLancer.Handler.class, MsgLancer.class, 3, Side.SERVER);
        NET.registerMessage(MsgResultat.Handler.class, MsgResultat.class, 4, Side.SERVER);
    }

    public static void sync(EntityPlayerMP p) {
        NutritionData d = Nutrition.get(p);
        if (d == null) return;
        NET.sendTo(new MsgSync(d, System.currentTimeMillis()), p);
        d.dirty = false;
    }
}
