package org.witness.sscphase1;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;

import java.io.IOException;

public class App extends Application {
    private static App gInstance;

    public static App getInstance() {
        return gInstance;
    }

    @Override
    public void onCreate() {
        gInstance = this;
        super.onCreate();

        // Create thumbnails for video
        //
        Picasso.setSingletonInstance(new Picasso.Builder(this)
                .addRequestHandler(new RequestHandler() {
                    @Override
                    public boolean canHandleRequest(Request data)
                    {
                        String scheme = data.uri.getScheme();
                        return ("video".equals(scheme));
                    }

                    @Override
                    public Result load(Request data, int arg1) throws IOException
                    {
                        String path = data.uri.getPath();
                        Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.MICRO_KIND);
                        return new Result(bitmap, Picasso.LoadedFrom.DISK);
                    }
                })
                //.indicatorsEnabled(true)
                .defaultBitmapConfig(Bitmap.Config.RGB_565)
                .build());
    }
}
