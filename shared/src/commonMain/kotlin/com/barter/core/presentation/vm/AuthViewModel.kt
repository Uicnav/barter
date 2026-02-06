package com.barter.core.presentation.vm

import com.barter.core.domain.location.LocationProvider
import com.barter.core.domain.model.AuthState
import com.barter.core.domain.model.RegistrationRequest
import com.barter.core.domain.model.UserProfile
import com.barter.core.domain.repo.BarterRepository
import com.barter.core.domain.usecase.LoginUseCase
import com.barter.core.domain.usecase.LogoutUseCase
import com.barter.core.domain.usecase.RegisterUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LoginFormState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
)

data class RegisterFormState(
    val displayName: String = "",
    val location: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val detectingLocation: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
)

class AuthViewModel(
    private val repo: BarterRepository,
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val locationProvider: LocationProvider,
) : BaseViewModel() {

    val authState: StateFlow<AuthState> = repo.authState
        .stateIn(scope, SharingStarted.Eagerly, AuthState.Unauthenticated)

    val currentUser: StateFlow<UserProfile> = repo.currentUser
        .stateIn(scope, SharingStarted.Eagerly, UserProfile("", "Guest", "", 0.0))

    private val _loginForm = MutableStateFlow(LoginFormState())
    val loginForm: StateFlow<LoginFormState> = _loginForm.asStateFlow()

    private val _registerForm = MutableStateFlow(RegisterFormState())
    val registerForm: StateFlow<RegisterFormState> = _registerForm.asStateFlow()

    // ── Login form ───────────────────────────────────────────
    fun onLoginEmailChange(value: String) {
        _loginForm.value = _loginForm.value.copy(email = value, error = null)
    }

    fun onLoginPasswordChange(value: String) {
        _loginForm.value = _loginForm.value.copy(password = value, error = null)
    }

    fun login() {
        val form = _loginForm.value
        if (form.email.isBlank() || form.password.isBlank()) {
            _loginForm.value = form.copy(error = "Please fill in all fields")
            return
        }

        scope.launch {
            _loginForm.value = _loginForm.value.copy(loading = true, error = null)
            loginUseCase(form.email.trim(), form.password)
                .onSuccess {
                    _loginForm.value = LoginFormState()
                }
                .onFailure { e ->
                    _loginForm.value = _loginForm.value.copy(
                        loading = false,
                        error = e.message ?: "Login failed",
                    )
                }
        }
    }

    // ── Register form ────────────────────────────────────────
    fun onRegisterNameChange(value: String) {
        _registerForm.value = _registerForm.value.copy(displayName = value, error = null)
    }

    fun onRegisterLocationChange(value: String) {
        _registerForm.value = _registerForm.value.copy(location = value, error = null)
    }

    fun onRegisterEmailChange(value: String) {
        _registerForm.value = _registerForm.value.copy(email = value, error = null)
    }

    fun onRegisterPasswordChange(value: String) {
        _registerForm.value = _registerForm.value.copy(password = value, error = null)
    }

    fun onRegisterConfirmPasswordChange(value: String) {
        _registerForm.value = _registerForm.value.copy(confirmPassword = value, error = null)
    }

    fun detectLocation() {
        scope.launch {
            _registerForm.value = _registerForm.value.copy(detectingLocation = true)
            val result = locationProvider.getCurrentLocation()
            if (result != null) {
                _registerForm.value = _registerForm.value.copy(
                    location = result.cityName ?: _registerForm.value.location,
                    latitude = result.latitude,
                    longitude = result.longitude,
                    detectingLocation = false,
                )
            } else {
                _registerForm.value = _registerForm.value.copy(
                    detectingLocation = false,
                    error = "Could not detect location",
                )
            }
        }
    }

    fun register() {
        val form = _registerForm.value

        if (form.displayName.isBlank() || form.email.isBlank() || form.password.isBlank()) {
            _registerForm.value = form.copy(error = "Please fill in all required fields")
            return
        }
        if (form.password != form.confirmPassword) {
            _registerForm.value = form.copy(error = "Passwords do not match")
            return
        }

        scope.launch {
            _registerForm.value = _registerForm.value.copy(loading = true, error = null)
            val request = RegistrationRequest(
                displayName = form.displayName.trim(),
                location = form.location.trim(),
                email = form.email.trim(),
                password = form.password,
                latitude = form.latitude,
                longitude = form.longitude,
            )
            registerUseCase(request)
                .onSuccess {
                    _registerForm.value = RegisterFormState()
                }
                .onFailure { e ->
                    _registerForm.value = _registerForm.value.copy(
                        loading = false,
                        error = e.message ?: "Registration failed",
                    )
                }
        }
    }

    // ── Balance ──────────────────────────────────────────────
    fun topUpBalance(amount: Double) {
        scope.launch {
            runCatching { repo.topUpBalance(amount) }
        }
    }

    // ── Logout ───────────────────────────────────────────────
    fun logout() {
        scope.launch { logoutUseCase() }
    }
}
