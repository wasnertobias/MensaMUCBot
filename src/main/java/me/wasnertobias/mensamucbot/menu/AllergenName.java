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

        allergenToString.put(Allergen.EI, "eggs");
        allergenToString.put(Allergen.EN, "peanuts");
        allergenToString.put(Allergen.FI, "fish");
        allergenToString.put(Allergen.GL, "cereals containing gluten");
        allergenToString.put(Allergen.GLW, "wheat");
        allergenToString.put(Allergen.GLR, "rye");
        allergenToString.put(Allergen.GLG, "barley");
        allergenToString.put(Allergen.GLH, "oats");
        allergenToString.put(Allergen.GLD, "spelt");
        allergenToString.put(Allergen.KR, "crustaceans");
        allergenToString.put(Allergen.LU, "lupin");
        allergenToString.put(Allergen.MI, "milk (including lactose)");
        allergenToString.put(Allergen.SC, "nuts");
        allergenToString.put(Allergen.SCM, "almonds");
        allergenToString.put(Allergen.SCH, "hazelnuts");
        allergenToString.put(Allergen.SCW, "walnuts");
        allergenToString.put(Allergen.SCC, "cashew nuts");
        allergenToString.put(Allergen.SCP, "pistachio nuts");
        allergenToString.put(Allergen.SE, "sesame seeds");
        allergenToString.put(Allergen.SF, "mustard");
        allergenToString.put(Allergen.SL, "celery");
        allergenToString.put(Allergen.SO, "soybeans");
        allergenToString.put(Allergen.SW, "sulphur dioxide and sulphites");
        allergenToString.put(Allergen.WT, "molluscs");

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
