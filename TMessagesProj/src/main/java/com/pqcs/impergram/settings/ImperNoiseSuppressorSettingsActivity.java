/*
 * This is the source code of Impergram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Gleb Obitotsky <gleb.obitotsky@gmail.com>, 2026.
 */

package com.pqcs.impergram.settings;

import android.view.View;

//import com.pqcs.impergram.ImperConfig;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
//import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;

public class ImperNoiseSuppressorSettingsActivity extends BaseNekoSettingsActivity {

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(LocaleController.getString(R.string.Experiment)));

        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        //int id = item.id;
    }

    @Override
    protected String getActionBarTitle() {
        return LocaleController.getString(R.string.NoiseCancellationSettings);
    }

    @Override
    protected String getKey() {
        return "imper_noise_settings";
    }
}