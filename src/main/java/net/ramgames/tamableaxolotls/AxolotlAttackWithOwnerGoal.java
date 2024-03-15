package net.ramgames.tamableaxolotls;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.TrackTargetGoal;
import net.minecraft.entity.passive.AxolotlEntity;

import java.util.EnumSet;

public class AxolotlAttackWithOwnerGoal extends TrackTargetGoal {
    private final AxolotlEntity tameable;
    private LivingEntity attacking;
    private int lastAttackTime;

    public AxolotlAttackWithOwnerGoal(AxolotlEntity tameable) {
        super(tameable, false);
        this.tameable = tameable;
        this.setControls(EnumSet.of(Control.TARGET));
    }

    public boolean canStart() {
        if (((AxolotlEntityAccess)this.tameable).isTamed()) {
            LivingEntity livingEntity = ((AxolotlEntityAccess)this.tameable).getOwner();
            if (livingEntity == null) {
                return false;
            } else {
                this.attacking = livingEntity.getAttacking();
                int i = livingEntity.getLastAttackTime();
                return i != this.lastAttackTime && this.canTrack(this.attacking, TargetPredicate.DEFAULT) && ((AxolotlEntityAccess)this.tameable).canAttackWithOwner(this.attacking, livingEntity);
            }
        } else {
            return false;
        }
    }

    public void start() {
        this.mob.setTarget(this.attacking);
        LivingEntity livingEntity = ((AxolotlEntityAccess)this.tameable).getOwner();
        if (livingEntity != null) {
            this.lastAttackTime = livingEntity.getLastAttackTime();
        }

        super.start();
    }
}
