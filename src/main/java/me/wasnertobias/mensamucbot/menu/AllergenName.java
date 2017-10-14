package me.wasnertobias.mensamucbot.menu;

import java.util.HashMap;

public class AllergenName {
    private static HashMap<String, Allergen> shortToAllergen = new HashMap<>();
    private static HashMap<Allergen, String> allergenToString = new HashMap<>();
    private static AllergenName singleton = null;

    private AllergenName() {
        shortToAllergen.put("ei", Allergen.EI);
        shortToAllergen.put("en", Allergen.EN);
        shortToAllergen.put("fi", Allergen.FI);
        shortToAllergen.put("gl", Allergen.GL);
        shortToAllergen.put("glw", Allergen.GLW);
        shortToAllergen.put("glr", Allergen.GLR);
        shortToAllergen.put("glg", Allergen.GLG);
        shortToAllergen.put("glh", Allergen.GLH);
        shortToAllergen.put("gld", Allergen.GLD);
        shortToAllergen.put("kr", Allergen.KR);
        shortToAllergen.put("lu", Allergen.LU);
        shortToAllergen.put("mi", Allergen.MI);
        shortToAllergen.put("sc", Allergen.SC);
        shortToAllergen.put("scm", Allergen.SCM);
        shortToAllergen.put("sch", Allergen.SCH);
        shortToAllergen.put("scw", Allergen.SCW);
        shortToAllergen.put("scc", Allergen.SCC);
        shortToAllergen.put("scp", Allergen.SCP);
        shortToAllergen.put("se", Allergen.SE);
        shortToAllergen.put("sf", Allergen.SF);
        shortToAllergen.put("sl", Allergen.SL);
        shortToAllergen.put("so", Allergen.SO);
        shortToAllergen.put("sw", Allergen.SW);
        shortToAllergen.put("wt", Allergen.WT);

        allergenToString.put(Allergen.EI, "Hühnerei");
        allergenToString.put(Allergen.EN, "Erdnuss");
        allergenToString.put(Allergen.FI, "Fisch");
        allergenToString.put(Allergen.GL, "Glutenhaltiges Getreide");
        allergenToString.put(Allergen.GLW, "Weizen");
        allergenToString.put(Allergen.GLR, "Roggen");
        allergenToString.put(Allergen.GLG, "Gerste");
        allergenToString.put(Allergen.GLH, "Hafer");
        allergenToString.put(Allergen.GLD, "Dinkel");
        allergenToString.put(Allergen.KR, "Krebstiere");
        allergenToString.put(Allergen.LU, "Lupinen");
        allergenToString.put(Allergen.MI, "Milch und Laktose");
        allergenToString.put(Allergen.SC, "Schalenfrüchte");
        allergenToString.put(Allergen.SCM, "Mandeln");
        allergenToString.put(Allergen.SCH, "Haselnüsse");
        allergenToString.put(Allergen.SCW, "Walnüsse");
        allergenToString.put(Allergen.SCC, "Cashewnüsse");
        allergenToString.put(Allergen.SCP, "Pistazien");
        allergenToString.put(Allergen.SE, "Sesamsamen");
        allergenToString.put(Allergen.SF, "Senf");
        allergenToString.put(Allergen.SL, "Sellerie");
        allergenToString.put(Allergen.SO, "Soja");
        allergenToString.put(Allergen.SW, "Schwefeldioxid und Sulfite");
        allergenToString.put(Allergen.WT, "Weichtiere");

    }

    public static AllergenName getInstance() {
        if (singleton == null) {
            singleton = new AllergenName();
        }
        return singleton;
    }

    public String getAllergenName(Allergen allergen) {
        return allergenToString.get(allergen);
    }

    public Allergen getAllergenFromShort(String shortString) {
        return shortToAllergen.get(shortString.toLowerCase());
    }
}
