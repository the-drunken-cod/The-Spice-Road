package com.drunkencod.spice_road.grinder;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.registry.ModBlockEntities;
import com.drunkencod.spice_road.registry.ModDataComponents;
import com.drunkencod.spice_road.registry.ModItems;

/**
 * The state of a {@link SpiceGrinderBlock}: the {@link SeasoningSession} the
 * Grinder carried when it was placed, if any. Everything else the item carried
 * (like a custom name) is kept as the block entity's components, so
 * {@link #toStack} and the block's loot table give back the same Grinder.
 * <p>
 * The session is never sent to clients, as it holds the hidden board; they only
 * get the per-player view of the open menu.
 */
public class SpiceGrinderBlockEntity extends BlockEntity {

    private static final String SESSION_KEY = "Session";

    private @Nullable SeasoningSession session;
    private @Nullable ServerPlayer user;

    /**
     * @param pos   The Grinder's position.
     * @param state The Grinder's block state.
     */
    public SpiceGrinderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPICE_GRINDER.get(), pos, state);
    }

    /** @return The run in progress, {@code null} while there is none. */
    public @Nullable SeasoningSession session() {
        return session;
    }

    /**
     * Replaces the run in progress and saves it.
     *
     * @param newSession The new run, {@code null} to end it.
     */
    public void setSession(@Nullable SeasoningSession newSession) {
        session = newSession;
        setChanged();
    }

    /** @return The Grinder item as it was placed, with its session. */
    public ItemStack toStack() {
        ItemStack stack = new ItemStack(ModItems.SPICE_GRINDER.get());
        stack.applyComponents(collectComponents());
        return stack;
    }

    // #region use

    /**
     * @return Whether a player has this Grinder's GUI open, which keeps anyone
     *         else from opening or picking it up.
     */
    public boolean isInUse() {
        return user != null && !user.hasDisconnected() && user.containerMenu instanceof SpiceGrinderMenu menu
                && menu.block() == this;
    }

    /**
     * Marks the Grinder as used by the player whose GUI just opened.
     *
     * @param player The player.
     */
    void setUser(@Nullable ServerPlayer player) {
        user = player;
    }

    // #region save

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        session = input.get(ModDataComponents.GRINDER_SESSION.get());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(ModDataComponents.GRINDER_SESSION.get(), session);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove(SESSION_KEY);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (session != null)
            SeasoningSession.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), session)
                    .resultOrPartial(error -> Constants.LOG.error("Couldn't save the session of a Spice Grinder: {}", error))
                    .ifPresent(encoded -> tag.put(SESSION_KEY, encoded));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        session = null;
        Tag saved = tag.get(SESSION_KEY);
        if (saved != null)
            session = SeasoningSession.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), saved)
                    .resultOrPartial(error -> Constants.LOG.error("Couldn't load the session of a Spice Grinder: {}", error))
                    .orElse(null);
    }
}
