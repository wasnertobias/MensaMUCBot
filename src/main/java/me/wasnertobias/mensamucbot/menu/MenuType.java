package me.wasnertobias.mensamucbot.menu;

import java.util.ArrayList;

public class MenuType {
    private ArrayList<MenuItem> items = new ArrayList<>();
    private String menuType;

    public MenuType(MenuItem item) {
        this.menuType = item.getMenuType();
        items.add(item);
    }

    public String getMenuType() {
        return menuType;
    }

    public void addMenuItem(MenuItem menuItem) {
        items.add(menuItem);
    }

    public ArrayList<MenuItem> getItems() {
        return items;
    }
}
