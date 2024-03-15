package net.ramgames.tamableaxolotls;

import net.minecraft.entity.LivingEntity;

public interface AxolotlEntityAccess {

    LivingEntity getOwner();

    boolean isSitting();

    boolean isTamed();
    boolean canAttackWithOwner(LivingEntity target, LivingEntity owner);
}
