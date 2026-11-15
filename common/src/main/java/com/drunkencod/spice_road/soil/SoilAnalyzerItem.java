package com.drunkencod.spice_road.soil;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.advancement.ModCriteriaTriggers;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.ModDataComponents;
import com.drunkencod.spice_road.registry.ModItems;
import com.drunkencod.spice_road.registry.ModSounds;
import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.region.SublevelPositions;
import com.drunkencod.spice_road.stats.ModStats;

/**
 * The Soil Analyzer: held on a dirt block for a few seconds it takes a Soil
 * Sample, removing the block. A finished set of samples (see
 * {@link SoilSamples}) is analyzed by {@link SoilAnalysis}.
 * <p>
 * The same {@link #check} runs on both sides so the client only starts the use
 * animation when the server will accept the sample; the server alone drives the
 * progress, the block removal and the messages. Sneak-clicking anything but a
 * dirt block clears the samples taken so far.
 */
public class SoilAnalyzerItem extends Item {

    private static final String USAGE_TOOLTIP_KEY = Constants.MOD_ID + ".tooltip.soil_analyzer_usage";
    private static final String SAMPLES_TOOLTIP_KEY = Constants.MOD_ID + ".tooltip.soil_analyzer_samples";
    private static final String MESSAGE_PREFIX = Constants.MOD_ID + ".message.soil_analyzer.";

    /** Segments of the progress bar shown above the hotbar. */
    private static final int BAR_SEGMENTS = 8;
    /** Ticks between two progress bar updates. */
    private static final int PROGRESS_INTERVAL_TICKS = 2;
    /** Ticks between two digging sounds. */
    private static final int SOUND_INTERVAL_TICKS = 5;
    /** Ticks of cooldown - like ender pearls and chorus fruit. */
    private static final int USE_COOLDOWN = 20;

    /** Non-filled segment character for the analyzer progress bar. */
    private static final String BAR_SEGMENT_EMPTY = "\u002d"; // -
    /** Filled segment character for the analyzer progress bar. */
    private static final String BAR_SEGMENT_FILLED = "\u25a0"; // ■
    // private static final String BAR_SEGMENT_FILLED = "\u2588"; // █

    /** The block each player is currently taking a sample of, server side. */
    private static final Map<UUID, BlockPos> TARGETS = new ConcurrentHashMap<>();

    /** Creates the item, one per stack. */
    public SoilAnalyzerItem() {
        super(new Properties().stacksTo(1));
    }

    /**
     * The durability each loader gives the item as a default component, when
     * it sets those up. That is after the common config has loaded in a normal
     * game launch, which item registration itself isn't on NeoForge.
     *
     * @return The configured durability, or {@code 0} if the item is unbreakable
     *         or the config isn't available (e.g. during data generation).
     */
    public static int configuredDurability() {
        try {
            return Services.CONFIG.getSoilAnalyzerDurability();
        } catch (IllegalStateException e) {
            return 0;
        }
    }

