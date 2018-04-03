package com.rayworks.droidweekly;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.rayworks.droidweekly.model.ArticleItem;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

public class ArticleAdapter extends RecyclerView.Adapter<MyViewHolder> {
    List<ArticleItem> articleItems = new ArrayList<>();
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

        final int preSize = articleItems.size();
        if (preSize > 0) {
            articleItems.clear();
        }

        Disposable disposable =
                Observable.fromIterable(itemList)
                        .filter(item -> !TextUtils.isEmpty(item.title))
                        .subscribe(
                                item -> articleItems.add(item),
                                Throwable::printStackTrace,
                                () -> {
                                    if (preSize == 0) {
                                        notifyItemRangeInserted(0, articleItems.size());
                                    } else {
                                        // as data updated
                                        notifyDataSetChanged();
                                    }
                                });
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
                    String linkage = item.linkage;

                    if (!TextUtils.isEmpty(linkage)) {
                        if (viewArticleListener != null) {
                            viewArticleListener.onView(linkage);
                        }
                    }
                });
    }

    @Override
    public int getItemCount() {
        return articleItems.size();
    }
}
