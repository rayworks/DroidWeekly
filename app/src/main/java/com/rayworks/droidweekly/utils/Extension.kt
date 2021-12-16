package com.rayworks.droidweekly.utils

import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.get
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/***
 * Convert JSON string to specified data object
 */
inline fun <reified T> Gson.jsonToObject(str: String): T? {
    return try {
        fromJson(str, object : TypeToken<T>() {}.type)
    } catch (exp: JsonSyntaxException) {
        print(exp)
        null
    }
}

// A kotlin property delegate to scope any object to an android lifecycle.
// Known issue : only one `Presenter` could be used pre Activity/Fragment

// https://gist.github.com/rharter/7871d08e9375ba2b001f20947f18868e
/**
 * Returns a property delegate to access the wrapped value, which will be retained for the
 * duration of the lifecycle of this [ViewModelStoreOwner].
 *
 * ```
 * class MyFragment : Fragment() {
 *   private val presenter by scoped { MyPresenter() }
 *
 *   override fun onCreate(savedInstanceState: Bundle?) {
 *     super.onCreate(savedInstanceState)
 *     presenter.models.collect {
 *       // ...
 *     }
 *   }
 * }
 * ```
 */
inline fun <reified T> ViewModelStoreOwner.scoped(noinline creator: () -> T): Lazy<T> {
    return LazyScopedValue({ viewModelStore }, { ScopeViewModel.Factory(creator) })
}

/***
 * The [ViewModel] wrapper.
 */
class ScopeViewModel<V>(
    val value: V
) : ViewModel() {
    /***
     * The [ViewModelProvider.Factory] wrapper.
     */
    class Factory<V>(val valueFactory: () -> V) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            ScopeViewModel(valueFactory()) as? T
                ?: throw java.lang.IllegalArgumentException("Unknown type")
    }
}

/***
 * The lazy scoped generic object.
 */
class LazyScopedValue<T>(
    private val storeProducer: () -> ViewModelStore,
    private val factoryProducer: () -> ViewModelProvider.Factory
) : Lazy<T> {
    private var cached: Any = NotSet

    @Suppress("UNCHECKED_CAST")
    override val value: T
        get() {
            val value = cached
            return if (value == NotSet) {
                val factory = factoryProducer()
                val store = storeProducer()
                val viewModel = ViewModelProvider(store, factory).get<ScopeViewModel<T>>()

                viewModel.value.also {
                    cached = it as Any
                }
            } else {
                value as T
            }
        }

    override fun isInitialized() = cached != NotSet

    companion object {
        private val NotSet = Any()
    }
}

/***
 * Extension method of [SearchView] to get the query text flow
 */
fun SearchView.queryTextFlow(): StateFlow<String> {
    val query = MutableStateFlow("")
    setOnQueryTextListener(
        object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    query.value = newText

                }
                return true
            }
        }
    )

    return query
}