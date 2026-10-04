# Asset BOM

Assets still to be made by hand. Placeholders exist where datagen or the game needs a file.

## Mob effect icons (18x18, `assets/spice_road/textures/mob_effect/`)
- [ ] `hot.png` - icon of the Hot effect (fiery pole of the heat axis). Placeholder generated.
- [ ] `chilled.png` - icon of the Chilled effect (cooling pole of the heat axis). Placeholder generated.

## Spice Grinder (item and GUI)
- [x] `assets/spice_road/textures/item/spice_grinder.png` - 16x16 item icon of the Spice Grinder. Placeholder generated.
- [ ] GUI background texture for the Spice Grinder screen. The screen is drawn with plain fills until this exists.
- [x] `textures/gui/spice_grinder/cells/{wall,unknown,mine,boon,bane}.png` - 7x7 board cell sprites (drawn centered-ish on a 10x10 cell; revealed cells show bane if any effect is harmful, else boon).
- [ ] `textures/gui/spice_grinder/buttons/{up,down,left,right,up_left,up_right,down_left,down_right,lock_in}.png` - 7x7 sprites for the 8 direction buttons and the lock-in button, on an 11x11 button. Placeholders (letters) generated. The button color (yellow = 1 step left, gray = none, hover) is still drawn by code; disabled buttons dim the sprite.
- [ ] Sprites for the board: bare cell and the pawn (currently plain fills).
- [ ] Sounds: grinding (lock-in), mine explosion, refused. Vanilla sounds are used as stand-ins (grindstone, explode, villager no).
