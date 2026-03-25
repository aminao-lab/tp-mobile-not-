package iut.dam.powerhome;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
public class ForgotPasswordActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        MaterialToolbar toolbar = findViewById(R.id.toolbarForgot);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        EditText etEmail = findViewById(R.id.etForgotEmail);
        findViewById(R.id.btnSendReset).setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (email.isEmpty() || !email.contains("@")) { etEmail.setError("Email invalide"); return; }
            Toast.makeText(this, "Un email de réinitialisation a été envoyé à " + email, Toast.LENGTH_LONG).show();
            finish();
        });
    }
}
