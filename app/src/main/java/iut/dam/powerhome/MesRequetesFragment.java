package iut.dam.powerhome;

import android.content.SharedPreferences;
import android.graphics.*;
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
import java.util.*;
import static android.content.Context.MODE_PRIVATE;

public class MesRequetesFragment extends Fragment {

    private static final String SERVER_URL = "http://10.0.2.2/powerhome_server";
    private GridLayout calendarGrid;
    private TextView tvMonth, tvEcoCoins;
    private int currentYear, currentMonth;
    // Stocke tous les slots du mois : date -> liste de slots
    private Map<Integer, List<JSONObject>> daySlots = new HashMap<>();

    public MesRequetesFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle(getString(R.string.nav_calendrier));
        View root = inflater.inflate(R.layout.fragment_mes_requetes, container, false);
        calendarGrid = root.findViewById(R.id.calendarGrid);
        tvMonth      = root.findViewById(R.id.tvMonth);
        tvEcoCoins   = root.findViewById(R.id.tvEcoCoins);

        Calendar now = Calendar.getInstance();
        currentYear  = now.get(Calendar.YEAR);
        currentMonth = now.get(Calendar.MONTH) + 1;

        root.findViewById(R.id.btnPrev).setOnClickListener(v -> {
            currentMonth--;
            if (currentMonth < 1) { currentMonth = 12; currentYear--; }
            loadCalendar();
        });
        root.findViewById(R.id.btnNext).setOnClickListener(v -> {
            currentMonth++;
            if (currentMonth > 12) { currentMonth = 1; currentYear++; }
            loadCalendar();
        });

