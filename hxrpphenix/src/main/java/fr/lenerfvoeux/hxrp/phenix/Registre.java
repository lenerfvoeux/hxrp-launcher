package fr.lenerfvoeux.hxrp.phenix;

import fr.lenerfvoeux.hxrp.phenix.entite.EntityBouleDeFeu;
import fr.lenerfvoeux.hxrp.phenix.entite.EntityPhenix;
import fr.lenerfvoeux.hxrp.phenix.entite.EntityPlumeArdente;
import fr.lenerfvoeux.hxrp.phenix.entite.EntityPlumeObjet;
import fr.lenerfvoeux.hxrp.phenix.item.ItemOeufDePhenix;
import fr.lenerfvoeux.hxrp.phenix.item.ItemPlumeDePhenix;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;

@Mod.EventBusSubscriber(modid = HxrpPhenix.MODID)
public final class Registre {
    public static Item PLUME, OEUF;

    public static final CreativeTabs ONGLET = new CreativeTabs(HxrpPhenix.MODID) {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(PLUME);
        }
    };

    private Registre() {}

    @SubscribeEvent
    public static void objets(RegistryEvent.Register<Item> e) {
        PLUME = new ItemPlumeDePhenix().setRegistryName(HxrpPhenix.MODID, "plume_de_phenix")
                .setTranslationKey(HxrpPhenix.MODID + ".plume_de_phenix").setCreativeTab(ONGLET);
        OEUF = new ItemOeufDePhenix().setRegistryName(HxrpPhenix.MODID, "oeuf_de_phenix")
                .setTranslationKey(HxrpPhenix.MODID + ".oeuf_de_phenix").setCreativeTab(ONGLET);
        e.getRegistry().registerAll(PLUME, OEUF);
    }

    @SubscribeEvent
    public static void entites(RegistryEvent.Register<EntityEntry> e) {
        e.getRegistry().registerAll(
                EntityEntryBuilder.create().entity(EntityPhenix.class).id(new ResourceLocation(HxrpPhenix.MODID, "phenix"), 0)
                        .name(HxrpPhenix.MODID + ".phenix").tracker(160, 1, true).build(),
                EntityEntryBuilder.create().entity(EntityBouleDeFeu.class).id(new ResourceLocation(HxrpPhenix.MODID, "boule_de_feu"), 1)
                        .name(HxrpPhenix.MODID + ".boule_de_feu").tracker(96, 1, true).build(),
                EntityEntryBuilder.create().entity(EntityPlumeArdente.class).id(new ResourceLocation(HxrpPhenix.MODID, "plume_ardente"), 2)
                        .name(HxrpPhenix.MODID + ".plume_ardente").tracker(96, 2, true).build(),
                EntityEntryBuilder.create().entity(EntityPlumeObjet.class).id(new ResourceLocation(HxrpPhenix.MODID, "plume_objet"), 3)
                        .name(HxrpPhenix.MODID + ".plume_objet").tracker(64, 20, true).build());
    }

    @SubscribeEvent
    public static void sons(RegistryEvent.Register<SoundEvent> e) {
        Sons.enregistrer(e.getRegistry());
    }
}
