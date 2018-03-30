package com.rayworks.droidweekly;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.rayworks.droidweekly.model.ArticleItem;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class ArticleManager {
    static final String SITE_URL = "http://androidweekly.net";// /issues/issue-302
    private final OkHttpClient okHttpClient;
    private final ExecutorService executorService;
    private final Handler uiHandler;

    private WeakReference<ArticleDataListener> dataListener;

    private ArticleManager() {
        okHttpClient = new OkHttpClient();
        executorService = Executors.newSingleThreadExecutor();

        uiHandler = new Handler(Looper.getMainLooper());
    }

    public static ArticleManager getInstance() {
        return ManagerHolder.articleManager;
    }

    public ArticleManager setDataListener(ArticleDataListener dataListener) {
        this.dataListener = new WeakReference<>(dataListener);
        return this;
    }

    public void loadData() {
        executorService.submit(() -> {
            final Request request = new Request.Builder()
                    .url(SITE_URL)
                    .get()
                    .build();

            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    String message = e.getMessage();

                    notifyErrorMsg(message);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String data = response.body().string();
                    Document doc = Jsoup.parse(data);

                    Elements latestIssues = doc.getElementsByClass("latest-issue");
                    if (!latestIssues.isEmpty()) {
                        Element issue = latestIssues.get(0);
                        Elements sections = issue.getElementsByClass("sections");
                        if (!sections.isEmpty()) {
                            Elements tables = sections.get(0).getElementsByTag("table");
                            System.out.println(">>> table size: " + tables.size());

                            final List<ArticleItem> articleItems = parseArticleItems(tables);

                            uiHandler.post(() -> {
                                if (dataListener != null && dataListener.get() != null) {
                                    dataListener.get().onComplete(articleItems);
                                }
                            });


                        } else {
                            notifyErrorMsg("Parsing failure: sections not found");
                        }

                    } else {
                        notifyErrorMsg("Parsing failure: latest-issue not found");
                    }
                }
            });
        });


        /*ArticleService service = new RetroJsoup.Builder()
                .url(url)
                .client(okHttpClient)
                .build()
                .create(ArticleService.class);

        Disposable disposable = service.articles()
                .toList()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        articleItems -> {
                            System.out.println(">>> " + articleItems.get(0));
                            System.out.println(Arrays.toString(articleItems.toArray()));

                        }, Throwable::printStackTrace);*/
    }

    private void notifyErrorMsg(String message) {
        uiHandler.post(() -> {
            if (dataListener.get() != null) {
                dataListener.get().onLoadError(message);
            }
        });
    }

    @NonNull
    private List<ArticleItem> parseArticleItemsForIssue(Document doc) {
        Elements issues = doc.getElementsByClass("issue");
        Elements tables = issues.get(0).getElementsByTag("table");

        List<ArticleItem> articleItems = parseArticleItems(tables);
        return articleItems;
    }

    @NonNull
    private List<ArticleItem> parseArticleItems(Elements tables) {
        List<ArticleItem> articleItems = new LinkedList<>();

        for (int i = 0; i < tables.size(); i++) {
            ArticleItem articleItem = new ArticleItem();

            Element element = tables.get(i);
            Elements elementsByClass = element.getElementsByClass("article-headline");

            if (!elementsByClass.isEmpty()) {
                Element headline = elementsByClass.get(0);

                if (headline != null) {
                    String text = headline.text();
                    System.out.println(">>> HEAD_LINE: " + text);

                    String href = headline.attr("href");
                    System.out.println(">>> HEAD_URL : " + href);

                    articleItem.title = text;
                    articleItem.linkage = href;
                }
            }

            Elements paragraphs = element.getElementsByTag("p");
            if (!paragraphs.isEmpty()) {
                String description = paragraphs.get(0).text();
                articleItem.description = description;

                System.out.println(">>> HEAD_DESC: " + description);
            }

            Element title = element.selectFirst("h2");
            if (title != null) {
                String text = title.text();
                System.out.println(">>>" + text);
                articleItem.title = text;
            }

            articleItems.add(articleItem);
        }
        return articleItems;
    }

    public interface ArticleDataListener {
        void onLoadError(String err);

        void onComplete(List<ArticleItem> items);
    }

    private static class ManagerHolder {
        private static ArticleManager articleManager = new ArticleManager();
    }
}
