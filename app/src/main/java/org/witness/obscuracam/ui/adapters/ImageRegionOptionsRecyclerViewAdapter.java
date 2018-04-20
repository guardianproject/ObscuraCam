package org.witness.obscuracam.ui.adapters;

import android.content.Context;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.witness.obscuracam.ui.ImageRegion;
import org.witness.sscphase1.R;

public class ImageRegionOptionsRecyclerViewAdapter extends RecyclerView.Adapter<ImageRegionOptionsRecyclerViewAdapter.OptionViewHolder> {
    private static final String LOGTAG = "ImageRegionOptionsRVAdapter";
    private static final boolean LOGGING = false;

    public interface ImageRegionOptionsRecyclerViewAdapterListener {
        void onOptionSelected(int operation);
    }

    private final Context mContext;
    private ImageRegionOptionsRecyclerViewAdapterListener mListener;
    private int mCurrentItem = 0;

    private class ObscureOption {
        public int name;
        public int icon;
        public int operation;
        public ObscureOption(int name, int icon, int operation) {
            this.name = name;
            this.icon = icon;
            this.operation = operation;
        }
    }

    private ObscureOption[] options = new ObscureOption[] {
        new ObscureOption(R.string.obscure_option_redact, R.drawable.ic_context_fill, ImageRegion.REDACT),
        new ObscureOption(R.string.obscure_option_pixelate, R.drawable.ic_context_pixelate, ImageRegion.PIXELATE),
        new ObscureOption(R.string.obscure_option_inverse, R.drawable.ic_context_pixelate_crowd, ImageRegion.BG_PIXELATE),
        new ObscureOption(R.string.obscure_option_mask, R.drawable.ic_context_mask, ImageRegion.MASK),
            new ObscureOption(R.string.mode_equalais, R.drawable.equalais, ImageRegion.EQUALAIS),
            new ObscureOption(R.string.obscure_option_remove, R.drawable.ic_context_delete, -1)
    };

    public ImageRegionOptionsRecyclerViewAdapter(Context context) {
        super();
        mContext = context;
        setHasStableIds(true);
    }

    protected Context getContext() {
        return mContext;
    }

    public void setListener(ImageRegionOptionsRecyclerViewAdapterListener listener) {
        mListener = listener;
    }

    public void setCurrentItem(int operation) {
        mCurrentItem = operation;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return options.length;
    }

    @Override
    public long getItemId(int position) {
        return options[position].name;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public OptionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.region_option_item, parent, false);
        return new OptionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(OptionViewHolder holder, int position) {
        ObscureOption option = options[position];
        holder.name.setText(option.name);
        holder.itemView.setOnClickListener(new ItemClickListener(option));
        if (option.operation == mCurrentItem) {
            TextViewCompat.setTextAppearance(holder.name, R.style.TextAppearanceObscureOptionSelected);
            holder.icon.setBackgroundResource(R.drawable.region_option_bg_selected);
        } else {
            TextViewCompat.setTextAppearance(holder.name, R.style.TextAppearanceObscureOption);
            holder.icon.setBackgroundResource(R.drawable.region_option_bg);
        }
        holder.icon.setImageResource(option.icon);
    }

    private class ItemClickListener implements View.OnClickListener {
        private final ObscureOption option;

        public ItemClickListener(ObscureOption option) {
            this.option = option;
        }

        @Override
        public void onClick(View view) {
            if (mListener != null) {
                mListener.onOptionSelected(this.option.operation);
            }
        }
    }

    class OptionViewHolder extends RecyclerView.ViewHolder {
        public final ImageView icon;
        public final TextView name;
        public OptionViewHolder(View itemView) {
            super(itemView);
            icon = (ImageView) itemView.findViewById(R.id.icon);
            name = (TextView) itemView.findViewById(R.id.name);
        }
    }
}
