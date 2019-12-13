package com.rayworks.droidweekly.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.rayworks.droidweekly.repository.ArticleRepository;

public class ViewModelFactory extends ViewModelProvider.AndroidViewModelFactory {
    private final ArticleRepository repository;

    public ViewModelFactory(@NonNull Application application, ArticleRepository repository) {
        super(application);
        this.repository = repository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ArticleListViewModel.class)) {
            return (T) new ArticleListViewModel(repository);
        }

        throw new UnsupportedOperationException(
                "No view model matched for " + modelClass.getSimpleName());
    }
}
