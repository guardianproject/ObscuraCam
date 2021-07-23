package org.witness.obscuracam.ui.adapters;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.picasso.Picasso;

import org.witness.sscphase1.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;


public class AlbumAdapter extends RecyclerView.Adapter<AlbumViewHolder> {

    private static final boolean LOGGING = false;
    private static final String LOGTAG = "AlbumAdapter";

    public interface AlbumAdapterListener {
        void onAlbumSelected(String id, String albumName);
        void onPickExternalSelected();
    }

    private final Context mContext;
    private final ArrayList<AlbumInfo> mAlbums;
    private final boolean mShowPickExternal;
    private AlbumAdapterListener mListener;

    public AlbumAdapter(Context context, boolean showPickExternal) {
        super();
        mContext = context;
        mAlbums = new ArrayList<>();
        mShowPickExternal = showPickExternal;
        getAlbums();
    }

    public void setListener(AlbumAdapterListener listener) {
        mListener = listener;
    }

    private void getThumbnailAndCountForAlbum(AlbumInfo album) {
        album.thumbnail = null;
        album.count = 0;
        try {
            final String orderBy = MediaStore.Images.Media.DATE_TAKEN;
            String searchParams = MediaStore.Images.Media.BUCKET_ID + " = \"" + album.id + "\"";

            Cursor photoCursor = mContext.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    null, searchParams, null, orderBy + " DESC");
            if (photoCursor.getCount() > 0) {
                album.count = photoCursor.getCount();
                photoCursor.moveToNext();
                int colIndexUri = photoCursor.getColumnIndex(MediaStore.MediaColumns.DATA);
                album.thumbnail = photoCursor.getString(colIndexUri);
            }
            photoCursor.close();
        } catch (Exception e) {
            e.printStackTrace();
            if (LOGGING)
                Log.e(LOGTAG, "Failed to get album info: " + e.toString());
        }
    }

    private void getAlbums() {

        mAlbums.clear();
        try {
            String[] PROJECTION_BUCKET = {MediaStore.Images.ImageColumns.BUCKET_ID,
                    MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME, MediaStore.Images.ImageColumns.DATE_TAKEN,
                    MediaStore.Images.ImageColumns.DATA};

            String BUCKET_GROUP_BY = "1) GROUP BY 1,(2";
            String BUCKET_ORDER_BY = "MAX(datetaken) DESC";

            Uri images = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

            Cursor cur = mContext.getContentResolver().query(images, PROJECTION_BUCKET,
                    BUCKET_GROUP_BY, null, BUCKET_ORDER_BY);

            if (cur.moveToFirst()) {
                String bucket;
                String bucketId;
                int bucketColumn = cur.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
                int bucketIdColumn = cur.getColumnIndex(MediaStore.Images.Media.BUCKET_ID);
                do {
                    // Get the field values
                    bucket = cur.getString(bucketColumn);
                    bucketId = cur.getString(bucketIdColumn);
                    if (!TextUtils.isEmpty(bucketId)) {
                        AlbumInfo album = new AlbumInfo();
                        album.id = bucketId;
                        album.albumName = bucket;
                        mAlbums.add(album);
                    }
                } while (cur.moveToNext());
            }
            cur.close();
        } catch (Exception e) {
            if (LOGGING)
                Log.e(LOGTAG, "Failed to get albums: " + e.toString());
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (mShowPickExternal) {
            if (position == 0)
                return 1;
        }
        return 0;
    }

    @Override
    public AlbumViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(viewType == 0 ? R.layout.album_item : R.layout.album_external_item, parent, false);
        return new AlbumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(AlbumViewHolder holder, int position) {
        if (mShowPickExternal) {
            if (position == 0) {
                holder.mRootView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mListener != null) {
                            mListener.onPickExternalSelected();
                        }
                    }
                });
                return;
            } else {
                position--; //Offset by this item
            }
        }
        holder.mRootView.setOnClickListener(new ItemClickListener(position));
        AlbumInfo album = mAlbums.get(position);
        holder.mAlbumName.setText(album.albumName);
        holder.mAlbumCount.setText(String.format(Locale.getDefault(), "(%d)", album.count));
        try {
            holder.mAlbumThumbnail.setBackgroundResource(0);
            try {
                Picasso.get()
                        .load(new File(album.thumbnail))
                        .fit()
                        .centerCrop()
                        .into(holder.mAlbumThumbnail);
            } catch (Exception ignored) {}
        } catch (Exception e) {
            holder.mAlbumThumbnail.setBackgroundResource(R.drawable.camera_frame);
            holder.mAlbumThumbnail.setImageDrawable(null);
        }

        if (album.thumbnail == null && album.count == 0) {
            // Fetch info
            AsyncTask<AlbumInfo,Void,AlbumInfo> task = new AsyncTask<AlbumInfo, Void, AlbumInfo>() {
                @Override
                protected AlbumInfo doInBackground(AlbumInfo... albumInfos) {
                    getThumbnailAndCountForAlbum(albumInfos[0]);
                    return albumInfos[0];
                }

                @Override
                protected void onPostExecute(AlbumInfo albumInfo) {
                    super.onPostExecute(albumInfo);
                    AlbumAdapter.this.notifyItemChanged((mShowPickExternal ? 1 : 0) + mAlbums.indexOf(albumInfo));
                }
            }.execute(album);
        }
    }

    @Override
    public int getItemCount() {
        return (mShowPickExternal ? 1 : 0) + mAlbums.size();
    }

    private class AlbumInfo {
        public AlbumInfo() {
        }
        public String id;
        public String albumName;
        public String thumbnail;
        public int count;
    }

    private class ItemClickListener implements View.OnClickListener {
        private final int mPosition;

        public ItemClickListener(int position) {
            mPosition = position;
        }

        @Override
        public void onClick(View view) {
            AlbumInfo album = mAlbums.get(mPosition);
            if (mListener != null) {
                mListener.onAlbumSelected(album.id, album.albumName);
            }
        }
    }
}

