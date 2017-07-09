package com.google.android.exoplayer2.videobench;

import android.os.Environment;
import android.util.Log;

import java.io.File;

/**
 * Created by chris on 09/07/17.
 */

public static class Utilities {
    // turn a filename into the local cache equivalent
    static String getLocalName(String localname) {
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        Log.d("getLocalName", "PublicDir is "+path);
        File file = new File(path, localname);
        Log.d("getLocalName", "FQ is "+file.toString());
        return file.toString();
    }
}
