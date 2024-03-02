package net.ramgames.tamableaxolotls.mixins;


import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.Bucketable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.ServerConfigHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.ramgames.tamableaxolotls.AxolotlEntityAccess;
import net.ramgames.tamableaxolotls.AxolotlFollowOwnerGoal;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.UUID;

@Mixin(value = AxolotlEntity.class, priority = 1001)
public abstract class AxolotlEntityMixin extends AnimalEntity implements AxolotlEntityAccess {
    @Unique
    private static final TrackedData<Byte> TAMEABLE_FLAGS = DataTracker.registerData(AxolotlEntity.class, TrackedDataHandlerRegistry.BYTE);
    @Unique
    private static final TrackedData<Optional<UUID>> OWNER_UUID = DataTracker.registerData(AxolotlEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    @Unique
    private boolean sitting;

    protected AxolotlEntityMixin(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
        this.onTamedChanged();
    }
    @Inject(method = "initDataTracker", at = @At("TAIL"))
    protected void initDataTracker(CallbackInfo ci) {
        this.dataTracker.startTracking(TAMEABLE_FLAGS, (byte)0);
        this.dataTracker.startTracking(OWNER_UUID, Optional.empty());
    }
    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    public void writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        if (this.getOwnerUuid() != null) {
            nbt.putUuid("Owner", this.getOwnerUuid());
        }

        nbt.putBoolean("Sitting", this.sitting);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    public void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
        UUID uUID;
        if (nbt.containsUuid("Owner")) {
            uUID = nbt.getUuid("Owner");
        } else {
            String string = nbt.getString("Owner");
            uUID = ServerConfigHandler.getPlayerUuidByName(this.getServer(), string);
        }

        if (uUID != null) {
            try {
                this.setOwnerUuid(uUID);
                this.setTamed(true);
            } catch (Throwable var4) {
                this.setTamed(false);
            }
        }
        this.sitting = nbt.getBoolean("Sitting");
        this.setInSittingPose(this.sitting);
    }
    @Inject(method = "copyDataToStack", at = @At(value = "TAIL"))
    public void addOwnerToBucketNbt(ItemStack stack, CallbackInfo ci) {
        if(this.getOwnerUuid() != null) stack.getOrCreateNbt().putUuid("Owner", getOwnerUuid());
    }

    @Inject(method = "copyDataFromNbt", at = @At(value = "TAIL"))
    public void addOwnerToBucketDeploy(NbtCompound nbt, CallbackInfo ci) {
        if(nbt.containsUuid("Owner")) setOwnerUuid(nbt.getUuid("Owner"));
    }

    public boolean canBeLeashedBy(PlayerEntity player) {
        return !this.isLeashed();
    }

    @Unique
    protected void showEmoteParticle(boolean positive) {
        ParticleEffect particleEffect = ParticleTypes.HEART;
        if (!positive) {
            particleEffect = ParticleTypes.SMOKE;
        }

        for(int i = 0; i < 7; ++i) {
            double d = this.random.nextGaussian() * 0.02;
            double e = this.random.nextGaussian() * 0.02;
            double f = this.random.nextGaussian() * 0.02;
            this.getWorld().addParticle(particleEffect, this.getParticleX(1.0), this.getRandomBodyY() + 0.5, this.getParticleZ(1.0), d, e, f);
        }

    }

    public void handleStatus(byte status) {
        if (status == 7) {
            this.showEmoteParticle(true);
        } else if (status == 6) {
            this.showEmoteParticle(false);
        } else {
            super.handleStatus(status);
        }

    }
    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Unique
    public boolean isTamed() {
        return (this.dataTracker.get(TAMEABLE_FLAGS) & 4) != 0;
    }

    @Unique
    public void setTamed(boolean tamed) {
        byte b = this.dataTracker.get(TAMEABLE_FLAGS);
        if (tamed) {
            this.dataTracker.set(TAMEABLE_FLAGS, (byte)(b | 4));
        } else {
            this.dataTracker.set(TAMEABLE_FLAGS, (byte)(b & -5));
        }

        this.onTamedChanged();
    }

    @Unique
    protected void onTamedChanged() {
    }

    @Unique
    public boolean isInSittingPose() {
        return ((Byte)this.dataTracker.get(TAMEABLE_FLAGS) & 1) != 0;
    }

    @Unique
    public void setInSittingPose(boolean inSittingPose) {
        byte b = (Byte)this.dataTracker.get(TAMEABLE_FLAGS);
        if (inSittingPose) {
            this.dataTracker.set(TAMEABLE_FLAGS, (byte)(b | 1));
        } else {
            this.dataTracker.set(TAMEABLE_FLAGS, (byte)(b & -2));
        }

    }

    @Unique
    @Nullable
    public UUID getOwnerUuid() {
        return (UUID)((Optional)this.dataTracker.get(OWNER_UUID)).orElse(null);
    }

    @Unique
    public void setOwnerUuid(@Nullable UUID uuid) {
        this.dataTracker.set(OWNER_UUID, Optional.ofNullable(uuid));
    }

