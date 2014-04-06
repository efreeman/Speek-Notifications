package uk.co.euanfreeman.speek.voice;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * This class makes requests to the CereProc Cloud service to
 * synthesis speech and then plays the synthesised message.
 * 
 * @author Euan Freeman
 */
public class CereCloudPlayer {
    private static final String TAG = "CereCloudPlayer";
    private static final String ACCOUNT = "";
    private static final String PASSWORD = "";

    private Context mContext;

    public CereCloudPlayer(Context context) {
        mContext = context;
    }

    /**
     * Make a request to synthesise speech and play the message.
     *
     * @param voice CereProc voice to use.
     * @param message Message to be synthesised.
     */
    public void play(String voice, String message) {
        CereVoiceTask task = new CereVoiceTask();
        task.execute(voice, message);
    }

    /**
     * Background task to make a request to the Cere Cloud service.
     */
    private class CereVoiceTask extends AsyncTask<String, Void, String> {
        /**
         * Make a request to the Cere Cloud service.
         *
         * @param params String array: [voice, message]
         *
         * @return URL to synthesised voice file.
         */
        @Override
        protected String doInBackground(String... params) {
            if (params == null || params.length < 2)
                return null;

            String requestXML = String.format(
                            "<?xml version='1.0'?>" +
                            "<speakSimple>" +
                            "<accountID>%s</accountID>" +
                            "<password>%s</password>" +
                            "<voice>%s</voice>" +
                            "<text>%s</text>" +
                            "</speakSimple>", ACCOUNT, PASSWORD, params[0], params[1]);

            URL url;
            URLConnection urlConnection;
            HttpURLConnection connection;

            try {
                url = new URL("https://cerevoice.com/rest/rest_1_1.php");
                urlConnection = url.openConnection();
                connection = (HttpURLConnection) urlConnection;

                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
                connection.setRequestProperty("Content-Length", String.valueOf(requestXML.length()));

                OutputStream out = connection.getOutputStream();
                Writer writerOut = new OutputStreamWriter(out);
                writerOut.write(requestXML);
                writerOut.flush();
                writerOut.close();

                InputStream in = connection.getInputStream();
                XmlPullParser parser = Xml.newPullParser();
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                parser.setInput(in, null);
                String responseURL = parseXML(parser);

                in.close();

                return responseURL;
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }

            return null;
        }

        /**
         * Called after URL has been retrieved. Starts a task to
         * play the synthesised voice.
         *
         * @param result Synthesised voice URL.
         */
        @Override
        protected void onPostExecute(String result) {
            if (result == null)
                return;

            Log.i(TAG, result);

            PlayTask task = new PlayTask();
            task.execute(result);
        }

        /**
         * Parses the file URL from the Cere Cloud resonse XML.
         *
         * @param parser XmlPullParser set to parse the Cere Cloud response.
         *
         * @return String containing the URL to synthesised voice.
         */
        private String parseXML(XmlPullParser parser) throws XmlPullParserException, IOException {
            int eventType = parser.getEventType();

            String fileUrl = null;

            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if (parser.getName().equals("fileUrl")) {
                            fileUrl = parser.nextText();
                        }

                        break;
                }

                eventType = parser.next();
            }

            return fileUrl;
        }
    }

    /**
     * Background task which streams the synthesised speech file.
     */
    private class PlayTask extends AsyncTask<String, Void, Integer> {
        @Override
        protected Integer doInBackground(String... params) {
            if (params == null || params.length == 0) {
                return -1;
            }

            Uri uri = Uri.parse(params[0]);
            MediaPlayer player = MediaPlayer.create(mContext, uri);

            if (player != null) {
                player.start();
            } else {
                Log.e(TAG, "Player is null.");
            }

            return 0;
        }
    }
}
