package edu.mit.media.equalais;

import android.util.Base64;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Equalais {

    private final static String ENDPOINT = "https://equal-ais.appspot.com";
    private final static MediaType MEDIA_TYPE_JPG = MediaType.parse("image/jpg");

    public byte[] perturb (byte[] faceJpeg)
    {

        OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", "face.jpg",
                        RequestBody.create(MEDIA_TYPE_JPG, faceJpeg))
                .build();

        // Create request for remote resource.
        Request request = new Request.Builder()
                .url(ENDPOINT)
                .post(requestBody)
                .build();

        try  {
            Response response = client.newCall(request).execute();

            if (response.isSuccessful()) {

                 String eqFace = response.body().string();

                byte[] resp = Base64.decode(eqFace,Base64.DEFAULT);

                return resp;


            }

        }
        catch (IOException ioe)
        {
            Log.e(getClass().getName(),"error making post request",ioe);
        }

        return null;
    }
}
