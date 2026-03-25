package iut.dam.powerhome;

import java.io.Serializable;

public class Appliance implements Serializable {

    int id;
    String name;
    String reference;
    int wattage;

    // Constructeur pour les données venant du serveur (sans iconRes)
    public Appliance(int id, String name, String reference, int wattage) {
        this.id = id;
        this.name = name;
        this.reference = reference;
        this.wattage = wattage;
    }

    // Constructeur legacy pour les données hardcodées (garde la compatibilité)
    public Appliance(int id, String name, String reference, int wattage, int iconRes) {
        this(id, name, reference, wattage);
    }

    // Détermine l'icône à partir du nom de l'appareil (insensible à la casse + accents)
    public int getIconRes() {
        if (name == null) return R.drawable.ic_vacuum;
        String n = name.toLowerCase();
        if (n.contains("lav") || n.contains("machine"))  return R.drawable.ic_washer;
        if (n.contains("aspir"))                          return R.drawable.ic_vacuum;
        if (n.contains("fer") || n.contains("repas"))     return R.drawable.ic_iron;
        if (n.contains("clim") || n.contains("ac"))       return R.drawable.ic_ac;
        return R.drawable.ic_vacuum; // icône par défaut
    }
}
