package com.drunkencod.spice_road.client.grinder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.grinder.GrinderActions;
import com.drunkencod.spice_road.grinder.GrinderIntentPayload;
import com.drunkencod.spice_road.grinder.GrinderLayout;
import com.drunkencod.spice_road.grinder.GrinderSpice;
import com.drunkencod.spice_road.grinder.GrinderView;
import com.drunkencod.spice_road.grinder.GrinderViewPayload;
import com.drunkencod.spice_road.grinder.SpiceGrinderMenu;
import com.drunkencod.spice_road.item.SpiceItemTags;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.FlavorAxis;
import com.drunkencod.spice_road.spice.board.BoardGeometry;
import com.drunkencod.spice_road.spice.board.CellView;
import com.drunkencod.spice_road.spice.board.Direction;
import com.drunkencod.spice_road.spice.effect.SeasoningEffect;
import com.drunkencod.spice_road.spice.effect.SeasoningEffectRegistry;
import com.drunkencod.spice_road.tooltip.TooltipUtil;

/**
 * The Spice Grinder GUI. Everything it shows comes from the latest
 * {@link GrinderView} the server sent, and everything the player does is sent
 * back as a {@link GrinderIntentPayload}; the screen never decides anything.
 * It is drawn with plain fills, text and a few small sprites (see
 * {@link GrinderLayout} for the pixel layout).
 * <p>
 * Keyboard: the player's own movement keys and the arrow keys step the pawn
 * (two pressed within the buffer window make a diagonal step, one step per
 * press), Jump locks in, and Enter presses the focused primary button. While a
 * run is going, the primary button (Accept) always keeps the focus; the pad
 * buttons never take it, so clicking them doesn't change what Enter does.
 */
public class SpiceGrinderScreen extends AbstractContainerScreen<SpiceGrinderMenu> {

    private static final String KEY_PREFIX = Constants.MOD_ID + ".gui.grinder.";
    private static final int COLOR_PANEL = 0xFF2B2B2B;
    private static final int COLOR_PANEL_BORDER = 0xFF111111;
    private static final int COLOR_TEXT = 0xFFE0E0E0;
    private static final int COLOR_DIM = 0xFF808080;
    private static final int COLOR_WARNING = 0xFFFFD040;
    private static final int MESSAGE_TICKS = 60;
    private static final ResourceLocation CELL_WALL = sprite("cells/wall");
    private static final ResourceLocation CELL_UNKNOWN = sprite("cells/unknown");
    private static final ResourceLocation CELL_MINE = sprite("cells/mine");
    private static final ResourceLocation CELL_BOON = sprite("cells/boon");
    private static final ResourceLocation CELL_BANE = sprite("cells/bane");
    private static final ResourceLocation PAWN = sprite("cells/pawn");
    private static final ResourceLocation BACKGROUND = sprite("background");
    /** Size of the background texture; it is stretched to the GUI's size. */
    private static final int BACKGROUND_TEXTURE_WIDTH = 9;
    private static final int BACKGROUND_TEXTURE_HEIGHT = 9;
    /** Width of the background texture's border, which is never stretched. */
    private static final int BACKGROUND_BORDER = 4;
    private static final ResourceLocation BUTTON_LOCK_IN = sprite("buttons/lock_in");

    private final List<Row> rows = new ArrayList<>();
    private final List<PadButton> padButtons = new ArrayList<>();
    private final List<Clipped> clipped = new ArrayList<>();
    private Button primaryButton;
    private Button cancelButton;
    private PadButton lockInButton;
    private int scroll;
    private boolean confirmPending;
    private int effectCountSeen;
    private Component message = CommonComponents.EMPTY;
    private int messageTicks;
    private long bufferStartMs;
    private int bufferedDx;
    private int bufferedDy;
    private boolean buffering;
    private boolean primaryWasActive;

    /**
     * One line of the spice list: a group header, or a loose spice or kind of
     * Spice Mix with the amount the player holds.
     */
    private record Row(Component header, GrinderSpice spice, int count) {
    }

    /**
     * Text that was cut short with an ellipsis this frame, and where, so hovering
     * it can show the full text.
     */
    private record Clipped(int x, int y, int width, Component full) {
    }

    /**
     * @param menu      The menu.
     * @param inventory The player's inventory.
     * @param title     The screen title.
     */
    public SpiceGrinderScreen(SpiceGrinderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = GrinderLayout.IMAGE_WIDTH;
        imageHeight = GrinderLayout.IMAGE_HEIGHT;
        titleLabelX = 8;
        titleLabelY = 5;
        inventoryLabelY = 10_000;
    }

