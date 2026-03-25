package iut.dam.powerhome;
import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;
public class BaseActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context base) {
        String lang = LocaleHelper.getSavedLanguage(base);
        super.attachBaseContext(LocaleHelper.updateResources(base, lang));
    }
}
