package iut.dam.powerhome;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class HabitatAdapter extends ArrayAdapter<Habitat> {

    Activity activity;
    int resource;
    List<Habitat> habitats;

    public HabitatAdapter(Activity activity, int resource, List<Habitat> habitats) {
        super(activity, resource, habitats);
        this.activity = activity;
        this.resource = resource;
        this.habitats = habitats;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View layout = convertView;

        if (layout == null) {
            LayoutInflater inflater = activity.getLayoutInflater();
            layout = inflater.inflate(resource, parent, false);
        }

        TextView txtResident   = layout.findViewById(R.id.tv_resident_name);
        TextView txtEquipments = layout.findViewById(R.id.tv_appliances_count);
        TextView txtFloor      = layout.findViewById(R.id.tv_floor_number);
        LinearLayout llIcons   = layout.findViewById(R.id.ll_appliance_icons);

        Habitat habitat = habitats.get(position);

        // Utilise getResidentName() qui a un fallback si null
        txtResident.setText(habitat.getResidentName());

        int nb = habitat.appliances.size();
        txtEquipments.setText(nb + (nb > 1 ? " équipements" : " équipement"));

        txtFloor.setText(String.valueOf(habitat.floor));

        // Icônes dynamiques via getIconRes()
        llIcons.removeAllViews();
        for (Appliance appliance : habitat.appliances) {
            ImageView iv = new ImageView(activity);
            iv.setImageResource(appliance.getIconRes()); // ← plus de .iconRes
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    dpToPx(16), dpToPx(16));
            params.setMargins(dpToPx(2), 0, dpToPx(2), 0);
            iv.setLayoutParams(params);
            llIcons.addView(iv);
        }

        return layout;
    }

    private int dpToPx(int dp) {
        float density = activity.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
