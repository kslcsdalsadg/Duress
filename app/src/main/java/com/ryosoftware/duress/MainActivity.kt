package com.ryosoftware.duress

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

import com.ryosoftware.duress.admin.DeviceAdminManager
import com.ryosoftware.duress.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Preferences
    private lateinit var prefsdb: Preferences
    private val admin by lazy { DeviceAdminManager(this) }
    private var accessibilityManager: AccessibilityManager? = null

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        prefs.copyTo(prefsdb, key)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init1()
        if (initBiometric()) return
        init2()
        setup()
        if (prefs.isShowProminentDisclosure) showProminentDisclosure()
    }

    override fun onStart() {
        super.onStart()
        prefs.registerListener(prefsListener)
        update()
    }

    override fun onStop() {
        super.onStop()
        prefs.unregisterListener(prefsListener)
    }

    private fun init1() {
        prefs = Preferences(this)
        prefsdb = Preferences(this, encrypted = false)
        prefs.copyTo(prefsdb)
        accessibilityManager = getSystemService(AccessibilityManager::class.java)
    }

    private fun init2() {
        binding.apply {
            val show_embedded_sim_wipe_option = when (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                true -> View.VISIBLE
                false -> View.GONE
            }
            passwordOrLen.editText?.setText(prefs.passwordOrLen)
            wipeEmbeddedSim.isChecked = prefs.isWipeEmbeddedSim
            wipeEmbeddedSim.visibility = show_embedded_sim_wipe_option
            keyguardType.check(when (prefs.keyguardType) {
                KeyguardType.A.value -> R.id.keyguardTypeA
                KeyguardType.B.value -> R.id.keyguardTypeB
                else -> R.id.keyguardTypeA
            })
            toggle.isChecked = prefs.isEnabled
        }
    }

    private fun initBiometric(): Boolean {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        when (BiometricManager
            .from(this)
            .canAuthenticate(authenticators))
        {
            BiometricManager.BIOMETRIC_SUCCESS -> {}
            else -> return false
        }
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback()
        {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                finishAndRemoveTask()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                init2()
                setup()
                if (prefs.isShowProminentDisclosure) showProminentDisclosure()
            }
        })
        try {
            prompt.authenticate(BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.authentication))
                .setConfirmationRequired(false)
                .setAllowedAuthenticators(authenticators)
                .build())
        } catch (exc: Exception) { return false }
        return true
    }

    private fun setup() = binding.apply {
        passwordOrLen.editText?.doAfterTextChanged {
            prefs.passwordOrLen = it?.toString()?.trim() ?: ""
        }
        wipeEmbeddedSim.setOnCheckedChangeListener { _, checked ->
            prefs.isWipeEmbeddedSim = checked
        }
        keyguardType.setOnCheckedChangeListener { _, checkedId ->
            prefs.keyguardType = when (checkedId) {
                R.id.keyguardTypeA -> KeyguardType.A.value
                R.id.keyguardTypeB -> KeyguardType.B.value
                else -> return@setOnCheckedChangeListener
            }
        }
        toggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !hasPermissions()) {
                toggle.isChecked = false
                requestPermissions()
                return@setOnCheckedChangeListener
            }
            prefs.isEnabled = isChecked
        }
    }

    private fun setOff() {
        prefs.isEnabled = false
        try { admin.remove() } catch (exc: SecurityException) {}
        binding.toggle.isChecked = false
    }

    private fun update() {
        if (prefs.isEnabled && !hasPermissions())
            Snackbar.make(
                binding.toggle,
                R.string.service_unavailable_popup,
                Snackbar.LENGTH_SHORT,
            ).show()
    }

    private fun showProminentDisclosure() =
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.prominent_disclosure_title)
            .setMessage(R.string.prominent_disclosure_message)
            .setPositiveButton(R.string.accept) { _, _ ->
                prefs.isShowProminentDisclosure = false
            }
            .setNegativeButton(R.string.exit) { _, _ ->
                finishAndRemoveTask()
            }
            .show()

    private fun requestPermissions() {
        if (!hasAccessibilityPermission()) {
            requestAccessibilityPermission()
            return
        }
        if (!hasAdminPermission()) requestAdminPermission()
    }

    private fun requestAccessibilityPermission() =
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    private fun requestAdminPermission() = startActivity(admin.makeRequestIntent())

    private fun hasPermissions(): Boolean { return hasAccessibilityPermission() && hasAdminPermission() }

    private fun hasAccessibilityPermission(): Boolean {
        for (info in accessibilityManager?.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_GENERIC,
        ) ?: return true) {
            if (info.resolveInfo.serviceInfo.packageName == packageName) return true
        }
        return false
    }

    private fun hasAdminPermission() = admin.isActive()
}