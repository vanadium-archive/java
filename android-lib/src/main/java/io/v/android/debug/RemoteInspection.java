// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.debug;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.StringRes;
import android.widget.EditText;

import org.joda.time.Duration;
import org.joda.time.ReadableDuration;

import io.v.android.inspectors.RemoteInspectors;
import io.v.android.VAndroidContext;
import io.v.v23.android.R;
import io.v.v23.context.VContext;
import io.v.v23.verror.VException;
import lombok.Getter;

/**
 * Encapsulates the remote inspection debug feature. This is a utility wrapper around
 * {@link RemoteInspectors}.
 * <p>
 * The UI for this feature includes a dialog prompting the user for an e-mail address to which it
 * will send an e-mail detailing how the recipient can inspect the state of the application. This
 * may be useful for live debugging (the recipient can inspect logs, exported stats,
 * system information, etc.).
 * <p>
 * The dialog also allows the user to disable remote inspection, thereby refusing requests from
 * remote inspectors even if they have permissions, or enable remote inspection, allowing anyone
 * with pre-existing permissions to inspect remotely.
 * <p>
 * This class also manages a shared preference to persist remote inspectability across app sessions.
 * This preference is stored as a Boolean preference named
 * <code>{@value #REMOTE_INSPECTION_PREF}</code>. If this setting is already set upon instantiation,
 * remote inspection is immediately enabled.
 */
public class RemoteInspection {
    public static final String REMOTE_INSPECTION_PREF = "remoteInspection";
    public static final Duration DEFAULT_INVITATION_DURATION = Duration.standardDays(1);
    public static final String EMAIL_MIME_TYPE = "message/rfc822";

    @Getter
    private final VAndroidContext<?> mVAndroidContext;
    private final SharedPreferences mSharedPreferences;

    private VContext mVContext;
    private RemoteInspectors mRemoteInspectors;

    public RemoteInspection(final VAndroidContext<?> context,
                            final SharedPreferences sharedPreferences) {
        mVAndroidContext = context;
        mSharedPreferences = sharedPreferences;
        if (mSharedPreferences.getBoolean(REMOTE_INSPECTION_PREF, false)) {
            enable();
        }
    }

    public boolean isEnabled() {
        return mRemoteInspectors != null;
    }

    private void onError(final @StringRes int message, final Throwable t) {
        mVAndroidContext.getErrorReporter().onError(message, t);
    }

    /**
     * Starts the remote inspection server. This allows any user with a remote inspection blessing
     * to access debug information through a browser.
     *
     * @see #disable()
     */
    public void enable() {
        if (mRemoteInspectors == null) {
            mVContext = mVAndroidContext.getVContext().withCancel();
            try {
                mRemoteInspectors = new RemoteInspectors(mVContext);
            } catch (final VException e) {
                onError(R.string.err_inspect_setup, e);
            }
            mSharedPreferences.edit()
                    .putBoolean(REMOTE_INSPECTION_PREF, true)
                    .apply();
        }
    }

    /**
     * Stops the remote inspection server. Users with remote inspection blessings will not be able
     * to access live debug information until remote inspection is re-enabled.
     *
     * @see #enable()
     */
    public void disable() {
        if (mRemoteInspectors != null) {
            mVContext.cancel();
            mRemoteInspectors = null;
            mSharedPreferences.edit()
                    .putBoolean(REMOTE_INSPECTION_PREF, false)
                    .apply();
        }
    }

    /**
     * Invite a remote user to inspect state of the application. This method enables remote
     * inspection if not already enabled.
     *
     * @see RemoteInspectors#invite(String, ReadableDuration)
     *
     * @param invitee the name to refer to the remote user as (typically an email address).
     * @param duration time after which inspection privileges expire.
     *
     * @return a textual description of how the remote user can access state of the running
     * application.
     */
    public String mintInvitation(final String invitee, final ReadableDuration duration)
            throws VException {
        // TODO(rosswang): Should we provide an overload that skips the enable() call?
        enable();
        return mRemoteInspectors.invite(invitee, duration);
    }

    /**
     * Sends a freshly minted invitation to the given e-mail address. If there is no e-mail client
     * installed, an error is reported to the user.
     */
    public void emailInvitation(final String email, final ReadableDuration duration) {
        final String content;
        try {
            content = mintInvitation(email, duration);
        } catch (final VException e) {
            onError(R.string.err_inspect_invite, e);
            return;
        }

        final Context context = mVAndroidContext.getAndroidContext();

        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(EMAIL_MIME_TYPE);
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
        intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.inspect_email_subject));
        intent.putExtra(Intent.EXTRA_TEXT, content);

        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        } else {
            onError(R.string.err_inspect_email, null);
        }
    }

    /**
     * Shows a dialog box to send remote inspection invitations, enable remote inspection, and
     * disable remote inspection.
     */
    public void showDialog() {
        final AlertDialog.Builder builder =
                new AlertDialog.Builder(mVAndroidContext.getAndroidContext())
                        .setView(R.layout.dialog_inspect)
                        .setCancelable(true)
                        .setPositiveButton(R.string.inspect_invite, (d, b) -> {
                            final EditText email = (EditText)
                                    ((AlertDialog) d).findViewById(R.id.inspect_email);
                            emailInvitation(email.getText().toString(),
                                    DEFAULT_INVITATION_DURATION);
                        });

        if (isEnabled()) {
            builder.setNegativeButton(R.string.inspect_disable, (d, b) -> disable());
        } else {
            builder.setNegativeButton(R.string.inspect_enable, (d, b) -> enable());
        }

        builder.show();
    }
}
