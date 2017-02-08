package com.motondon.imagedownloader_thread.thread;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.motondon.imagedownloader_thread.MainFragment;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Prior to start downloading an image (i.e. on run() method) we call ThreadCallback::taskStarted()
 * in order to update GUI with the file name. Since we are inside a worker thread, we cannot
 * access main UI thread directly. Instead, ImmageDownloaderFragment::tastStarted() will pass this call to the
 * tartegFragment (i.e. the MainFragment) which will use Activity::runOnUiThread(). This will make the code inside
 * it to be ran on the main UI thread.
 *
 * After image download finishes, we use another approach to update GUI. Instead of using runOnUiThread() we will
 * send a message by using MainFragment handler object. Doing so, MainFragment::Handler::handleMessage() will
 * handle this message in the main thread and will update its GUI accordingly. Every time an orientation occurs,
 * it is MainFragment responsability to update handler reference in this class.
 *
 * Another way we could use is the LocalBroadcastReceiver to send a task directly to the fragment. We will use it
 * in the next example: ImageDonwloader_Service_01
 *
 */
public class ImageDownloaderTask implements Runnable {

    private static final String TAG = ImageDownloaderTask.class.getSimpleName();

    public interface ThreadCallback {
        void taskStarted(String fileName);
        void taskFinished(Bitmap bitmap);
    }

    private ThreadCallback mCallback;
    private Handler mHandler;
    private String mUrl;

    public ImageDownloaderTask(ThreadCallback callback, String url, Handler handler) {
        Log.v(TAG, "Constructor");

        this.mCallback = callback;
        this.mHandler = handler;
        this.mUrl = url;
    }

    @Override
    public void run() {
        Log.v(TAG, "run() - Begin");

        try {
            String fileName = Uri.parse(mUrl).getLastPathSegment();

            // WARNING, since we are in a worker thread, taskStarted() method cannot access main UI thread objects directly. Instead
            // we need to use runOnUiThread (or another approach). Otherwise we will get an exception!!!!!!!
            mCallback.taskStarted(fileName);

            Log.v(TAG, "run() - Downloading image...");
            Bitmap bitmap = downloadBitmap(mUrl);
            Log.v(TAG, "run() - Download finished");

            // This is a worker thread, so we cannot access any UI objects directly. But since we are using a handler object and this handler
            // is running inside the main loop, it will be able to access those UI objects with no problem.
            Message msg = mHandler.obtainMessage();
            msg.what = MainFragment.TASK_FINISHED;
            Bundle bundle = new Bundle();
            bundle.putParcelable(MainFragment.DOWNLOADED_IMAGE, bitmap);
            msg.setData(bundle);

            Log.v(TAG, "run() - Sending message to the MainFragment...");
            mHandler.sendMessage(msg);

        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "run() - Exception while trying to download image from url: " + mUrl + ". Message: " + e.getMessage());
        }

        Log.v(TAG, "run() - End");
    }

    /**
     * Download image here
     *
     * @param strUrl
     * @return
     * @throws IOException
     */
    private Bitmap downloadBitmap(String strUrl) throws IOException {
        Bitmap bitmap=null;
        InputStream iStream = null;
        try{
            URL url = new URL(strUrl);
            /** Creating an http connection to communcate with url */
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            /** Connecting to url */
            urlConnection.connect();

            /** Reading data from url */
            iStream = urlConnection.getInputStream();

            /** Creating a bitmap from the stream returned from the url */
            bitmap = BitmapFactory.decodeStream(iStream);

        }catch(Exception e){
            Log.d(TAG, "Exception while downloading url: " + strUrl + ". Error: " + e.toString());
        }finally{
            if (iStream != null) {
                iStream.close();
            }
        }
        return bitmap;
    }

    public void updateHandler(Handler handler) {
        this.mHandler = handler;
    }
}
