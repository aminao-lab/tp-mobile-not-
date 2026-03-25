package iut.dam.powerhome;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.List;

public class Habitat implements Serializable {

    int id;
    String residentName;
    int floor;
    double area;

    @SerializedName("user_id")
    int userId;

    List<Appliance> appliances;

    public Habitat(int id, String residentName, int floor, double area, List<Appliance> appliances) {
        this.id = id;
        this.residentName = residentName;
        this.floor = floor;
        this.area = area;
        this.appliances = appliances;
    }

    public String getResidentName() {
        return (residentName != null && !residentName.isEmpty())
                ? residentName
                : "Résident #" + userId;
    }

    public static List<Habitat> getListFromJson(String json) {
        Type type = new TypeToken<List<Habitat>>(){}.getType();
        return new Gson().fromJson(json, type);
    }
}
