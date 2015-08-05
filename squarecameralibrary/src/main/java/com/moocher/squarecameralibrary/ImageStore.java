package com.moocher.squarecameralibrary;

import android.net.Uri;
import android.provider.MediaStore;

/**
 * Created by moocher on 2015/8/5.
 */
public class ImageStore {
    private static final String TAG = "ImageStore";
    public static final String SQUARE_LENGTH_KEY = "square_length";
    public static final String SQUARE_EXTRA_OUTPUT_KEY = "extra_output";
    //album field.
    public static final Uri IMAGES_CONTENT_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    public static final String IMAGES_BUCKET_NAME = MediaStore.Images.Media.BUCKET_DISPLAY_NAME;
    public static final String IMAGES_DISPLAY_NAME = MediaStore.Images.Media.DISPLAY_NAME;
    public static final String IMAGES_PATH = MediaStore.Images.Media.DATA;

}
