package com.nduyuwilson.thitima.util;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class ImageUtils {

    public static String saveImageToInternalStorage(Context context, Uri uri) {
        if (uri == null) return "";
        
        // If it's already an internal path, return it
        if (uri.toString().contains(context.getFilesDir().getAbsolutePath())) {
            return uri.toString();
        }

        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            String fileName = "IMG_" + UUID.randomUUID().toString() + ".jpg";
            File file = new File(context.getFilesDir(), fileName);
            FileOutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();

            return Uri.fromFile(file).toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
}
