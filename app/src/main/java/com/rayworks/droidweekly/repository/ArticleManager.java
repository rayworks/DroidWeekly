package com.rayworks.droidweekly.repository;

import androidx.room.Room;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;

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
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class ArticleManager {
    public static final String PAST_ISSUES = "past-issues";
    public static final String LATEST_ISSUE = "latest-issue";
    public static final String ISSUE_HEADER = "issue-header";
    public static final String SECTIONS = "sections";
    public static final String TABLE = "table";
    public static final String ISSUE_INFO = "issue_info";
    public static final String LATEST_ISSUE_ID = "latest_issue_id";
    private static final String SITE_URL = "http://androidweekly.net"; // /issues/issue-302
    private static final String DROID_WEEKLY = "DroidWeekly";
    private static final int TIMEOUT_IN_SECOND = 10;
    private static final String DATABASE_NAME = "MyDatabase";
    private static final int ISSUE_ID_NONE = -1;

    private final OkHttpClient okHttpClient;
    private final Handler uiHandler;
    private final SharedPreferences preferences;
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

        uiHandler = new Handler(Looper.getMainLooper());

        Context context = App.getApp().getApplicationContext();
        preferences = context.getSharedPreferences(ISSUE_INFO, Context.MODE_PRIVATE);
        initStorage(context);
    }

    public static ArticleManager getInstance() {
        return ManagerHolder.articleManager;
    }

    private void initStorage(Context context) {
        // create database
        database = Room.databaseBuilder(context, IssueDatabase.class, DATABASE_NAME).build();
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

    public void search(String key, WeakReference<ArticleDataListener> listener) {
        Disposable disposable =
                Observable.just("%" + key + "%") // for the fuzzy search
                        .map(str -> database.articleDao().getArticleByKeyword(str))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .singleOrError()
                        .subscribe(
                                list -> {
                                    System.out.println(">>><<< matched item titles : ");
                                    for (Article article : list) {
                                        System.out.println(">>> article : " + article.getTitle());
                                    }
                                    System.out.println(">>><<< matched item titles");

                                    if (listener.get() != null) {
                                        listener.get().onComplete(getArticleModels(list));
                                    }
                                },
                                throwable -> {
                                    throwable.printStackTrace();
                                    if (listener.get() != null) {
                                        listener.get().onLoadError(throwable.getMessage());
                                    }
                                });
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
                                            rxFetchRemote(url, issueId);
                                        }
                                    },
                                    throwable -> {
                                        throwable.printStackTrace();
                                        rxFetchRemote(url, issueId);
                                    },
                                    () -> {});
        } else {
            rxFetchRemote(url, issueId);
        }
    }

    private void rxFetchRemote(String url, int issueId) {
        Single<Response> observable =
                Single.create(
                        subscriber -> {
                            try {
                                Request request = new Request.Builder().url(url).get().build();
                                Response response = okHttpClient.newCall(request).execute();

                                subscriber.onSuccess(response);
                            } catch (IOException e) {
                                subscriber.onError(e);
                            }
                        });

        Disposable disposable =
                observable
                        .doOnError(
                                throwable -> {
                                    int lastId = preferences.getInt(LATEST_ISSUE_ID, 0);
                                    if (lastId > 0 && issueId == ISSUE_ID_NONE) {
                                        List<Article> articleList =
                                                database.articleDao().getArticlesByIssue(lastId);

                                        if (articleList != null && articleList.size() > 0) {
                                            System.err.println(
                                                    ">>> cache hit for last issue id : " + lastId);
                                            notifyArticlesLoadedComplete(
                                                    getArticleModels(articleList));
                                        }
                                    }
                                })
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                                response -> {
                                    String data = response.body().string();
                                    processResponse(data, issueId);
                                },
                                throwable -> {
                                    throwable.printStackTrace();

                                    String message = throwable.getMessage();
                                    notifyErrorMsg(message);
                                });
    }

    private void processResponse(String data, int issueId) {
        Document doc = Jsoup.parse(data);

        List<OldItemRef> itemRefs = new LinkedList<>();
        Elements pastIssues = doc.getElementsByClass(PAST_ISSUES);

        if (!pastIssues.isEmpty()) { // contained only in the request for the latest issue
            Element passIssueGrp = pastIssues.get(0);
            Elements tags = passIssueGrp.getElementsByTag("ul");
            Element ulTag = tags.get(0);
            Elements liTags = ulTag.getElementsByTag("li");

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
        }

        Elements latestIssues = doc.getElementsByClass(LATEST_ISSUE);
        Elements currentIssues = doc.getElementsByClass("issue");

        if (!latestIssues.isEmpty()) {
            int latestId = 0;
            Element issue = latestIssues.get(0);
            Elements headers = issue.getElementsByClass(ISSUE_HEADER);

            if (!headers.isEmpty()) {
                Element header = headers.get(0);

                // #308
                String latestIssueId =
                        header.getElementsByClass("clearfix")
                                .get(0)
                                .getElementsByTag("span")
                                .text();

                if (latestIssueId.startsWith("#")) {
                    latestId = Integer.parseInt(latestIssueId.substring(1));
                }

                // build the issue menu items
                itemRefs.add(
                        0, new OldItemRef("Issue " + latestIssueId, "issues/issue-" + latestId));

                preferences.edit().putInt(LATEST_ISSUE_ID, latestId).apply();

                uiHandler.post(
                        () -> {
                            if (dataListener != null && dataListener.get() != null) {
                                dataListener.get().onOldRefItemsLoaded(itemRefs);
                            }
                        });

                List<Article> articles = database.articleDao().getArticlesByIssue(latestId);
                if (articles.size() > 0) {
                    // the latest issue content cache hits.
                    System.out.println(">>> cache hit for issue id : " + latestId);
                    notifyArticlesLoadedComplete(getArticleModels(articles));
                    return;
                }
            }

            Elements sections = issue.getElementsByClass(SECTIONS);
            if (!sections.isEmpty()) {
                Elements tables = sections.get(0).getElementsByTag(TABLE);
                System.out.println(">>> table size: " + tables.size());

                final List<ArticleItem> articleItems = parseArticleItems(tables);
                notifyArticlesLoadedComplete(articleItems);

                List<Article> entities = getArticleEntities(latestId, articleItems);
                database.articleDao().insertAll(entities);
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

            articleItem.setImgFrameColor(article.getImgFrameColor());
            articleItem.setImageUrl(article.getImageUrl());

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
            Article article = new Article(item.hashCode(),
                    item.getTitle(),item.getDescription(),
                    item.getLinkage(),  item.getImageUrl() == null ? "" : item.getImageUrl(),
                    item.getImgFrameColor(), issueId, index);

           /* article.setTitle(item.getTitle());
            article.setDescription(item.getDescription());
            article.setImageUrl(item.getImageUrl());
            article.setImgFrameColor(item.getImgFrameColor());
            article.setLinkage(item.getLinkage());
            article.setOrder(index);
            article.setIssueId(issueId);*/

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
                    articleItem.setImageUrl(imageElem.attr("src"));

                    String style = imageElem.attr("style");
                    int begPos = style.indexOf("border");
                    if (begPos >= 0) {
                        int startPos = style.indexOf('#', begPos);
                        int endPos = style.indexOf(";", startPos);

                        if (startPos >= 0 && endPos >= 0) {
                            articleItem.setImgFrameColor(
                                    Color.parseColor(style.substring(startPos, endPos)));
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

                    articleItem.setTitle(text);
                    articleItem.setLinkage(href);
                }
            }

            Elements paragraphs = element.getElementsByTag("p");
            if (!paragraphs.isEmpty()) {
                String description = paragraphs.get(0).text();
                articleItem.setDescription(description);

                System.out.println(">>> HEAD_DESC: " + description);
            }

            Element title = element.selectFirst("h2");
            if (title != null) {
                String text = title.text();
                System.out.println(">>>" + text);
                articleItem.setTitle(text);
            } else { // tag Sponsored
                title = element.selectFirst("h5");

                if (title != null) {
                    articleItem.setTitle(title.text());
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
