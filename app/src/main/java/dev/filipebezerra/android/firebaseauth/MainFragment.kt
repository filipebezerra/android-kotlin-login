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

package dev.filipebezerra.android.firebaseauth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import dev.filipebezerra.android.firebaseauth.ui.login.LoginViewModel.AuthenticationState.*
import dev.filipebezerra.android.firebaseauth.databinding.FragmentMainBinding
import dev.filipebezerra.android.firebaseauth.ui.login.LoginViewModel

class MainFragment : Fragment() {

    companion object {
        const val TAG = "MainFragment"
        const val SIGN_IN_RESULT_CODE = 1001
    }

    // Get a reference to the ViewModel scoped to this Fragment
    private val viewModel by viewModels<LoginViewModel>()
    private lateinit var binding: FragmentMainBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeAuthenticationState()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            SIGN_IN_RESULT_CODE -> {
                resultCode.takeIf { it == Activity.RESULT_OK }?.run {
                    Log.i(
                        TAG,
                        "Successfully signed in user ${FirebaseAuth.getInstance().currentUser?.displayName}"
                    )
                }
                resultCode.takeUnless { it == Activity.RESULT_OK }?.run {
                    Log.i(
                        TAG,
                        "Sign in unsuccessful ${IdpResponse.fromResultIntent(data)?.error?.errorCode}"
                    )
                }
            }
        }
    }

    /**
     * Observes the authentication state and changes the UI accordingly.
     * If there is a logged in user: (1) show a logout button and (2) display their name.
     * If there is no logged in user: show a login button
     */
    private fun observeAuthenticationState() {
        val factToDisplay = viewModel.getFactToDisplay(requireContext())
        viewModel.authenticationState.observe(viewLifecycleOwner) { authState ->
            when (authState) {
                AUTHENTICATED -> showUserAuthenticatedUI(factToDisplay)
                UNAUTHENTICATED -> showUserUnauthenticatedUI(factToDisplay)
                INVALID_AUTHENTICATION -> displayInvalidAuthenticationUI()
            }
        }
    }

    private fun showUserAuthenticatedUI(factToDisplay: String) {
        binding.authButton.text = getString(R.string.logout_button_text)
        binding.authButton.setOnClickListener { runSignOutFlow() }
        binding.welcomeText.text = getFactWithPersonalization(factToDisplay)
    }

    private fun showUserUnauthenticatedUI(factToDisplay: String) {
        binding.authButton.text = getString(R.string.login_button_text)
        binding.authButton.setOnClickListener { launchSignInFlow() }
        binding.welcomeText.text = factToDisplay
    }

    private fun displayInvalidAuthenticationUI() {
        Snackbar.make(
            binding.mainRootLayout,
            getString(R.string.invalid_authentication),
            Snackbar.LENGTH_SHORT
        )
            .setAction(android.R.string.ok) {

            }
            .show();
    }

    private fun getFactWithPersonalization(fact: String): String {
        return String.format(
            resources.getString(
                R.string.welcome_message_authed,
                FirebaseAuth.getInstance().currentUser?.displayName,
                Character.toLowerCase(fact[0]) + fact.substring(1)
            )
        )
    }

    private fun launchSignInFlow() {
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
            SIGN_IN_RESULT_CODE
        )
    }

    private fun runSignOutFlow() {
        context?.run { AuthUI.getInstance().signOut(this) }
    }
}