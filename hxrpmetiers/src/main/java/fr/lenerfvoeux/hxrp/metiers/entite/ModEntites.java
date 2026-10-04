package fr.lenerfvoeux.hxrp.metiers.entite;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.monde.Climat;
import fr.lenerfvoeux.hxrp.metiers.monde.Recolte;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Enregistre les animaux du Gourmet, leurs œufs d'apparition et leurs lieux de vie. */
@Mod.EventBusSubscriber(modid = HxrpMetiers.MODID)
public final class ModEntites {
    /** id -> classe, dans l'ordre de recolte.json. */
    public static final Map<String, Class<? extends EntityLiving>> CLASSES = new LinkedHashMap<>();

    static {
        CLASSES.put("saumon", EntityPoisson.Saumon.class);
        CLASSES.put("truite", EntityPoisson.Truite.class);
        CLASSES.put("thon", EntityPoisson.Thon.class);
        CLASSES.put("cabillaud", EntityPoisson.Cabillaud.class);
        CLASSES.put("sardine", EntityPoisson.Sardine.class);
        CLASSES.put("maquereau", EntityPoisson.Maquereau.class);
        CLASSES.put("anchois", EntityPoisson.Anchois.class);
        CLASSES.put("crevette", EntityPoisson.Crevette.class);
        CLASSES.put("crabe", EntityCrabe.class);
        CLASSES.put("dinde", EntityVolaille.Dinde.class);
        CLASSES.put("canard", EntityVolaille.Canard.class);
        CLASSES.put("cerf", EntityCerf.class);
        CLASSES.put("sanglier", EntitySanglier.class);
        CLASSES.put("chevre", EntityChevre.class);
    }

    private ModEntites() {}

    @SubscribeEvent
    public static void entites(RegistryEvent.Register<EntityEntry> e) {
        int net = 0;
        for (Recolte.Animal a : Recolte.get().animaux) {
            Class<? extends EntityLiving> c = CLASSES.get(a.id);
            if (c == null) throw new IllegalStateException("Animal sans classe : " + a.id);
            boolean aquatique = EntityPoisson.class.isAssignableFrom(c);
            EntityEntryBuilder<net.minecraft.entity.Entity> b = EntityEntryBuilder.create()
                    .entity(c)
                    .id(new ResourceLocation(HxrpMetiers.MODID, a.id), net++)
                    .name(HxrpMetiers.MODID + "." + a.id)
                    .tracker(aquatique ? 64 : 80, 3, true);
            if (a.oeuf != null && a.oeuf.length == 2) b = b.egg(a.oeuf[0], a.oeuf[1]);
            e.getRegistry().register(b.build());
        }
    }

    /** Lieux de vie (appelé à l'initialisation, une fois les biomes connus). */
    public static void apparitions() {
        for (Recolte.Animal a : Recolte.get().animaux) {
            Class<? extends EntityLiving> c = CLASSES.get(a.id);
            List<Biome> biomes = new ArrayList<>();
            for (Biome b : ForgeRegistries.BIOMES.getValuesCollection())
                for (String m : a.milieux) if (Climat.convient(m, b)) { biomes.add(b); break; }
            if (biomes.isEmpty()) continue;
            Biome[] tab = biomes.toArray(new Biome[0]);
            switch (a.comportement) {
                case "poisson": EntityRegistry.addSpawn(c, 5, 1, 2, EnumCreatureType.WATER_CREATURE, tab); break;
                case "banc": EntityRegistry.addSpawn(c, 6, 3, 6, EnumCreatureType.WATER_CREATURE, tab); break;
                case "fond": EntityRegistry.addSpawn(c, 5, 2, 4, EnumCreatureType.WATER_CREATURE, tab); break;
                case "crabe": EntityRegistry.addSpawn(c, 8, 1, 3, EnumCreatureType.CREATURE, tab); break;
                case "volaille": EntityRegistry.addSpawn(c, 7, 2, 4, EnumCreatureType.CREATURE, tab); break;
                case "gibier": EntityRegistry.addSpawn(c, 5, 1, 3, EnumCreatureType.CREATURE, tab); break;
                case "sanglier": EntityRegistry.addSpawn(c, 5, 1, 2, EnumCreatureType.CREATURE, tab); break;
                default: EntityRegistry.addSpawn(c, 6, 2, 3, EnumCreatureType.CREATURE, tab); break;
            }
            HxrpMetiers.LOG.debug("Gourmet : {} vit dans {} biome(s)", a.id, tab.length);
        }
    }
}
