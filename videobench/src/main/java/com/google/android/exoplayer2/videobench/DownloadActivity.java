package com.google.android.exoplayer2.videobench;

import android.Manifest;
import android.app.backup.BackupHelper;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.app.DownloadManager;

import java.io.File;
import java.util.Arrays;
import android.database.Cursor;
import android.util.Log;
import android.content.SharedPreferences;

public class DownloadActivity extends Activity {

    public static final String LOCAL_NAME = "local_name";
    public static final String URI = "uri";
    public static final String ACTION_DOWNLOAD =
            "com.google.android.exoplayer.videobench.action.DOWNLOAD";
    private static final String TAG = "DownloadActivity";

    private TextView textView;
    private ProgressBar progressBar;
    private long downloadID;
    private Uri uri;
    private String localname;
    private String fq_localname;
    private Handler handler;
    private Runnable callback;
    private DownloadManager downloadManager;
    private SharedPreferences pref;

    public static final int STORAGE_REQUEST = 1;

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
        Intent intent = getIntent();
        localname = intent.getStringExtra(LOCAL_NAME);
        uri = Uri.parse(intent.getStringExtra(URI));
        downloadID = -1;
        fq_localname = getLocalName(localname);
        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        pref = getSharedPreferences("VideoBenchmarkFiles", MODE_PRIVATE);
        handler = new Handler();
        callback = new Runnable() {
            @Override
            public void run() {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadID);
                Cursor cursor = downloadManager.query(query);
                if (cursor.moveToFirst()) {
                    //column for download  status
                    int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    int status = cursor.getInt(columnIndex);
                    //column for reason code if the download failed or paused
                    int columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                    int reason = cursor.getInt(columnReason);
                    // size data
                    int columnTotalSize = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                    int totalSize = cursor.getInt(columnTotalSize);
                    int columnSize = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                    int size = cursor.getInt(columnSize);

                    String statusText = "";
                    String reasonText = "";

                    switch (status) {
                        case DownloadManager.STATUS_FAILED:
                            statusText = "STATUS_FAILED";
                            switch (reason) {
                                case DownloadManager.ERROR_CANNOT_RESUME:
                                    reasonText = "ERROR_CANNOT_RESUME";
                                    break;
                                case DownloadManager.ERROR_DEVICE_NOT_FOUND:
                                    reasonText = "ERROR_DEVICE_NOT_FOUND";
                                    break;
                                case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                                    reasonText = "ERROR_FILE_ALREADY_EXISTS";
                                    break;
                                case DownloadManager.ERROR_FILE_ERROR:
                                    reasonText = "ERROR_FILE_ERROR";
                                    break;
                                case DownloadManager.ERROR_HTTP_DATA_ERROR:
                                    reasonText = "ERROR_HTTP_DATA_ERROR";
                                    break;
                                case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                                    reasonText = "ERROR_INSUFFICIENT_SPACE";
                                    break;
                                case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
                                    reasonText = "ERROR_TOO_MANY_REDIRECTS";
                                    break;
                                case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:
                                    reasonText = "ERROR_UNHANDLED_HTTP_CODE";
                                    break;
                                case DownloadManager.ERROR_UNKNOWN:
                                    reasonText = "ERROR_UNKNOWN";
                                    break;
                            }
                            break;
                        case DownloadManager.STATUS_PAUSED:
                            statusText = "STATUS_PAUSED";
                            switch (reason) {
                                case DownloadManager.PAUSED_QUEUED_FOR_WIFI:
                                    reasonText = "PAUSED_QUEUED_FOR_WIFI";
                                    break;
                                case DownloadManager.PAUSED_UNKNOWN:
                                    reasonText = "PAUSED_UNKNOWN";
                                    break;
                                case DownloadManager.PAUSED_WAITING_FOR_NETWORK:
                                    reasonText = "PAUSED_WAITING_FOR_NETWORK";
                                    break;
                                case DownloadManager.PAUSED_WAITING_TO_RETRY:
                                    reasonText = "PAUSED_WAITING_TO_RETRY";
                                    break;
                            }
                            break;
                        case DownloadManager.STATUS_PENDING:
                            statusText = "STATUS_PENDING";
                            break;
                        case DownloadManager.STATUS_RUNNING:
                            statusText = "STATUS_RUNNING";
                            break;
                        case DownloadManager.STATUS_SUCCESSFUL:
                            Log.i(TAG, "complete localname="+localname);
                            set_status(2);
                            finish();
                            break;
                    }
                    addConsoleOutput(statusText);
                    if (status != DownloadManager.STATUS_RUNNING) {
                        set_status(0);
                        addConsoleOutput(reasonText);
                        Log.i(TAG, statusText + " : " + reasonText);
                    } else {
                        addConsoleOutput(size + " : " + totalSize);
                        Log.i(TAG, size + " : " + totalSize);
                    }
                    addConsoleOutput("----------");
                    progressBar.setMax(totalSize);
                    progressBar.setProgress(size);
                    // Repeat this the same runnable code block again another 1 second
                    handler.postDelayed(callback, 1000);
                }
            }
        };
    }

    @Override
    protected void onResume() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Just keep requesting if denied, it's the whole point of the app
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_REQUEST);
        } else {
            startDownload();
        }
        super.onResume();
    }

    private void set_status(long status) {
        SharedPreferences.Editor ed = pref.edit();
        ed.putLong(localname,status);
        ed.apply();
    }
    // turn a filename into the local cache equivalent
    static String getLocalName(String localname) {
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        Log.d("getLocalName", "PublicDir is "+path);
        File file = new File(path, localname);
        Log.d("getLocalName", "FQ is "+file.toString());
        return file.toString();
    }

    protected void startDownload() {
        // Check if download is in progress
        String local_uri = Uri.fromFile(new java.io.File(fq_localname)).toString();
        DownloadManager.Query query = new DownloadManager.Query();
        Cursor cursor = downloadManager.query(query);
        Log.i(TAG, "Looking for "+local_uri);
        while (cursor.moveToNext()) {
            int nameCol = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
            String name = cursor.getString(nameCol);
            int idCol = cursor.getColumnIndex(DownloadManager.COLUMN_ID);
            long id = cursor.getLong(idCol);
            Log.i(TAG, "downloadmanager has "+name+" as "+id);
            if (name.equals(local_uri)) {
                Log.i(TAG, "found file");
                downloadID = id;
                break;
            }
        }
        if (downloadID == -1) {
            // Create request - we did not find one
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setTitle(localname);
            request.setDescription("Download sample data");
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, localname);
            request.setNotificationVisibility(1);
            request.setVisibleInDownloadsUi(true);
            downloadID = downloadManager.enqueue(request);
            set_status(1);
        }
        // Start the initial runnable task by posting through the handler
        handler.post(callback);
    }

    @Override
    protected void onPause(){
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
