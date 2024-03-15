package net.ramgames.tamableaxolotls;

import net.minecraft.entity.LivingEntity;

public interface AxolotlEntityAccess {

    LivingEntity tamableAxolotls$getOwner();

    boolean tamableAxolotls$isSitting();

    boolean tamableAxolotls$isTamed();
    boolean tamableAxolotls$canAttackWithOwner(LivingEntity target, LivingEntity owner);
}
