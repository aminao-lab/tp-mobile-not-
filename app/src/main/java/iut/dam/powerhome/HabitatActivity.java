package iut.dam.powerhome;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.navigation.NavigationView;
public class HabitatActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {
    DrawerLayout drawerDL; NavigationView navNV; ActionBarDrawerToggle toggle; FragmentManager fm;
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_habitat);
        String login = getIntent().getStringExtra("login");
        drawerDL = findViewById(R.id.drawer);
        navNV    = findViewById(R.id.nav_view);
        if (login != null) {
            TextView tvEmail = navNV.getHeaderView(0).findViewById(R.id.nav_header_email);
            if (tvEmail != null) tvEmail.setText(login);
        }
        toggle = new ActionBarDrawerToggle(this, drawerDL, R.string.open, R.string.close);
        drawerDL.addDrawerListener(toggle); toggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        fm = getSupportFragmentManager();
        navNV.setNavigationItemSelectedListener(this);
        if (savedInstanceState == null) {
            navNV.setCheckedItem(R.id.nav_habitats);
            fm.beginTransaction().replace(R.id.contentFL, new HabitatsFragment()).commit();
        }
    }
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) return true;
        return super.onOptionsItemSelected(item);
    }
    @Override public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        if      (id==R.id.nav_habitats)    fm.beginTransaction().replace(R.id.contentFL, new HabitatsFragment()).commit();
        else if (id==R.id.nav_mon_habitat) fm.beginTransaction().replace(R.id.contentFL, new MonHabitatFragment()).commit();
        else if (id==R.id.nav_mes_requetes)fm.beginTransaction().replace(R.id.contentFL, new MesRequetesFragment()).commit();
        else if (id==R.id.nav_parametres) fm.beginTransaction().replace(R.id.contentFL, new ParametresFragment()).commit();
        else if (id==R.id.nav_about)       showAboutDialog();
        else if (id==R.id.nav_deconnexion){
            getSharedPreferences("powerhome", MODE_PRIVATE).edit().clear().apply();
            Intent i=new Intent(this,LoginActivity.class); i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK); startActivity(i); finish();
        }
        drawerDL.closeDrawer(GravityCompat.START);
        return true;
    }
    private void showAboutDialog() {
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.about_title))
            .setMessage(getString(R.string.about_text))
            .setPositiveButton(getString(R.string.close_btn), null)
            .show();
    }
    @Override public void onBackPressed() {
        if (drawerDL.isDrawerOpen(GravityCompat.START)) drawerDL.closeDrawer(GravityCompat.START);
        else super.onBackPressed();
    }
}
