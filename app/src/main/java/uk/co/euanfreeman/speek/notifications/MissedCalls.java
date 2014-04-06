package uk.co.euanfreeman.speek.notifications;

import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog;

/**
 * Gets number of missed phone calls.
 *
 * @author Euan Freeman
 */
public class MissedCalls {
    public static int missedCalls(Context context) {
        String[] select = {CallLog.Calls.TYPE};
        String where = CallLog.Calls.TYPE + "=" + CallLog.Calls.MISSED_TYPE + " AND " + CallLog.Calls.NEW + "=1";
        Cursor c = context.getContentResolver().query(CallLog.Calls.CONTENT_URI, select, where, null, null);
        int missedCalls = c == null ? 0 : c.getCount();
        c.close();

        return missedCalls;
    }
}
