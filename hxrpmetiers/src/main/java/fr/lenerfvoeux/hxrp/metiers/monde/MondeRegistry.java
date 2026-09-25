package fr.lenerfvoeux.hxrp.metiers.monde;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.ModRegistry;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemSeeds;
import net.minecraftforge.registries.IForgeRegistry;

/** Enregistre les blocs et objets de récolte décrits dans recolte.json. */
public final class MondeRegistry {
    private MondeRegistry() {}

    public static void blocs(IForgeRegistry<Block> reg) {
        Recolte r = Recolte.get();
        for (Recolte.Culture c : r.cultures) {
            BlockCulture b = new BlockCulture(c);
            ModRegistry.CULTURES.put(c.id, b);
            reg.register(b);
        }
        for (Recolte.Arbre a : r.arbres) {
            BlockFeuillage f = new BlockFeuillage(a);
            BlockPousse p = new BlockPousse(a);
            f.pousse = p;
            p.feuilles = f;
            ModRegistry.FEUILLES.put(a.id, f);
            ModRegistry.POUSSES.put(a.id, p);
            reg.register(f);
            reg.register(p);
        }
        ModRegistry.MINERAI_SEL = new BlockMineraiSel();
        ModRegistry.RUCHE = new BlockRuche();
        ModRegistry.MOULES = new BlockCoquillages("banc_de_moules", "moule");
        ModRegistry.HUITRES = new BlockCoquillages("banc_d_huitres", "huitre");
        for (Block b : new Block[]{ModRegistry.MINERAI_SEL, ModRegistry.RUCHE, ModRegistry.MOULES, ModRegistry.HUITRES}) {
            b.setCreativeTab(ModRegistry.TAB_RECOLTE);
            reg.register(b);
        }
    }

    public static void objets(IForgeRegistry<Item> reg) {
        for (BlockCulture c : ModRegistry.CULTURES.values()) {
            Item g = new ItemSeeds(c, Blocks.FARMLAND)
                    .setRegistryName(HxrpMetiers.MODID, "graines_" + c.def.id)
                    .setTranslationKey(HxrpMetiers.MODID + ".graines_" + c.def.id)
                    .setCreativeTab(ModRegistry.TAB_RECOLTE);
            c.graine = g;
            ajouter(reg, g);
        }
        for (String id : ModRegistry.POUSSES.keySet()) {
            ajouter(reg, new ItemBlock(ModRegistry.POUSSES.get(id)).setRegistryName(ModRegistry.POUSSES.get(id).getRegistryName()).setCreativeTab(ModRegistry.TAB_RECOLTE));
            ajouter(reg, new ItemBlock(ModRegistry.FEUILLES.get(id)).setRegistryName(ModRegistry.FEUILLES.get(id).getRegistryName()).setCreativeTab(ModRegistry.TAB_RECOLTE));
        }
        for (Block b : new Block[]{ModRegistry.MINERAI_SEL, ModRegistry.RUCHE, ModRegistry.MOULES, ModRegistry.HUITRES})
            ajouter(reg, new ItemBlock(b).setRegistryName(b.getRegistryName()));
        for (String o : Recolte.get().objets) {
            Item i = new Item().setRegistryName(HxrpMetiers.MODID, o).setTranslationKey(HxrpMetiers.MODID + "." + o).setCreativeTab(ModRegistry.TAB_RECOLTE);
            ModRegistry.OBJETS_MONDE.put(o, i);
            ajouter(reg, i);
        }
    }

    private static void ajouter(IForgeRegistry<Item> reg, Item i) {
        reg.register(i);
        ModRegistry.ALL_ITEMS.add(i);
    }
}
