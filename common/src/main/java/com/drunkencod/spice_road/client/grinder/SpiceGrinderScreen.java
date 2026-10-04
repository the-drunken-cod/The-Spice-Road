package com.drunkencod.spice_road.client.grinder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.grinder.GrinderIntentPayload;
import com.drunkencod.spice_road.grinder.GrinderLayout;
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

/**
 * The Spice Grinder GUI. Everything it shows comes from the latest
 * {@link GrinderView} the server sent, and everything the player does is sent
 * back as a {@link GrinderIntentPayload}; the screen never decides anything.
 * It is drawn with plain fills and text for now (see {@link GrinderLayout} for
 * the pixel layout), and every control is a vanilla widget, so the whole
 * thing can be operated with Tab, Enter and the keyboard shortcuts below.
 * <p>
 * Keyboard: the player's own movement keys and the arrow keys step the pawn
 * (two pressed within the buffer window make a diagonal step, one step per
 * press); Jump or Enter locks in.
 */
public class SpiceGrinderScreen extends AbstractContainerScreen<SpiceGrinderMenu> {

    private static final String KEY_PREFIX = Constants.MOD_ID + ".gui.grinder.";
    private static final int COLOR_PANEL = 0xFF2B2B2B;
    private static final int COLOR_PANEL_BORDER = 0xFF111111;
    private static final int COLOR_TEXT = 0xFFE0E0E0;
    private static final int COLOR_DIM = 0xFF808080;
    private static final int COLOR_WARNING = 0xFFFFD040;
    private static final int MESSAGE_TICKS = 60;

    private final List<Row> rows = new ArrayList<>();
    private final List<PadButton> padButtons = new ArrayList<>();
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

    /**
     * One line of the spice list: a group header, or a spice item with the amount
     * the player holds.
     */
    private record Row(Component header, Item item, int count) {
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
        lockInButton = new PadButton(null, leftPos + GrinderLayout.PAD_X + GrinderLayout.PAD_BUTTON,
                topPos + GrinderLayout.PAD_Y + GrinderLayout.PAD_BUTTON,
                () -> send(GrinderIntentPayload.Kind.LOCK_IN, 0, null));
        addRenderableWidget(lockInButton);

        primaryButton = addRenderableWidget(Button.builder(CommonComponents.EMPTY, button -> onPrimary())
                .bounds(leftPos + GrinderLayout.PRIMARY_X, topPos + GrinderLayout.PRIMARY_Y,
                        GrinderLayout.PRIMARY_WIDTH, GrinderLayout.BUTTON_HEIGHT)
                .build());
        cancelButton = addRenderableWidget(Button.builder(Component.translatable(KEY_PREFIX + "cancel"),
                button -> onClose())
                .bounds(leftPos + GrinderLayout.CANCEL_X, topPos + GrinderLayout.CANCEL_Y,
                        GrinderLayout.CANCEL_WIDTH, GrinderLayout.BUTTON_HEIGHT)
                .build());
        updateWidgets();
    }

    private int padX(Direction direction) {
        return leftPos + GrinderLayout.PAD_X + (direction.dx() + 1) * GrinderLayout.PAD_BUTTON;
    }

    private int padY(Direction direction) {
        return topPos + GrinderLayout.PAD_Y + (direction.dy() + 1) * GrinderLayout.PAD_BUTTON;
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
        primaryButton.active = running || view.canSeason();
        primaryButton.setMessage(running
                ? Component.translatable(KEY_PREFIX + (confirmPending ? "accept_confirm" : "accept"))
                : Component.translatable(KEY_PREFIX + "season"));
        for (PadButton button : padButtons) {
            int steps = running ? view.stepsLeft().get(button.direction.ordinal()) : 0;
            button.steps = steps;
            button.visible = running;
            button.active = steps > 0;
        }
        lockInButton.visible = running;
        lockInButton.active = running && view.canLockIn();
    }

    // #region Actions

