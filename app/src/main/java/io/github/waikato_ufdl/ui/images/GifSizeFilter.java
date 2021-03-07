package io.github.waikato_ufdl.ui.images;


import android.content.Context;
import android.graphics.Point;

import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.filter.Filter;
import com.zhihu.matisse.internal.entity.IncapableCause;
import com.zhihu.matisse.internal.entity.Item;
import com.zhihu.matisse.internal.utils.PhotoMetadataUtils;

import java.util.HashSet;
import java.util.Set;

import io.github.waikato_ufdl.R;

/***
 *  this class is required by the Matisse Library in order to constrain the mime types as well as define how big preview images are.
 */

class GifSizeFilter extends Filter {

    private final int mMinWidth;
    private final int mMinHeight;
    private final int mMaxSize;

    /***
     * Constructor for the gif size filter
     * @param minWidth minimum width for preview image
     * @param minHeight minimum height for preview image
     * @param maxSizeInBytes maximum size of preview image
     */
    GifSizeFilter(int minWidth, int minHeight, int maxSizeInBytes) {
        mMinWidth = minWidth;
        mMinHeight = minHeight;
        mMaxSize = maxSizeInBytes;
    }

    /***
     * Method to create the set of Mime Types
     * @return set of Mime Types
     */
    @Override
    public Set<MimeType> constraintTypes() {
        return new HashSet<MimeType>() {{
            add(MimeType.GIF);
            add(MimeType.MP4);
        }};
    }

    /***
     * A method to filter the item content to match the defined bounds
     * @param context the context
     * @param item the item which contains the image
     * @return null if successful
     */
    @Override
    public IncapableCause filter(Context context, Item item) {
        if (!needFiltering(context, item))
            return null;

        Point size = PhotoMetadataUtils.getBitmapBound(context.getContentResolver(), item.getContentUri());
        if (size.x < mMinWidth || size.y < mMinHeight || item.size > mMaxSize) {
            return new IncapableCause(IncapableCause.DIALOG, context.getString(R.string.error_gif, mMinWidth,
                    String.valueOf(PhotoMetadataUtils.getSizeInMB(mMaxSize))));
        }
        return null;
    }
}


