package org.witness.securesmartcam.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.witness.sscphase1.R;

public class AlbumViewHolder extends RecyclerView.ViewHolder {
    final View mRootView;
    final TextView mAlbumName;
    final TextView mAlbumCount;
    final ImageView mAlbumThumbnail;

    public AlbumViewHolder(View itemView) {
        super(itemView);
        this.mRootView = itemView;
        this.mAlbumName = (TextView) itemView.findViewById(R.id.tvAlbumName);
        this.mAlbumCount = (TextView) itemView.findViewById(R.id.tvAlbumCount);
        this.mAlbumThumbnail = (ImageView) itemView.findViewById(R.id.ivAlbumThumbnail);
    }
}
