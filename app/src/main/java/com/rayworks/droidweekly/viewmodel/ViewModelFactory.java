package com.rayworks.droidweekly.viewmodel;

import android.app.Application;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.rayworks.droidweekly.repository.ArticleManager;

public class ViewModelFactory extends ViewModelProvider.AndroidViewModelFactory {
    private final ArticleManager manager;

    public ViewModelFactory(@NonNull Application application, ArticleManager articleManager) {
        super(application);
        this.manager = articleManager;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ArticleListViewModel.class)) {
            return (T) new ArticleListViewModel(manager);
        }

        throw new UnsupportedOperationException(
                "No view model matched for " + modelClass.getSimpleName());
    }
}
