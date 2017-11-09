package org.witness.obscuracam.ui.adapters;

import android.content.Context;
import android.database.Cursor;
import android.database.MergeCursor;
import android.database.SQLException;
import android.graphics.Color;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.picasso.Picasso;

import org.witness.sscphase1.R;

import java.io.File;

public class GalleryCursorRecyclerViewAdapter extends RecyclerView.Adapter<PhotoViewHolder> {
    private static final String LOGTAG = "GalleryCursorRVAdapter";
    private static final boolean LOGGING = false;

    private static final int CAMERA_ID = -100;
    private static final int ALBUMS_ID = -101;

    public interface GalleryCursorRecyclerViewAdapterListener {
        void onPhotoSelected(String photo, View thumbView);
        void onVideoSelected(String video, View thumbView);
        void onCameraSelected();
        void onAlbumsSelected();
    }

    private final Context mContext;
    private GalleryCursorRecyclerViewAdapterListener mListener;

    private UpdateTask mUpdateTask;
    private GalleryCursor mGalleryCursor;

    private final String mAlbum;
    private final boolean mShowCamera;
    private final boolean mShowAlbums;

    public GalleryCursorRecyclerViewAdapter(Context context, String album, boolean showCamera, boolean showAlbums) {
        super();
        mContext = context;
        setHasStableIds(true);
        mAlbum = album;
        mShowCamera = showCamera;
        mShowAlbums = showAlbums;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        updateCursor();
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        synchronized (this) {
            if (mGalleryCursor != null) {
                mGalleryCursor.close();
                mGalleryCursor = null;
            }
        }
    }

    protected Context getContext() {
        return mContext;
    }

    public void setListener(GalleryCursorRecyclerViewAdapterListener listener) {
        mListener = listener;
    }

    public void update() {
        updateCursor();
    }

    @Override
    public int getItemCount() {
        int count = (mShowCamera ? 1 : 0) + (mShowAlbums ? 1 : 0);
        if (mGalleryCursor != null) {
            count += mGalleryCursor.getCount();
        }
        return count;
    }

    @Override
    public long getItemId(int position) {
        if (mShowCamera) {
            if (position == 0) {
                return CAMERA_ID;
            } else {
                position--;
            }
        }
        if (mShowAlbums) {
            if (position == 0) {
                return ALBUMS_ID;
            } else {
                position--;
            }
        }
        if (mGalleryCursor != null) {
            return mGalleryCursor.getItemId(position);
        }
        return -1;
    }

    @Override
    public int getItemViewType(int position) {
        long itemId = getItemId(position);
        if (itemId == CAMERA_ID) {
            return 1;
        } else if (itemId == ALBUMS_ID) {
            return 2;
        }
        return 0;
    }

