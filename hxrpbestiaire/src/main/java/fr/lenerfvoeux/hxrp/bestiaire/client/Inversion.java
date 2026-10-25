package fr.lenerfvoeux.hxrp.bestiaire.client;

import fr.lenerfvoeux.hxrp.bestiaire.HxrpBestiaire;
import fr.lenerfvoeux.hxrp.bestiaire.Registre;
import net.minecraft.util.MovementInput;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/** Vœu maudit d'Aï : les touches de déplacement du joueur local sont inversées tant que dure l'effet. */
@Mod.EventBusSubscriber(modid = HxrpBestiaire.MODID, value = Side.CLIENT)
public final class Inversion {
    private Inversion() {}

    @SubscribeEvent
    public static void touches(InputUpdateEvent e) {
        if (Registre.INVERSE == null || !e.getEntityPlayer().isPotionActive(Registre.INVERSE)) return;
        MovementInput m = e.getMovementInput();
        m.moveForward = -m.moveForward;
        m.moveStrafe = -m.moveStrafe;
    }
}
