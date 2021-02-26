package dev.filipebezerra.android.firebaseauth.util.ext

import androidx.lifecycle.MutableLiveData
import dev.filipebezerra.android.firebaseauth.util.observable.Event

fun <T> MutableLiveData<Event<T>>.postEvent(content: T) {
    postValue(Event(content))
}