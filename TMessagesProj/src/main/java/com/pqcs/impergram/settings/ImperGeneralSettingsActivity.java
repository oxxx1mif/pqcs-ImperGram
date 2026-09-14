/*
 * This is the source code of Impergram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Gleb Obitotsky <gleb.obitotsky@gmail.com>, 2026.
 */

package com.pqcs.impergram.settings;

import android.view.View;

import com.pqcs.impergram.ImperConfig;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;

public class ImperGeneralSettingsActivity extends BaseNekoSettingsActivity {
    private final int blurPhoneRow = rowId++;
    private final int blurIdRow = rowId++;
    private final int blurUsernameRow = rowId++;

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(LocaleController.getString(R.string.General)));

        items.add(UItem.asCheck(blurPhoneRow, LocaleController.getString(R.string.BlurPhone)).slug("blurPhone").setChecked(ImperConfig.blurPhone));
        items.add(UItem.asCheck(blurIdRow, LocaleController.getString(R.string.BlurId)).slug("blurID").setChecked(ImperConfig.blurID));
        items.add(UItem.asCheck(blurUsernameRow, LocaleController.getString(R.string.BlurUsername)).slug("blurUsername").setChecked(ImperConfig.blurUsername));

        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        int id = item.id;

        if (id == blurPhoneRow) {
            ImperConfig.toggleBlurPhone();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ImperConfig.blurPhone);
            }
            showRestartBulletin();
        } else if (id == blurIdRow) {
            ImperConfig.toggleBlurID();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ImperConfig.blurID);
            }
            showRestartBulletin();
        } else if (id == blurUsernameRow) {
            ImperConfig.toggleBlurUsername();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ImperConfig.blurUsername);
            }
            showRestartBulletin();
        }
    }

    @Override
    protected String getActionBarTitle() {
        return LocaleController.getString(R.string.General);
    }

    @Override
    protected String getKey() {
        return "imper_general";
    }
}
