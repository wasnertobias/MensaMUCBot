package me.wasnertobias.mensamucbot;

import me.wasnertobias.mensamucbot.canteen.Canteen;
import me.wasnertobias.mensamucbot.canteen.CanteenType;
import me.wasnertobias.mensamucbot.menu.Allergen;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static me.wasnertobias.mensamucbot.canteen.CanteenType.*;

public class Main {
    private static ArrayList<Canteen> canteens = new ArrayList<>();
    private static MensaMUCBot mensaMUCBot;
    private static int helpCounter = 0;

    public static void main(String[] args) {
        canteens.add(new Canteen(MENSA, "Arcisstraße", 421));
        canteens.add(new Canteen(MENSA, "Garching", 422));
        canteens.add(new Canteen(MENSA, "Leopoldstraße", 411));
        canteens.add(new Canteen(MENSA, "Lothstraße", 431));
        canteens.add(new Canteen(MENSA, "Martinsried", 412));
        canteens.add(new Canteen(MENSA, "Pasing", 432));
        canteens.add(new Canteen(MENSA, "Weihenstephan", 423));
        canteens.add(new Canteen(BISTRO, "Arcisstraße", 450));
        canteens.add(new Canteen(BISTRO, "Goethestraße", 418));
        canteens.add(new Canteen(BISTRO, "Großhadern", 414));
        canteens.add(new Canteen(BISTRO, "Rosenheim", 441));
        canteens.add(new Canteen(BISTRO, "Schellingstraße", 416));
        canteens.add(new Canteen(CAFE, "Adalbertstraße", 512));
        canteens.add(new Canteen(CAFE, "Akademie Weihenstephan", 526));
        canteens.add(new Canteen(CAFE, "Boltzmannstraße", 527));
        canteens.add(new Canteen(CAFE, "in der Mensa Garching", 524));
        canteens.add(new Canteen(CAFE, "Karlstraße", 532));
        canteens.add(new Canteen(CAFE, "Pasing", 534));

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

                        if (split.length < 2) {
                            continue;
                        }

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
                return;
            }
        }

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
        scrapeCalendar.set(Calendar.MINUTE, 5);
        scrapeCalendar.set(Calendar.SECOND, 0);
        scrapeCalendar.set(Calendar.MILLISECOND, 0);

        if (scrapeCalendar.getTime().before(Calendar.getInstance().getTime())) {
            scrapeCalendar.setTimeInMillis(scrapeCalendar.getTimeInMillis() + 1000 * 60 * 60 * 24);
        }

        Timer scrapeTimer = new Timer();

        scrapeTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (isWeekday(Calendar.getInstance())) {
                    scrapeAll();
                }
                helpCounter = 0;
            }
        }, scrapeCalendar.getTime(), TimeUnit.HOURS.toMillis(24));

        Calendar notificationCalendar = Calendar.getInstance();

        notificationCalendar.set(Calendar.HOUR_OF_DAY, 7);
        notificationCalendar.set(Calendar.MINUTE, 0);
        notificationCalendar.set(Calendar.SECOND, 0);
        notificationCalendar.set(Calendar.MILLISECOND, 0);

        while (notificationCalendar.getTime().before(Calendar.getInstance().getTime())) {
            notificationCalendar.setTimeInMillis(notificationCalendar.getTimeInMillis() + 1000 * 60 * 15);
            helpCounter++;
        }

        Timer notificationTimer = new Timer();

        notificationTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Calendar calendar = Calendar.getInstance();
                if (isWeekday(calendar)) {
                    mensaMUCBot.sendNotifications(helpCounter, calendar.get(Calendar.DAY_OF_WEEK));
                    helpCounter++;
                }
            }
        }, notificationCalendar.getTime(), TimeUnit.MINUTES.toMicros(15));

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
    }

    public static ArrayList<Canteen> getCanteens() {
        return canteens;
    }

    public static String getSlackText() {
        for (Canteen canteen : canteens) {
            if (canteen.getType().equals(CanteenType.MENSA) && canteen.getLocation().equals("Garching")) {
                return canteen.getStyledString(true, null, null, false);
            }
        }
        return "";
    }

    public static String getUserNotification(ArrayList<Allergen> allergies, EatingHabit eatingHabit, boolean isTomorrow, int canteenId, boolean emojisDisabled) {
        for (Canteen canteen : canteens) {
            if (canteen.getUrlId() == canteenId) {
                return canteen.getStyledString(!emojisDisabled, allergies, eatingHabit, isTomorrow);
            }
        }
        return "";
    }

    public static boolean isWeekday(Calendar calendar) {
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        return (dayOfWeek != Calendar.SUNDAY && dayOfWeek != Calendar.SATURDAY);
    }

    static String processAdminInput(String string) {
        if (string.toLowerCase().startsWith("/broadcast ")) {
            return mensaMUCBot.sendBroadcast(string.substring(11));
        }
        if (string.toLowerCase().startsWith("/save")) {
            MensaMUCBot.saveUserConfigsInstantly();
            return "[Info] Saved!";
        }
        if (string.toLowerCase().startsWith("/count")) {
            return "[Info] Current count of users: " + mensaMUCBot.userConfigSize();
        }
        if (string.toLowerCase().startsWith("/slack")) {
            return mensaMUCBot.sendSlackNotification();
        }
        return "[Error] Say what?!";
    }

    public static void notifyAdminUrgently(String msg) {
        mensaMUCBot.notifyAdmin(msg);
        System.out.println(msg);
    }

    private static void scrapeAll() {
        new Thread(() -> {
            for (Canteen canteen : canteens) {
                canteen.scrapeNow();

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
