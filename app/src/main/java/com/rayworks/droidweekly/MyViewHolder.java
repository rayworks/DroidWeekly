package com.rayworks.droidweekly;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.rayworks.droidweekly.model.ArticleItem;

class MyViewHolder extends RecyclerView.ViewHolder {

    private final TextView title;
    private final TextView desc;
    private final ImageView imageView;
    private final ViewGroup imageParent;
    private View.OnClickListener clickListener;

    public MyViewHolder(View itemView) {
        super(itemView);

        title = itemView.findViewById(R.id.textViewTitle);
        desc = itemView.findViewById(R.id.textViewDescription);
        imageView = itemView.findViewById(R.id.imageView);
        imageParent = itemView.findViewById(R.id.image_parent);
    }

    public void bind(ArticleItem item) {
        Context context = itemView.getContext();

        int unit = TypedValue.COMPLEX_UNIT_PX;
        Resources resources = context.getResources();

        String imageUrl = item.imageUrl;
        if (!TextUtils.isEmpty(imageUrl)) {
            imageParent.setVisibility(View.VISIBLE);
            ViewCompat.setBackgroundTintList(imageParent, ColorStateList.valueOf(item.imgFrameColor));

            Glide.with(itemView).load(imageUrl).into(imageView);
        } else {
            imageParent.setVisibility(View.GONE);
        }

        if (TextUtils.isEmpty(item.description)) {
            title.setTextColor(Color.BLACK);
            title.setTextSize(unit, resources.getDimension(R.dimen.font_size_large));

            desc.setVisibility(View.GONE);
        } else {
            title.setTextSize(unit, resources.getDimension(R.dimen.font_size_normal));
            title.setTextColor(resources.getColor(R.color.light_blue));

            desc.setVisibility(View.VISIBLE);
        }

        desc.setTextSize(unit, resources.getDimension(R.dimen.font_size_small));

        title.setText(item.title);
        desc.setText(item.description);
    }
}
