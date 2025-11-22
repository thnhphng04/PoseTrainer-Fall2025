package fpt.fall2025.posetrainer.Activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.List;

import fpt.fall2025.posetrainer.R;

public class ImageGalleryActivity extends AppCompatActivity {
    public static final String EXTRA_IMAGES = "images";
    public static final String EXTRA_POSITION = "position";

    private ViewPager2 viewPager;
    private ImageButton btnClose;
    private TextView tvImageCount;
    private List<String> imageUrls;
    private int currentPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_gallery);

        imageUrls = getIntent().getStringArrayListExtra(EXTRA_IMAGES);
        currentPosition = getIntent().getIntExtra(EXTRA_POSITION, 0);

        if (imageUrls == null || imageUrls.isEmpty()) {
            finish();
            return;
        }

        viewPager = findViewById(R.id.viewPager);
        btnClose = findViewById(R.id.btnClose);
        tvImageCount = findViewById(R.id.tvImageCount);

        if (imageUrls.size() > 1) {
            tvImageCount.setVisibility(View.VISIBLE);
            tvImageCount.setText((currentPosition + 1) + " / " + imageUrls.size());
        } else {
            tvImageCount.setVisibility(View.GONE);
        }

        viewPager.setAdapter(new GalleryAdapter(imageUrls));
        viewPager.setCurrentItem(currentPosition, false);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentPosition = position;
                tvImageCount.setText((position + 1) + " / " + imageUrls.size());
            }
        });

        btnClose.setOnClickListener(v -> finish());
    }

    private static class GalleryAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<GalleryVH> {
        private final List<String> imageUrls;

        public GalleryAdapter(List<String> imageUrls) {
            this.imageUrls = imageUrls;
        }

        @NonNull
        @Override
        public GalleryVH onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_gallery_image_fullscreen, parent, false);
            return new GalleryVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull GalleryVH holder, int position) {
            holder.bind(imageUrls.get(position));
        }

        @Override
        public int getItemCount() {
            return imageUrls.size();
        }
    }

    private static class GalleryVH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        private final android.widget.ImageView ivImage;
        private final android.widget.ProgressBar progressBar;

        public GalleryVH(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivImage);
            progressBar = itemView.findViewById(R.id.progressBar);
        }

        public void bind(String imageUrl) {
            progressBar.setVisibility(View.VISIBLE);
            Glide.with(itemView.getContext())
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(ivImage);
            progressBar.setVisibility(View.GONE);
        }
    }
}

