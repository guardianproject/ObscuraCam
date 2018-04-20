package org.witness.obscuracam.photo.filters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.Properties;

import edu.mit.media.equalais.Equalais;

public class EqualaisObscure implements RegionProcesser {

    Paint mPainter;
    Context mContext;

    RectF mRect;
    Bitmap mLastBmp;

    public EqualaisObscure (Context context, Paint painter)
    {
        mContext = context;
        mPainter = painter;
    }

    @Override
    public void processRegion(final RectF rect, final Canvas canvas, final Bitmap originalBmp) {

        if (mRect != null && mRect.equals(rect))
        {
            if (mLastBmp != null)
            {
                canvas.drawBitmap(mLastBmp, null, rect, mPainter);
                return;
            }
        }

        mRect = rect;

        // The Very Basic
        new AsyncTask<Void, Void, byte[]>() {
            protected void onPreExecute() {
                // Pre Code
            }
            protected byte[] doInBackground(Void... unused) {
                // Background Code
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                try {
                    Bitmap rectBitmap = Bitmap.createBitmap(originalBmp, (int) rect.left, (int) rect.top, (int) rect.width(), (int) rect.height());
                    rectBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

                    Equalais equalais = new Equalais();
                    return equalais.perturb(baos.toByteArray());
                }
                catch (Exception e)
                {
                    Log.e("Equalais","Error perturbing!",e);
                    return null;
                }

            }
            protected void onPostExecute(byte[] resp) {
                // Post Code
                if (resp != null)
                {
                    mLastBmp = BitmapFactory.decodeByteArray(resp, 0, resp.length);
                    canvas.drawBitmap(mLastBmp, null, rect, mPainter);
                }
            }
        }.execute();
    }

    @Override
    public Properties getProperties() {
        return null;
    }

    @Override
    public Bitmap getBitmap() {
        return null;
    }

    @Override
    public void setProperties(Properties props) {

    }
}
