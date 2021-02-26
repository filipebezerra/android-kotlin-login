package dev.filipebezerra.android.firebaseauth.util.ext

import androidx.fragment.app.Fragment
import com.firebase.ui.auth.AuthUI
import dev.filipebezerra.android.firebaseauth.MainFragment
import dev.filipebezerra.android.firebaseauth.R

fun Fragment.launchSignInFlow() {
    val providers = listOf(
        AuthUI.IdpConfig.GoogleBuilder().build(),
        AuthUI.IdpConfig.PhoneBuilder().build(),
        AuthUI.IdpConfig.EmailBuilder().build()
    )
    startActivityForResult(
        AuthUI.getInstance().createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setLogo(R.mipmap.ic_launcher_round)
            .setTosAndPrivacyPolicyUrls(
                "https://policies.google.com/terms",
                "https://policies.google.com/privacy"
            )
            .setTheme(R.style.AppTheme)
            .setLockOrientation(true)
            .build(),
        MainFragment.SIGN_IN_RESULT_CODE
    )
}