package uk.co.euanfreeman.speek;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class MainActivity extends PreferenceActivity {
    protected static final String KEY_PREF_MESSAGES = "pref_key_messages";
    protected static final String KEY_PREF_EMAILS = "pref_key_emails";
    protected static final String KEY_PREF_CALLS = "pref_key_calls";
    protected static final String KEY_PREF_MALE = "pref_key_male";

    private Intent mPeekIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Only start the peek gesture service if it isn't running
        if (!PeekService.mRunning) {
            mPeekIntent = new Intent(this, PeekService.class);

            startService(mPeekIntent);
        }
    }

    /**
     * Callback function for when the activity is destroyed. Checks
     * if the peek service is set to always run; if not then the
     * service is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopService(mPeekIntent);
    }

}
