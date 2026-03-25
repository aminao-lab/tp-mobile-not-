package iut.dam.powerhome;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;
import org.json.*;
import static android.content.Context.MODE_PRIVATE;
public class MonHabitatFragment extends Fragment {
    private static final String SERVER_URL = "http://10.0.2.2/powerhome_server";
    private TextView tvResident, tvFloor, tvArea, tvCount, tvTotalWatt;
    private LinearLayout llAppliances;
    private int currentHabitatId = -1;
    public MonHabitatFragment() {}
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle(getString(R.string.nav_mon_habitat));
        View root = inflater.inflate(R.layout.fragment_mon_habitat, container, false);
        tvResident   = root.findViewById(R.id.tv_resident);
        tvFloor      = root.findViewById(R.id.tv_floor);
        tvArea       = root.findViewById(R.id.tv_area);
        tvCount      = root.findViewById(R.id.tv_count);
        tvTotalWatt  = root.findViewById(R.id.tv_total_watt);
        llAppliances = root.findViewById(R.id.ll_appliances);
        Button btnAdd  = root.findViewById(R.id.btn_add_appliance);
        Button btnEdit = root.findViewById(R.id.btn_edit_habitat);
        if (btnAdd  != null) btnAdd.setOnClickListener(v  -> { if (currentHabitatId != -1) showAddApplianceDialog(); });
        if (btnEdit != null) btnEdit.setOnClickListener(v -> { if (currentHabitatId != -1) showEditHabitatDialog(); });
        loadMyHabitat();
        return root;
    }
    private void loadMyHabitat() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("powerhome", MODE_PRIVATE);
        String token = prefs.getString("token", "");
        tvResident.setText(prefs.getString("firstname", "") + " " + prefs.getString("lastname", ""));
        Ion.with(getContext()).load(SERVER_URL + "/getMyHabitat.php?token=" + token)
            .asString().withResponse().setCallback(new FutureCallback<Response<String>>() {
                @Override public void onCompleted(Exception e, Response<String> response) {
                    if (e != null || response == null) { Toast.makeText(getActivity(), getString(R.string.server_error), Toast.LENGTH_SHORT).show(); return; }
                    int code = response.getHeaders().code();
                    if (code == 200) {
                        try {
                            JSONObject h = new JSONObject(response.getResult());
                            currentHabitatId = h.getInt("id");
                            tvFloor.setText(String.valueOf(h.getInt("floor")));
                            tvArea.setText(h.getDouble("area") + " m²");
                            tvTotalWatt.setText(h.getInt("total_wattage") + " W");
                            JSONArray apps = h.getJSONArray("appliances");
                            tvCount.setText(apps.length() + " appareil" + (apps.length() > 1 ? "s" : ""));
                            llAppliances.removeAllViews();
                            for (int i = 0; i < apps.length(); i++) {
                                JSONObject a = apps.getJSONObject(i);
                                addRow(a.getInt("id"), a.getString("name"), a.getInt("wattage"), a.getString("reference"));
                            }
                        } catch (Exception ex) { Log.e("MonHabitat", "JSON", ex); }
                    } else if (code == 404) {
                        Toast.makeText(getActivity(), "Aucun habitat trouvé", Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }
    @SuppressLint("SetTextI18n")
    private void addRow(int applianceId, String name, int wattage, String reference) {
        View row = LayoutInflater.from(getActivity()).inflate(R.layout.item_appliance_dialog, llAppliances, false);
        ImageView icon  = row.findViewById(R.id.iv_appliance_icon);
        TextView tvName = row.findViewById(R.id.tv_appliance_name);
        TextView tvRef  = row.findViewById(R.id.tv_appliance_ref);
        TextView tvWatt = row.findViewById(R.id.tv_appliance_watt);
        icon.setImageResource(getIconForAppliance(name));
        tvName.setText(name);
        tvRef.setText("Réf: " + reference);
        tvWatt.setText(wattage + " W");
        // Bouton supprimer
        row.setOnLongClickListener(v -> {
            new AlertDialog.Builder(getActivity())
                .setTitle("Supprimer")
                .setMessage("Supprimer " + name + " ?")
                .setPositiveButton("Supprimer", (d, w) -> deleteAppliance(applianceId))
                .setNegativeButton("Annuler", null).show();
            return true;
        });
        llAppliances.addView(row);
    }
    private int getIconForAppliance(String name) {
        if (name == null) return R.drawable.ic_vacuum;
        String n = name.toLowerCase();
        if (n.contains("lav") || n.contains("machine") || n.contains("wash")) return R.drawable.ic_washer;
        if (n.contains("aspir") || n.contains("vacuum"))                       return R.drawable.ic_vacuum;
        if (n.contains("fer") || n.contains("repas") || n.contains("iron"))    return R.drawable.ic_iron;
        if (n.contains("clim") || n.contains("ac") || n.contains("air"))       return R.drawable.ic_ac;
        return R.drawable.ic_vacuum;
    }
    private void deleteAppliance(int applianceId) {
        String token = requireActivity().getSharedPreferences("powerhome", MODE_PRIVATE).getString("token", "");
        Ion.with(getContext()).load(SERVER_URL + "/deleteAppliance.php?token=" + token + "&appliance_id=" + applianceId)
            .asString().withResponse().setCallback(new FutureCallback<Response<String>>() {
                @Override public void onCompleted(Exception e, Response<String> response) {
                    if (response != null && response.getHeaders().code() == 200) {
                        Toast.makeText(getActivity(), "Équipement supprimé", Toast.LENGTH_SHORT).show();
                        loadMyHabitat();
                    } else {
                        Toast.makeText(getActivity(), "Erreur suppression", Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }
    private void showEditHabitatDialog() {
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_edit_habitat, null);
        EditText etFloor = v.findViewById(R.id.etEditFloor);
        EditText etArea  = v.findViewById(R.id.etEditArea);
        etFloor.setText(tvFloor.getText().toString());
        etArea.setText(tvArea.getText().toString().replace(" m²", ""));
        new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.edit_habitat)).setView(v)
            .setPositiveButton(getString(R.string.save), (d, w) -> {
                String floor = etFloor.getText().toString().trim();
                String area  = etArea.getText().toString().trim();
                if (floor.isEmpty() || area.isEmpty()) { Toast.makeText(getActivity(), getString(R.string.all_fields_required), Toast.LENGTH_SHORT).show(); return; }
                String token = requireActivity().getSharedPreferences("powerhome", MODE_PRIVATE).getString("token", "");
                Ion.with(getContext()).load(SERVER_URL + "/updateHabitat.php?token=" + token + "&habitat_id=" + currentHabitatId + "&floor=" + floor + "&area=" + area)
                    .asString().withResponse().setCallback(new FutureCallback<Response<String>>() {
                        @Override public void onCompleted(Exception e, Response<String> response) {
                            if (response != null && response.getHeaders().code() == 200) {
                                Toast.makeText(getActivity(), getString(R.string.habitat_saved), Toast.LENGTH_SHORT).show();
                                loadMyHabitat();
                            } else Toast.makeText(getActivity(), "Erreur", Toast.LENGTH_SHORT).show();
                        }
                    });
            }).setNegativeButton(getString(R.string.cancel), null).show();
    }
    private void showAddApplianceDialog() {
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_add_appliance, null);
        EditText etName = v.findViewById(R.id.etApplianceName);
        EditText etRef  = v.findViewById(R.id.etApplianceRef);
        EditText etWatt = v.findViewById(R.id.etApplianceWattage);
        new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.add_appliance)).setView(v)
            .setPositiveButton(getString(R.string.save), (d, w) -> {
                String name = etName.getText().toString().trim();
                String ref  = etRef.getText().toString().trim();
                String watt = etWatt.getText().toString().trim();
                if (name.isEmpty() || ref.isEmpty() || watt.isEmpty()) {
                    Toast.makeText(getActivity(), getString(R.string.all_fields_required), Toast.LENGTH_SHORT).show(); return;
                }
                String token = requireActivity().getSharedPreferences("powerhome", MODE_PRIVATE).getString("token", "");
                Ion.with(getContext()).load(SERVER_URL + "/addAppliance.php?token=" + token + "&habitat_id=" + currentHabitatId + "&name=" + name + "&reference=" + ref + "&wattage=" + watt)
                    .asString().withResponse().setCallback(new FutureCallback<Response<String>>() {
                        @Override public void onCompleted(Exception e, Response<String> response) {
                            if (response != null && response.getHeaders().code() == 201) {
                                Toast.makeText(getActivity(), getString(R.string.appliance_added), Toast.LENGTH_SHORT).show();
                                loadMyHabitat();
                            } else Toast.makeText(getActivity(), "Erreur", Toast.LENGTH_SHORT).show();
                        }
                    });
            }).setNegativeButton(getString(R.string.cancel), null).show();
    }
}
