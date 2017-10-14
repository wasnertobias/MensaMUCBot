package me.wasnertobias.mensamucbot.menu;

import com.vdurmont.emoji.EmojiParser;
import me.wasnertobias.mensamucbot.EatingHabit;
import me.wasnertobias.mensamucbot.EmojiMapping;

import java.util.ArrayList;

public class MenuItem {
    private String menuType = null, menuText;
    private EatingHabit containedEatingHabit;
    private ArrayList<Allergen> containedAllergens;

    public MenuItem(String menuType, String menuText, EatingHabit containedEatingHabit, ArrayList<Allergen> containedAllergens) {
        this.menuType = menuType;
        this.menuText = menuText;
        this.containedEatingHabit = containedEatingHabit;
        this.containedAllergens = containedAllergens;
    }

    public String getStyledText(boolean withEmojis, ArrayList<Allergen> allergicTo, EatingHabit eatingHabit) {
        StringBuilder sb = new StringBuilder();
        String canNotEat = canNotEatString(allergicTo, eatingHabit);

        sb.append(" *>* ");

        if (!canNotEat.isEmpty()) {
            sb.append('[');
        }
        sb.append(menuText);
        if (!canNotEat.isEmpty()) {
            sb.append("]");
        }

        sb.append(' ');

        if (canNotEat.isEmpty()) {
            if (containedEatingHabit.equals(EatingHabit.VEGAN)) {
                sb.append("__(vegan)__");
            } else if (containedEatingHabit.equals(EatingHabit.VEGETARIAN)) {
                sb.append("__(v)__");
            } else if (containedEatingHabit.equals(EatingHabit.PIG_AND_COW)) {
                sb.append(EmojiParser.parseToUnicode(":pig::cow:"));
            } else if (containedEatingHabit.equals(EatingHabit.PIG)) {
                sb.append(EmojiParser.parseToUnicode(":pig:"));
            } else if (containedEatingHabit.equals(EatingHabit.COW)) {
                sb.append(EmojiParser.parseToUnicode(":cow:"));
            }

            if (withEmojis) {
                sb.append(EmojiMapping.getInstance().addEmojis(menuText));
            }
        } else {
            sb.append("*(").append(canNotEat).append(")*");
        }

        sb.append('\n');
        return sb.toString();
    }

    private String canNotEatString(ArrayList<Allergen> allergicTo, EatingHabit eatingHabit) {
        if (eatingHabit != null) {
            if (eatingHabit.equals(EatingHabit.VEGAN) && !containedEatingHabit.equals(EatingHabit.VEGAN)) {
                return "not vegan";
            }
            if (eatingHabit.equals(EatingHabit.VEGETARIAN)) {
                if (!containedEatingHabit.equals(EatingHabit.VEGETARIAN) && !containedEatingHabit.equals(EatingHabit.VEGAN)) {
                    return "not vegetarian";
                }
            }
            if (eatingHabit.equals(EatingHabit.PIG)) {
                if (!containedEatingHabit.equals(EatingHabit.PIG) && !containedEatingHabit.equals(EatingHabit.PIG_AND_COW)) {
                    return "contains pig";
                }
            }
        }

        if (containedAllergens != null && allergicTo != null &&
                containedAllergens.size() != 0 && allergicTo.size() != 0) {
            StringBuilder sb = new StringBuilder();
            boolean isFirst = true;

            for (Allergen allergicToItem : allergicTo) {
                if (containedAllergens.contains(allergicToItem)) {
                    sb.append(isFirst ? "contains " : ", ").append(AllergenName.getInstance().getAllergenName(allergicToItem));
                    isFirst = false;
                }
            }

            if (!isFirst) {
                return sb.toString();
            }
        }

        return "";
    }

    public String getMenuType() {
        return menuType;
    }
}
