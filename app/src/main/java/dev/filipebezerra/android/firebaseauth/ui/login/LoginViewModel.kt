/*
 * Copyright 2019, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.filipebezerra.android.firebaseauth.ui.login

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dev.filipebezerra.android.firebaseauth.R
import dev.filipebezerra.android.firebaseauth.util.ext.postEvent
import dev.filipebezerra.android.firebaseauth.util.livedata.FirebaseUserLiveData
import dev.filipebezerra.android.firebaseauth.util.observable.Event
import kotlin.random.Random

class LoginViewModel : ViewModel() {

    companion object {
        val androidFacts = arrayOf(
            "The first commercial Android device was launched in September 2008",
            "The Android operating system has over 2 billion monthly active users",
            "The first Android version (1.0) was released on September 23, 2008",
            "The first smart phone running Android was the HTC Dream called the T-Mobile G1 in " + "some countries"
        )

        val californiaFacts = arrayOf(
            "The most populated state in the United States is California",
            "Three out of the ten largest U. S. cities are in California",
            "The largest tree in the world can be found in California",
            "California became a state in 1850"
        )
    }

    enum class AuthenticationState {
        AUTHENTICATED, UNAUTHENTICATED, INVALID_AUTHENTICATION
    }

    val authenticationState = MediatorLiveData<AuthenticationState>()

    private val firebaseUser = FirebaseUserLiveData()

    private val _snackbarText = MutableLiveData<Event<Int>>()
    val snackbarText: LiveData<Event<Int>>
        get() = _snackbarText

    private val _isUpdatingProfile = MutableLiveData<Boolean>()
    val isUpdatingProfile: LiveData<Boolean>
        get() = _isUpdatingProfile

    init {
        authenticationState.apply {
            addFirebaseUserAsSource()
            addSnackbarTextAsSource()
        }
    }

    private fun MediatorLiveData<AuthenticationState>.addFirebaseUserAsSource() {
        addSource(firebaseUser) { currentUser ->
            authenticationState.value = when {
                currentUser == null -> AuthenticationState.UNAUTHENTICATED
                currentUser.providerData.any { it.providerId == "phone" } &&
                        currentUser.displayName.isNullOrBlank() -> {
                    AuthenticationState.INVALID_AUTHENTICATION
                }
                else -> AuthenticationState.AUTHENTICATED
            }
        }
    }

    private fun MediatorLiveData<AuthenticationState>.addSnackbarTextAsSource() {
        addSource(snackbarText) {
            if (it.peekContent() == R.string.profile_updated_successfully) {
                authenticationState.value = AuthenticationState.AUTHENTICATED
            }
        }
    }

    /**
     * Gets a fact to display based on the user's set preference of which type of fact they want
     * to see (Android fact or California fact). If there is no logged in user or if the user has
     * not set a preference, defaults to showing Android facts.
     */
    fun getFactToDisplay(context: Context): String {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val factTypePreferenceKey = context.getString(R.string.preference_fact_type_key)
        val defaultFactType = context.resources.getStringArray(R.array.fact_type)[0]
        val funFactType = sharedPreferences.getString(factTypePreferenceKey, defaultFactType)

        return androidFacts[Random.nextInt(0, androidFacts.size)]
    }

    fun updateDisplayName(text: CharSequence) {
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(text.toString())
            .build()
        Firebase.auth.currentUser?.run {
            _isUpdatingProfile.value = true
            updateProfile(profileUpdates).addOnCompleteListener {
                _isUpdatingProfile.value = false
                it.takeIf { it.isSuccessful }?.let {
                    _snackbarText.postEvent(R.string.profile_updated_successfully)
                }
                it.takeUnless { it.isSuccessful }?.let {
                    _snackbarText.postEvent(R.string.profile_update_failed)
                }
            }
        }
    }
}
