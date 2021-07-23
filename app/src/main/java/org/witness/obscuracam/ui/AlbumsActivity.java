package org.witness.obscuracam.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import org.witness.obscuracam.ui.adapters.AlbumAdapter;
import org.witness.obscuracam.ui.adapters.GalleryCursorRecyclerViewAdapter;
import org.witness.sscphase1.R;

public class AlbumsActivity extends AppCompatActivity implements AlbumAdapter.AlbumAdapterListener, GalleryCursorRecyclerViewAdapter.GalleryCursorRecyclerViewAdapterListener {

    private static final boolean LOGGING = false;
    private static final String LOGTAG = "AlbumsActivity";

    private static final int PICK_EXTERNAL_REQUEST = 5;

    private View mRootView;
    private RecyclerView mRecyclerView;
    private AlbumLayoutManager mLayoutManager;
    private View mLayoutGalleryInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_albums);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mRootView = findViewById(R.id.main_content);

        mLayoutGalleryInfo = mRootView.findViewById(R.id.gallery_info);
        Button btnOk = (Button) mLayoutGalleryInfo.findViewById(R.id.btnGalleryInfoOk);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO App.getInstance().getSettings().setSkipGalleryInfo(true);
                mLayoutGalleryInfo.setVisibility(View.GONE);
            }
        });
        mLayoutGalleryInfo.setVisibility(View.GONE);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view_albums);
        int colWidth = getResources().getDimensionPixelSize(R.dimen.photo_column_size);
        mLayoutManager = new AlbumLayoutManager(this, colWidth);
        mRecyclerView.setLayoutManager(mLayoutManager);

        setAlbumAdapter();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mRecyclerView != null && mRecyclerView.getAdapter() instanceof GalleryCursorRecyclerViewAdapter) {
            ((GalleryCursorRecyclerViewAdapter) mRecyclerView.getAdapter()).update();
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (mRecyclerView.getAdapter() instanceof GalleryCursorRecyclerViewAdapter) {
                setAlbumAdapter();
                return true;
            }
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAlbumSelected(String id, String albumName) {
        getSupportActionBar().setTitle(albumName);
        setPhotosAdapter(id, false, false);
    }

    @Override
    public void onPickExternalSelected() {
        Intent intent = new Intent(Build.VERSION.SDK_INT >= 19 ? Intent.ACTION_OPEN_DOCUMENT : Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_EXTERNAL_REQUEST);
    }

    @Override
    public void onPhotoSelected(String photo, final View thumbView) {
        Intent data = new Intent();
        data.putExtra("uri", photo);
        data.putExtra("video", false);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void onVideoSelected(String video, final View thumbView) {
        Intent data = new Intent();
        data.putExtra("uri", video);
        data.putExtra("video", true);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void onCameraSelected() {
    }

    @Override
    public void onAlbumsSelected() {
    }

    @Override
    public void onBackPressed() {
        if (mRecyclerView.getAdapter() instanceof GalleryCursorRecyclerViewAdapter) {
            setAlbumAdapter();
            return;
        }
        super.onBackPressed();
    }

    private void setAlbumAdapter() {
        getSupportActionBar().setTitle(R.string.title_albums);
        mRecyclerView.setLayoutManager(mLayoutManager);
        AlbumAdapter adapter = new AlbumAdapter(this, true);
        adapter.setListener(this);
        int colWidth = getResources().getDimensionPixelSize(R.dimen.album_column_size);
        mLayoutManager.setColumnWidth(colWidth);
        mRecyclerView.setAdapter(adapter);
    }

    private void setPhotosAdapter(String album, boolean showCamera, boolean showAlbums) {
        mRecyclerView.setLayoutManager(mLayoutManager);
        GalleryCursorRecyclerViewAdapter adapter = new GalleryCursorRecyclerViewAdapter(this, album, showCamera, showAlbums);
        adapter.setListener(this);
        int colWidth = getResources().getDimensionPixelSize(R.dimen.photo_column_size);
        mLayoutManager.setColumnWidth(colWidth);
        mRecyclerView.setAdapter(adapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_EXTERNAL_REQUEST) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                onPhotoSelected(data.getData().toString(), null); //TODO - video
            }
        }
    }
}
