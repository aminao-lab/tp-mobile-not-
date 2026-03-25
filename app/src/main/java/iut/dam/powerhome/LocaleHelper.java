package iut.dam.powerhome;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import java.util.Locale;
public class LocaleHelper {
    public static Context setLocale(Context ctx, String lang) {
        ctx.getSharedPreferences("powerhome", Context.MODE_PRIVATE).edit().putString("language", lang).apply();
        return updateResources(ctx, lang);
    }
    public static String getSavedLanguage(Context ctx) {
        return ctx.getSharedPreferences("powerhome", Context.MODE_PRIVATE).getString("language", "fr");
    }
    public static Context updateResources(Context ctx, String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration(ctx.getResources().getConfiguration());
        config.setLocale(locale);
        return ctx.createConfigurationContext(config);
    }
}
