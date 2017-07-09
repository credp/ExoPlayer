package com.google.android.exoplayer2.videobench;

import android.os.Environment;
import android.util.Log;

import java.io.File;

/**
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */

public class Utilities {
    // turn a filename into the local cache equivalent
    static String getLocalName(String localname) {
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        Log.d("getLocalName", "PublicDir is "+path);
        File file = new File(path, localname);
        Log.d("getLocalName", "FQ is "+file.toString());
        return file.toString();
    }
}
