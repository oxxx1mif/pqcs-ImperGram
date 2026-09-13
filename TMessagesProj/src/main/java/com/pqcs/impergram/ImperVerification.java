/*
 * This is the source code of Impergram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Gleb Obitotsky <gleb.obitotsky@gmail.com>, 2026.
 */

package com.pqcs.impergram;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;

import androidx.core.content.ContextCompat;

import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ImperVerification {

    private static final Set<Long> VERIFIED_USERS = new HashSet<>(Arrays.asList(
            889255364L,
            6242196973L,
            1530870980L,
            6249422673L,
            8985758553L,
            1971907287L
    ));

    private static final Set<Long> VERIFIED_CHATS_RAW = new HashSet<>(Arrays.asList(
            2270928900L,
            2945768966L,
            3163611075L
    ));

    private static final Set<Long> VERIFIED_BOTS = new HashSet<>(Arrays.asList(
            8396608130L
    ));

    private static final long CUSTOM_PROFILE_EMOJI = 0L;

    public static final String VERIFIER_NAME   = "ImperGram";
    public static final String VERIFIER_NAME_2 = "pqcs";

    public static long toRawChatId(long chatId) {
        long raw = Math.abs(chatId);
        if (raw > 1_000_000_000_000L) raw -= 1_000_000_000_000L;
        return raw;
    }

    public static boolean isVerifiedUser(long userId) {
        return userId > 0 && VERIFIED_USERS.contains(userId);
    }

    public static boolean isVerifiedChat(long chatId) {
        return VERIFIED_CHATS_RAW.contains(toRawChatId(chatId));
    }

    public static boolean isVerifiedBot(long botId) {
        return VERIFIED_BOTS.contains(botId);
    }

    public static boolean isVerifiedAny(long dialogId) {
        if (dialogId > 0) return isVerifiedUser(dialogId) || isVerifiedBot(dialogId);
        return isVerifiedChat(dialogId);
    }

    public static long getCustomProfileEmoji() { return CUSTOM_PROFILE_EMOJI; }

    public static Drawable getVerifiedDrawable(Context context) {
        Drawable d = ContextCompat.getDrawable(context, R.drawable.notification);
        return d != null ? d.mutate() : null;
    }

    public static Drawable getPlaneDrawable(Context context) {
        Drawable d = ContextCompat.getDrawable(context, R.drawable.notification);
        return d != null ? d.mutate() : null;
    }

    private static final Set<Long> SHOWN_BULLETINS = new HashSet<>();

    public static boolean shouldShowBulletin(long dialogId) {
        return SHOWN_BULLETINS.add(dialogId);
    }

    public static void showVerifiedBulletin(BaseFragment fragment, boolean isChannelOrBot) {
        if (fragment == null || fragment.getParentActivity() == null) return;

        SpannableStringBuilder sb = new SpannableStringBuilder();
        sb.append(isChannelOrBot ? "Channel verifier ImperGram " : "Account verifier ImperGram ");
        sb.append(VERIFIER_NAME);
        sb.append(" / ");
        sb.append(VERIFIER_NAME_2);

        try {
            BulletinFactory.of(fragment)
                    .createSimpleBulletin(R.drawable.notification, sb)
                    .show();
        } catch (Exception ignore) {}
    }
}