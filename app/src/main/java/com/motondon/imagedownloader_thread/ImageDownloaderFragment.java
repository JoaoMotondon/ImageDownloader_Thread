package com.motondon.imagedownloader_thread;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.motondon.imagedownloader_thread.thread.ImageDownloaderTask;


/**
 * This is basically a "no GUI fragment" with uses setRetained(true).
 *
 * It will start the ImageDownloaderTask runnable task, and since it will be retained in case of an orientation change,
 * it will not destroy the ImageDownloaderTask task while a task is in progress.
 *
 * After an orientation change, the MainFragment instance will retrieve the ImageDownloaderFragment instance from the
 * FragmentManager and update its instance on it. This will make this fragment to always contains the right reference to the
 * MainFragment.
 *
 */
public class ImageDownloaderFragment extends Fragment implements ImageDownloaderTask.ThreadCallback {

    public static final String TAG = ImageDownloaderFragment.class.getSimpleName();

    private Thread mImageDownloaderThread;
    private ImageDownloaderTask imageDownloaderTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return null;
    }

    public void startDownload(String downloadUrl, Handler handler) {
        imageDownloaderTask = new ImageDownloaderTask(this, downloadUrl, handler);
        mImageDownloaderThread = new Thread(imageDownloaderTask);
        mImageDownloaderThread.start();
    }

    /**
     * Use Fragment::getTargetFragment() method in order to ensure we will always get the right reference.
     *
     * @param file
     */
    public void taskStarted(String file) {
        ((MainFragment)getTargetFragment()).taskStarted(file);
    }

    /**
     * This method is not used, since after a download is finished, ImageDownloaderTask::run()  will send a message via Handler
     * which will be handled directly by the MainFragment::Handler::handleMessage().
     *
     * @param bitmap
     */
    public void taskFinished(Bitmap bitmap) {
    }

    /**
     * Called by the MainFragment after an orientation change in order to update the new Handler instance.
     *
     * @param handler
     */
    public void updateHandler(Handler handler) {
        if (imageDownloaderTask != null) {
            imageDownloaderTask.updateHandler(handler);
        }
    }
}