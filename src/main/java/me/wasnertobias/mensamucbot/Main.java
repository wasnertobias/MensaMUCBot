package me.wasnertobias.mensamucbot;

import com.vdurmont.emoji.EmojiParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Main {
    static String[] urls = {"http://www.studentenwerk-muenchen.de/mensa/speiseplan/speiseplan_421_-de.html", "http://www.studentenwerk-muenchen.de/mensa/speiseplan/speiseplan_422_-de.html", "http://www.studentenwerk-muenchen.de/mensa/speiseplan/speiseplan_411_-de.html", "http://www.studentenwerk-muenchen.de/mensa/speiseplan/speiseplan_431_-de.html", "http://www.studentenwerk-muenchen.de/mensa/speiseplan/speiseplan_412_-de.html", "http://www.studentenwerk-muenchen.de/mensa/speiseplan/speiseplan_432_-de.html", "http://www.studentenwerk-muenchen.de/mensa/speiseplan/speiseplan_423_-de.html", "http://www.studentenwerk-muenchen.de/mensa/speiseplan/speiseplan_450_-de.html", "", "http://www.studentenwerk-muenchen.de/mensa/speiseplan/speiseplan_418_-de.html", "http://www.studentenwerk-muenchen.de/mensa/speiseplan/speiseplan_414_-de.html", "http://www.studentenwerk-muenchen.de/mensa/speiseplan/speiseplan_441_-de.html", "http://www.studentenwerk-muenchen.de/mensa/speiseplan/speiseplan_416_-de.html", "", "http://www.studentenwerk-muenchen.de/mensa/speiseplan/speiseplan_512_-de.html", "http://www.studentenwerk-muenchen.de/mensa/speiseplan/speiseplan_526_-de.html", "", "http://www.studentenwerk-muenchen.de/mensa/speiseplan/speiseplan_527_-de.html", "http://www.studentenwerk-muenchen.de/mensa/speiseplan/speiseplan_524_-de.html", "", "http://www.studentenwerk-muenchen.de/mensa/speiseplan/speiseplan_532_-de.html", "", "", "http://www.studentenwerk-muenchen.de/mensa/speiseplan/speiseplan_534_-de.html", ""};
    static String[] canteens = {"Mensa Arcisstraße", "Mensa Garching", "Mensa Leopoldstraße", "Mensa Lothstraße", "Mensa Martinsried", "Mensa Pasing", "Mensa Weihenstephan", "StuBistroMensa Arcisstraße", "StuBistroMensa Benediktbeuern", "StuBistroMensa Goethestraße", "StuBistroMensa Großhadern", "StuBistroMensa Rosenheim", "StuBistroMensa Schellingstraße", "StuBistroMensa Schillerstraße", "StuCafé Adalbertstraße", "StuCafé Akademie Weihenstephan", "StuCafé Audimax", "StuCafé Boltzmannstraße", "StuCafé in der Mensa Garching", "StuCafé Heßstraße", "StuCafé Karlstraße", "StuCafé Leopoldstraße", "StuCafé Olympiapark", "StuCafé Pasing", "StuCafé in der Mensa Weihenstephan"};
    static String[] cache = new String[urls.length];
    static String[] emojiMapping = {"süßkartoffel", "sweet_potato", "orange", "tangerine", "keks", "cookie", "cookie", "cookie", "honig", "honey_pot", "arrabiata", "hot_pepper", "ente", "duck", "krokette", "potato", "sushi", "sushi", "garnele", "shrimp", "shrimp", "fried_shrimp", "fisch", "fish", "salat", "green_salad", "gurken", "cucumber", "erdbeeren", "strawberry", "reis", "rice", "suppe", "stew", "pommes", "fries", "kartoffel", "potato", "schoko", "chocolate_bar", "banane", "banana", "spaghetti", "spaghetti", "apfel", "apple", "birne", "pear", "pfirsich", "peach", "tomate", "tomato", "kirsch", "cherries", "käse", "cheese", "bier", "beer", "donut", "doughnut", "eier", "egg", "burger", "hamburger", "pizza", "pizza", "aubergine", "eggplant", "kiwi", "kiwifruit", "karotte", "carrot", "möhre", "carrot", "pfannkuchen", "pancakes", "mais", "corn", "ananas", "pineapple", "tortilla", "taco", "scharf", "hot_pepper", "chili", "hot_pepper", "erdnüssen", "peanuts", "erdnuss", "peanuts", "kuchen", "cake"};
    static HashMap<String, String> menuPrices = new HashMap<>();
    static HashMap<String, String> allergenes = new HashMap<>();
    private static MensaMUCBot mensaMUCBot;

    public static void main(String[] args) {
        String telegramBotToken = null, slackSecret = null, telegramBotName = null;
        long telegramAdminChatID = -1;

        File file = new File("config.dat");
        boolean fileCreated = true;

        try {
            fileCreated = file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!fileCreated) {
            FileReader fileReader = null;

            try {
                fileReader = new FileReader(new File("config.dat"));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            if (fileReader != null) {
                BufferedReader bufferedReader = new BufferedReader(fileReader);

                String line;

                try {
                    while ((line = bufferedReader.readLine()) != null) {
                        String[] split = line.split("=");

                        if (split[0].equalsIgnoreCase("telegramBotToken")) {
                            telegramBotToken = split[1];
                        } else if (split[0].equalsIgnoreCase("slackSecret")) {
                            slackSecret = split[1];
                        } else if (split[0].equalsIgnoreCase("telegramAdminChatID")) {
                            telegramAdminChatID = Long.parseLong(split[1]);
                        } else if (split[0].equalsIgnoreCase("telegramBotName")) {
                            telegramBotName = split[1];
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (telegramAdminChatID == -1 || telegramBotToken == null || slackSecret == null || telegramBotName == null || fileCreated) {
            try {
                FileWriter fileWriter = new FileWriter(file);

                fileWriter.append("telegramBotToken=").append(telegramBotToken == null ? "" : telegramBotToken).append('\n');
                fileWriter.append("slackSecret=").append(slackSecret == null ? "" : slackSecret).append('\n');
                fileWriter.append("telegramAdminChatID=").append(String.valueOf(telegramAdminChatID == -1 ? "" : telegramAdminChatID)).append('\n');
                fileWriter.append("telegramBotName=").append(telegramBotName == null ? "" : telegramBotName).append('\n');

                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (!fileCreated) {
                System.out.println("[Error] Secret configuration could not be loaded correctly! Cleaned config file! Please fill the needed information.");
                return;
            } else {
                System.out.println("[Info] New config file was created. Please fill the needed information.");
            }
        }

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

        allergenes.put("ei", "Hühnerei");
        allergenes.put("en", "Erdnuss");
        allergenes.put("fi", "Fisch");
        allergenes.put("gl", "Glutenhaltiges Getreide");
        allergenes.put("glw", "Weizen");
        allergenes.put("glr", "Roggen");
        allergenes.put("glg", "Gerste");
        allergenes.put("glh", "Hafer");
        allergenes.put("gld", "Dinkel");
        allergenes.put("kr", "Krebstiere");
        allergenes.put("lu", "Lupinen");
        allergenes.put("mi", "Milch und Laktose");
        allergenes.put("sc", "Schalenfrüchte");
        allergenes.put("scm", "Mandeln");
        allergenes.put("sch", "Haselnüsse");
        allergenes.put("scw", "Walnüsse");
        allergenes.put("scc", "Cashewnüsse");
        allergenes.put("scp", "Pistazien");
        allergenes.put("se", "Sesamsamen");
        allergenes.put("sf", "Senf");
        allergenes.put("sl", "Sellerie");
        allergenes.put("so", "Soja");
        allergenes.put("sw", "Schwefeldioxid und Sulfite");
        allergenes.put("wt", "Weichtiere");

        ApiContextInitializer.init();

        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();

        try {
            mensaMUCBot = new MensaMUCBot(telegramBotToken, slackSecret, telegramBotName, telegramAdminChatID);
            telegramBotsApi.registerBot(mensaMUCBot);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        Calendar scrapeCalendar = Calendar.getInstance();
        scrapeCalendar.set(Calendar.HOUR_OF_DAY, 0);
        scrapeCalendar.set(Calendar.MINUTE, 30);
        scrapeCalendar.set(Calendar.SECOND, 0);
        scrapeCalendar.set(Calendar.MILLISECOND, 0);

        if (scrapeCalendar.getTime().before(Calendar.getInstance().getTime())) {
            scrapeCalendar.set(Calendar.DAY_OF_YEAR, scrapeCalendar.get(Calendar.DAY_OF_YEAR) + 1);
        }

        Timer scrapeTimer = new Timer();

        scrapeTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (isWeekday()) {
                    scrapeAll();
                }
            }
        }, scrapeCalendar.getTime(), TimeUnit.HOURS.toMillis(24));

        Calendar notificationCalendar = Calendar.getInstance();

        notificationCalendar.set(Calendar.HOUR_OF_DAY, 10);
        notificationCalendar.set(Calendar.MINUTE, 30);
        notificationCalendar.set(Calendar.SECOND, 0);
        notificationCalendar.set(Calendar.MILLISECOND, 0);

        if (notificationCalendar.getTime().before(Calendar.getInstance().getTime())) {
            notificationCalendar.set(Calendar.DAY_OF_YEAR, notificationCalendar.get(Calendar.DAY_OF_YEAR) + 1);
        }

        Timer notificationTimer = new Timer();

        notificationTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (isWeekday()) {
                    mensaMUCBot.sendNotifications();
                }
            }
        }, notificationCalendar.getTime(), TimeUnit.HOURS.toMillis(24));

        scrapeAll();

        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);

            while (true) {
                String temp = scanner.nextLine();

                if (temp != null) {
                    System.out.println(processAdminInput(temp));
                }
            }
        }).start();

        for (int i = 1; i < emojiMapping.length; i += 2) {
            String emoji = ":" + emojiMapping[i] + ":";
            if (EmojiParser.parseToUnicode(emoji).equals(emoji)) {
                notifyAdminUrgently("[Error] Emoji " + emoji + " is not working! :(");
            }
        }
    }

    static boolean isWeekday() {
        int dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        return (dayOfWeek != Calendar.SUNDAY && dayOfWeek != Calendar.SATURDAY);
    }

    static String processAdminInput(String string) {
        if (string.toLowerCase().startsWith("/broadcast ")) {
            return mensaMUCBot.sendBroadcast(string.substring(11));
        }
        if (string.toLowerCase().startsWith("/save")) {
            return mensaMUCBot.saveUserConfigs();
        }
        if (string.toLowerCase().startsWith("/count")) {
            return "[Info] Current count of users: " + mensaMUCBot.userConfigSize();
        }
        if (string.toLowerCase().startsWith("/notify")) {
            return mensaMUCBot.sendNotifications();
        }
        if (string.toLowerCase().startsWith("/slack")) {
            return mensaMUCBot.sendSlackNotification();
        }
        return "[Error] Say what?!";
    }

    static void notifyAdminUrgently(String msg) {
        mensaMUCBot.notifyAdmin(msg);
        System.out.println(msg);
    }

    private static void scrapeAll() {
        new Thread(() -> {
            for (int i = 0; i < urls.length; i++) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (urls[i].length() == 0) {
                    continue;
                }

                cache[i] = startHTMLRequest(urls[i], canteens[i]);
            }
        }).start();
    }

    private static String startHTMLRequest(String url, String canteen) {
        System.out.println("[Info] Scraping URL: " + url);

        Document doc = null;
        try {
            doc = Jsoup.connect(url).get();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (doc == null) {
            notifyAdminUrgently("[Error] whilst loading URL: " + url);
            return "Error: The site wasn't loaded correctly! My master is already informed and will have a look into that!";
        }

        Elements elements = doc.select(".anker a");
        Element todayElement = null;

        for (Element element : elements) {
            String currentName = element.attr("name");
            if (currentName != null && currentName.equals("heute")) {
                todayElement = element.parent().parent();
                break;
            }
        }

        if (todayElement == null) {
            notifyAdminUrgently("[Error] Today's menu could not be found: " + url);
            return "Today's menu could not be found, probably it's not available anyway?";
        }

        String beilagenURL = null;

        for (Element element : todayElement.select("a")) {
            if (element.text().equals("Beilagen")) {
                beilagenURL = "http://www.studentenwerk-muenchen.de" + element.attr("href");
            }
        }

        try {
            System.out.println("[Info] Redirected to URL: " + beilagenURL);
            doc = Jsoup.connect(beilagenURL).get();
        } catch (IOException e) {
            e.printStackTrace();
        }

        todayElement = doc.select(".c-schedule__item").first();

        StringBuilder sb = new StringBuilder("*" + canteen + "*\n*" + todayElement.getElementsByTag("span").first().text() + "*\n");

        for (Element element : todayElement.getElementsByClass("c-schedule__list-item")) {
            sb.append(parseLiElement(element));
        }

        return sb.toString();
    }

    private static String parseLiElement(Element element) {
        StringBuilder sb = new StringBuilder();

        Element element2 = element.select(".stwm-artname").first();
        if (element2 != null && element2.text().length() > 0) {
            sb.append("\n*").append(element2.text());

            if (menuPrices.containsKey(element2.text().toLowerCase())) {
                sb.append(" » ").append(menuPrices.get(element2.text().toLowerCase())).append(" €");
            }
            sb.append("*\n");
        }

        String meal = element.select(".js-schedule-dish-description").first().ownText();
        sb.append(" *>* ").append(meal).append(' ');

        switch (element.attr("data-essen-fleischlos")) {
            case "2":
                sb.append("_(vegan)_ ");
                break;
            case "1":
                sb.append("_(v)_ ");
                break;
            default:
                if (element.attr("data-essen-typ").contains("S")) {
                    sb.append(EmojiParser.parseToUnicode(":pig:"));
                }
                if (element.attr("data-essen-typ").contains("R")) {
                    sb.append(EmojiParser.parseToUnicode(":cow:"));
                }
                break;
        }

        sb.append(addEmojis(meal)).append('\n');

        return sb.toString();
    }

    private static String addEmojis(String meal) {
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
