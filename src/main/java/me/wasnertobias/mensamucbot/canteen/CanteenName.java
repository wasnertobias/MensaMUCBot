package me.wasnertobias.mensamucbot.canteen;

public class CanteenName {
    public static String getCanteenName(CanteenType canteenType, String location) {
        if (canteenType.equals(CanteenType.MENSA)) {
            return "Mensa " + location;
        } else if (canteenType.equals(CanteenType.CAFE)) {
            return "StuCafé " + location;
        } else if (canteenType.equals(CanteenType.BISTRO)) {
            return "StuBistroMensa " + location;
        }
        return "";
    }
}
