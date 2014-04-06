package uk.co.euanfreeman.speek.notifications;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.google.android.gm.contentprovider.GmailContract;

/**
 * Gets number of unread Gmail conversations from the default account.
 *
 * @author Euan Freeman
 */
public class Gmail {
    private final String TAG = "Gmail";
    private final String ACCOUNT_TYPE_GOOGLE = "com.google";
    private final String[] FEATURES_MAIL = {"service_mail"};

    private Context mContext;
    private String mAccount;

    /**
     * Creates a new Gmail checker. Gets the first account from the
     * account manager so we can access a content provider for this later.
     */
    public Gmail(Context context) {
        mContext = context;

        AccountManager.get(context).getAccountsByTypeAndFeatures(ACCOUNT_TYPE_GOOGLE, FEATURES_MAIL, new AccountManagerCallback() {
            @Override
            public void run(AccountManagerFuture future) {
                Account[] accounts;

                try {
                    accounts = (Account[]) future.getResult();

                    if (accounts != null && accounts.length > 0) {
                        mAccount = accounts[0].name;
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }, null);
    }

    /**
     * Counts how many unread conversations are in the inbox.
     *
     * @return Returns a count of unread emails. Returns -1 if
     * no accounts are available. Returns -2 if the content
     * provider is unavailable.
     */
    public int getUnreadCount() {
        if (mAccount == null)
            return -1;

        Cursor labelsCursor = mContext.getContentResolver().query(GmailContract.Labels.getLabelsUri(mAccount), null, null, null, null);

        if (labelsCursor == null)
            return -2;

        final String inboxCanonicalName = GmailContract.Labels.LabelCanonicalNames.CANONICAL_NAME_INBOX;
        final int canonicalNameIndex = labelsCursor.getColumnIndexOrThrow(GmailContract.Labels.CANONICAL_NAME);
        final int unreadConversationsIndex = labelsCursor.getColumnIndexOrThrow(GmailContract.Labels.NUM_UNREAD_CONVERSATIONS);

        int unread = 0;

        while (labelsCursor.moveToNext()) {
            if (inboxCanonicalName.equals(labelsCursor.getString(canonicalNameIndex))) {
                unread = labelsCursor.getInt(unreadConversationsIndex);
                break;
            }
        }

        labelsCursor.close();

        return unread;
    }
}
