package com.rayworks.droidweekly.data;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.rayworks.droidweekly.model.ArticleItem;
import com.rayworks.droidweekly.model.OldItemRef;

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

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class ArticleManager {
    public static final String DROID_WEEKLY = "DroidWeekly";
    public static final int TIMEOUT_IN_SECOND = 10;
    static final String SITE_URL = "http://androidweekly.net"; // /issues/issue-302
    private final OkHttpClient okHttpClient;
    private final ExecutorService executorService;
    private final Handler uiHandler;

    private WeakReference<ArticleDataListener> dataListener;

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
    }

    public static ArticleManager getInstance() {
        return ManagerHolder.articleManager;
    }

    public ArticleManager setDataListener(ArticleDataListener dataListener) {
        this.dataListener = new WeakReference<>(dataListener);
        return this;
    }

    public void loadData() {
        load(SITE_URL);
    }

    public void loadData(String urlSubPath) {
        load(SITE_URL + urlSubPath);
    }

    private void load(String url) {
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
                                            processResponse(data);
                                        }
                                    });
                });
    }

    private void processResponse(String data) {
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
            notifyArticlesLoadedComplete(parseArticleItemsForIssue(doc));
        } else {
            notifyErrorMsg("Parsing failure: latest-issue not found");
        }
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
