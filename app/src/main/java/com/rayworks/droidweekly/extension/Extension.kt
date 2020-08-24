package com.rayworks.droidweekly.extension

import androidx.lifecycle.*
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken

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

class ScopeViewModel<V>(
        val value: V
) : ViewModel() {
    class Factory<V>(val valueFactory: () -> V) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
                ScopeViewModel(valueFactory()) as? T
                        ?: throw java.lang.IllegalArgumentException("Unknown type")
    }
}

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