    @Override
    public PhotoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(viewType == 0 ? R.layout.photo_item : R.layout.photo_camera_item, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PhotoViewHolder holder, int position) {
        if (mShowCamera) {
            if (position == 0) {
                holder.mRootView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mListener != null) {
                            mListener.onCameraSelected();
                        }
                    }
                });
                return;
            } else {
                position--; //Offset by this item
            }
        }
        if (mShowAlbums) {
            if (position == 0) {
                holder.mPhoto.setImageResource(R.drawable.ic_photo_album_primary_36dp);
                holder.mRootView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mListener != null) {
                            mListener.onAlbumsSelected();
                        }
                    }
                });
                return;
            } else {
                position--; //Offset by this item
            }
        }

        String data = "";
        boolean isVideo = false; //TODO
        try {
            int dataColumn = mGalleryCursor.getColumnIndex(MediaStore.MediaColumns.DATA);
            int typeColumn = mGalleryCursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE);
            if (mGalleryCursor.moveToPosition(position)) {
                data = mGalleryCursor.getString(dataColumn);
                String type = mGalleryCursor.getString(typeColumn);
                isVideo = (type != null && type.toLowerCase().startsWith("video/"));
            }
        } catch (SQLException | IllegalStateException e) {
            if (LOGGING)
                e.printStackTrace();
        }

        holder.mPhoto.setBackgroundColor(Color.TRANSPARENT);
        holder.mRootView.setOnClickListener(new ItemClickListener(data, isVideo, holder.mPhoto));
        try {
            holder.mVideoIcon.setVisibility(isVideo ? View.VISIBLE : View.GONE);
            if (isVideo) {
                Picasso.with(mContext)
                        .load("video:" + data)
                        .placeholder(R.drawable.btn_preview)
                        .fit()
                        .centerCrop()
                        .into(holder.mPhoto);
            } else {
                Picasso.with(mContext)
                        .load(new File(data))
                        .fit()
                        .centerCrop()
                        .into(holder.mPhoto);
            }
        } catch (Exception ignored) {}
    }

    private void updateCursor() {
        if (mUpdateTask != null) {
            if (LOGGING)
                Log.d(LOGTAG, "Cancel update task");
            mUpdateTask.cancel(true);
        }
        mUpdateTask = new UpdateTask();
        mUpdateTask.execute();
    }

    private class UpdateTask extends AsyncTask<Void, Void, Cursor>
    {
        private Cursor cursor = null;

        UpdateTask()
        {
        }

        @Override
        protected Cursor doInBackground(Void... values)
        {
            if (LOGGING)
                Log.v(LOGTAG, "UpdateTask: doInBackground");

            cursor = null;
            Cursor photos = getMediaCursor(); // getPhotoCursor();
            Cursor videos = null; //getVideoCursor();
            if (photos != null && videos != null) {
                cursor = new MergeCursor(new Cursor[] { photos, videos });
            } else if (videos != null) {
                cursor = videos;
            } else if (photos != null) {
                cursor = photos;
            }
            return cursor;
        }

        private Cursor getPhotoCursor() {
            try {
                final String orderBy = MediaStore.Images.Media.DATE_TAKEN;
                String searchParams = null;
                if (!TextUtils.isEmpty(mAlbum)) {
                    searchParams = MediaStore.Images.Media.BUCKET_ID + " = \"" + mAlbum + "\"";
                }
                return mContext.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        null, searchParams, null, orderBy + " DESC");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        private Cursor getVideoCursor() {
            try {
                final String orderBy = MediaStore.Video.Media.DATE_TAKEN;
                String searchParams = null;
                if (!TextUtils.isEmpty(mAlbum)) {
                    searchParams = MediaStore.Video.Media.BUCKET_ID + " = \"" + mAlbum + "\"";
                }
                return mContext.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        null, searchParams, null, orderBy + " DESC");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        private Cursor getMediaCursor() {
            try {
                final String orderBy = MediaStore.MediaColumns.DATE_ADDED;
                String searchParams = "";
                boolean hasAlbum = !TextUtils.isEmpty(mAlbum);
                if (hasAlbum) {
                    searchParams = MediaStore.Video.Media.BUCKET_ID + " = \"" + mAlbum + "\"";
                }

                String[] projection = {
                        MediaStore.Files.FileColumns._ID,
                        MediaStore.Files.FileColumns.DATA,
                        MediaStore.Files.FileColumns.DATE_ADDED,
                        MediaStore.Files.FileColumns.MEDIA_TYPE,
                        MediaStore.Files.FileColumns.MIME_TYPE,
                        MediaStore.Files.FileColumns.TITLE
                };

                // Return only video and image metadata.
                if (hasAlbum) {
                    searchParams += " AND (";
                }
                searchParams += MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                        + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                        + " OR "
                        + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                        + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;
                if (hasAlbum) {
                    searchParams += ")";
                }

                return mContext.getContentResolver().query(MediaStore.Files.getContentUri("external"),
                        projection, searchParams, null, orderBy + " DESC");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            if (cursor != null) {
                if (LOGGING)
                    Log.d(LOGTAG, "Cancelled - Close cursor " + cursor.hashCode());
                cursor.close();
            }
        }

        @Override
        protected void onPostExecute(Cursor cursor)
        {
            synchronized (GalleryCursorRecyclerViewAdapter.this) {
                if (mUpdateTask == this)
                    mUpdateTask = null;
                if (LOGGING)
                    Log.v(LOGTAG, "UpdateTask: finished");
                GalleryCursor newCursor = null;
                if (cursor != null) {
                    newCursor = new GalleryCursor(cursor);
                }
                if (getCursor() != null) {
                    if (LOGGING)
                        Log.v(LOGTAG, "Old cursor set, Close cursor " + getCursor().hashCode());
                    getCursor().close();
                }
                if (LOGGING)
                    Log.v(LOGTAG, newCursor == null ? "No cursor" : ("Opened cursor " + newCursor.hashCode()));
                mGalleryCursor = newCursor;
                //if (newCursor != null)
                //    DatabaseUtils.dumpCursor((net.sqlcipher.Cursor) newCursor.getWrappedCursor());
                notifyDataSetChanged();
//                if (mListener != null) {
//                    mListener.onCursorUpdated();
//                }
            }
        }
    }

    public GalleryCursor getCursor() {
        return mGalleryCursor;
    }

    private class ItemClickListener implements View.OnClickListener {
        private final String mData;
        private final boolean mIsVideo;
        private final View mThumbView;

        public ItemClickListener(String data, boolean isVideo, View thumbView) {
            mData = data;
            mIsVideo = isVideo;
            mThumbView = thumbView;
        }

        @Override
        public void onClick(View view) {
            if (mListener != null) {
                if (mIsVideo) {
                    mListener.onVideoSelected(mData, mThumbView);
                } else {
                    mListener.onPhotoSelected(mData, mThumbView);
                }
            }
        }
    }
}