    // #region Tooltip

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(USAGE_TOOLTIP_KEY).withStyle(ChatFormatting.GRAY));
        SoilSamples samples = stack.get(ModDataComponents.SOIL_SAMPLES.get());
        if (samples != null)
            tooltip.add(Component.translatable(SAMPLES_TOOLTIP_KEY, samples.count(),
                    Services.CONFIG.getSoilAnalyzerSamplesRequired()).withStyle(ChatFormatting.GRAY));
    }

    // #region Use

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BRUSH;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return Math.max(1, (int) Math.round(Services.CONFIG.getSoilAnalyzerSampleSeconds() * 20));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null)
            return InteractionResult.PASS;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        if (player.isSecondaryUseActive() && !level.getBlockState(pos).is(BlockTags.DIRT)) {
            clearSamples(player, stack);
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        SoilSampleCheck check = check(level, player, stack, pos);
        if (check != SoilSampleCheck.OK) {
            tell(player, check.message());
            return InteractionResult.FAIL;
        }
        if (player.isCreative()) {
            if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer)
                takeSample(serverPlayer, serverLevel, stack, pos);
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (!level.isClientSide())
            TARGETS.put(player.getUUID(), pos.immutable());
        player.startUsingItem(context.getHand());
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isSecondaryUseActive())
            return InteractionResultHolder.pass(stack);
        clearSamples(player, stack);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingTicks) {
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof ServerPlayer player))
            return;
        BlockPos target = TARGETS.get(player.getUUID());
        BlockHitResult hit = lookedAtBlock(player);
        if (target == null || hit == null || !hit.getBlockPos().equals(target)
                || !level.getBlockState(target).is(BlockTags.DIRT)) {
            player.releaseUsingItem();
            return;
        }

        int total = getUseDuration(stack, player);
        int elapsed = total - remainingTicks + 1;
        if (elapsed == 1 || elapsed % PROGRESS_INTERVAL_TICKS == 0)
            player.displayClientMessage(progressMessage(elapsed / (double) total), true);
        if (elapsed % SOUND_INTERVAL_TICKS == 0) {
            BlockState state = level.getBlockState(target);
            Vec3 at = hit.getLocation();
            serverLevel.playSound(null, target, state.getSoundType().getHitSound(), SoundSource.BLOCKS, 0.6F, 0.8F);
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), at.x, at.y, at.z, 3, 0.15,
                    0.1, 0.15, 0.02);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player))
            return;
        TARGETS.remove(player.getUUID());
        tell(player, Component.empty());
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level instanceof ServerLevel serverLevel && entity instanceof ServerPlayer player) {
            BlockPos target = TARGETS.remove(player.getUUID());
            if (target != null)
                takeSample(player, serverLevel, stack, target);
        }
        return stack;
    }

    // #region Sampling

    /**
     * Checks whether a sample may be taken at a block. The protection checks
     * need the server, so the client only runs the rest.
     *
     * @param level  The level the block is in.
     * @param player The player taking the sample.
     * @param stack  The Soil Analyzer.
     * @param pos    The block's position.
     * @return {@link SoilSampleCheck#OK}, or why the sample is refused.
     */
    public static SoilSampleCheck check(Level level, Player player, ItemStack stack, BlockPos pos) {
        if (!Services.CONFIG.isSoilAnalyzerEnabled())
            return SoilSampleCheck.DISABLED;
        if (!level.getBlockState(pos).is(BlockTags.DIRT))
            return SoilSampleCheck.NOT_DIRT;

        BlockPos abovePos = pos.above();
        BlockState above = level.getBlockState(abovePos);
        boolean breaksAbove = isBrokenWithSample(above);
        if (!above.isAir() && !(above.getBlock() instanceof LiquidBlock) && !breaksAbove
                && !above.isFaceSturdy(level, abovePos, Direction.DOWN))
            return SoilSampleCheck.BLOCKED_ABOVE;

        SoilSamples.Spacing spacing = SoilSamples.check(stack.get(ModDataComponents.SOIL_SAMPLES.get()),
                level.dimension(), SublevelPositions.projectOutOfSubLevel(level, pos),
                Services.CONFIG.getSoilAnalyzerMinSampleDistance(), Services.CONFIG.getSoilAnalyzerMaxSampleSpan());
        if (spacing != SoilSamples.Spacing.OK)
            return SoilSampleCheck.of(spacing);

        if (!player.isCreative() && player instanceof ServerPlayer serverPlayer
                && (isProtected(level, serverPlayer, pos) || (breaksAbove && isProtected(level, serverPlayer, abovePos))))
            return SoilSampleCheck.PROTECTED;
        return SoilSampleCheck.OK;
    }

    /**
     * @param state The block above a sample.
     * @return Whether the block is in {@code #minecraft:replaceable} and not air or
     *         a liquid, so it gets broken like by hand when the sample is taken.
     */
    private static boolean isBrokenWithSample(BlockState state) {
        return state.is(BlockTags.REPLACEABLE) && !state.isAir() && !(state.getBlock() instanceof LiquidBlock);
    }

    /**
     * @return Whether the player isn't allowed to break the block at the position,
     *         because of spawn protection, game mode or another mod.
     */
    private static boolean isProtected(Level level, ServerPlayer player, BlockPos pos) {
        return !level.mayInteract(player, pos)
                || player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer())
                || !Services.PLATFORM.canBreakBlock(player, pos);
    }

    /**
     * Records a sample, removes its block, and analyzes the soil once enough
     * samples are in. Checks the rules again, since time has passed since the
     * sample was started.
     */
    private static void takeSample(ServerPlayer player, ServerLevel level, ItemStack stack, BlockPos pos) {
        SoilSampleCheck check = check(level, player, stack, pos);
        if (check != SoilSampleCheck.OK) {
            tell(player, check.message());
            return;
        }

        BlockPos effective = SublevelPositions.projectOutOfSubLevel(level, pos);
        SoilSamples before = stack.get(ModDataComponents.SOIL_SAMPLES.get());
        SoilSamples after = before == null ? SoilSamples.startedWith(level.dimension(), effective)
                : before.with(effective);
        if (!player.isCreative()) {
            BlockPos abovePos = pos.above();
            if (isBrokenWithSample(level.getBlockState(abovePos)))
                level.destroyBlock(abovePos, true, player);
            level.destroyBlock(pos, false, player);
        }
        ModStats.award(player, ModStats.SOIL_SAMPLES_TAKEN);

        int required = player.isCreative() ? 1 : Services.CONFIG.getSoilAnalyzerSamplesRequired();
        if (after.count() >= required) {
            stack.remove(ModDataComponents.SOIL_SAMPLES.get());
            Optional<Spice> resolved = SoilAnalysis.resolve(level, after);
            player.sendSystemMessage(SoilAnalysis.report(player, resolved));
            if (resolved.isPresent())
                ModCriteriaTriggers.SOIL_ANALYZED.get().trigger(player);
            level.playSound(null, player.blockPosition(), ModSounds.SOIL_ANALYZER_FINISH_SAMPLING.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        } else {
            stack.set(ModDataComponents.SOIL_SAMPLES.get(), after);
            tell(player, Component.translatable(MESSAGE_PREFIX + "collected", after.count(), required));
        }

        if (!player.isCreative()) {
            triggerUseCooldown(player);
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
        }
    }

    private static void triggerUseCooldown(Player player) {
        player.resetCurrentImpulseContext();
        player.getCooldowns().addCooldown(ModItems.SOIL_ANALYZER.get(), USE_COOLDOWN);
    }

    /** Forgets the samples taken so far, and tells the server-side player. */
    private static void clearSamples(Player player, ItemStack stack) {
        boolean had = stack.remove(ModDataComponents.SOIL_SAMPLES.get()) != null;
        tell(player, Component.translatable(MESSAGE_PREFIX + (had ? "cleared" : "no_samples")));
    }

    // #region Helpers

    private static @Nullable BlockHitResult lookedAtBlock(Player player) {
        HitResult hit = ProjectileUtil.getHitResultOnViewVector(player,
                entity -> !entity.isSpectator() && entity.isPickable(), player.blockInteractionRange());
        return hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK ? blockHit : null;
    }

    /** Shows a message above the hotbar, to server-side players only. */
    private static void tell(Player player, Component message) {
        if (player instanceof ServerPlayer serverPlayer)
            serverPlayer.displayClientMessage(message, true);
    }

    /**
     * @param progress How far along the sample is, from {@code 0.0} to {@code 1.0}.
     * @return The bar and percentage shown above the hotbar.
     */
    private static Component progressMessage(double progress) {
        double clamped = Math.max(0.0, Math.min(1.0, progress));
        int filled = (int) Math.floor(clamped * BAR_SEGMENTS);
        MutableComponent bar = Component.literal(BAR_SEGMENT_FILLED.repeat(filled)).withStyle(ChatFormatting.GREEN)
                .append(Component.literal(BAR_SEGMENT_EMPTY.repeat(BAR_SEGMENTS - filled))
                        .withStyle(ChatFormatting.DARK_GRAY));
        int percent = (int) Math.floor(clamped * 100);
        return Component.translatable(MESSAGE_PREFIX + "progress", bar, (percent < 10 ? "0" : "") + percent);
    }
}