    private void send(GrinderIntentPayload.Kind kind, int data, Item item) {
        Optional<net.minecraft.resources.ResourceLocation> id = item == null ? Optional.empty()
                : Optional.of(BuiltInRegistries.ITEM.getKey(item));
        Services.NETWORK.sendToServer(new GrinderIntentPayload(menu.containerId, kind, data, id));
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

    private void clickSpice(Row row, boolean remove) {
        if (running()) {
            if (!remove && canAddMore(row.item))
                send(GrinderIntentPayload.Kind.ADD_SPICE, 0, row.item);
            else
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1F));
            return;
        }
        if (remove && view().spices().getOrDefault(row.item, 0) > 0)
            send(GrinderIntentPayload.Kind.REMOVE_DRAFT_SPICE, 0, row.item);
        else if (!remove && canAddMore(row.item))
            send(GrinderIntentPayload.Kind.ADD_DRAFT_SPICE, 0, row.item);
        else
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1F));
    }

    /**
     * What the client can tell without asking: the caps and the amount it holds.
     * The server decides.
     */
    private boolean canAddMore(Item item) {
        GrinderView view = view();
        int chosen = view.spices().getOrDefault(item, 0);
        int total = view.spices().values().stream().mapToInt(Integer::intValue).sum();
        if (view.food().isEmpty())
            return false;
        int foods = view.food().getCount();
        int needed = running() ? foods : foods * (chosen + 1);
        return chosen < view.maxPerKind() && total < view.maxTotal() && held(item) >= needed;
    }

    /**
     * How many the server says the player's inventory and the nearby spice storage
     * hold together.
     */
    private int held(Item item) {
        return view().available().getOrDefault(item, 0);
    }

    // #region Spice list

    private void rebuildRows() {
        Map<Item, Integer> held = new TreeMap<>(Comparator.comparing(
                (Item item) -> BuiltInRegistries.ITEM.getKey(item).toString()));
        held.putAll(view().available());

        rows.clear();
        addGroup("group.raw", held, item -> !item.getDefaultInstance().is(SpiceItemTags.PROCESSED_SPICES)
                && item.getDefaultInstance().is(SpiceItemTags.SPICES));
        addGroup("group.processed", held, item -> item.getDefaultInstance().is(SpiceItemTags.PROCESSED_SPICES));
        addGroup("group.other", held, item -> !item.getDefaultInstance().is(SpiceItemTags.SPICES)
                && !item.getDefaultInstance().is(SpiceItemTags.PROCESSED_SPICES));
        scroll = Mth.clamp(scroll, 0, Math.max(0, rows.size() - GrinderLayout.LIST_ROWS));
    }

    private void addGroup(String key, Map<Item, Integer> held, java.util.function.Predicate<Item> filter) {
        List<Row> group = new ArrayList<>();
        held.forEach((item, count) -> {
            if (filter.test(item))
                group.add(new Row(null, item, count));
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
        return row.item == null ? null : row;
    }

    // #region Input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Row row = rowAt(mouseX, mouseY);
        if (row != null && (button == 0 || button == 1)) {
            clickSpice(row, button == 1);
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
        if (running() && getFocused() == null) {
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
            if (options.keyJump.matches(keyCode, scanCode) || keyCode == InputConstants.KEY_RETURN
                    || keyCode == InputConstants.KEY_NUMPADENTER) {
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
        rebuildRows();
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        for (Slot slot : menu.slots) {
            if (menu.isLocked(slot))
                graphics.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16,
                        0x99000000);
        }
        renderTooltips(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        panel(graphics, leftPos, topPos, imageWidth, imageHeight, 0xFF3C3C3C);
        panel(graphics, leftPos + GrinderLayout.LIST_X, topPos + GrinderLayout.LIST_Y, GrinderLayout.LIST_WIDTH,
                GrinderLayout.LIST_HEIGHT, COLOR_PANEL);
        panel(graphics, leftPos + GrinderLayout.PANEL_X, topPos + GrinderLayout.PANEL_Y, GrinderLayout.PANEL_WIDTH,
                GrinderLayout.PANEL_HEIGHT, COLOR_PANEL);
        graphics.fill(leftPos + GrinderLayout.FOOD_SLOT_X - 1, topPos + GrinderLayout.FOOD_SLOT_Y - 1,
                leftPos + GrinderLayout.FOOD_SLOT_X + 17, topPos + GrinderLayout.FOOD_SLOT_Y + 17, 0xFF555555);
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
            graphics.drawString(font, message, leftPos + GrinderLayout.PANEL_X + 4,
                    topPos + GrinderLayout.PANEL_Y + GrinderLayout.PANEL_HEIGHT - 2 - 9 - 12, COLOR_WARNING, false);
        }
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
        for (int i = 0; i < GrinderLayout.LIST_ROWS && scroll + i < rows.size(); i++) {
            Row row = rows.get(scroll + i);
            int y = baseY + i * GrinderLayout.LIST_ROW_HEIGHT;
            if (row.item == null) {
                graphics.drawString(font, row.header, baseX, y + 2, COLOR_DIM, false);
                continue;
            }
            if (row == hovered)
                graphics.fill(baseX - 1, y, baseX + GrinderLayout.LIST_WIDTH - 9, y + GrinderLayout.LIST_ROW_HEIGHT,
                        0xFF454545);
            int chosen = view().spices().getOrDefault(row.item, 0);
            graphics.renderItem(row.item.getDefaultInstance(), baseX - 1, y - 2 + 0);
            String name = font.plainSubstrByWidth(row.item.getDescription().getString(), 56);
            graphics.drawString(font, name, baseX + 8, y + 2, canAddMore(row.item) ? COLOR_TEXT : COLOR_DIM, false);
            String amount = (chosen > 0 ? chosen + "/" : "") + row.count;
            graphics.drawString(font, amount, baseX + GrinderLayout.LIST_WIDTH - 12 - font.width(amount), y + 2,
                    chosen > 0 ? COLOR_WARNING : COLOR_DIM, false);
        }
    }

    private void renderDraft(GuiGraphics graphics) {
        GrinderView view = view();
        int total = view.spices().values().stream().mapToInt(Integer::intValue).sum();
        graphics.drawString(font, Component.translatable(KEY_PREFIX + "draft", total, view.maxTotal()),
                leftPos + GrinderLayout.PANEL_X + 4, topPos + GrinderLayout.PANEL_Y + 5, COLOR_TEXT, false);
        if (view.food().isEmpty()) {
            graphics.drawString(font, Component.translatable(KEY_PREFIX + "no_food"),
                    leftPos + GrinderLayout.PANEL_X + 4, topPos + GrinderLayout.PANEL_Y + 16, COLOR_DIM, false);
        }
        double scale = Math.max(1D, view.points().stream().mapToDouble(Double::doubleValue).max().orElse(1D));
        for (FlavorAxis axis : FlavorAxis.values()) {
            int y = topPos + GrinderLayout.DRAFT_ROWS_Y + axis.ordinal() * GrinderLayout.DRAFT_ROW_HEIGHT;
            int pole = view.poles().get(axis.ordinal());
            double points = view.points().get(axis.ordinal());
            graphics.drawString(font, poleName(axis, pole), leftPos + GrinderLayout.DRAFT_ROWS_X, y + 1,
                    pole == 0 ? COLOR_DIM : 0xFF000000 | axis.getColor(pole > 0), false);
            int barX = leftPos + GrinderLayout.DRAFT_ROWS_X + 60;
            int barWidth = GrinderLayout.DRAFT_ROW_WIDTH - 60 - 24;
            graphics.fill(barX, y + 2, barX + barWidth, y + 8, 0xFF1A1A1A);
            if (points > 0D)
                graphics.fill(barX, y + 2, barX + Math.max(1, (int) (barWidth * Math.min(1D, points / scale))), y + 8,
                        0xFF000000 | axis.getColor(pole > 0));
            graphics.drawString(font, String.format("%.1f", points), barX + barWidth + 3, y + 1, COLOR_TEXT, false);
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
        graphics.fill(pawnX + 2, pawnY + 2, pawnX + GrinderLayout.CELL - 2, pawnY + GrinderLayout.CELL - 2, 0xFFFFFFFF);
        graphics.drawString(font, Component.translatable(KEY_PREFIX + "effects", view.effects().size()),
                leftPos + GrinderLayout.EFFECTS_X, topPos + GrinderLayout.EFFECTS_Y, COLOR_TEXT, false);
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
        if (cell.kind() == CellView.Kind.UNKNOWN)
            graphics.drawString(font, "?", x + 3, y + 1, 0xFFFFFFFF, false);
        else if (cell.kind() == CellView.Kind.SPENT_MINE)
            graphics.drawString(font, "x", x + 3, y + 1, 0xFFFFFFFF, false);
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
            graphics.renderTooltip(font, row.item.getDefaultInstance(), mouseX, mouseY);
            return;
        }
        if (!running())
            return;
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

    private List<Component> cellTooltip(CellView cell) {
        List<Component> lines = new ArrayList<>();
        switch (cell.kind()) {
            case UNKNOWN -> {
                lines.add(Component.translatable(KEY_PREFIX + "cell.unknown"));
                lines.add(Component.translatable(KEY_PREFIX + "cell.effect_count", cell.effectCount())
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
                if (cell.boons() >= 0) {
                    int unknown = cell.randoms();
                    lines.add(Component.translatable(KEY_PREFIX + "cell.hint", cell.boons(), cell.banes(), unknown)
                            .withStyle(net.minecraft.ChatFormatting.GRAY));
                }
            }
            case REVEALED -> lines.addAll(effectLines(cell.effects()));
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
     * A direction or lock-in button drawn as a flat square: yellow with one step
     * left, gray without any.
     */
    private static final class PadButton extends Button {

        private final Direction direction;
        private int steps;

        private PadButton(Direction direction, int x, int y, Runnable action) {
            super(x, y, GrinderLayout.PAD_BUTTON, GrinderLayout.PAD_BUTTON, CommonComponents.EMPTY,
                    button -> action.run(), DEFAULT_NARRATION);
            this.direction = direction;
            setMessage(direction == null ? Component.translatable(KEY_PREFIX + "lock_in")
                    : Component
                            .translatable(KEY_PREFIX + "move." + direction.name().toLowerCase(java.util.Locale.ROOT)));
            setTooltip(net.minecraft.client.gui.components.Tooltip.create(getMessage()));
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
            String glyph = direction == null ? "+" : arrow(direction);
            graphics.drawCenteredString(Minecraft.getInstance().font, glyph, getX() + width / 2, getY() + 2,
                    active ? 0xFF000000 : 0xFF666666);
        }

        private static String arrow(Direction direction) {
            return switch (direction) {
                case UP_LEFT, DOWN_RIGHT -> "\\";
                case UP_RIGHT, DOWN_LEFT -> "/";
                case UP -> "^";
                case LEFT -> "<";
                case RIGHT -> ">";
                case DOWN -> "v";
            };
        }
    }
}
