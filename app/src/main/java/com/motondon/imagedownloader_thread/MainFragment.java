package com.motondon.imagedownloader_thread;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;


public class MainFragment extends Fragment {

    public static final String TAG = MainFragment.class.getSimpleName();
    private static final String CURRENT_FILE_NAME = "CURRENT_FILE_NAME";
    public static final String DOWNLOADED_IMAGE = "DOWNLOADED_IMAGE";

    public static final int TASK_FINISHED = 333;

    // Although this link is defined here, we will use intents in order for the service to inform this fragment the file name for the image
    // being downloaded. This is just to demonstrate how to communicate between a thread and a fragment.
    private String downloadUrl = "http://eskipaper.com/images/large-2.jpg";

    // No GUI fragment with setRetained(true). It will start a download task.
    private ImageDownloaderFragment imageDownloaderFragment;

    private Button btnDownload;
    private ImageView imageView;
    private Activity mActivity;

    private ProgressDialog mProgressDialog;

    private String currentFileName;

    public Handler handler;

    /**
     * Store a reference to the Activity in order to recreate ProgressDialog after a configuration change.
     *
     * @param context
     */
    @Override
    public void onAttach(Context context) {
        Log.v(TAG, "onAttach() - Context: " + context);
        super.onAttach(context);
        this.mActivity = (Activity) context;
    }

    /**
     * Nullify Activity reference in order to avoid memory leak.
     */
    @Override
    public void onDetach() {
        Log.v(TAG, "onDetach() - Context NULL");
        super.onDetach();
        this.mActivity = null;
        this.handler = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate() - savedInstanceState: " + savedInstanceState);

        super.onCreate(savedInstanceState);

        // Recreate the handler every time this fragment is reconstructed (i.e. after an orientation change), so that it will be able to
        // handle TASK_FINISHED messages.
        //
        // Note that when creating a new Handler, it is bound to the thread/message queue of the thread that is creating it. So, since we are
        // creating it from the main thread, there is no reason to use this constructor which takes a looper as an argument. Let it here just
        // for reference. We could simply use the default constructor.
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                Log.v(TAG, "handleMessage() - Received message: " + msg.what);

                switch (msg.what) {
                    case TASK_FINISHED:
                        Bundle bundle = msg.getData();
                        Bitmap bitmap = bundle.getParcelable(DOWNLOADED_IMAGE);

                        taskFinished(bitmap);
                        break;
                }
            }
        };

        // First try to get the ImageDownloaderFragment from the FragmentManager.
        imageDownloaderFragment = (ImageDownloaderFragment) getFragmentManager().findFragmentByTag(ImageDownloaderFragment.TAG);
        if (imageDownloaderFragment == null) {

            // If no instance is found, create a new one
            imageDownloaderFragment = new ImageDownloaderFragment();

            // Add the ImageDownloaderFragment instance to the fragment manager so that we can retrieve it after an orientation
            getFragmentManager().beginTransaction().add(imageDownloaderFragment, ImageDownloaderFragment.TAG).commit();

        }

        // Update our retained fragment with the handler instance, since a new one will be created on every configuration changes
        imageDownloaderFragment.updateHandler(handler);

        // Now, set ImageDownloaderFragment target fragment to refer to "this"so that ImageDownloaderFragment will be able to send the result back
        // to this fragment when calling Fragment::getTargetFragment() method
        imageDownloaderFragment.setTargetFragment(this, 0);

        if (savedInstanceState != null) {
            currentFileName = savedInstanceState.getString(CURRENT_FILE_NAME);
        }

        // If "currentFileName" contains any value, it means a download is being processed. So, after an orientation change under this
        // circumstance, show ProgressDialog again.
        if (currentFileName != null && !currentFileName.isEmpty()) {
            showProgressDialog(currentFileName);
        }
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy()");

        super.onDestroy();

        // Close ProgressDialog (it is being showed), since it will be recreated after this fragment be reconstructed again (on
        // orientation change)
        dismissProgressDialog();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.v(TAG, "onCreateView()");

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        btnDownload = (Button) rootView.findViewById(R.id.btnDownloadImage);
        imageView = (ImageView) rootView.findViewById(R.id.imgView);

        // After a successful download, in case of an orientation change, the image will be saved in the bundle
        // (by the onSaveInstanceState method). So, retrieve it here and load it on the ImageView.
        if (savedInstanceState != null) {
            Bitmap bitmap = savedInstanceState.getParcelable(DOWNLOADED_IMAGE);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
        }

        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDownloadImageClick(v);
            }
        });
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.v(TAG, "onSaveInstanceState() = currentFileName: " + currentFileName);

        super.onSaveInstanceState(outState);
        outState.putString(CURRENT_FILE_NAME, currentFileName);

        // After a successful download, in case of an orientation change, store the image in the bundle so that it
        // can be restored later.
        Drawable downloadedImage = imageView.getDrawable();
        if (downloadedImage != null) {
            BitmapDrawable bitmapDrawable = ((BitmapDrawable) downloadedImage);
            Bitmap bitmap = bitmapDrawable .getBitmap();

            outState.putParcelable(DOWNLOADED_IMAGE, bitmap);
        }
    }

    private void onDownloadImageClick(View v) {
        Log.v(TAG, "onDownloadImageClick()");

        // First clear image (if have one)
        imageView.setImageDrawable(null);

        // And finally calls a method which will start the download task.
        imageDownloaderFragment.startDownload(downloadUrl, handler);
    }

    /**
     * Prior to start a download, ImageDownloaderTask will inform ImageDownloaderFragment which will call this method. So, update
     * the file (name) being downloaded
     *
     * Since it is called by a worker thread, we must execute it in the main thread. To do so, call it inside runOnUiThread().
     *
     * @param file
     */
    public void taskStarted(final String file) {
        Log.v(TAG, "taskStarted()");

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                currentFileName = file;

                showProgressDialog(file);
            }
        });
    }

    /**
     * When the download is finished, ImageDownloaderTask::run() will send a message by using a Handler object.
     *
     * Since that Handler object was created in this class (and passed on to the ImageDownloaderTask as an argument), when it is used to send
     * a message, messages will be handled by the Handler::handleMessage method (inside onCreate method) which will then call this method.
     *
     * There is no need to call runOnUiThread, once handler object was already created inside the MainLooper.
     *
     * @param bitmap
     */
    public void taskFinished(Bitmap bitmap) {
        Log.v(TAG, "taskFinished()");

        dismissProgressDialog();

        // Do not forget to nullify currentFileName, otherwise, after a finished download, if user change orientation,
        // progressDialog will be showed forever
        currentFileName = null;

        if(bitmap != null){
            imageView.setImageBitmap(bitmap);

        } else {
            Toast.makeText(mActivity, "Image Does Not exist or Network Error", Toast.LENGTH_SHORT).show();
        }
    }

    private void showProgressDialog(String file) {
        Log.v(TAG, "showProgressDialog()");

        mProgressDialog = new ProgressDialog(mActivity);
        mProgressDialog.setTitle("Download Image");
        mProgressDialog.setMessage("Loading " + file + " file...");
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.show();
    }

    private void dismissProgressDialog() {
        Log.v(TAG, "dismissProgressDialog()");

        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }
}
