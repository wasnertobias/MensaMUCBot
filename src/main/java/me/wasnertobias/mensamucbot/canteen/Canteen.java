package me.wasnertobias.mensamucbot.canteen;

import me.wasnertobias.mensamucbot.EatingHabit;
import me.wasnertobias.mensamucbot.Main;
import me.wasnertobias.mensamucbot.menu.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

public class Canteen {
    private CanteenType type;
    private String location;
    private int urlId;
    private ArrayList<MenuType> types_today = null;
    private ArrayList<MenuType> types_tomorrow = null;

    public Canteen(CanteenType type, String location, int urlId) {
        this.type = type;
        this.location = location;
        this.urlId = urlId;
    }

    public CanteenType getType() {
        return type;
    }

    public String getLocation() {
        return location;
    }

    public int getUrlId() {
        return urlId;
    }

    public void scrapeNow() {
        if (types_tomorrow != null && types_today != null) {
            types_today.clear();
            types_today.addAll(types_tomorrow);
            types_tomorrow.clear();
            scrapeNow(true);
        } else {
            if (types_tomorrow == null) {
                types_tomorrow = new ArrayList<>();
            } else {
                types_tomorrow.clear();
            }
            if (types_today == null) {
                types_today = new ArrayList<>();
            } else {
                types_today.clear();
            }
            scrapeNow(false);
            scrapeNow(true);
        }
    }

    private void scrapeNow(boolean isTomorrow) {
        Calendar calendarToBeScraped = getCalendar(isTomorrow);
        String url = getURL(calendarToBeScraped);
        System.out.println("[Info] Scraping URL: " + url);

        Document doc = null;

        try {
            doc = Jsoup.connect(url).get();
        } catch (IOException ignored) {
        }

        if (doc != null) {
            String lastMenuTypeName = "";
            for (Element element : doc.select(".c-schedule__item").first().getElementsByClass("c-schedule__list-item")) {
                MenuItem menuItem = parseLiElement(element);

                if (menuItem.getMenuType() == null) {
                    if (lastMenuTypeName.length() == 0) {
                        Main.notifyAdminUrgently("[Error] MenuTypeName is unknown for first element!");
                    }
                    menuItem.setMenuType(lastMenuTypeName);
                } else {
                    lastMenuTypeName = menuItem.getMenuType();
                }

                addMenuItem((isTomorrow ? types_tomorrow : types_today), menuItem);
            }
        } else {
            Main.notifyAdminUrgently("[Error] Could not find menu for " + calendarDay(calendarToBeScraped));
        }
    }

    private static MenuItem parseLiElement(Element element) {
        String menuType = null;

        Element menuTypeElement = element.select(".stwm-artname").first();
        if (menuTypeElement != null && menuTypeElement.text().length() > 0) {
            menuType = menuTypeElement.text();
        }

        String menuText = element.select(".js-schedule-dish-description").first().ownText();
        EatingHabit eatingHabit = EatingHabit.NONE;

        switch (element.attr("data-essen-fleischlos")) {
            case "2":
                eatingHabit = EatingHabit.VEGAN;
                break;
            case "1":
                eatingHabit = EatingHabit.VEGETARIAN;
                break;
            default:
                if (element.attr("data-essen-typ").contains("S")
                        && element.attr("data-essen-typ").contains("R")) {
                    eatingHabit = EatingHabit.PIG_AND_COW;
                } else if (element.attr("data-essen-typ").contains("S")) {
                    eatingHabit = EatingHabit.PIG;
                    break;
                } else if (element.attr("data-essen-typ").contains("R")) {
                    eatingHabit = EatingHabit.COW;
                    break;
                }
                break;
        }

        ArrayList<Allergen> allergens = new ArrayList<>();
        String temp = element.select(".c-schedule__marker--allergen").text();

        if (temp.length() > 2) {
            String[] unParsedAllergens = temp.substring(1, temp.length() - 1).split(",");

            for (String allergen : unParsedAllergens) {
                allergens.add(AllergenName.getInstance().getAllergenFromShort(allergen));
            }
        }

        return new MenuItem(menuType, menuText.replace(" (GQB)", "").replace("(GWB)", ""), eatingHabit, allergens);
    }

