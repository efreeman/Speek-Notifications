package uk.co.euanfreeman.speek.notifications;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

/**
 * Gets number of unread SMS messages.
 *
 * @author Euan Freeman
 */
public class SMS {
    /**
     * Gets a count of unread SMS messages.
     */
    public static int unreadSMS(Context context) {
        Uri uriSMS = Uri.parse("content://sms/inbox");
        Cursor c = context.getContentResolver().query(uriSMS, null, "read = 0", null, null);
        int unreadSMS = c == null ? 0 : c.getCount();
        c.close();

        return unreadSMS;
    }
}
