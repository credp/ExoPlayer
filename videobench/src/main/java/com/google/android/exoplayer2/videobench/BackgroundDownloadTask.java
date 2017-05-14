package com.google.android.exoplayer2.videobench;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by chris on 13/05/17.
 */

// TODO: Switch to google.iosched.DiskBasedCache implementation?

class BackgroundDownloadTask extends AsyncTask<String, String, String> {

    enum Status {
        eCreated,
        eDownloading,
        eComplete,
        eBroken,
        eStopped
    };

    private String tag="BgDlTsk";
    public URL fetch_URL;
    public int expected_size;
    public int current_size;
    public Status status;
    public Boolean stop=false;

    /**
     * Before starting background thread
     * */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        this.expected_size = 0;
        this.current_size = 0;
        this.status = Status.eCreated;
    }

    // turn a filename into the local cache equivalent
    static String getLocalName(String localname) {
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        Log.d("getLocalName", "PublicDir is "+path);
        File file = new File(path, localname);
        Log.d("getLocalName", "FQ is "+file.toString());
        return file.toString();
    }

    /**
     * Downloading file in background thread
     * */
    @Override
    protected String doInBackground(String... strings){
        String filename = getLocalName(strings[1]);
        try {
            int count;

            this.fetch_URL = new URL(strings[0]);

            Log.d(tag, Environment.getExternalStorageState(new File(filename)));

            URLConnection conn = this.fetch_URL.openConnection();
            conn.connect();
            Log.d(tag, "conn connected to "+fetch_URL);
            // getting file length

            this.expected_size = conn.getContentLength();
            Log.d(tag, "Expected size is "+this.expected_size);

            // input stream to read file - with 64k buffer
            InputStream input = new BufferedInputStream(fetch_URL.openStream(), 65536);
            Log.d(tag, "Created input stream");

            // Output stream to write file
            OutputStream output = new FileOutputStream(filename);
            Log.d(tag, "Created output stream");

            byte data[] = new byte[8192];

            this.status = Status.eDownloading;
            this.current_size = 0;
            while ((count = input.read(data)) != -1) {
                this.current_size += count;
                // writing data to file
                output.write(data, 0, count);
                if (this.stop)
                    throw new IOException("Stopped while running");
            }

            // flushing output
            output.flush();

            // closing streams
            output.close();
            input.close();
            if (this.current_size < this.expected_size)
                throw new RuntimeException("Less data received than expected.");

            this.status = Status.eComplete;

        } catch (Throwable e) {
            if (!this.stop)
                this.status = Status.eBroken;
            else
                this.status = Status.eStopped;
            java.io.File file = new java.io.File(filename);
            try {
                file.delete();
            } catch (Throwable err) {
                // do nothing
            }
            Log.e("Error: ", e.getMessage());
        }

        return null;
    }



    /**
     * After completing background task
     * **/
    @Override
    protected void onPostExecute(String file_url) {
        Log.d(tag, "BackgroundDownloadTask Ended");

        //pDialog.dismiss();
    }

}