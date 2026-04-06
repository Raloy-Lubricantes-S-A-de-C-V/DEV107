package com.example.deviceappend

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.deviceappend.core.session.SessionManager
import com.example.deviceappend.ui.home.HomeFragment
import com.example.deviceappend.ui.login.LoginFragment
import com.example.deviceappend.core.network.RetrofitClient
import com.google.android.material.appbar.MaterialToolbar

class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)

        RetrofitClient.init(applicationContext)
        sessionManager = SessionManager(this)

        if (savedInstanceState == null) {
            checkSessionAndNavigate()
        }
    }

    private fun checkSessionAndNavigate() {
        if (sessionManager.getToken() != null) {
            replaceFragment(HomeFragment())
        } else {
            replaceFragment(LoginFragment())
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_home -> {
                // Limpia la pila y regresa al HomeFragment
                supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                replaceFragment(com.example.deviceappend.ui.home.HomeFragment())
                true
            }
            R.id.action_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun replaceFragment(fragment: Fragment, addToBackStack: Boolean = false) {
        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, fragment)
        if (addToBackStack) transaction.addToBackStack(null)
        transaction.commit()
    }

    fun logout() {
        sessionManager.clearSession()
        invalidateOptionsMenu()
        replaceFragment(LoginFragment())
    }
}