package iut.dam.powerhome;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;
import java.util.*;
import static android.content.Context.MODE_PRIVATE;
public class HabitatsFragment extends Fragment {
    private static final String SERVER_URL = "http://10.0.2.2/powerhome_server";
    private List<Habitat> habitats;
    private HabitatAdapter adapter;
    private TextView tvGlobalConso;
    public HabitatsFragment() {}
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle(getString(R.string.nav_habitats));
        View root = inflater.inflate(R.layout.fragment_habitats, container, false);
        ListView lv = root.findViewById(R.id.listHabitat);
        tvGlobalConso = root.findViewById(R.id.tv_global_conso);
        habitats = new ArrayList<>();
        adapter  = new HabitatAdapter(getActivity(), R.layout.item_habitat, habitats);
        lv.setAdapter(adapter);
        loadHabitats();
        lv.setOnItemClickListener((p, v, pos, id) -> showHabitatDialog(habitats.get(pos)));
        return root;
    }
    private void loadHabitats() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("powerhome", MODE_PRIVATE);
        String token = prefs.getString("token", "");
        String url   = SERVER_URL + "/getHabitats.php" + (token.isEmpty() ? "" : "?token=" + token);
        ProgressDialog pd = new ProgressDialog(getActivity());
        pd.setMessage("Chargement des habitats…"); pd.setIndeterminate(true); pd.setCancelable(false); pd.show();
        long t0 = System.currentTimeMillis();
        Ion.with(getContext()).load(url).asString().withResponse().setCallback(new FutureCallback<Response<String>>() {
            @Override public void onCompleted(Exception e, Response<String> response) {
                long rem = 2000 - (System.currentTimeMillis() - t0);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    pd.dismiss();
                    if (e != null || response == null) { Toast.makeText(getActivity(), getString(R.string.server_error), Toast.LENGTH_SHORT).show(); return; }
                    if (response.getHeaders().code() == 200) {
                        List<Habitat> list = Habitat.getListFromJson(response.getResult());
                        if (list != null && !list.isEmpty()) {
                            for (Habitat h : list) if (h.appliances == null) h.appliances = new ArrayList<>();
                            habitats.clear(); habitats.addAll(list); adapter.notifyDataSetChanged();
                            updateGlobalConso();
                        }
                    }
                }, Math.max(0, rem));
            }
        });
    }
    private void updateGlobalConso() {
        if (tvGlobalConso == null) return;
        int total = 0;
        for (Habitat h : habitats) for (Appliance a : h.appliances) total += a.wattage;
        tvGlobalConso.setText(getString(R.string.global_conso, String.valueOf(total)));
    }
    private void showHabitatDialog(Habitat habitat) {
        View dv = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_habitat_detail, null);
        TextView tvRes   = dv.findViewById(R.id.tv_dialog_resident);
        TextView tvInfo  = dv.findViewById(R.id.tv_dialog_info);
        TextView tvTotal = dv.findViewById(R.id.tv_dialog_total);
        LinearLayout ll  = dv.findViewById(R.id.ll_dialog_appliances);
        tvRes.setText(habitat.getResidentName());
        if (habitat.appliances.isEmpty()) {
            tvInfo.setText("Étage " + habitat.floor + " • " + habitat.area + " m² • 0 équipement");
            tvTotal.setText("Consommation totale : 0 W");
            Button btn = new Button(getActivity());
            btn.setText(getString(R.string.add_appliance));
            btn.setBackgroundColor(0xFF4CAF50); btn.setTextColor(0xFFFFFFFF);
            btn.setOnClickListener(v -> showAddApplianceDialog(habitat.id));
            ll.addView(btn);
        } else {
            int total = 0;
            for (Appliance app : habitat.appliances) {
                View row = LayoutInflater.from(getActivity()).inflate(R.layout.item_appliance_dialog, ll, false);
                ((ImageView) row.findViewById(R.id.iv_appliance_icon)).setImageResource(app.getIconRes());
                ((TextView) row.findViewById(R.id.tv_appliance_name)).setText(app.name);
                ((TextView) row.findViewById(R.id.tv_appliance_ref)).setText("Réf: " + app.reference);
                ((TextView) row.findViewById(R.id.tv_appliance_watt)).setText(app.wattage + " W");
                ll.addView(row);
                total += app.wattage;
            }
            tvInfo.setText("Étage " + habitat.floor + " • " + habitat.area + " m² • " + habitat.appliances.size() + " équipement" + (habitat.appliances.size() > 1 ? "s" : ""));
            tvTotal.setText("Consommation totale : " + total + " W");
        }
        new AlertDialog.Builder(getActivity()).setView(dv).setPositiveButton(getString(R.string.close_btn), null).show();
    }
    private void showAddApplianceDialog(int habitatId) {
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_add_appliance, null);
        EditText etName = v.findViewById(R.id.etApplianceName);
        EditText etRef  = v.findViewById(R.id.etApplianceRef);
        EditText etWatt = v.findViewById(R.id.etApplianceWattage);
        new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.add_appliance)).setView(v)
            .setPositiveButton(getString(R.string.save), (d, w) -> {
                String name = etName.getText().toString().trim();
                String ref  = etRef.getText().toString().trim();
                String watt = etWatt.getText().toString().trim();
                if (name.isEmpty() || ref.isEmpty() || watt.isEmpty()) { Toast.makeText(getActivity(), getString(R.string.all_fields_required), Toast.LENGTH_SHORT).show(); return; }
                String token = requireActivity().getSharedPreferences("powerhome", MODE_PRIVATE).getString("token", "");
                Ion.with(getContext()).load(SERVER_URL + "/addAppliance.php?token=" + token + "&habitat_id=" + habitatId + "&name=" + name + "&reference=" + ref + "&wattage=" + watt)
                    .asString().withResponse().setCallback(new FutureCallback<Response<String>>() {
                        @Override public void onCompleted(Exception e, Response<String> response) {
                            if (response != null && response.getHeaders().code() == 201) {
                                Toast.makeText(getActivity(), getString(R.string.appliance_added), Toast.LENGTH_SHORT).show();
                                loadHabitats();
                            } else Toast.makeText(getActivity(), "Erreur", Toast.LENGTH_SHORT).show();
                        }
                    });
            }).setNegativeButton(getString(R.string.cancel), null).show();
    }
}
