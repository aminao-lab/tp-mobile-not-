package iut.dam.powerhome;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.snackbar.Snackbar;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;
import org.json.JSONObject;
public class LoginActivity extends BaseActivity {
    private static final String SERVER_URL = "http://10.0.2.2/powerhome_server";
    private EditText etEmail, etPassword;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        etEmail    = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        TextView tvCreate = findViewById(R.id.tvCreateAccount);
        tvCreate.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        TextView tvForgot = findViewById(R.id.tvForgot);
        if (tvForgot != null) tvForgot.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
    }
    public void login(View v) {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        if (email.isEmpty() || password.isEmpty()) { Toast.makeText(this, getString(R.string.login_error), Toast.LENGTH_SHORT).show(); return; }
        Ion.with(this).load(SERVER_URL + "/login.php?email=" + email + "&password=" + password)
            .asString().withResponse().setCallback(new FutureCallback<Response<String>>() {
                @Override public void onCompleted(Exception e, Response<String> response) {
                    if (e != null || response == null) { Toast.makeText(LoginActivity.this, getString(R.string.server_error), Toast.LENGTH_SHORT).show(); return; }
                    int code = response.getHeaders().code();
                    if (code == 200) {
                        try {
                            JSONObject json = new JSONObject(response.getResult());
                            SharedPreferences.Editor ed = getSharedPreferences("powerhome", MODE_PRIVATE).edit();
                            ed.putString("token",     json.getString("token"));
                            ed.putInt   ("user_id",   json.getInt("id"));
                            ed.putString("firstname", json.getString("firstname"));
                            ed.putString("lastname",  json.getString("lastname"));
                            ed.putString("email",     json.optString("email", ""));
                            ed.putString("phone",     json.optString("phone", ""));
                            ed.putInt   ("eco_coins", json.optInt("eco_coins", 0));
                            ed.apply();
                            String name = json.getString("firstname") + " " + json.getString("lastname");
                            Snackbar.make(v, "Connexion réussie", Snackbar.LENGTH_SHORT)
                                    .setBackgroundTint(getColor(R.color.c_btn_primary_bg))
                                    .setTextColor(getColor(android.R.color.white)).show();
                            Intent i = new Intent(LoginActivity.this, HabitatActivity.class);
                            i.putExtra("login", name);
                            v.postDelayed(() -> startActivity(i), 600);
                        } catch (Exception ex) { Log.e("Login", "JSON error", ex); }
                    } else {
                        etPassword.setError(getString(R.string.login_error));
                        Toast.makeText(LoginActivity.this, getString(R.string.login_error), Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }
}
