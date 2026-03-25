package iut.dam.powerhome;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;
import static android.content.Context.MODE_PRIVATE;
public class ParametresFragment extends Fragment {
    private static final String SERVER_URL = "http://10.0.2.2/powerhome_server";
    public ParametresFragment() {}
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle(getString(R.string.nav_parametres));
        View root = inflater.inflate(R.layout.fragment_parametres, container, false);
        SharedPreferences prefs = requireActivity().getSharedPreferences("powerhome", MODE_PRIVATE);
        TextView tvNom    = root.findViewById(R.id.tv_nom);
        TextView tvPrenom = root.findViewById(R.id.tv_prenom);
        TextView tvEmail  = root.findViewById(R.id.tv_email);
        TextView tvPhone  = root.findViewById(R.id.tv_phone);
        TextView tvCoins  = root.findViewById(R.id.tv_ecocoins);
        if (tvNom    != null) tvNom.setText(prefs.getString("lastname", "-"));
        if (tvPrenom != null) tvPrenom.setText(prefs.getString("firstname", "-"));
        if (tvEmail  != null) tvEmail.setText(prefs.getString("email", "-"));
        if (tvPhone  != null) tvPhone.setText(prefs.getString("phone", "-"));
        if (tvCoins  != null) tvCoins.setText(prefs.getInt("eco_coins", 0) + " éco-coins");
        View llPwd  = root.findViewById(R.id.ll_change_password);
        View llLang = root.findViewById(R.id.ll_change_language);
        if (llPwd  != null) llPwd.setOnClickListener(v -> showChangePasswordDialog());
        if (llLang != null) llLang.setOnClickListener(v -> showLanguageDialog());
        return root;
    }
    private void showChangePasswordDialog() {
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_change_password, null);
        EditText etCur  = v.findViewById(R.id.etCurrentPassword);
        EditText etNew  = v.findViewById(R.id.etNewPassword);
        EditText etConf = v.findViewById(R.id.etConfirmPassword);
        new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.change_password)).setView(v)
            .setPositiveButton(getString(R.string.save), (d, w) -> {
                String cur  = etCur.getText().toString();
                String nw   = etNew.getText().toString();
                String conf = etConf.getText().toString();
                if (cur.isEmpty() || nw.isEmpty() || conf.isEmpty()) {
                    Toast.makeText(getActivity(), getString(R.string.all_fields_required), Toast.LENGTH_SHORT).show(); return;
                }
                if (!nw.equals(conf)) {
                    Toast.makeText(getActivity(), getString(R.string.passwords_no_match), Toast.LENGTH_SHORT).show(); return;
                }
                if (nw.length() < 6) {
                    Toast.makeText(getActivity(), getString(R.string.password_min), Toast.LENGTH_SHORT).show(); return;
                }
                String token = requireActivity().getSharedPreferences("powerhome", MODE_PRIVATE).getString("token", "");
                Ion.with(getContext()).load(SERVER_URL + "/updateProfile.php?token=" + token + "&current_password=" + cur + "&new_password=" + nw)
                    .asString().withResponse().setCallback(new FutureCallback<Response<String>>() {
                        @Override public void onCompleted(Exception e, Response<String> response) {
                            if (e != null || response == null) { Toast.makeText(getActivity(), getString(R.string.server_error), Toast.LENGTH_SHORT).show(); return; }
                            if (response.getHeaders().code() == 200) Toast.makeText(getActivity(), "Mot de passe modifié !", Toast.LENGTH_SHORT).show();
                            else if (response.getHeaders().code() == 401) Toast.makeText(getActivity(), "Mot de passe actuel incorrect", Toast.LENGTH_SHORT).show();
                            else Toast.makeText(getActivity(), "Erreur " + response.getHeaders().code(), Toast.LENGTH_SHORT).show();
                        }
                    });
            }).setNegativeButton(getString(R.string.cancel), null).show();
    }
    private void showLanguageDialog() {
        String cur = LocaleHelper.getSavedLanguage(requireContext());
        String[] opts  = {"Français", "English", "العربية"};
        String[] codes = {"fr", "en", "ar"};
        int sel = 0;
        for (int i = 0; i < codes.length; i++) if (codes[i].equals(cur)) { sel = i; break; }
        final int[] chosen = {sel};
        new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.language_label))
            .setSingleChoiceItems(opts, sel, (d, which) -> chosen[0] = which)
            .setPositiveButton(getString(R.string.save), (d, w) -> {
                String lang = codes[chosen[0]];
                // Save language preference
                LocaleHelper.setLocale(requireContext(), lang);
                // Restart the whole activity so BaseActivity.attachBaseContext re-applies
                requireActivity().recreate();
            })
            .setNegativeButton(getString(R.string.cancel), null).show();
    }
}
