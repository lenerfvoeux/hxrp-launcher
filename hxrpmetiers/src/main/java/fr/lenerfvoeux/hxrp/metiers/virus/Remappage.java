package fr.lenerfvoeux.hxrp.metiers.virus;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraft.util.ResourceLocation;

import java.util.Map;
import java.util.Set;

/**
 * Mondes d'avant la v0.7 : les préparations renommées deviennent les nouvelles (rien ne se perd dans les coffres),
 * les objets et blocs retirés disparaissent sans bloquer le démarrage (pas de question « blocs manquants »).
 */
@Mod.EventBusSubscriber(modid = HxrpMetiers.MODID)
public final class Remappage {
    /** Ancien identifiant → nouveau. */
    static final Map<String, String> RENOMMES = ImmutableMap.<String, String>builder()
            .put("pilules_de_mousse_des_abysses", "pilules_de_champignon_luminescent")
            .put("elixir_de_l_aigle_araignee", "elixir_de_truffe_noire")
            .put("sirop_de_fer_sang", "sirop_de_gentiane_et_de_bardane")
            .put("decoction_de_fer_sang_et_pied_bleu", "decoction_de_pied_bleu_et_de_chaga")
            .put("essence_des_quinze_raretes", "essence_des_dix_raretes")
            .build();

    /** Retirés en v0.7 : objets, et blocs du monde avec leur objet. */
    static final Set<String> RETIRES = ImmutableSet.of("baie_de_l_ile_de_la_baleine", "feuille_de_kukuroo", "oeuf_d_aigle_araignee",
            "racine_de_fer_sang", "mousse_des_abysses", "fumigation_de_plumes", "nid_d_aigle_araignee", "roche_a_fer_sang",
            "roche_des_abysses", "plante_baie_de_l_ile_de_la_baleine", "plante_feuille_de_kukuroo");

    private Remappage() {}

    @SubscribeEvent
    public static void objets(RegistryEvent.MissingMappings<Item> e) {
        for (RegistryEvent.MissingMappings.Mapping<Item> m : e.getAllMappings()) {
            if (!HxrpMetiers.MODID.equals(m.key.getNamespace())) continue;
            String nouveau = RENOMMES.get(m.key.getPath());
            Item i = nouveau == null ? null : ForgeRegistries.ITEMS.getValue(new ResourceLocation(HxrpMetiers.MODID, nouveau));
            if (i != null) {
                m.remap(i);
                HxrpMetiers.LOG.info("Virus : {} devient {}", m.key, i.getRegistryName());
            } else if (RETIRES.contains(m.key.getPath())) {
                m.ignore();
            }
        }
    }

    @SubscribeEvent
    public static void blocs(RegistryEvent.MissingMappings<Block> e) {
        for (RegistryEvent.MissingMappings.Mapping<Block> m : e.getAllMappings()) {
            if (HxrpMetiers.MODID.equals(m.key.getNamespace()) && RETIRES.contains(m.key.getPath())) m.ignore();
        }
    }
}
