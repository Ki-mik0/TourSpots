package ua.chnu.tourspots.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import ua.chnu.tourspots.R
import ua.chnu.tourspots.data.TourDbHelper
import ua.chnu.tourspots.data.UserDao
import ua.chnu.tourspots.databinding.ActivityRegisterBinding

/** Lab 5: registration — the user's name and login are stored in the database. */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var users: UserDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        users = UserDao(TourDbHelper.get(this))

        binding.btnRegister.setOnClickListener { register() }
    }

    private fun register() {
        val name = binding.etName.text?.toString()?.trim().orEmpty()
        val login = binding.etLogin.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        val confirm = binding.etConfirm.text?.toString().orEmpty()

        val valid = listOf(
            binding.tilName.show(Validators.name(name)),
            binding.tilLogin.show(Validators.login(login)),
            binding.tilPassword.show(Validators.password(password)),
            binding.tilConfirm.show(Validators.confirmation(password, confirm)),
        ).all { it }
        if (!valid) return

        if (users.isLoginTaken(login) || users.register(name, login, password) == null) {
            binding.tilLogin.error = getString(R.string.error_login_taken)
            return
        }
        Toast.makeText(this, R.string.registration_success, Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK, Intent().putExtra(EXTRA_LOGIN, login))
        finish()
    }

    /** Shows the validation error under the field; returns true when the field is valid. */
    private fun TextInputLayout.show(error: Validators.Error?): Boolean {
        this.error = error?.let { getString(it.messageRes()) }
        return error == null
    }

    private fun Validators.Error.messageRes(): Int = when (this) {
        Validators.Error.EMPTY -> R.string.error_required
        Validators.Error.NAME_TOO_SHORT -> R.string.error_name_short
        Validators.Error.LOGIN_FORMAT -> R.string.error_login_format
        Validators.Error.PASSWORD_TOO_SHORT -> R.string.error_password_short
        Validators.Error.PASSWORDS_DIFFER -> R.string.error_passwords_differ
    }

    companion object {
        const val EXTRA_LOGIN = "ua.chnu.tourspots.extra.LOGIN"
    }
}
