package org.witness.obscuracam.ui.adapters;

import android.database.Cursor;
import android.database.CursorWrapper;
import android.provider.MediaStore;

public class GalleryCursor extends CursorWrapper {

    private final int rowIdColumn;

    public GalleryCursor(Cursor cursor) {
        super(cursor);
        rowIdColumn = getColumnIndex(MediaStore.MediaColumns._ID);
    }

    public long getItemId(int position) {
        if (moveToPosition(position)) {
            return getLong(rowIdColumn);
        }
        return -1;
    }
}
