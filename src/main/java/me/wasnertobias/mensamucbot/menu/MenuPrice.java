package me.wasnertobias.mensamucbot.menu;

import java.util.HashMap;

public class MenuPrice {
    private static HashMap<String, String> menuPrices = new HashMap<>();
    private static MenuPrice singleton = null;

    private MenuPrice() {
        menuPrices.put("tagesgericht 1", "1");
        menuPrices.put("tagesgericht 2", "1,55");
        menuPrices.put("tagesgericht 3", "1,90");
        menuPrices.put("tagesgericht 4", "2,40");
        menuPrices.put("aktionsessen 1", "1,55");
        menuPrices.put("aktionsessen 2", "1,90");
        menuPrices.put("aktionsessen 3", "2,40");
        menuPrices.put("aktionsessen 4", "2,60");
        menuPrices.put("aktionsessen 5", "2,80");
        menuPrices.put("aktionsessen 6", "3");
        menuPrices.put("aktionsessen 7", "3,20");
        menuPrices.put("aktionsessen 8", "3,50");
        menuPrices.put("aktionsessen 9", "4");
        menuPrices.put("aktionsessen 10", "4,50");
        menuPrices.put("biogericht 1", "1,55");
        menuPrices.put("biogericht 2", "1,90");
        menuPrices.put("biogericht 3", "2,40");
        menuPrices.put("biogericht 4", "2,60");
        menuPrices.put("biogericht 5", "2,80");
        menuPrices.put("biogericht 6", "3");
        menuPrices.put("biogericht 7", "3,20");
        menuPrices.put("biogericht 8", "3,50");
        menuPrices.put("biogericht 9", "4");
        menuPrices.put("biogericht 10", "4,50");
    }

    public static MenuPrice getInstance() {
        if (singleton == null) {
            singleton = new MenuPrice();
        }
        return singleton;
    }

    public String getMenuPrice(String dishType) {
        if (dishType == null) {
            return null;
        }

        dishType = dishType.toLowerCase();
        if (menuPrices.containsKey(dishType)) {
            return menuPrices.get(dishType);
        }

        return null;
    }
}
