package com.rayworks.droidweekly.repository;

import android.arch.persistence.room.Room;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.rayworks.droidweekly.App;
import com.rayworks.droidweekly.model.ArticleItem;
import com.rayworks.droidweekly.model.OldItemRef;
import com.rayworks.droidweekly.repository.database.IssueDatabase;
import com.rayworks.droidweekly.repository.database.entity.Article;

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
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class ArticleManager {
    private static final String SITE_URL = "http://androidweekly.net"; // /issues/issue-302
    private static final String DROID_WEEKLY = "DroidWeekly";
    private static final int TIMEOUT_IN_SECOND = 10;
    private static final String DATABASE_NAME = "MyDatabase";
    private static final int ISSUE_ID_NONE = -1;
    private final OkHttpClient okHttpClient;
    private final ExecutorService executorService;
    private final Handler uiHandler;

    private WeakReference<ArticleDataListener> dataListener;

    // To be injected
    private IssueDatabase database;

    private ArticleManager() {
        okHttpClient =
                new OkHttpClient.Builder()
                        .readTimeout(TIMEOUT_IN_SECOND, TimeUnit.SECONDS)
                        .writeTimeout(10, TimeUnit.SECONDS)
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .addInterceptor(new AgentInterceptor(DROID_WEEKLY))
                        .build();

        executorService = Executors.newSingleThreadExecutor();

        uiHandler = new Handler(Looper.getMainLooper());

        initStorage();
    }

    public static ArticleManager getInstance() {
        return ManagerHolder.articleManager;
    }

    private void initStorage() {
        // create database
        database = Room.databaseBuilder(App.getApp(), IssueDatabase.class, DATABASE_NAME).build();
    }

    public ArticleManager setDataListener(ArticleDataListener dataListener) {
        this.dataListener = new WeakReference<>(dataListener);
        return this;
    }

    public void loadData() {
        load(SITE_URL, ISSUE_ID_NONE);
    }

    public void loadData(String urlSubPath) {
        int id = ISSUE_ID_NONE;

        // format like : issues/issue-302
        String[] segments = urlSubPath.split("-");
        if (segments.length > 0) {
            int index = segments.length - 1;
            id = Integer.parseInt(segments[index]);
            System.out.println(">>> found issue id : " + id);
        }

        load(SITE_URL + urlSubPath, id);
    }

    private void load(final String url, final int issueId) {
        // check the cache first
        if (issueId > 0) {
            Disposable disposable =
                    Observable.just(issueId)
                            .map(id -> database.articleDao().getArticlesByIssue(id))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    articleList -> {
                                        if (articleList != null && articleList.size() > 0) {
                                            System.out.println(
                                                    ">>> cache hit for issue id : " + issueId);
                                            notifyArticlesLoadedComplete(
                                                    getArticleModels(articleList));
                                        } else {
                                            fetchFromRemote(url, issueId);
                                        }
                                    },
                                    throwable -> {
                                        throwable.printStackTrace();
                                        fetchFromRemote(url, issueId);
                                    },
                                    () -> {});
        } else {
            fetchFromRemote(url, issueId);
        }
    }

    private void fetchFromRemote(String url, int issueId) {
        executorService.submit(
                () -> {
                    final Request request = new Request.Builder().url(url).get().build();

                    okHttpClient
                            .newCall(request)
                            .enqueue(
                                    new Callback() {
                                        @Override
                                        public void onFailure(Call call, IOException e) {
                                            e.printStackTrace();
                                            String message = e.getMessage();

                                            notifyErrorMsg(message);
                                        }

                                        @Override
                                        public void onResponse(Call call, Response response)
                                                throws IOException {
                                            String data = response.body().string();
                                            processResponse(data, issueId);
                                        }
                                    });
                });
    }

    private void processResponse(String data, int issueId) {
        Document doc = Jsoup.parse(data);

        Elements pastIssues = doc.getElementsByClass("past-issues");
        if (!pastIssues.isEmpty()) {
            Element passIssueGrp = pastIssues.get(0);
            Elements tags = passIssueGrp.getElementsByTag("ul");
            Element ulTag = tags.get(0);
            Elements liTags = ulTag.getElementsByTag("li");

            final List<OldItemRef> itemRefs = new LinkedList<>();
            int cnt = liTags.size();
            for (int i = 0; i < cnt; i++) {
                Element li = liTags.get(i);

                Elements elements = li.getElementsByTag("a");
                Element refItem = elements.get(0);

                OldItemRef oldItemRef = new OldItemRef(refItem.text(), refItem.attr("href"));
                System.out.println("<<< old issue: " + refItem.text());

                if (oldItemRef.getRelativePath().contains("issue-")) {
                    itemRefs.add(oldItemRef);
                }
            }

            uiHandler.post(
                    () -> {
                        if (dataListener != null && dataListener.get() != null) {
                            dataListener.get().onOldRefItemsLoaded(itemRefs);
                        }
                    });
        }

        Elements latestIssues = doc.getElementsByClass("latest-issue");
        Elements currentIssues = doc.getElementsByClass("issue");

        if (!latestIssues.isEmpty()) {
            Element issue = latestIssues.get(0);
            Elements sections = issue.getElementsByClass("sections");
            if (!sections.isEmpty()) {
                Elements tables = sections.get(0).getElementsByTag("table");
                System.out.println(">>> table size: " + tables.size());

                final List<ArticleItem> articleItems = parseArticleItems(tables);
                notifyArticlesLoadedComplete(articleItems);

            } else {
                notifyErrorMsg("Parsing failure: sections not found");
            }

        } else if (!currentIssues.isEmpty()) {
            List<ArticleItem> items = parseArticleItemsForIssue(doc);
            notifyArticlesLoadedComplete(items);

            List<Article> entities = getArticleEntities(issueId, items);

            database.articleDao().insertAll(entities);

        } else {
            notifyErrorMsg("Parsing failure: latest-issue not found");
        }
    }

    @NonNull
    private List<ArticleItem> getArticleModels(List<Article> articles) {
        List<ArticleItem> items = new LinkedList<>();
        for (Article article : articles) {
            ArticleItem articleItem =
                    new ArticleItem(
                            article.getTitle(), article.getDescription(), article.getLinkage());

            articleItem.imgFrameColor = article.getImgFrameColor();
            articleItem.imageUrl = article.getImageUrl();

            items.add(articleItem);
        }
        return items;
    }

    @NonNull
    private List<Article> getArticleEntities(int issueId, List<ArticleItem> items) {
        List<Article> entities = new LinkedList<>();
        int index = 0;
        for (ArticleItem item : items) {
            ++index;
            Article article = new Article();

            article.setTitle(item.title);
            article.setDescription(item.description);
            article.setImageUrl(item.imageUrl);
            article.setImgFrameColor(item.imgFrameColor);
            article.setLinkage(item.linkage);
            article.setOrder(index);
            article.setIssueId(issueId);

            entities.add(article);
        }
        return entities;
    }

    private void notifyArticlesLoadedComplete(List<ArticleItem> articleItems) {
        uiHandler.post(
                () -> {
                    if (dataListener != null && dataListener.get() != null) {
                        dataListener.get().onComplete(articleItems);
                    }
                });
    }

    private void notifyErrorMsg(String message) {
        uiHandler.post(
                () -> {
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

            Elements imageElems = element.getElementsByTag("img");
            if (!imageElems.isEmpty()) {
                Element imageElem = imageElems.get(0);

                if (imageElem != null) {
                    articleItem.imageUrl = imageElem.attr("src");

                    String style = imageElem.attr("style");
                    int begPos = style.indexOf("border");
                    if (begPos >= 0) {
                        int startPos = style.indexOf('#', begPos);
                        int endPos = style.indexOf(";", startPos);

                        if (startPos >= 0 && endPos >= 0) {
                            articleItem.imgFrameColor =
                                    Color.parseColor(style.substring(startPos, endPos));
                        }
                    }
                }
            }

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
            } else { // tag Sponsored
                title = element.selectFirst("h5");

                if (title != null) {
                    articleItem.title = title.text();
                }
            }

            articleItems.add(articleItem);
        }
        return articleItems;
    }

    public interface ArticleDataListener {
        void onLoadError(String err);

        void onOldRefItemsLoaded(List<OldItemRef> itemRefs);

        void onComplete(List<ArticleItem> items);
    }

    private static class ManagerHolder {
        private static ArticleManager articleManager = new ArticleManager();
    }
}
