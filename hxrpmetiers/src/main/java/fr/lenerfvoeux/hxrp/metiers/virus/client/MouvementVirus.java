package fr.lenerfvoeux.hxrp.metiers.virus.client;

import fr.lenerfvoeux.hxrp.metiers.virus.sante.Effet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.MovementInput;
import net.minecraft.util.MovementInputFromOptions;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Entrées de déplacement modifiées par les maladies : toux, éternuement et crises immobilisent ;
 * la désorientation inverse gauche et droite ; le saut peut être impossible.
 * Posé seulement le temps d'un effet, puis l'ancien gestionnaire d'entrées est remis.
 */
@SideOnly(Side.CLIENT)
public class MouvementVirus extends MovementInputFromOptions {
    private final MovementInput ancien;

    public MouvementVirus(Minecraft mc, MovementInput ancien) {
        super(mc.gameSettings);
        this.ancien = ancien;
    }

    static boolean utile() {
        return ClientVirus.actif(Effet.TOUX) || ClientVirus.actif(Effet.ETERNUE) || ClientVirus.actif(Effet.IMMOBILE3)
                || ClientVirus.actif(Effet.IMMOBILE5) || ClientVirus.actif(Effet.DESORIENTE) || ClientVirus.actif(Effet.NOSAUT);
    }

    static void installer(EntityPlayerSP p, Minecraft mc) {
        if (utile()) {
            if (!(p.movementInput instanceof MouvementVirus)) p.movementInput = new MouvementVirus(mc, p.movementInput);
        } else if (p.movementInput instanceof MouvementVirus) {
            MovementInput a = ((MouvementVirus) p.movementInput).ancien;
            p.movementInput = a != null ? a : new MovementInputFromOptions(mc.gameSettings);
        }
    }

    @Override
    public void updatePlayerMoveState() {
        if (ancien != null && !(ancien instanceof MouvementVirus)) {
            ancien.updatePlayerMoveState();
            moveStrafe = ancien.moveStrafe;
            moveForward = ancien.moveForward;
            forwardKeyDown = ancien.forwardKeyDown;
            backKeyDown = ancien.backKeyDown;
            leftKeyDown = ancien.leftKeyDown;
            rightKeyDown = ancien.rightKeyDown;
            jump = ancien.jump;
            sneak = ancien.sneak;
        } else {
            super.updatePlayerMoveState();
        }
        boolean bloque = ClientVirus.actif(Effet.TOUX) || ClientVirus.actif(Effet.ETERNUE) || ClientVirus.actif(Effet.IMMOBILE3) || ClientVirus.actif(Effet.IMMOBILE5);
        if (bloque) {
            moveStrafe = 0;
            moveForward = 0;
            forwardKeyDown = backKeyDown = leftKeyDown = rightKeyDown = false;
            jump = false;
            return;
        }
        if (ClientVirus.actif(Effet.DESORIENTE)) {
            moveStrafe = -moveStrafe;
            boolean g = leftKeyDown;
            leftKeyDown = rightKeyDown;
            rightKeyDown = g;
        }
        if (ClientVirus.actif(Effet.NOSAUT)) jump = false;
    }
}
