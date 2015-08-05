package com.moocher.squarecameralibrary;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by moocher on 2015/8/5.
 */
public class AlbumActivity extends AppCompatActivity {
    private static final String TAG = "AlbumActivity";

    private ImageView albumBack;

    private List<String> showPhotoList = new ArrayList<>();
    private AlbumGridAdapter albumGridAdapter;
    private GridView albumGridView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);

        initData();

        initViewsAndListener();
        if(albumGridAdapter == null) {
            albumGridAdapter = new AlbumGridAdapter(this, showPhotoList);
            albumGridView.setAdapter(albumGridAdapter);
        }

    }

    private void initData(){
        Intent intent = getIntent();
        List<String> folder = intent.getStringArrayListExtra("folder");
        List<String> path = intent.getStringArrayListExtra("path");
        showPhotoList.clear();
        showPhotoList.addAll(path);
    }

    private void initViewsAndListener(){
        albumBack = (ImageView)findViewById(R.id.album_back);
        albumBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        albumGridView = (GridView)findViewById(R.id.album_grid);
        albumGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String imagePath = showPhotoList.get(i);
                Intent intent = getIntent();
                intent.putExtra("imagePath", imagePath);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    class AlbumGridAdapter extends BaseAdapter {

        private List<String> mList;
        private Context mContext;
        private LayoutInflater mLayoutInflater;

        public AlbumGridAdapter(Context context, List<String> list){
            mContext = context;
            mList = list;
            mLayoutInflater = LayoutInflater.from(mContext);
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if(view == null){
                view = mLayoutInflater.inflate(R.layout.album_list_item, viewGroup, false);
            }
            ImageView itemImage = (ImageView)view.findViewById(R.id.album_item_image);
            String imagePath = mList.get(i);
            Glide.with(mContext)
                    .load(imagePath)
                    .override(300, 300)
                    .centerCrop()
                    .crossFade()
                    .into(itemImage);
//            BitmapFactory.Options options = new BitmapFactory.Options();
//            options.inJustDecodeBounds = true;
//            BitmapFactory.decodeFile(imagePath, options);
//            options.inJustDecodeBounds = false;
//            float width = options.outWidth;
//            float height = options.outHeight;
//            int w = 200;
//            int sampleSize;
//            if(width > height){
//                sampleSize = (int)height / w;
//            }else {
//                sampleSize = (int)width / w;
//            }
//            if(sampleSize < 1){
//                sampleSize = 1;
//            }
//            options.inSampleSize = sampleSize;
//            Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);
//            itemImage.setImageBitmap(bitmap);

            return view;
        }
    }

}
