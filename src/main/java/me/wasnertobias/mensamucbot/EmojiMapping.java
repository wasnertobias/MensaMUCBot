package me.wasnertobias.mensamucbot;

import com.vdurmont.emoji.EmojiParser;

public class EmojiMapping {
    private static EmojiMapping instance;
    private static String[] emojiMapping = {"süßkartoffel", "sweet_potato", "orange", "tangerine", "keks", "cookie", "cookie", "cookie", "honig", "honey_pot", "arrabiata", "hot_pepper", "ente", "duck", "krokette", "potato", "sushi", "sushi", "garnele", "shrimp", "shrimp", "fried_shrimp", "fisch", "fish", "salat", "green_salad", "gurken", "cucumber", "erdbeeren", "strawberry", "reis", "rice", "suppe", "stew", "pommes", "fries", "kartoffel", "potato", "schoko", "chocolate_bar", "banane", "banana", "spaghetti", "spaghetti", "apfel", "apple", "birne", "pear", "pfirsich", "peach", "tomate", "tomato", "kirsch", "cherries", "käse", "cheese", "bier", "beer", "donut", "doughnut", "eier", "egg", "burger", "hamburger", "pizza", "pizza", "aubergine", "eggplant", "kiwi", "kiwifruit", "karotte", "carrot", "möhre", "carrot", "pfannkuchen", "pancakes", "mais", "corn", "ananas", "pineapple", "tortilla", "taco", "scharf", "hot_pepper", "chili", "hot_pepper", "erdnüssen", "peanuts", "erdnuss", "peanuts", "kuchen", "cake"};

    private EmojiMapping() {
        for (int i = 1; i < emojiMapping.length; i += 2) {
            String emoji = ":" + emojiMapping[i] + ":";
            if (EmojiParser.parseToUnicode(emoji).equals(emoji)) {
                Main.notifyAdminUrgently("[Error] Emoji " + emoji + " is not working! :(");
            }
        }
    }

    public static EmojiMapping getInstance() {
        if (instance == null) {
            instance = new EmojiMapping();
        }
        return instance;
    }

    public String addEmojis(String meal) {
        StringBuilder sb = new StringBuilder();
        meal = meal.toLowerCase();

        for (int i = 0; i < emojiMapping.length - 1; i += 2) {
            if (meal.contains(emojiMapping[i])) {
                sb.append(':').append(emojiMapping[i + 1]).append(':');
            }
        }

        return EmojiParser.parseToUnicode(sb.toString());
    }
}
