/*
 * This is the source code of Impergram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Gleb Obitotsky <gleb.obitotsky@gmail.com>, 2026.
 */

package com.pqcs.impergram.settings;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.SettingsSearchCell;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.FragmentFloatingButton;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.ProfileActivity;

import java.util.ArrayList;
import java.util.Locale;

import me.vkryl.android.animator.BoolAnimator;
import me.vkryl.android.animator.FactorAnimator;
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import tw.nekomimi.nekogram.settings.NekoChatSettingsActivity;
import tw.nekomimi.nekogram.settings.NekoExperimentalSettingsActivity;

public class ImperSettingsActivity extends BaseNekoSettingsActivity implements FactorAnimator.Target {
    private static final int ANIMATOR_ID_SEARCH_PAGE_VISIBLE = 0;

    private final BoolAnimator animatorSearchPageVisible = new BoolAnimator(ANIMATOR_ID_SEARCH_PAGE_VISIBLE,
            this, CubicBezierInterpolator.EASE_OUT_QUINT, 350);

    private final int experimentRow = rowId++;
    private final int channelRow = rowId++;
    private final int sourceCodeRow = rowId++;

    private ActionBarMenuItem syncItem;
    private final ArrayList<ProfileActivity.SearchAdapter.SearchResult> searchArray = createSearchArray();
    private final ArrayList<CharSequence> resultNames = new ArrayList<>();
    private final ArrayList<ProfileActivity.SearchAdapter.SearchResult> searchResults = new ArrayList<>();
    private boolean searchWas;
    private Runnable searchRunnable;
    private String lastSearchString;

    private FrameLayout topView;

    @Override
    public View createView(Context context) {
        if (parentLayout != null && parentLayout.isRightLayout()) {
            actionBar.setBackButtonImage(R.drawable.ic_ab_close);
        }
        topView = new FrameLayout(context);

        var logoContainer = new FrameLayout(context);
        var logoView = new BackupImageView(context);

        logoView.setImageDrawable(AppCompatResources.getDrawable(context, R.mipmap.ic_launcher));
        logoContainer.addView(logoView, LayoutHelper.createFrame(90, 90, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 15, 0, 0));
        topView.addView(logoContainer, LayoutHelper.createFrame(120, 120, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 23 - 12, 0, 0));

        var titleView = new TextView(context);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setGravity(Gravity.CENTER);
        titleView.setSingleLine();
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        titleView.setText(LocaleController.getString(R.string.AppNameNeko));
        titleView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        topView.addView(titleView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 138.333f - 12, 0, 0));

