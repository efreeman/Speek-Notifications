package uk.co.euanfreeman.speek;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import uk.co.euanfreeman.speek.notifications.Gmail;
import uk.co.euanfreeman.speek.notifications.MissedCalls;
import uk.co.euanfreeman.speek.notifications.SMS;
import uk.co.euanfreeman.speek.voice.CereCloudPlayer;

/**
 * This service receives proximity and gravity sensor updates
 * and checks for peek gestures.
 *
 * @author Euan Freeman
 */
public class PeekService extends Service implements SensorEventListener {
    private static final String TAG = "PeekService";

    private final IBinder mBinder = new LocalBinder();

    protected static boolean mRunning = false;

    private SensorManager mSensorManager;
    private Sensor mProximitySensor;
    private Sensor mGravitySensor;

    private boolean mWaitingForGravity;

    private Gmail mGmail;

    @Override
    public void onCreate() {
        Log.i(TAG, "Created service.");

        mRunning = true;
        mWaitingForGravity = false;

        mGmail = new Gmail(this);

        // Initialise proximity sensor
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mGravitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        mSensorManager.registerListener(this, mProximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Callback for when the service is started.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id: " + startId);

        return Service.START_STICKY;
    }

    /**
     * Destroy the service.
     */
    @Override
    public void onDestroy() {
        Log.i(TAG, "Destroying service");

        mRunning = false;

        mSensorManager.unregisterListener(this);
    }

    /**
     * Binder for this service.
     */
    public class LocalBinder extends Binder {
        PeekService getService() {
            return PeekService.this;
        }
    }

    /**
     * This function is called when a peek gesture is recognised. Checks
     * for unread SMS, missed calls and unread emails and then reads the
     * number of notifications to the user.
     */
    private void onPeek() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        String message = "You have ";

        boolean checkSMS = preferences.getBoolean(MainActivity.KEY_PREF_MESSAGES, false);

        if (checkSMS) {
            int unreadSMS = SMS.unreadSMS(this);

            message = String.format("%s%s new message%s", message, unreadSMS == 0 ? "no" : String.valueOf(unreadSMS), unreadSMS == 1 ? "" : "s");
        }

        boolean checkCalls = preferences.getBoolean(MainActivity.KEY_PREF_CALLS, false);

        if (checkCalls) {
            int missedCalls = MissedCalls.missedCalls(this);

            message = String.format("%s%s%s missed call%s", message, checkSMS ? ", and " : "", missedCalls == 0 ? "no" : String.valueOf(missedCalls), missedCalls == 1 ? "" : "s");
        }

        boolean checkGmail = preferences.getBoolean(MainActivity.KEY_PREF_EMAILS, false);

        if (checkGmail) {
            int unreadEmail = mGmail.getUnreadCount();

            message = String.format("%s%s%s new email%s", message, checkSMS || checkCalls ? ", and " : "", unreadEmail == 0 ? "no" : String.valueOf(unreadEmail), unreadEmail == 1 ? "" : "s");
        }

        if (checkSMS || checkCalls || checkGmail) {
            Log.i(TAG, message);

            boolean male = preferences.getBoolean(MainActivity.KEY_PREF_MALE, true);

            CereCloudPlayer voice = new CereCloudPlayer(this);
            voice.play(male ? "Stuart" : "Jess", message);
        }
    }

    /**
     * Callback for sensor accuracy changed events.
     */
    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /**
     * Callback for sensor updates. If a proximity sensor update
     * is received then this function checks if users are covering
     * the proximity sensor. If they are then wait for a gravity
     * update to check if the phone is on a table. If a gravity update
     * is received then estimate if the phone is on a table.
     */
    @Override
    public final void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            float distance = event.values[0];

            if (!mWaitingForGravity && distance <= 1.0f) {
                mWaitingForGravity = true;

                mSensorManager.registerListener(this, mGravitySensor, SensorManager.SENSOR_DELAY_FASTEST);

                Log.i(TAG, "Waiting for gravity update");
            } else {
                mWaitingForGravity = false;
            }
        } else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            Log.i(TAG, "Received gravity update");

            if (mWaitingForGravity) {
                mWaitingForGravity = false;

                if (event.values[2] > 9.0f) {
                    Log.i(TAG, "Gravity state: flat");

                    onPeek();
                } else {
                    Log.i(TAG, "Gravity state: tilted");
                }
            }

            mSensorManager.unregisterListener(this, mGravitySensor);
        }
    }

}
