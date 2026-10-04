package fr.lenerfvoeux.hxrp.metiers.virus;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.virus.block.BlockMeuble;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.DonneesVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.item.ItemOrdonnance;
import fr.lenerfvoeux.hxrp.metiers.virus.item.ItemSeringue;
import fr.lenerfvoeux.hxrp.metiers.virus.item.Objets;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/** Enregistre les blocs, objets et sons du Hunter Virus. */
@Mod.EventBusSubscriber(modid = HxrpMetiers.MODID)
public final class VirusRegistre {
    /** Tous les objets du Virus (pour les modèles côté client). */
    public static final List<Item> OBJETS = new ArrayList<>();
    public static final List<Block> BLOCS = new ArrayList<>();

    public static Item SERINGUE, ORDONNANCE;
    public static Block MEUBLE;

    public static final CreativeTabs TAB = new CreativeTabs(HxrpMetiers.MODID + ".virus") {
        @Override
        public ItemStack createIcon() {
            return SERINGUE == null ? ItemStack.EMPTY : new ItemStack(SERINGUE);
        }
    };

    private VirusRegistre() {}

    @SubscribeEvent
    public static void blocs(RegistryEvent.Register<Block> e) {
        DonneesVirus.chargerJar();
        MEUBLE = bloc(e, new BlockMeuble());
    }

    private static Block bloc(RegistryEvent.Register<Block> e, Block b) {
        b.setCreativeTab(TAB);
        e.getRegistry().register(b);
        BLOCS.add(b);
        return b;
    }

    @SubscribeEvent
    public static void objets(RegistryEvent.Register<Item> e) {
        DonneesVirus.chargerJar();
        SERINGUE = objet(e, new ItemSeringue(), "seringue_vide");
        ORDONNANCE = objet(e, new ItemOrdonnance(), "ordonnance");
        for (Block b : BLOCS) {
            Item ib = new ItemBlock(b).setRegistryName(b.getRegistryName());
            e.getRegistry().register(ib);
            OBJETS.add(ib);
        }
    }

    public static Item objet(RegistryEvent.Register<Item> e, Item i, String id) {
        if (i.getRegistryName() == null) i.setRegistryName(HxrpMetiers.MODID, id);
        if (i.getTranslationKey().equals("item.null")) i.setTranslationKey(HxrpMetiers.MODID + "." + id);
        if (i.getCreativeTab() == null) i.setCreativeTab(TAB);
        e.getRegistry().register(i);
        OBJETS.add(i);
        Objets.VIRUS.put(id, i);
        return i;
    }

    @SubscribeEvent
    public static void sons(RegistryEvent.Register<SoundEvent> e) {
        VirusSons.enregistrer(e.getRegistry());
    }
}