        var subtitleView = new TextView(context);
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        subtitleView.setGravity(Gravity.CENTER);
        subtitleView.setSingleLine();
        subtitleView.setEllipsize(TextUtils.TruncateAt.END);
        subtitleView.setText(String.format(Locale.US, "%s (%d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
        subtitleView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText));
        topView.addView(subtitleView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 168 - 12, 0, 0));

        var fragmentView = super.createView(context);

        var menu = actionBar.createMenu();
        createSearchItem(menu, new ActionBarMenuItem.ActionBarMenuItemSearchListener() {

            @Override
            public void onSearchCollapse() {
                animatorSearchPageVisible.setValue(false, true);
                updateActionBarVisible();
                listView.adapter.update(true);
            }

            @Override
            public void onSearchExpand() {
                animatorSearchPageVisible.setValue(true, true);
                updateActionBarVisible();
                search("");
                listView.adapter.update(true);
            }

            @Override
            public void onTextChanged(EditText editText) {
                search(editText.getText().toString());
            }
        });
        return fragmentView;
    }

    @Override
    protected boolean needActionBarPadding() {
        return false;
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (isSearchFieldVisible()) {
            items.add(UItem.asSpace(ActionBar.getCurrentActionBarHeight()));
            fillSearchItems(items);
            return;
        }

        items.add(UItem.asCustomShadow(topView, 200 - 12));

        items.add(UItem.asButton(experimentRow, R.drawable.msg_fave, LocaleController.getString(R.string.NotificationsOther)).slug("experiment"));
        items.add(UItem.asShadow(null));

        items.add(UItem.asButton(channelRow, R.drawable.msg_channel, LocaleController.getString(R.string.OfficialChannel), "@" + LocaleController.getString(R.string.OfficialChannelUsername)).slug("channel"));
        items.add(UItem.asButton(sourceCodeRow, R.drawable.msg_link, LocaleController.getString(R.string.ViewSourceCode), "GitHub").slug("sourceCode"));
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        if (item.instanceOf(SettingsSearchCell.Factory.class)) {
            if (item.object instanceof ProfileActivity.SearchAdapter.SearchResult r) {
                r.open(null);
            }
            return;
        }
        var id = item.id;
        if (id == experimentRow) {
            presentFragment(new NekoChatSettingsActivity());
        } else if (id == channelRow) {
            getMessagesController().openByUserName(LocaleController.getString(R.string.OfficialChannelUsername), this, 1);
        } else if (id == sourceCodeRow) {
            Browser.openUrl(getParentActivity(), "https://github.com/oxxx1mif/pqcs-ImperGram");
        }
    }

    @Override
    protected String getActionBarTitle() {
        return LocaleController.getString(R.string.NekoSettings);
    }

    @Override
    protected String getKey() {
        return "";
    }

    @Override
    public void onFactorChanged(int id, float factor, float fraction, FactorAnimator callee) {
        if (id == ANIMATOR_ID_SEARCH_PAGE_VISIBLE) {
            FragmentFloatingButton.setAnimatedVisibility(syncItem, 1f - factor);
        }
    }

    @Override
    public boolean isSwipeBackEnabled(MotionEvent event) {
        return !animatorSearchPageVisible.getValue();
    }

    private ArrayList<ProfileActivity.SearchAdapter.SearchResult> createSearchArray() {
        var searchResultList = new ArrayList<ProfileActivity.SearchAdapter.SearchResult>();
        var fragment = new NekoExperimentalSettingsActivity();
        var items = new ArrayList<UItem>();
        fragment.fillItemsPublic(items, null);
        var fragmentTitle = fragment.getActionBarTitlePublic();
        String headerText = null;

        for (var item : items) {
            if (item.viewType == UniversalAdapter.VIEW_TYPE_HEADER) {
                headerText = item.text.toString();
                continue;
            } else if (item.viewType == UniversalAdapter.VIEW_TYPE_SHADOW) {
                headerText = null;
                continue;
            }
            if (TextUtils.isEmpty(item.slug)) continue;
            searchResultList.add(new ProfileActivity.SearchAdapter.SearchResult(
                    item.id,
                    item.text.toString(),
                    null,
                    fragmentTitle,
                    fragmentTitle.equals(headerText) ? null : headerText,
                    R.drawable.msg_fave,
                    () -> {
                        var fragment1 = new NekoExperimentalSettingsActivity();
                        presentFragment(fragment1);
                        AndroidUtilities.runOnUIThread(() -> fragment1.scrollToRow(item.slug, () -> {
                        }));
                    }
            ));
        }

        searchResultList.add(new ProfileActivity.SearchAdapter.SearchResult(
                10000,
                fragmentTitle,
                R.drawable.msg_fave,
                () -> presentFragment(fragment)
        ));

        searchResultList.add(new ProfileActivity.SearchAdapter.SearchResult(
                20000,
                LocaleController.getString(R.string.OfficialChannel),
                "@" + LocaleController.getString(R.string.OfficialChannelUsername),
                R.drawable.msg2_help,
                () -> getMessagesController().openByUserName(LocaleController.getString(R.string.OfficialChannelUsername), this, 1)
        ));

        searchResultList.add(new ProfileActivity.SearchAdapter.SearchResult(
                20002,
                LocaleController.getString(R.string.ViewSourceCode),
                "GitHub",
                R.drawable.msg2_help,
                () -> Browser.openUrl(getParentActivity(), "https://github.com/oxxx1mif/pqcs-ImperGram")
        ));

        return searchResultList;
    }

    private void fillSearchItems(ArrayList<UItem> items) {
        if (searchWas) {
            for (int i = 0; i < searchResults.size(); i++) {
                items.add(SettingsSearchCell.Factory.of(resultNames.get(i), searchResults.get(i)));
            }
            if (!searchResults.isEmpty()) items.add(UItem.asShadow(null));
        }
    }

    private void search(String text) {
        lastSearchString = text;
        if (searchRunnable != null) {
            Utilities.searchQueue.cancelRunnable(searchRunnable);
            searchRunnable = null;
        }
        if (TextUtils.isEmpty(text)) {
            searchWas = false;
            searchResults.clear();
            resultNames.clear();
            listView.adapter.update(true);
            return;
        }
        Utilities.searchQueue.postRunnable(searchRunnable = () -> {
            var results = new ArrayList<ProfileActivity.SearchAdapter.SearchResult>();
            var names = new ArrayList<CharSequence>();
            var lowerQuery = text.toLowerCase();
            for (var result : searchArray) {
                var title = result.searchTitle.toLowerCase();
                var index = title.indexOf(lowerQuery);
                var matchLen = lowerQuery.length();
                if (index < 0) continue;
                var ssb = new SpannableStringBuilder(result.searchTitle);
                ssb.setSpan(new ForegroundColorSpan(getThemedColor(Theme.key_windowBackgroundWhiteBlueText4)), index, Math.min(index + matchLen, ssb.length()), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                results.add(result);
                names.add(ssb);
            }

            AndroidUtilities.runOnUIThread(() -> {
                if (!text.equals(lastSearchString)) {
                    return;
                }
                searchWas = true;
                searchResults.clear();
                resultNames.clear();
                searchResults.addAll(results);
                resultNames.addAll(names);
                listView.adapter.update(true);
            });
        }, 300);
    }
}