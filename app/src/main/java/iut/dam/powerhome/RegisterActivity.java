package iut.dam.powerhome;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;
import org.json.JSONObject;
public class RegisterActivity extends BaseActivity {
    private static final String SERVER_URL = "http://10.0.2.2/powerhome_server";
    private EditText etFirstName, etLastName, etEmail, etPassword, etMobile;
    private EditText etFloor, etArea;
    private Spinner spCountryCode;
    private LinearLayout layoutStep1, layoutStep2;
    private String savedToken;
    private int savedUserId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        MaterialToolbar toolbar = findViewById(R.id.toolbarCreateAccount);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        etFirstName  = findViewById(R.id.etFirstName);
        etLastName   = findViewById(R.id.etLastName);
        etEmail      = findViewById(R.id.etEmail);
        etPassword   = findViewById(R.id.etPassword);
        etMobile     = findViewById(R.id.etMobile);
        etFloor      = findViewById(R.id.etFloor);
        etArea       = findViewById(R.id.etArea);
        layoutStep1  = findViewById(R.id.layoutStep1);
        layoutStep2  = findViewById(R.id.layoutStep2);
        spCountryCode = findViewById(R.id.spCountryCode);
        String[] codes = {"+33","+32","+34","+39","+41","+44","+49","+212","+213","+216"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, codes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCountryCode.setAdapter(adapter);
        findViewById(R.id.btnCreateAccount).setOnClickListener(this::onStep1Clicked);
        Button btnStep2 = findViewById(R.id.btnAddHabitat);
        if (btnStep2 != null) btnStep2.setOnClickListener(this::onStep2Clicked);
    }
    private void onStep1Clicked(View v) {
        String fn  = safe(etFirstName); String ln = safe(etLastName);
        String em  = safe(etEmail);     String pw = safe(etPassword);
        String mob = safe(etMobile);
        if (fn.isEmpty())                      { etFirstName.setError("Prénom requis"); return; }
        if (ln.isEmpty())                      { etLastName.setError("Nom requis"); return; }
        if (em.isEmpty()||!em.contains("@"))   { etEmail.setError("Email invalide"); return; }
        if (pw.length() < 6)                   { etPassword.setError("6 caractères minimum"); return; }
        if (mob.isEmpty())                     { etMobile.setError("Mobile requis"); return; }
        String code = spCountryCode.getSelectedItem() != null ? spCountryCode.getSelectedItem().toString() : "";
        String url = SERVER_URL + "/Register.php?firstname=" + fn + "&lastname=" + ln + "&email=" + em + "&password=" + pw + "&phone=" + code + mob;
        Ion.with(this).load(url).asString().withResponse().setCallback(new FutureCallback<Response<String>>() {
            @Override public void onCompleted(Exception e, Response<String> response) {
                if (e!=null||response==null) { Toast.makeText(RegisterActivity.this,"Serveur inaccessible",Toast.LENGTH_SHORT).show(); return; }
                int code2 = response.getHeaders().code();
                if (code2 == 201) {
                    try {
                        JSONObject json = new JSONObject(response.getResult());
                        savedUserId = json.getInt("id");
                        // auto-login pour récupérer le token
                        loginAndShowStep2(safe(etEmail), safe(etPassword));
                    } catch (Exception ex) { ex.printStackTrace(); }
                } else if (code2 == 409) { etEmail.setError("Email déjà utilisé");
                } else { Toast.makeText(RegisterActivity.this,"Erreur "+code2,Toast.LENGTH_SHORT).show(); }
            }
        });
    }
    private void loginAndShowStep2(String email, String password) {
        String url = SERVER_URL + "/login.php?email=" + email + "&password=" + password;
        Ion.with(this).load(url).asString().withResponse().setCallback(new FutureCallback<Response<String>>() {
            @Override public void onCompleted(Exception e, Response<String> response) {
                if (e!=null||response==null) return;
                try {
                    JSONObject json = new JSONObject(response.getResult());
                    savedToken = json.getString("token");
                    savedUserId = json.getInt("id");
                    SharedPreferences.Editor ed = getSharedPreferences("powerhome",MODE_PRIVATE).edit();
                    ed.putString("token",savedToken); ed.putInt("user_id",savedUserId);
                    ed.putString("firstname",json.getString("firstname")); ed.putString("lastname",json.getString("lastname"));
                    ed.putString("email",json.optString("email","")); ed.apply();
                    runOnUiThread(() -> { layoutStep1.setVisibility(View.GONE); layoutStep2.setVisibility(View.VISIBLE); });
                } catch(Exception ex){ ex.printStackTrace(); }
            }
        });
    }
    private void onStep2Clicked(View v) {
        String floor = safe(etFloor); String area = safe(etArea);
        if (floor.isEmpty()) { etFloor.setError("Étage requis"); return; }
        if (area.isEmpty())  { etArea.setError("Surface requise"); return; }
        String url = SERVER_URL + "/addHabitat.php?token=" + savedToken + "&floor=" + floor + "&area=" + area + "&user_id=" + savedUserId;
        Ion.with(this).load(url).asString().withResponse().setCallback(new FutureCallback<Response<String>>() {
            @Override public void onCompleted(Exception e, Response<String> response) {
                Toast.makeText(RegisterActivity.this, "Compte créé avec succès !", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
    private String safe(EditText et) { return et.getText()==null?"":et.getText().toString().trim(); }
}