    @Unique
    public void setOwner(PlayerEntity player) {
        this.setTamed(true);
        this.setOwnerUuid(player.getUuid());
        if (player instanceof ServerPlayerEntity) {
            Criteria.TAME_ANIMAL.trigger((ServerPlayerEntity)player, this);
        }

    }

    public boolean canTarget(LivingEntity target) {
        return !this.isOwner(target) && super.canTarget(target);
    }

    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Unique
    public boolean canAttackWithOwner(LivingEntity target, LivingEntity owner) {
        if(target instanceof TameableEntity tameable) return tameable.getOwner() != owner;
        return true;
    }

    @Unique
    public boolean isOwner(LivingEntity entity) {
        return entity == this.tamableAxolotls$getOwner();
    }

    @Override
    public AbstractTeam getScoreboardTeam() {
        if (this.isTamed()) {
            LivingEntity livingEntity = this.tamableAxolotls$getOwner();
            if (livingEntity != null) {
                return livingEntity.getScoreboardTeam();
            }
        }

        return super.getScoreboardTeam();
    }

    public boolean isTeammate(Entity other) {
        if (this.isTamed()) {
            LivingEntity livingEntity = this.tamableAxolotls$getOwner();
            if (other == livingEntity) {
                return true;
            }

            if (livingEntity != null) {
                return livingEntity.isTeammate(other);
            }
        }

        return super.isTeammate(other);
    }

    public void onDeath(DamageSource damageSource) {
        if (!this.getWorld().isClient && this.getWorld().getGameRules().getBoolean(GameRules.SHOW_DEATH_MESSAGES) && this.tamableAxolotls$getOwner() instanceof ServerPlayerEntity) {
            this.tamableAxolotls$getOwner().sendMessage(this.getDamageTracker().getDeathMessage());
        }

        super.onDeath(damageSource);
    }

    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Unique
    public boolean isSitting() {
        return this.sitting;
    }

    @Unique
    public void setSitting(boolean sitting) {
        this.sitting = sitting;
    }


    public @Nullable LivingEntity tamableAxolotls$getOwner() {
        UUID uUID = this.getOwnerUuid();
        if(this.getWorld() == null) return null;
        return uUID == null ? null : this.getWorld().getPlayerByUuid(uUID);
    }

    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        Optional<ActionResult> result = Bucketable.tryBucket(player, hand, (AxolotlEntity)(Object)this);
        if(result.isPresent()) return result.get();
        ItemStack itemStack = player.getStackInHand(hand);
        Item item = itemStack.getItem();
        if (this.getWorld().isClient) {
            boolean bl = this.isOwner(player) || this.isTamed() || itemStack.isOf(Items.TROPICAL_FISH) && !this.isTamed();
            return bl ? ActionResult.CONSUME : ActionResult.PASS;
        } else {
            label90: {
                if (this.isTamed()) {
                    System.out.println("tamed!");
                    if (Items.TROPICAL_FISH.equals(itemStack.getItem()) || this.getHealth() < this.getMaxHealth()) {
                        System.out.println("holding item or not full health!");
                        if (!player.getAbilities().creativeMode) {
                            itemStack.decrement(1);
                        }
                        if(this.getBreedingAge() == 0 && !this.isInLove()) {
                            System.out.println("ready to breed...");
                            this.setLoveTicks(600);
                            this.getWorld().sendEntityStatus(this, (byte)18);
                            showEmoteParticle(true);
                        }
                        else this.heal((float)item.getFoodComponent().getHunger());
                        System.out.println("finished");
                        return ActionResult.SUCCESS;
                    }

                    if (!(item instanceof DyeItem)) {
                        break label90;
                    }

                    if (!this.isOwner(player)) {
                        break label90;
                    }

                } else if (itemStack.isOf(Items.TROPICAL_FISH)) {
                    if (!player.getAbilities().creativeMode) {
                        itemStack.decrement(1);
                    }

                    if (this.random.nextInt(3) == 0) {
                        this.setOwner(player);
                        this.navigation.stop();
                        this.setTarget(null);
                        this.setSitting(true);
                        this.getWorld().sendEntityStatus(this, (byte)7);
                    } else {
                        this.getWorld().sendEntityStatus(this, (byte)6);
                    }

                    return ActionResult.SUCCESS;
                }

                return super.interactMob(player, hand);
            }

            ActionResult actionResult = super.interactMob(player, hand);
            if ((!actionResult.isAccepted() || this.isBaby()) && this.isOwner(player)) {
                this.setSitting(!this.isSitting());
                this.navigation.stop();
                this.setTarget(null);
                return ActionResult.SUCCESS;
            }
        }
        return super.interactMob(player, hand);
    }

    protected void initGoals() {
        this.goalSelector.add(12, new AxolotlFollowOwnerGoal((AxolotlEntity)(Object)this, 0.75, 24.0F, 2.0F, false));
    }

    @Inject(method = "createChild", at = @At("TAIL"))
    private void addOwnerTagToChild(ServerWorld world, PassiveEntity entity, CallbackInfoReturnable<PassiveEntity> cir) {
        PassiveEntity child = cir.getReturnValue();
        if(isTamed()) ((AxolotlEntityMixin) child).setOwnerUuid(getOwnerUuid());
    }
}
