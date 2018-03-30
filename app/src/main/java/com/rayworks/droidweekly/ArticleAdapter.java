package com.rayworks.droidweekly;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
    private View.OnClickListener onItemClickCallback;

    public ArticleAdapter(Context context, List<ArticleItem> items) {
        this.context = context;
        this.articleItems.addAll(items);
    }

    public void update(List<ArticleItem> itemList) {
        final int preSize = articleItems.size();
        if (preSize > 0) {
            articleItems.clear();
        }

        Disposable disposable = Observable.fromIterable(itemList)
                .filter(item ->
                        !TextUtils.isEmpty(item.title)
                )
                .subscribe(item -> articleItems.add(item),
                        Throwable::printStackTrace,
                        () -> {
                            if (preSize == 0) {
                                notifyItemRangeInserted(0, articleItems.size());
                            } else {
                                // as data updated
                                notifyDataSetChanged();
                            }
                        }
                );
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

        holder.itemView.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(item.linkage)) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(item.linkage));
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return articleItems.size();
    }

}