    private String getURL(Calendar calendar) {
        return "http://www.studentenwerk-muenchen.de/mensa/speiseplan/speiseplan_" + calendar.get(Calendar.YEAR)
                + "-" + (calendar.get(Calendar.MONTH) + 1 < 10 ? "0" : "") + (calendar.get(Calendar.MONTH) + 1)
                + "-" + (calendar.get(Calendar.DAY_OF_MONTH) < 10 ? "0" : "") + calendar.get(Calendar.DAY_OF_MONTH) + "_" + urlId + "_-en.html";
    }

    private void addMenuItem(ArrayList<MenuType> types, MenuItem item) {
        for (MenuType menuType : types) {
            if (menuType.getMenuType() == null && item.getMenuType() == null) {
                menuType.addMenuItem(item);
                return;
            } else if (menuType.getMenuType() != null && item.getMenuType() != null
                    && menuType.getMenuType().equals(item.getMenuType())) {
                for (MenuItem alreadyAdded : menuType.getItems()) {
                    if (alreadyAdded.getMenuText().equals(item.getMenuText())) {
                        return;
                    }
                }
                menuType.addMenuItem(item);
                return;
            }
        }
        types.add(new MenuType(item));
    }

    public String getStyledString(boolean withEmojis, ArrayList<Allergen> allergicTo, EatingHabit eatingHabit, boolean isTomorrow) {
        StringBuilder sb = new StringBuilder("*");
        sb.append(CanteenName.getCanteenName(type, location)).append("*\n*");

        Calendar calendar = getCalendar(isTomorrow);

        sb.append(calendarDay(calendar)).append(", ");
        sb.append(calendar.get(Calendar.DAY_OF_MONTH)).append(".").append(calendar.get(Calendar.MONTH) + 1).append(".").append(calendar.get(Calendar.YEAR));

        sb.append("*\n");

        if ((isTomorrow ? types_tomorrow : types_today).size() == 0) {
            sb.append("\nThere is no menu for that day. :(");
        } else {
            for (MenuType menuType : (isTomorrow ? types_tomorrow : types_today)) {
                if (menuType.getMenuType() != null) {
                    sb.append("\n*").append(menuType.getMenuType());

                    String dishPrice = MenuPrice.getInstance().getMenuPrice(menuType.getMenuType());

                    if (dishPrice != null) {
                        sb.append(" » ").append(dishPrice).append(" €");
                    }

                    sb.append("*\n");
                } else {
                    sb.append("\n\n");
                }

                for (MenuItem item : menuType.getItems()) {
                    sb.append(item.getStyledText(withEmojis, allergicTo, eatingHabit));
                }
            }
        }


        return sb.toString();
    }

    private String calendarDay(Calendar calendar) {
        switch (calendar.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:
                return "Monday";
            case Calendar.TUESDAY:
                return "Tuesday";
            case Calendar.WEDNESDAY:
                return "Wednesday";
            case Calendar.THURSDAY:
                return "Thursday";
            case Calendar.FRIDAY:
                return "Friday";
            case Calendar.SATURDAY:
                return "Saturday";
            case Calendar.SUNDAY:
                return "Sunday";
        }
        return "";
    }

    private Calendar getCalendar(boolean isTomorrow) {
        Calendar calendar = skipToNextWeekday(Calendar.getInstance());
        if (isTomorrow) {
            calendar.setTimeInMillis(calendar.getTimeInMillis() + 1000 * 60 * 60 * 24);
            calendar = skipToNextWeekday(calendar);
        }
        return calendar;
    }

    private Calendar skipToNextWeekday(Calendar calendar) {
        while (!Main.isWeekday(calendar)) {
            calendar.setTimeInMillis(calendar.getTimeInMillis() + 1000 * 60 * 60 * 24);
        }
        return calendar;
    }
}
