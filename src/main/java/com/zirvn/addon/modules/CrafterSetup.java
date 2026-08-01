package com.zirvn.addon.modules;

import com.zirvn.addon.ZirAddon;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.systems.modules.Module;

public class CrafterSetup extends Module {
    public final boolean[] disabledSlots = new boolean[9];

    public CrafterSetup() {
        super(ZirAddon.CATEGORY, "crafter-setup", "Crafter slot pattern auto-apply.");
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WTable table = theme.table();

        for (int r = 0; r < 3; ++r) {
            for (int c = 0; c < 3; ++c) {
                int slotIndex = r * 3 + c;
                WCheckbox checkbox = (WCheckbox) table.add(theme.checkbox(this.disabledSlots[slotIndex])).widget();
                checkbox.action = () -> this.disabledSlots[slotIndex] = checkbox.checked;
            }
            table.row();
        }

        return table;
    }

    public boolean isSlotDisabled(int slot) {
        return slot >= 0 && slot < 9 && this.disabledSlots[slot];
    }
}

