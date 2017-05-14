package com.google.android.exoplayer2.videobench;

import android.Manifest;
import android.app.backup.BackupHelper;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Arrays;

public class DownloadActivity extends Activity {

    public static final String LOCAL_NAME = "local_name";
    public static final String ACTION_DOWNLOAD =
            "com.google.android.exoplayer.videobench.action.DOWNLOAD";

    private TextView textView;
    private ProgressBar progressBar;
    private BackgroundDownloadTask bgDownload;
    private Uri uri;
    private String localname;
    private String fq_localname;
    private Handler handler;
    private Runnable callback;
    private Boolean download_running=false;

    public static final int STORAGE_REQUEST=1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);
        // get the progress bar
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        // config textView
        textView = (TextView) findViewById(R.id.textView);
        textView.setMovementMethod(new ScrollingMovementMethod());
        textView.setTypeface(Typeface.MONOSPACE);
        textView.setTextSize(18);
    }

    @Override
    protected void onResume() {


        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Just keep requesting if denied, it's the whole point of the app
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_REQUEST);
            }
        else {
            startBgTask();
        }
        super.onResume();
    }

    private void startBgTask() {
        Intent intent = getIntent();
        uri = intent.getData();
        localname = intent.getStringExtra(LOCAL_NAME);
        fq_localname = BackgroundDownloadTask.getLocalName(localname);

        if (!download_running) {
            // configure the background task
            bgDownload = new BackgroundDownloadTask();
            addConsoleOutput("Downloading URI " + uri);
            addConsoleOutput("File will be called " + localname);
            addConsoleOutput("Stored in");
            addConsoleOutput(fq_localname);
            addConsoleOutput("--------");
            handler = new Handler();
            callback = new Runnable() {
                @Override
                public void run() {
                    int expectedSize = bgDownload.expected_size;
                    int currentSize = bgDownload.current_size;
                    //BackgroundDownloadTask.Status status = bgDownload.status;
                    addConsoleOutput("ExpectedSize=" + expectedSize);
                    addConsoleOutput("CurrentSize=" + currentSize);
                    progressBar.setMax(expectedSize);
                    progressBar.setProgress(currentSize);
                    Boolean repeat = true;
                    BackgroundDownloadTask.Status status = bgDownload.status;
                    switch (status) {
                        case eCreated:
                            addConsoleOutput("created");
                            break;
                        case eDownloading:
                            addConsoleOutput("downloading");
                            break;
                        case eStopped:
                            addConsoleOutput("stopped");
                            repeat = false;
                            download_running = false;
                            break;
                        case eBroken:
                            addConsoleOutput("broken");
                            repeat = false;
                            download_running = false;
                            break;
                        case eComplete:
                            addConsoleOutput("complete");
                            repeat = false;
                            download_running = false;
                            String tempname = BackgroundDownloadTask.getLocalName(localname)+"_downloading";
                            java.io.File file = new java.io.File(tempname);
                            java.io.File newfile = new java.io.File(BackgroundDownloadTask.getLocalName(localname));
                            file.renameTo(newfile);
                            break;
                    }
                    // Repeat this the same runnable code block again another 1 second
                    if (repeat)
                        handler.postDelayed(callback, 1000);
                }
            };
            bgDownload.execute(uri.toString(), localname+"_downloading");
            download_running = true;
        }
        // Start the initial runnable task by posting through the handler
        handler.post(callback);
    }

    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case STORAGE_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    startBgTask();
                return;
            }
        }
    }

    @Override
    protected void onPause(){
        if (download_running)
            handler.removeCallbacks(callback);
        super.onPause();
    }

    private void addConsoleOutput(String output) {
        textView.append(output+'\n');
        // find the amount we need to scroll.  This works by
        // asking the TextView's internal layout for the position
        // of the final line and then subtracting the TextView's height
        int scrollAmount = 0;
        Layout l = textView.getLayout();
        if (l != null)
            scrollAmount = l.getLineTop(textView.getLineCount()) - textView.getHeight();
        // if there is no need to scroll, scrollAmount will be <=0
        if (scrollAmount > 0)
            textView.scrollTo(0, scrollAmount);
        else
            textView.scrollTo(0, 0);
    }
}