        updateEcoCoinsDisplay();
        loadCalendar();
        return root;
    }

    private void updateEcoCoinsDisplay() {
        if (tvEcoCoins == null) return;
        int coins = requireActivity().getSharedPreferences("powerhome", MODE_PRIVATE).getInt("eco_coins", 0);
        tvEcoCoins.setText("💰 " + coins + " éco-coins");
    }

    private void loadCalendar() {
        updateMonthLabel();
        daySlots.clear();
        String token = requireActivity().getSharedPreferences("powerhome", MODE_PRIVATE).getString("token", "");
        String url = SERVER_URL + "/getSlots.php?token=" + token + "&month=" + currentMonth + "&year=" + currentYear;
        Ion.with(getContext()).load(url).asString().withResponse()
            .setCallback(new FutureCallback<Response<String>>() {
                @Override public void onCompleted(Exception e, Response<String> response) {
                    if (response != null && response.getHeaders().code() == 200) {
                        try {
                            JSONArray arr = new JSONArray(response.getResult());
                            // Regrouper les slots par jour
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject slot = arr.getJSONObject(i);
                                String date = slot.getString("slot_date"); // "2026-03-25"
                                int day = Integer.parseInt(date.substring(8, 10));
                                if (!daySlots.containsKey(day)) daySlots.put(day, new ArrayList<>());
                                daySlots.get(day).add(slot);
                            }
                        } catch (Exception ex) { Log.e("Cal", "JSON", ex); }
                    }
                    buildCalendar();
                }
            });
    }

    private void updateMonthLabel() {
        String[] months = {"", "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
                           "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};
        if (tvMonth != null) tvMonth.setText(months[currentMonth] + " " + currentYear);
    }

    // Couleur d'un jour = taux max parmi tous ses créneaux
    private int getMaxRateForDay(int day) {
        if (!daySlots.containsKey(day)) return -1;
        int max = 0;
        for (JSONObject slot : daySlots.get(day)) {
            try { max = Math.max(max, slot.getInt("occupation_rate")); } catch (Exception ignored) {}
        }
        return max;
    }

    private void buildCalendar() {
        if (calendarGrid == null || getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            calendarGrid.removeAllViews();
            // En-têtes jours
            String[] days = {"L", "M", "M", "J", "V", "S", "D"};
            for (String d : days) {
                TextView tv = makeCell(d, Color.TRANSPARENT, Color.parseColor("#888888"), false);
                tv.setTypeface(null, Typeface.BOLD);
                calendarGrid.addView(tv);
            }
            Calendar cal = Calendar.getInstance();
            cal.set(currentYear, currentMonth - 1, 1);
            int firstDow = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7;
            int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            Calendar today = Calendar.getInstance();

            for (int i = 0; i < firstDow; i++) calendarGrid.addView(makeCell("", Color.TRANSPARENT, Color.TRANSPARENT, false));

            for (int day = 1; day <= daysInMonth; day++) {
                final int d = day;
                int rate = getMaxRateForDay(day);
                int bgColor, txtColor;
                if (rate > 70)      { bgColor = Color.parseColor("#F7C1C1"); txtColor = Color.parseColor("#791F1F"); }
                else if (rate > 30) { bgColor = Color.parseColor("#FAC775"); txtColor = Color.parseColor("#633806"); }
                else if (rate >= 0) { bgColor = Color.parseColor("#C0DD97"); txtColor = Color.parseColor("#27500A"); }
                else                { bgColor = Color.parseColor("#F5F5F5"); txtColor = Color.parseColor("#555555"); }

                boolean isToday = (currentYear == today.get(Calendar.YEAR) &&
                                   currentMonth == today.get(Calendar.MONTH) + 1 &&
                                   day == today.get(Calendar.DAY_OF_MONTH));

                // Vérifier si l'user a déjà un engagement ce jour
                boolean hasCommitment = false;
                if (daySlots.containsKey(day)) {
                    for (JSONObject slot : daySlots.get(day)) {
                        try { if (slot.optBoolean("user_committed", false)) { hasCommitment = true; break; } }
                        catch (Exception ignored) {}
                    }
                }

                TextView tv = makeCell(String.valueOf(day), bgColor, txtColor, isToday);
                if (hasCommitment) {
                    // Petite pastille verte pour signaler l'engagement
                    tv.setText("• " + day);
                }
                tv.setOnClickListener(v -> showDayDialog(d));
                calendarGrid.addView(tv);
            }
        });
    }

    private TextView makeCell(String text, int bg, int txt, boolean bold) {
        TextView tv = new TextView(getActivity());
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(12);
        tv.setBackgroundColor(bg);
        tv.setTextColor(txt);
        if (bold) tv.setTypeface(null, Typeface.BOLD);
        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width  = 0;
        p.height = dpToPx(38);
        p.setMargins(2, 2, 2, 2);
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        tv.setLayoutParams(p);
        return tv;
    }

    // -------------------------------------------------------
    // DIALOG JOUR : liste tous les créneaux du jour
    // -------------------------------------------------------
    private void showDayDialog(int day) {
        List<JSONObject> slots = daySlots.get(day);
        if (slots == null || slots.isEmpty()) {
            Toast.makeText(getActivity(), "Aucun créneau ce jour", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] labels = new String[slots.size()];
        for (int i = 0; i < slots.size(); i++) {
            try {
                JSONObject s = slots.get(i);
                String start = s.getString("start_time").substring(0, 5);
                String end   = s.getString("end_time").substring(0, 5);
                int    rate  = s.getInt("occupation_rate");
                boolean committed = s.optBoolean("user_committed", false);
                String icon  = rate > 70 ? "🔴" : rate > 30 ? "🟡" : "🟢";
                String mark  = committed ? " ✓" : "";
                labels[i] = icon + " " + start + "–" + end + "  (" + rate + "%)" + mark;
            } catch (Exception e) { labels[i] = "Créneau " + (i + 1); }
        }

        new AlertDialog.Builder(getActivity())
            .setTitle("Créneaux du " + day + "/" + currentMonth + "/" + currentYear)
            .setItems(labels, (d, which) -> showSlotActionDialog(slots.get(which), slots))
            .setNegativeButton("Fermer", null)
            .show();
    }

    // -------------------------------------------------------
    // DIALOG CRÉNEAU : action sur un créneau spécifique
    // -------------------------------------------------------
    private void showSlotActionDialog(JSONObject slot, List<JSONObject> allDaySlots) {
        try {
            int    rate      = slot.getInt("occupation_rate");
            String start     = slot.getString("start_time").substring(0, 5);
            String end       = slot.getString("end_time").substring(0, 5);
            boolean committed = slot.optBoolean("user_committed", false);
            int    slotId    = slot.getInt("id");

            String level     = rate > 70 ? "🔴 Saturé" : rate > 30 ? "🟡 Chargé" : "🟢 Disponible";
            String coinsInfo;
            if (rate > 70)      coinsInfo = committed ? "⚠️ Malus : -10 à -15 éco-coins" : "✅ Choisir alternatif vert → +10 éco-coins\n⚠️ Réserver ici → -5 éco-coins";
            else if (rate > 30) coinsInfo = "✅ Choisir alternatif vert → +5 éco-coins\n⚠️ Réserver ici → -2 éco-coins";
            else                coinsInfo = "✅ Réservation directe → +3 éco-coins";

            StringBuilder msg = new StringBuilder();
            msg.append("Horaire : ").append(start).append("–").append(end).append("\n");
            msg.append("Occupation : ").append(rate).append("% (").append(level).append(")\n\n");
            msg.append(coinsInfo);
            if (committed) msg.append("\n\n⚡ Vous avez déjà un engagement sur ce créneau.");

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle("Créneau " + start + "–" + end)
                .setMessage(msg.toString())
                .setNegativeButton("Fermer", null);

            if (rate > 30) {
                // Créneau chargé/saturé : proposer de choisir un alternatif vert
                builder.setPositiveButton("Choisir un créneau alternatif vert", (d, w) ->
                    showAlternativePickerDialog(slotId, allDaySlots));
                builder.setNeutralButton("Réserver quand même", (d, w) ->
                    commitSlot(slotId, -1, null));
            } else {
                // Créneau vert : réservation directe
                builder.setPositiveButton("Réserver ce créneau (+3 🪙)", (d, w) ->
                    showApplianceInputDialog(slotId, -1));
            }

            builder.show();

        } catch (Exception e) { Log.e("Cal", "Dialog error", e); }
    }

    // -------------------------------------------------------
    // SÉLECTION D'UN CRÉNEAU ALTERNATIF VERT
    // -------------------------------------------------------
    private void showAlternativePickerDialog(int peakSlotId, List<JSONObject> allDaySlots) {
        // Chercher les créneaux verts du même jour
        List<JSONObject> greenSlots = new ArrayList<>();
        for (JSONObject s : allDaySlots) {
            try {
                if (s.getInt("occupation_rate") <= 30 && s.getInt("id") != peakSlotId) {
                    greenSlots.add(s);
                }
            } catch (Exception ignored) {}
        }

        // Si pas de créneaux verts ce jour, chercher dans d'autres jours du mois
        if (greenSlots.isEmpty()) {
            // Collecter tous les créneaux verts du mois
            for (Map.Entry<Integer, List<JSONObject>> entry : daySlots.entrySet()) {
                for (JSONObject s : entry.getValue()) {
                    try {
                        if (s.getInt("occupation_rate") <= 30 && s.getInt("id") != peakSlotId) {
                            greenSlots.add(s);
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        if (greenSlots.isEmpty()) {
            Toast.makeText(getActivity(), "Aucun créneau vert disponible ce mois.", Toast.LENGTH_LONG).show();
            return;
        }

        String[] labels = new String[greenSlots.size()];
        for (int i = 0; i < greenSlots.size(); i++) {
            try {
                JSONObject s = greenSlots.get(i);
                String date  = s.getString("slot_date").substring(5); // MM-DD
                String start = s.getString("start_time").substring(0, 5);
                String end   = s.getString("end_time").substring(0, 5);
                int    rate  = s.getInt("occupation_rate");
                labels[i] = "🟢 " + date + "  " + start + "–" + end + " (" + rate + "%)";
            } catch (Exception e) { labels[i] = "Créneau vert " + (i + 1); }
        }

        final List<JSONObject> finalGreen = greenSlots;
        new AlertDialog.Builder(getActivity())
            .setTitle("Choisissez un créneau vert 🟢")
            .setItems(labels, (d, which) -> {
                try {
                    int altSlotId = finalGreen.get(which).getInt("id");
                    showApplianceInputDialog(peakSlotId, altSlotId);
                } catch (Exception e) { Log.e("Cal", "Alt select", e); }
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    // -------------------------------------------------------
    // SAISIE DU NOM DE L'ÉQUIPEMENT
    // -------------------------------------------------------
    private void showApplianceInputDialog(int slotId, int altSlotId) {
        View v = LayoutInflater.from(getActivity()).inflate(android.R.layout.activity_list_item, null);
        EditText et = new EditText(getActivity());
        et.setHint("Équipement (ex: Lave-linge) — optionnel");
        et.setPadding(40, 20, 40, 20);

        new AlertDialog.Builder(getActivity())
            .setTitle("Quel équipement ?")
            .setView(et)
            .setPositiveButton("Confirmer", (d, w) -> {
                String name = et.getText().toString().trim();
                commitSlot(slotId, altSlotId, name.isEmpty() ? null : name);
            })
            .setNeutralButton("Passer", (d, w) -> commitSlot(slotId, altSlotId, null))
            .setNegativeButton("Annuler", null)
            .show();
    }

    // -------------------------------------------------------
    // APPEL SERVEUR : enregistrer l'engagement
    // -------------------------------------------------------
    private void commitSlot(int slotId, int altSlotId, String applianceName) {
        String token = requireActivity().getSharedPreferences("powerhome", MODE_PRIVATE).getString("token", "");
        StringBuilder url = new StringBuilder(SERVER_URL + "/commitSlot.php?token=" + token + "&slot_id=" + slotId);
        if (altSlotId > 0)        url.append("&slot_id_alternative=").append(altSlotId);
        if (applianceName != null) url.append("&appliance_name=").append(applianceName);

        Ion.with(getContext()).load(url.toString()).asString().withResponse()
            .setCallback(new FutureCallback<Response<String>>() {
                @Override public void onCompleted(Exception e, Response<String> response) {
                    if (e != null || response == null) {
                        Toast.makeText(getActivity(), getString(R.string.server_error), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        JSONObject json = new JSONObject(response.getResult());
                        int    change  = json.optInt("eco_coins_change", 0);
                        int    total   = json.optInt("total_eco_coins", 0);
                        String message = json.optString("message", "Engagement enregistré !");

                        // Mettre à jour les SharedPreferences
                        requireActivity().getSharedPreferences("powerhome", MODE_PRIVATE)
                            .edit().putInt("eco_coins", total).apply();

                        String summary = message + "\n\nSolde : " + total + " éco-coins";
                        new AlertDialog.Builder(getActivity())
                            .setTitle(change >= 0 ? "✅ Engagement confirmé" : "⚠️ Attention")
                            .setMessage(summary)
                            .setPositiveButton("OK", null)
                            .show();

                        updateEcoCoinsDisplay();
                        loadCalendar(); // Rafraîchir pour cocher le créneau
                    } catch (Exception ex) {
                        Toast.makeText(getActivity(), "Engagement enregistré !", Toast.LENGTH_SHORT).show();
                        loadCalendar();
                    }
                }
            });
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
