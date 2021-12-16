package com.rayworks.droidweekly.ui.component;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rayworks.droidweekly.R;
import com.rayworks.droidweekly.dashboard.interfaces.OnViewArticleListener;
import com.rayworks.droidweekly.model.ArticleItem;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class ArticleAdapter extends RecyclerView.Adapter<MyViewHolder> {
    private List<ArticleItem> articleItems = new ArrayList<>();
    private Context context;
    private OnViewArticleListener viewArticleListener;

    public ArticleAdapter(Context context, List<ArticleItem> items) {
        this.context = context;
        this.articleItems.addAll(items);
    }

    public ArticleAdapter setViewArticleListener(OnViewArticleListener viewArticleListener) {
        this.viewArticleListener = viewArticleListener;
        return this;
    }

    public void update(List<ArticleItem> itemList) {
        if (itemList == null) return;

        articleItems = itemList.stream()
                .filter(it -> !TextUtils.isEmpty(it.getTitle()))
                .collect(Collectors.toList());

        // as data updated
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.view_news_item, parent, false);

        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ArticleItem item = articleItems.get(position);
        holder.bind(item);

        holder.itemView.setOnClickListener(
                v -> {
                    String linkage = item.getLinkage();

                    if (!TextUtils.isEmpty(linkage) && viewArticleListener != null) {
                        viewArticleListener.onView(linkage);
                    }
                });
    }

    @Override
    public int getItemCount() {
        return articleItems.size();
    }

}
