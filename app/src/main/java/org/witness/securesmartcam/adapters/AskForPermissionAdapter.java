package org.witness.securesmartcam.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.witness.sscphase1.MainActivity;
import org.witness.sscphase1.ObscuraApp;
import org.witness.sscphase1.R;


public class AskForPermissionAdapter extends RecyclerView.Adapter<AskForPermissionAdapter.RootViewHolder> {
    private final MainActivity mActivity;

    public AskForPermissionAdapter(MainActivity parentActivity) {
        super();
        mActivity = parentActivity;
    }

    @Override
    public RootViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mActivity).inflate(R.layout.no_permission_item, parent, false);
        return new RootViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RootViewHolder holder, int position) {
        holder.mRootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mActivity.askForReadExternalStoragePermission();
            }
        });
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    public class RootViewHolder extends RecyclerView.ViewHolder {
        final View mRootView;
        public RootViewHolder(View itemView) {
            super(itemView);
            this.mRootView = itemView;
        }
    }
}