    /**
     * @param path The path below {@code textures/gui/spice_grinder/}, without
     *             extension.
     * @return The location of that
     *         {@value GrinderLayout#SPRITE_SIZE}x{@value GrinderLayout#SPRITE_SIZE}
     *         texture.
     */
    private static ResourceLocation sprite(String path) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/spice_grinder/" + path + ".png");
    }

    /**
     * Draws a {@value GrinderLayout#SPRITE_SIZE}x{@value GrinderLayout#SPRITE_SIZE}
     * sprite texture.
     */
    private static void blitSprite(GuiGraphics graphics, ResourceLocation texture, int x, int y) {
        RenderSystem.enableBlend();
        graphics.blit(texture, x, y, 0, 0, GrinderLayout.SPRITE_SIZE, GrinderLayout.SPRITE_SIZE,
                GrinderLayout.SPRITE_SIZE, GrinderLayout.SPRITE_SIZE);
    }

    /**
     * Draws a texture stretched to the given size, keeping its
     * {@value #BACKGROUND_BORDER} px border unstretched: the corners as they are,
     * the edges and the center stretched.
     */
    private static void blitNineSliced(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width,
            int height) {
        int b = BACKGROUND_BORDER;
        int[] destX = { x, x + b, x + width - b };
        int[] destW = { b, width - 2 * b, b };
        int[] srcX = { 0, b, BACKGROUND_TEXTURE_WIDTH - b };
        int[] srcW = { b, BACKGROUND_TEXTURE_WIDTH - 2 * b, b };
        int[] destY = { y, y + b, y + height - b };
        int[] destH = { b, height - 2 * b, b };
        int[] srcY = { 0, b, BACKGROUND_TEXTURE_HEIGHT - b };
        int[] srcH = { b, BACKGROUND_TEXTURE_HEIGHT - 2 * b, b };
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++)
                graphics.blit(texture, destX[col], destY[row], destW[col], destH[row], srcX[col], srcY[row],
                        srcW[col], srcH[row], BACKGROUND_TEXTURE_WIDTH, BACKGROUND_TEXTURE_HEIGHT);
        }
    }

    /**
     * Makes the client apply every view the server sends to the open screen. Call
     * once from client init.
     */
    public static void registerViewHandler() {
        GrinderViewPayload.setClientHandler(payload -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null && minecraft.player.containerMenu instanceof SpiceGrinderMenu menu
                    && menu.containerId == payload.containerId()) {
                menu.setView(payload.view());
                if (minecraft.screen instanceof SpiceGrinderScreen screen)
                    screen.onView(payload.view());
            }
        });
    }

    // #region Setup

    @Override
    protected void init() {
        super.init();
        padButtons.clear();
        for (Direction direction : Direction.values()) {
            PadButton button = new PadButton(direction, padX(direction), padY(direction),
                    () -> send(GrinderIntentPayload.Kind.MOVE, direction.ordinal(), null));
            padButtons.add(button);
            addRenderableWidget(button);
        }
        lockInButton = new PadButton(null,
                leftPos + GrinderLayout.PAD_X + GrinderLayout.PAD_BUTTON + GrinderLayout.PAD_GAP,
                topPos + GrinderLayout.PAD_Y + GrinderLayout.PAD_BUTTON + GrinderLayout.PAD_GAP,
                () -> send(GrinderIntentPayload.Kind.LOCK_IN, 0, null));
        addRenderableWidget(lockInButton);

        primaryButton = addRenderableWidget(Button.builder(CommonComponents.EMPTY, button -> onPrimary())
                .bounds(leftPos + GrinderLayout.DRAFT_PRIMARY_X, topPos + GrinderLayout.DRAFT_PRIMARY_Y,
                        GrinderLayout.DRAFT_PRIMARY_WIDTH, GrinderLayout.BUTTON_HEIGHT)
                .build());
        cancelButton = addRenderableWidget(Button.builder(Component.translatable(KEY_PREFIX + "cancel"),
                button -> onClose())
                .bounds(leftPos + GrinderLayout.CANCEL_X, topPos + GrinderLayout.CANCEL_Y,
                        GrinderLayout.CANCEL_WIDTH, GrinderLayout.BUTTON_HEIGHT)
                .build());
        updateWidgets();
    }

    private int padX(Direction direction) {
        return leftPos + GrinderLayout.PAD_X + (direction.dx() + 1) * (GrinderLayout.PAD_BUTTON + GrinderLayout.PAD_GAP);
    }

    private int padY(Direction direction) {
        return topPos + GrinderLayout.PAD_Y + (direction.dy() + 1) * (GrinderLayout.PAD_BUTTON + GrinderLayout.PAD_GAP);
    }

    /** Called when the server sent a new view for this screen. */
    private void onView(GrinderView view) {
        if (view.effects().size() != effectCountSeen)
            confirmPending = false;
        effectCountSeen = view.effects().size();
        switch (view.event()) {
            case LOCKED_IN -> announce(SoundEvents.GRINDSTONE_USE, 1.2F, "event.locked_in");
            case MINE_HIT -> announce(SoundEvents.GENERIC_EXPLODE.value(), 1F, "event.mine_hit");
            case MINE_DUD -> announce(SoundEvents.FIRE_EXTINGUISH, 1F, "event.mine_dud");
            case REFUSED -> announce(SoundEvents.VILLAGER_NO, 1F, "event.refused");
            case CONFIRM_REQUIRED -> {
                confirmPending = true;
                announce(SoundEvents.NOTE_BLOCK_BASS.value(), 1F, "event.confirm");
            }
            default -> {
            }
        }
        updateWidgets();
    }

    private void announce(SoundEvent sound, float pitch, String key) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch));
        message = Component.translatable(KEY_PREFIX + key);
        messageTicks = MESSAGE_TICKS;
    }

    private GrinderView view() {
        return menu.view();
    }

    private boolean running() {
        return view().phase() == GrinderView.Phase.RUNNING;
    }

    private void updateWidgets() {
        if (primaryButton == null)
            return;
        GrinderView view = view();
        boolean running = running();
        cancelButton.visible = !running;
        cancelButton.active = !running;
        primaryButton.setX(leftPos + (running ? GrinderLayout.RUN_PRIMARY_X : GrinderLayout.DRAFT_PRIMARY_X));
        primaryButton.setY(topPos + (running ? GrinderLayout.RUN_PRIMARY_Y : GrinderLayout.DRAFT_PRIMARY_Y));
        primaryButton.setWidth(running ? GrinderLayout.RUN_PRIMARY_WIDTH : GrinderLayout.DRAFT_PRIMARY_WIDTH);
        primaryButton.active = running || view.canSeason();
        primaryButton.setMessage(running
                ? Component.translatable(KEY_PREFIX + (confirmPending ? "accept_confirm" : "accept"))
                : Component.translatable(KEY_PREFIX + "season"));
        overflowTooltip(primaryButton);
        overflowTooltip(cancelButton);
        for (PadButton button : padButtons) {
            int steps = running ? view.stepsLeft().get(button.direction.ordinal()) : 0;
            button.steps = steps;
            button.visible = running;
            button.active = steps > 0;
        }
        lockInButton.visible = running;
        lockInButton.active = running && view.canLockIn();
        if (primaryButton.active && (running || !primaryWasActive) && getFocused() != primaryButton)
            setFocused(primaryButton);
        primaryWasActive = primaryButton.active;
    }

    /**
     * Gives a button a tooltip with its full label if the label doesn't fit into
     * it.
     */
    private void overflowTooltip(Button button) {
        button.setTooltip(font.width(button.getMessage()) > button.getWidth() - 4
                ? Tooltip.create(button.getMessage())
                : null);
    }

    /** Keeps the pad buttons from taking the focus away from the primary button. */
    @Override
    public void setFocused(GuiEventListener focused) {
        if (!(focused instanceof PadButton))
            super.setFocused(focused);
    }

    // #region Actions

    private void send(GrinderIntentPayload.Kind kind, int data, GrinderSpice spice) {
        Services.NETWORK.sendToServer(
                new GrinderIntentPayload(menu.containerId, kind, data, Optional.ofNullable(spice)));
    }

    private void onPrimary() {
        if (!running()) {
            send(GrinderIntentPayload.Kind.SEASON, 0, null);
            return;
        }
        boolean wouldWaste = view().effects().isEmpty();
        if (wouldWaste && !confirmPending) {
            confirmPending = true;
            message = Component.translatable(KEY_PREFIX + "event.confirm");
            messageTicks = MESSAGE_TICKS;
            updateWidgets();
            return;
        }
        send(wouldWaste ? GrinderIntentPayload.Kind.ACCEPT_CONFIRMED : GrinderIntentPayload.Kind.ACCEPT, 0, null);
    }

    /**
     * @param row    The clicked spice.
     * @param remove Whether to take it out instead of adding it.
     * @param batch  Whether to transfer up to {@link GrinderActions#BATCH_SIZE}
     *               at once instead of one.
     */
    private void clickSpice(Row row, boolean remove, boolean batch) {
        int amount = batch ? GrinderActions.BATCH_SIZE : 1;
        if (running()) {
            if (!remove && canAddMore(row.spice))
                send(GrinderIntentPayload.Kind.ADD_SPICE, amount, row.spice);
            else
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1F));
            return;
        }
        if (remove && view().spices().getOrDefault(row.spice, 0) > 0)
            send(GrinderIntentPayload.Kind.REMOVE_DRAFT_SPICE, amount, row.spice);
        else if (!remove && canAddMore(row.spice))
            send(GrinderIntentPayload.Kind.ADD_DRAFT_SPICE, amount, row.spice);
        else
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1F));
    }

    /**
     * What the client can tell without asking: the caps and the amount it holds.
     * The server decides.
     */
    private boolean canAddMore(GrinderSpice spice) {
        GrinderView view = view();
        if (view.food().isEmpty())
            return false;
        int chosen = view.spices().getOrDefault(spice, 0);
        Map<GrinderSpice, Integer> next = new LinkedHashMap<>(view.spices());
        next.merge(spice, 1, Integer::sum);
        int foods = view.food().getCount();
        int needed = running() ? foods : foods * (chosen + 1);
        return withinCaps(GrinderSpice.expand(next)) && held(spice) >= needed;
    }

    /**
     * @param spices How many of each Spice Item per food.
     * @return Whether that fits under the caps the server sent.
     */
    private boolean withinCaps(Map<Item, Integer> spices) {
        int total = 0;
        for (int count : spices.values()) {
            if (count > view().maxPerKind())
                return false;
            total += count;
        }
        return total <= view().maxTotal();
    }

    /**
     * How many the server says the player's inventory and the nearby spice storage
     * hold together.
     */
    private int held(GrinderSpice spice) {
        return view().available().getOrDefault(spice, 0);
    }

    // #region Spice list

    private void rebuildRows() {
        List<Map.Entry<GrinderSpice, Integer>> held = new ArrayList<>(view().available().entrySet());
        held.sort(Comparator.comparing((Map.Entry<GrinderSpice, Integer> entry) -> sortKey(entry.getKey())));

        rows.clear();
        addGroup("group.raw", held, spice -> spice.mix().isEmpty()
                && !spice.item().getDefaultInstance().is(SpiceItemTags.PROCESSED_SPICES)
                && spice.item().getDefaultInstance().is(SpiceItemTags.SPICES));
        addGroup("group.processed", held, spice -> spice.mix().isEmpty()
                && spice.item().getDefaultInstance().is(SpiceItemTags.PROCESSED_SPICES));
        addGroup("group.mixes", held, spice -> spice.mix().isPresent());
        addGroup("group.other", held, spice -> spice.mix().isEmpty()
                && !spice.item().getDefaultInstance().is(SpiceItemTags.SPICES)
                && !spice.item().getDefaultInstance().is(SpiceItemTags.PROCESSED_SPICES));
        scroll = Mth.clamp(scroll, 0, Math.max(0, rows.size() - GrinderLayout.LIST_ROWS));
    }

    /** Loose spices sort by item ID; mixes by name, then by what they hold. */
    private static String sortKey(GrinderSpice spice) {
        String id = BuiltInRegistries.ITEM.getKey(spice.item()).toString();
        return spice.mix().map(mix -> id + "|" + spice.displayStack().getHoverName().getString() + "|"
                + mix.spices().entrySet().stream()
                        .map(entry -> BuiltInRegistries.ITEM.getKey(entry.getKey()) + "*" + entry.getValue())
                        .toList())
                .orElse(id);
    }

    private void addGroup(String key, List<Map.Entry<GrinderSpice, Integer>> held,
            java.util.function.Predicate<GrinderSpice> filter) {
        List<Row> group = new ArrayList<>();
        held.forEach(entry -> {
            if (filter.test(entry.getKey()))
                group.add(new Row(null, entry.getKey(), entry.getValue()));
        });
        if (group.isEmpty())
            return;
        rows.add(new Row(Component.translatable(KEY_PREFIX + key), null, 0));
        rows.addAll(group);
    }

    private Row rowAt(double mouseX, double mouseY) {
        int localX = (int) mouseX - leftPos - GrinderLayout.LIST_X;
        int localY = (int) mouseY - topPos - GrinderLayout.LIST_Y - 2;
        if (localX < 0 || localX >= GrinderLayout.LIST_WIDTH - 8 || localY < 0)
            return null;
        int index = scroll + localY / GrinderLayout.LIST_ROW_HEIGHT;
        if (localY / GrinderLayout.LIST_ROW_HEIGHT >= GrinderLayout.LIST_ROWS || index >= rows.size())
            return null;
        Row row = rows.get(index);
        return row.spice == null ? null : row;
    }

    // #region Input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Row row = rowAt(mouseX, mouseY);
        if (row != null && (button == 0 || button == 1)) {
            clickSpice(row, button == 1, hasShiftDown());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= leftPos + GrinderLayout.LIST_X
                && mouseX < leftPos + GrinderLayout.LIST_X + GrinderLayout.LIST_WIDTH) {
            scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0,
                    Math.max(0, rows.size() - GrinderLayout.LIST_ROWS));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (running()) {
            int dx = 0;
            int dy = 0;
            var options = Minecraft.getInstance().options;
            if (options.keyUp.matches(keyCode, scanCode) || keyCode == InputConstants.KEY_UP)
                dy = -1;
            else if (options.keyDown.matches(keyCode, scanCode) || keyCode == InputConstants.KEY_DOWN)
                dy = 1;
            else if (options.keyLeft.matches(keyCode, scanCode) || keyCode == InputConstants.KEY_LEFT)
                dx = -1;
            else if (options.keyRight.matches(keyCode, scanCode) || keyCode == InputConstants.KEY_RIGHT)
                dx = 1;
            if (dx != 0 || dy != 0) {
                if (!buffering) {
                    buffering = true;
                    bufferStartMs = Util.getMillis();
                    bufferedDx = 0;
                    bufferedDy = 0;
                }
                bufferedDx = dx != 0 ? dx : bufferedDx;
                bufferedDy = dy != 0 ? dy : bufferedDy;
                return true;
            }
            if (options.keyJump.matches(keyCode, scanCode)) {
                send(GrinderIntentPayload.Kind.LOCK_IN, 0, null);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (messageTicks > 0)
            messageTicks--;
        if (buffering && Util.getMillis() - bufferStartMs >= Services.CONFIG.getGrinderInputBufferMs()) {
            buffering = false;
            for (Direction direction : Direction.values()) {
                if (direction.dx() == bufferedDx && direction.dy() == bufferedDy)
                    send(GrinderIntentPayload.Kind.MOVE, direction.ordinal(), null);
            }
        }
        updateWidgets();
    }

    // #region Rendering

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        clipped.clear();
        rebuildRows();
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        for (Slot slot : menu.slots) {
            if (menu.isLocked(slot))
                graphics.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16,
                        0x99000000);
        }
        if (showsLockedFood())
            graphics.fill(leftPos + GrinderLayout.FOOD_SLOT_X, topPos + GrinderLayout.FOOD_SLOT_Y,
                    leftPos + GrinderLayout.FOOD_SLOT_X + 16, topPos + GrinderLayout.FOOD_SLOT_Y + 16, 0x99000000);
        renderTooltips(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        blitNineSliced(graphics, BACKGROUND, leftPos, topPos, imageWidth, imageHeight);
        panel(graphics, leftPos + GrinderLayout.LIST_X, topPos + GrinderLayout.LIST_Y, GrinderLayout.LIST_WIDTH,
                GrinderLayout.LIST_HEIGHT, COLOR_PANEL);
        panel(graphics, leftPos + GrinderLayout.PANEL_X, topPos + GrinderLayout.PANEL_Y, GrinderLayout.PANEL_WIDTH,
                GrinderLayout.PANEL_HEIGHT, COLOR_PANEL);
        graphics.fill(leftPos + GrinderLayout.FOOD_SLOT_X - 1, topPos + GrinderLayout.FOOD_SLOT_Y - 1,
                leftPos + GrinderLayout.FOOD_SLOT_X + 17, topPos + GrinderLayout.FOOD_SLOT_Y + 17, 0xFF555555);
        if (showsLockedFood()) {
            ItemStack food = view().food();
            graphics.renderItem(food, leftPos + GrinderLayout.FOOD_SLOT_X, topPos + GrinderLayout.FOOD_SLOT_Y);
            graphics.renderItemDecorations(font, food, leftPos + GrinderLayout.FOOD_SLOT_X,
                    topPos + GrinderLayout.FOOD_SLOT_Y);
        }
        for (int i = 0; i < 9; i++) {
            for (int row = 0; row < 3; row++)
                slotFrame(graphics, GrinderLayout.INVENTORY_X + i * 18, GrinderLayout.INVENTORY_Y + row * 18);
            slotFrame(graphics, GrinderLayout.INVENTORY_X + i * 18, GrinderLayout.HOTBAR_Y);
        }
        renderSpiceList(graphics, mouseX, mouseY);
        if (running())
            renderRun(graphics);
        else
            renderDraft(graphics);
        if (messageTicks > 0) {
            drawClipped(graphics, message, leftPos + GrinderLayout.MESSAGE_X, topPos + GrinderLayout.MESSAGE_Y,
                    GrinderLayout.MESSAGE_WIDTH, COLOR_WARNING);
        }
    }

    /**
     * Draws text without shadow, cut short with an ellipsis if wider than
     * {@code maxWidth}; hovering the cut text then shows it in full.
     */
    private void drawClipped(GuiGraphics graphics, Component text, int x, int y, int maxWidth, int color) {
        if (font.width(text) <= maxWidth) {
            graphics.drawString(font, text, x, y, color, false);
            return;
        }
        FormattedText cut = FormattedText.composite(
                font.substrByWidth(text, Math.max(0, maxWidth - font.width(CommonComponents.ELLIPSIS))),
                CommonComponents.ELLIPSIS);
        graphics.drawString(font, Language.getInstance().getVisualOrder(cut), x, y, color, false);
        clipped.add(new Clipped(x, y, maxWidth, text));
    }

    /**
     * @return Whether the food slot shows the food being seasoned, locked: only
     *         during a run, when the real slot is hidden.
     */
    private boolean showsLockedFood() {
        return running() && !view().food().isEmpty();
    }

    private void slotFrame(GuiGraphics graphics, int x, int y) {
        graphics.fill(leftPos + x - 1, topPos + y - 1, leftPos + x + 17, topPos + y + 17, 0xFF555555);
        graphics.fill(leftPos + x, topPos + y, leftPos + x + 16, topPos + y + 16, 0xFF8B8B8B);
    }

    private static void panel(GuiGraphics graphics, int x, int y, int width, int height, int fill) {
        graphics.fill(x, y, x + width, y + height, COLOR_PANEL_BORDER);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
    }

    private void renderSpiceList(GuiGraphics graphics, int mouseX, int mouseY) {
        int baseX = leftPos + GrinderLayout.LIST_X + 2;
        int baseY = topPos + GrinderLayout.LIST_Y + 2;
        Row hovered = rowAt(mouseX, mouseY);
        if (rows.isEmpty()) {
            graphics.drawWordWrap(font, Component.translatable(KEY_PREFIX + "no_spices"), baseX + 2, baseY + 2,
                    GrinderLayout.LIST_WIDTH - 12, COLOR_DIM);
            return;
        }
        for (int i = 0; i < GrinderLayout.LIST_ROWS && scroll + i < rows.size(); i++) {
            Row row = rows.get(scroll + i);
            int y = baseY + i * GrinderLayout.LIST_ROW_HEIGHT;
            if (row.spice == null) {
                drawClipped(graphics, row.header, baseX, y + 2, GrinderLayout.LIST_WIDTH - 8, COLOR_DIM);
                continue;
            }
            if (row == hovered)
                graphics.fill(baseX - 1, y, baseX + GrinderLayout.LIST_WIDTH - 9, y + GrinderLayout.LIST_ROW_HEIGHT,
                        0xFF454545);
            int chosen = view().spices().getOrDefault(row.spice, 0);
            ItemStack stack = row.spice.displayStack();
            graphics.renderItem(stack, baseX - 1, y - 2 + 0);
            String amount = (chosen > 0 ? chosen + "/" : "") + row.count;
            int amountX = baseX + GrinderLayout.LIST_WIDTH - 12 - font.width(amount);
            drawClipped(graphics, stack.getHoverName(), baseX + GrinderLayout.LIST_NAME_X, y + 2,
                    amountX - 3 - (baseX + GrinderLayout.LIST_NAME_X), canAddMore(row.spice) ? COLOR_TEXT : COLOR_DIM);
            graphics.drawString(font, amount, amountX, y + 2, chosen > 0 ? COLOR_WARNING : COLOR_DIM, false);
        }
    }

    private void renderDraft(GuiGraphics graphics) {
        GrinderView view = view();
        int total = GrinderSpice.expand(view.spices()).values().stream().mapToInt(Integer::intValue).sum();
        int labelWidth = GrinderLayout.FOOD_SLOT_X - GrinderLayout.PANEL_X - 8;
        drawClipped(graphics, Component.translatable(KEY_PREFIX + "draft", total, view.maxTotal()),
                leftPos + GrinderLayout.PANEL_X + 4, topPos + GrinderLayout.PANEL_Y + 5, labelWidth, COLOR_TEXT);
        if (view.food().isEmpty()) {
            drawClipped(graphics, Component.translatable(KEY_PREFIX + "no_food"),
                    leftPos + GrinderLayout.PANEL_X + 4, topPos + GrinderLayout.PANEL_Y + 16, labelWidth, COLOR_DIM);
        }
        double scale = Math.max(1D, view.points().stream().mapToDouble(Double::doubleValue).max().orElse(1D));
        for (FlavorAxis axis : FlavorAxis.values()) {
            int y = topPos + GrinderLayout.DRAFT_ROWS_Y + axis.ordinal() * GrinderLayout.DRAFT_ROW_HEIGHT;
            int pole = view.poles().get(axis.ordinal());
            double points = view.points().get(axis.ordinal());
            int axisColor = pole == 0 ? COLOR_DIM : 0xFF000000 | axis.getColor(pole > 0);
            drawClipped(graphics, poleName(axis, pole), leftPos + GrinderLayout.DRAFT_ROWS_X, y + 1, 56, axisColor);
            int barX = leftPos + GrinderLayout.DRAFT_ROWS_X + 60;
            int barWidth = GrinderLayout.DRAFT_ROW_WIDTH - 60 - 24;
            graphics.fill(barX, y + 2, barX + barWidth, y + 8, 0xFF1A1A1A);
            if (points > 0D)
                graphics.fill(barX, y + 2, barX + Math.max(1, (int) (barWidth * Math.min(1D, points / scale))), y + 8,
                        0xFF000000 | axis.getColor(pole > 0));
            graphics.drawString(font, String.format("%.1f", points), barX + barWidth + 3, y + 1, axisColor, false);
        }
    }

    private Component poleName(FlavorAxis axis, int pole) {
        return Component.translatable(pole < 0 ? axis.negativeTranslationKey() : axis.positiveTranslationKey());
    }

    private void renderRun(GuiGraphics graphics) {
        GrinderView view = view();
        for (FlavorAxis axis : FlavorAxis.values()) {
            int y = topPos + GrinderLayout.POINTS_Y + axis.ordinal() * GrinderLayout.POINTS_ROW_HEIGHT;
            int x = leftPos + GrinderLayout.POINTS_X;
            int pole = view.poles().get(axis.ordinal());
            double points = view.points().get(axis.ordinal());
            graphics.fill(x, y + 1, x + GrinderLayout.POINTS_WIDTH, y + 6, 0xFF1A1A1A);
            double scale = Math.max(1D, Services.CONFIG.getFlavorSoftCap());
            if (points > 0D)
                graphics.fill(x, y + 1,
                        x + Math.max(1, (int) (GrinderLayout.POINTS_WIDTH * Math.min(1D, points / scale))),
                        y + 6, 0xFF000000 | axis.getColor(pole > 0));
        }
        for (int cy = 0; cy < BoardGeometry.SIZE; cy++) {
            for (int cx = 0; cx < BoardGeometry.SIZE; cx++)
                renderCell(graphics, view, cx, cy);
        }
        int pawnX = leftPos + GrinderLayout.BOARD_X + view.x() * GrinderLayout.CELL;
        int pawnY = topPos + GrinderLayout.BOARD_Y + view.y() * GrinderLayout.CELL;
        blitSprite(graphics, PAWN, pawnX + GrinderLayout.CELL_SPRITE_OFFSET, pawnY + GrinderLayout.CELL_SPRITE_OFFSET);
        drawClipped(graphics, Component.translatable(KEY_PREFIX + "effects", view.effects().size()),
                leftPos + GrinderLayout.EFFECTS_X, topPos + GrinderLayout.EFFECTS_Y,
                GrinderLayout.PANEL_X + GrinderLayout.PANEL_WIDTH - 4 - GrinderLayout.EFFECTS_X, COLOR_TEXT);
    }

    private void renderCell(GuiGraphics graphics, GrinderView view, int cx, int cy) {
        int x = leftPos + GrinderLayout.BOARD_X + cx * GrinderLayout.CELL;
        int y = topPos + GrinderLayout.BOARD_Y + cy * GrinderLayout.CELL;
        FlavorAxis zone = BoardGeometry.zoneAxis(cx, cy);
        int tint = zone == null ? 0xFF303030
                : 0xFF000000 | blend(axisColor(zone, view), 0x303030, 0.22F);
        CellView cell = view.cell(cx, cy);
        int fill = switch (cell.kind()) {
            case WALL -> 0xFF8A8A8A;
            case UNKNOWN -> 0xFF5A3F8A;
            case REVEALED -> cell.lockedIn() ? 0xFF3F8A4B : 0xFF8A7A3F;
            case SPENT_MINE -> 0xFF8A3F3F;
            default -> tint;
        };
        graphics.fill(x, y, x + GrinderLayout.CELL, y + GrinderLayout.CELL, 0xFF1A1A1A);
        graphics.fill(x + 1, y + 1, x + GrinderLayout.CELL - 1, y + GrinderLayout.CELL - 1, fill);
        ResourceLocation sprite = switch (cell.kind()) {
            case WALL -> CELL_WALL;
            case UNKNOWN -> CELL_UNKNOWN;
            case SPENT_MINE -> CELL_MINE;
            case REVEALED -> isBane(cell) ? CELL_BANE : CELL_BOON;
            default -> null;
        };
        if (sprite != null)
            blitSprite(graphics, sprite, x + GrinderLayout.CELL_SPRITE_OFFSET, y + GrinderLayout.CELL_SPRITE_OFFSET);
    }

    /**
     * @return Whether any of the revealed cell's effects is harmful.
     */
    private static boolean isBane(CellView cell) {
        for (SeasoningEffect effect : cell.effects()) {
            var def = SeasoningEffectRegistry.get(effect.id());
            if (def.isPresent() && def.get().effect().value().getCategory() == MobEffectCategory.HARMFUL)
                return true;
        }
        return false;
    }

    private static int axisColor(FlavorAxis axis, GrinderView view) {
        return axis.getColor(view.poles().get(axis.ordinal()) >= 0);
    }

    private static int blend(int color, int base, float amount) {
        int r = (int) (((color >> 16) & 0xFF) * amount + ((base >> 16) & 0xFF) * (1 - amount));
        int g = (int) (((color >> 8) & 0xFF) * amount + ((base >> 8) & 0xFF) * (1 - amount));
        int b = (int) ((color & 0xFF) * amount + (base & 0xFF) * (1 - amount));
        return (r << 16) | (g << 8) | b;
    }

    // #region Tooltips

    private void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        Row row = rowAt(mouseX, mouseY);
        if (row != null) {
            ItemStack stack = row.spice.displayStack();
            List<Component> hints = new ArrayList<>();
            if (row.spice.mix().isPresent() && !withinCaps(row.spice.perUnit()))
                hints.add(Component.translatable(KEY_PREFIX + "mix_over_caps").withStyle(ChatFormatting.RED));
            hints.add(keyHint("spice_hint.add", Component.translatable(KEY_PREFIX + "click.left")));
            if (!running())
                hints.add(keyHint("spice_hint.remove", Component.translatable(KEY_PREFIX + "click.right")));
            hints.add(keyHint("spice_hint.batch", Component.translatable(TooltipUtil.SHIFT_KEY_TRANSLATION_KEY),
                    GrinderActions.BATCH_SIZE));
            List<Component> lines = TooltipUtil.withExpanded(hints,
                    () -> getTooltipFromItem(Minecraft.getInstance(), stack));
            graphics.renderTooltip(font, lines, stack.getTooltipImage(), mouseX, mouseY);
            return;
        }
        for (Clipped text : clipped) {
            if (mouseX >= text.x && mouseX < text.x + text.width && mouseY >= text.y && mouseY < text.y + 9) {
                graphics.renderTooltip(font, text.full, mouseX, mouseY);
                return;
            }
        }
        if (!running())
            return;
        if (showsLockedFood() && mouseX >= leftPos + GrinderLayout.FOOD_SLOT_X
                && mouseX < leftPos + GrinderLayout.FOOD_SLOT_X + 16 && mouseY >= topPos + GrinderLayout.FOOD_SLOT_Y
                && mouseY < topPos + GrinderLayout.FOOD_SLOT_Y + 16) {
            graphics.renderTooltip(font, view().food(), mouseX, mouseY);
            return;
        }
        for (PadButton button : padButtons) {
            if (hoversPad(button, mouseX, mouseY)) {
                graphics.renderTooltip(font, padTooltip(button), Optional.empty(), mouseX, mouseY);
                return;
            }
        }
        if (hoversPad(lockInButton, mouseX, mouseY)) {
            graphics.renderTooltip(font, padTooltip(lockInButton), Optional.empty(), mouseX, mouseY);
            return;
        }
        FlavorAxis bar = barAt(mouseX, mouseY);
        if (bar != null) {
            graphics.renderTooltip(font, barTooltip(bar), Optional.empty(), mouseX, mouseY);
            return;
        }
        int cellX = (mouseX - leftPos - GrinderLayout.BOARD_X);
        int cellY = (mouseY - topPos - GrinderLayout.BOARD_Y);
        if (cellX >= 0 && cellY >= 0 && cellX < GrinderLayout.CELL * BoardGeometry.SIZE
                && cellY < GrinderLayout.CELL * BoardGeometry.SIZE) {
            List<Component> lines = cellTooltip(view().cell(cellX / GrinderLayout.CELL, cellY / GrinderLayout.CELL));
            if (!lines.isEmpty())
                graphics.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
        }
        int effectsX = mouseX - leftPos - GrinderLayout.EFFECTS_X;
        int effectsY = mouseY - topPos - GrinderLayout.EFFECTS_Y;
        if (effectsX >= 0 && effectsX < 80 && effectsY >= 0 && effectsY < 9 && !view().effects().isEmpty())
            graphics.renderTooltip(font, effectLines(view().effects()), Optional.empty(), mouseX, mouseY);
    }

    /**
     * @return How many steps the points of the axis pay for, whatever stands in
     *         the way.
     */
    private int stepsLeft(FlavorAxis axis) {
        double stepCost = view().stepCost();
        return stepCost > 0D ? (int) Math.floor(view().points().get(axis.ordinal()) / stepCost + 1e-9) : 0;
    }

    /**
     * @return The axis whose points bar is at the mouse, or {@code null}.
     */
    private FlavorAxis barAt(int mouseX, int mouseY) {
        int localX = mouseX - leftPos - GrinderLayout.POINTS_X;
        int localY = mouseY - topPos - GrinderLayout.POINTS_Y;
        if (localX < 0 || localY < 0)
            return null;
        int index = localY / GrinderLayout.POINTS_ROW_HEIGHT;
        if (index >= FlavorAxis.values().length)
            return null;
        FlavorAxis axis = FlavorAxis.values()[index];
        return localX < GrinderLayout.POINTS_WIDTH ? axis : null;
    }

    /**
     * @return The tooltip of a points bar: the axis, its points and the steps
     *         they pay for.
     */
    private List<Component> barTooltip(FlavorAxis axis) {
        int pole = view().poles().get(axis.ordinal());
        int color = pole == 0 ? 0xFFFFFF : axis.getColor(pole > 0);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(KEY_PREFIX + "axis", Component.translatable(axis.positiveTranslationKey()),
                Component.translatable(axis.negativeTranslationKey())).withStyle(Style.EMPTY.withColor(color & 0xFFFFFF)));
        lines.add(Component.translatable(KEY_PREFIX + "bar.points", formatPoints(view().points().get(axis.ordinal())))
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(KEY_PREFIX + "bar.steps", stepsLeft(axis)).withStyle(ChatFormatting.GRAY));
        return lines;
    }

    /**
     * @param key   The key of a line below the spice's own tooltip, whose first
     *              argument is the name of the key or click.
     * @param name  The name of the key or click to show in it.
     * @param extra Further arguments of the line.
     * @return The line telling what a key or click on a spice does.
     */
    private static Component keyHint(String key, Component name, Object... extra) {
        Object[] args = new Object[extra.length + 1];
        args[0] = name.copy().withStyle(Style.EMPTY.withColor(0xDDDDDD));
        System.arraycopy(extra, 0, args, 1, extra.length);
        return Component.translatable(KEY_PREFIX + key, args).withStyle(ChatFormatting.GRAY);
    }

    /**
     * Unlike a widget's own hover, this also counts while the button is inactive,
     * so a button that can't be pressed still tells what it would cost.
     */
    private boolean hoversPad(PadButton button, int mouseX, int mouseY) {
        return button.visible && mouseX >= button.getX() && mouseX < button.getX() + button.getWidth()
                && mouseY >= button.getY() && mouseY < button.getY() + button.getHeight();
    }

    /**
     * @return The tooltip of a pad button: its name, then which points it costs
     *         from which axis, in that axis' color.
     */
    private List<Component> padTooltip(PadButton button) {
        GrinderView view = view();
        boolean lockIn = button.direction == null;
        FlavorAxis axis = lockIn ? BoardGeometry.zoneAxis(view.x(), view.y()) : button.direction.axis();
        double cost = lockIn ? view.lockInCost() : view.stepCost();
        List<Component> lines = new ArrayList<>();
        lines.add(button.getMessage());
        Component description = Component.translatable(KEY_PREFIX + (lockIn ? "lock_in.desc" : "move.desc"));
        if (axis != null && cost > 0D) {
            int pole = view.poles().get(axis.ordinal());
            int color = pole == 0 ? COLOR_DIM : axis.getColor(pole > 0);
            Component axisName = Component.translatable(KEY_PREFIX + "axis",
                    Component.translatable(axis.positiveTranslationKey()),
                    Component.translatable(axis.negativeTranslationKey()));
            lines.add(Component.empty()
                    .append(Component.translatable(KEY_PREFIX + "cost", formatPoints(cost), axisName)
                            .withStyle(Style.EMPTY.withColor(color & 0xFFFFFF)))
                    .append(Component.literal(" ").append(description).withStyle(ChatFormatting.GRAY)));
        } else {
            lines.add(description.copy().withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    /**
     * @return The points with at least one and at most two decimals.
     */
    private static String formatPoints(double points) {
        String text = String.format(java.util.Locale.ROOT, "%.2f", points);
        return text.endsWith("0") ? text.substring(0, text.length() - 1) : text;
    }

    private List<Component> cellTooltip(CellView cell) {
        List<Component> lines = new ArrayList<>();
        switch (cell.kind()) {
            case UNKNOWN -> {
                lines.add(Component.translatable(KEY_PREFIX + "cell.unknown"));
                lines.add(Component.translatable(KEY_PREFIX + (cell.effectCount() == 1 ? "cell.effect_count.one" : "cell.effect_count"),
                        cell.effectCount())
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
                if (cell.boons() >= 0) {
                    int unknown = cell.randoms();
                    lines.add(Component.translatable(KEY_PREFIX + "cell.hint", cell.boons(), cell.banes(), unknown)
                            .withStyle(net.minecraft.ChatFormatting.GRAY));
                }
            }
            case REVEALED -> {
                boolean bane = isBane(cell);
                lines.add(Component.translatable(KEY_PREFIX + (bane ? "cell.bane" : "cell.boon"))
                        .withStyle(bane ? ChatFormatting.RED : ChatFormatting.GREEN));
                lines.addAll(effectLines(cell.effects()));
            }
            case WALL -> {
                lines.add(Component.translatable(KEY_PREFIX + "cell.wall"));
                lines.add(Component.translatable(KEY_PREFIX + "cell.wall.desc").withStyle(ChatFormatting.GRAY));
            }
            case SPENT_MINE -> {
                lines.add(Component.translatable(KEY_PREFIX + "cell.mine"));
                lines.add(Component.translatable(KEY_PREFIX + "cell.mine.desc").withStyle(ChatFormatting.GRAY));
            }
            default -> {
            }
        }
        return lines;
    }

    private static List<Component> effectLines(List<SeasoningEffect> effects) {
        List<Component> lines = new ArrayList<>();
        double multiplier = Services.CONFIG.getSeasoningEffectDurationMultiplier();
        for (SeasoningEffect effect : effects) {
            SeasoningEffectRegistry.get(effect.id()).ifPresent(def -> {
                MutableComponent line = def.effect().value().getDisplayName().copy();
                if (effect.level() > 1)
                    line = Component.translatable("potion.withAmplifier", line,
                            Component.translatable("potion.potency." + (effect.level() - 1)));
                int ticks = com.drunkencod.spice_road.spice.effect.SeasoningEffects.durationTicks(def, effect.level(),
                        multiplier);
                lines.add(line.append(" (" + net.minecraft.util.StringUtil.formatTickDuration(ticks, 20F) + ")")
                        .withStyle(def.effect().value().getCategory().getTooltipFormatting()));
            });
        }
        return lines;
    }

    // #region Pad button

    /**
     * A direction or lock-in button drawn as a flat square with a sprite on top:
     * yellow with one step left, gray without any. It is never focusable.
     */
    private static final class PadButton extends Button {

        private final Direction direction;
        private final ResourceLocation icon;
        private int steps;

        private PadButton(Direction direction, int x, int y, Runnable action) {
            super(x, y, GrinderLayout.PAD_BUTTON, GrinderLayout.PAD_BUTTON, CommonComponents.EMPTY,
                    button -> action.run(), DEFAULT_NARRATION);
            this.direction = direction;
            this.icon = direction == null ? BUTTON_LOCK_IN
                    : sprite("buttons/" + direction.name().toLowerCase(java.util.Locale.ROOT));
            setMessage(direction == null ? Component.translatable(KEY_PREFIX + "lock_in")
                    : Component
                            .translatable(KEY_PREFIX + "move." + direction.name().toLowerCase(java.util.Locale.ROOT)));
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int color;
            if (!active)
                color = 0xFF3A3A3A;
            else if (direction == null)
                color = isHoveredOrFocused() ? 0xFF6FD06F : 0xFF3F8A4B;
            else if (steps == 1)
                color = isHoveredOrFocused() ? 0xFFFFE070 : 0xFFD9B030;
            else
                color = isHoveredOrFocused() ? 0xFFB0B0B0 : 0xFF808080;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF111111);
            graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, color);
            int offset = (width - GrinderLayout.SPRITE_SIZE) / 2;
            graphics.setColor(1F, 1F, 1F, active ? 1F : 0.4F);
            blitSprite(graphics, icon, getX() + offset, getY() + offset);
            graphics.setColor(1F, 1F, 1F, 1F);
        }

        @Override
        public ComponentPath nextFocusPath(FocusNavigationEvent event) {
            return null;
        }
    }
}
