package me.wasnertobias.mensamucbot;

import me.wasnertobias.mensamucbot.canteen.Canteen;
import me.wasnertobias.mensamucbot.canteen.CanteenType;
import me.wasnertobias.mensamucbot.menu.Allergen;
import me.wasnertobias.mensamucbot.menu.AllergenName;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.telegram.telegrambots.api.methods.BotApiMethod;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.updateshandlers.SentCallback;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MensaMUCBot extends TelegramLongPollingBot {
    private static ArrayList<UserConfig> userConfigs;
    private static File userDatFile;
    private long adminChatID = -1;
    private String botToken, slackSecret, botName;

    MensaMUCBot(String botToken, String slackSecret, String botName, long adminChatID) {
        this.botToken = botToken;
        this.slackSecret = slackSecret;
        this.botName = botName;
        this.adminChatID = adminChatID;

        userDatFile = new File("user-config.dat");

        if (!userDatFile.exists()) {
            try {
                if (userDatFile.createNewFile()) {
                    System.out.println("[Info] Created new file user-config.dat!");
                } else {
                    System.out.println("[Error] Failed to create new file user-config.dat!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            List<String> lines = Files.readAllLines(Paths.get("user-config.dat"));
            userConfigs = UserConfig.decodeUserConfigs(lines);
            System.out.println("[Info] Read " + lines.size() + " user configs!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void saveUserConfigs() {
        if (timeoutThread == null) {
            timeoutThread = timeoutSaveThread(10000);
            timeoutThread.start();
        }
    }

    private static Thread timeoutThread = null;

    private static Thread timeoutSaveThread(int delay) {
        return new Thread(() -> {
            try {
                Thread.sleep(delay);
                timeoutThread = null;

                saveUserConfigsInstantly();
            } catch (Exception e) {
                System.out.println("[Error] " + e);
            }
        });
    }

    public static void saveUserConfigsInstantly() {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(userDatFile));
            bufferedWriter.write(UserConfig.encodeUserConfigs(userConfigs));
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onUpdateReceived(Update update) {
        // We check if the update has a message and the message has text
        if (update.hasMessage() && update.getMessage().hasText()) {
            System.out.println(update.getMessage().getChatId() + ": " + update.getMessage().getText());

            UserConfig userConfig = getUserConfig(update.getMessage().getChatId());

            if (userConfig == null) {
                userConfig = createUserConfig(update.getMessage().getChatId());
                sendBareMessage(userConfig.getChatId(), "Welcome! To stop at any time you may want to use the command /stop. All your data incl. preferences will be deleted instantly.");
                navigateToMainMenu(userConfig);
                return;
            }

            if (update.getMessage().getText().equalsIgnoreCase("/stop")) {
                removeUserConfig(userConfig);
                sendBareMessage(userConfig.getChatId(), "Sorry, but what have I done wrong? I will shut up now. :(");
                return;
            }

            if (update.getMessage().getText().equalsIgnoreCase("/start")) {
                navigateToMainMenu(userConfig);
                return;
            }

            if (update.getMessage().getText().equalsIgnoreCase("/help")) {
                sendBareMessage(userConfig.getChatId(), "Use the keyboard to navigate through the menu. Type / in the chat to see which commands are available.");
                return;
            }

            if (update.getMessage().getText().equalsIgnoreCase("/settings")) {
                navigateToSettingsMenu(userConfig);
                return;
            }

            if (update.getMessage().getText().startsWith("/")) {
                if (update.getMessage().getChatId() == adminChatID) {
                    Main.notifyAdminUrgently(Main.processAdminInput(update.getMessage().getText()));
                    return;
                }
            }

            if (update.getMessage().getText().equals("< Back")) {
                navigateToMainMenu(userConfig);
                return;
            }

            String[] status = userConfig.getUserConfig("status").split("/");

            if (status.length > 0) {
                switch (status[0]) {
                    case "":
                        switch (update.getMessage().getText()) {
                            case "Today >":
                                userConfig.setUserConfig("status", "today");
                                saveUserConfigs();
                                sendLocationMenu(userConfig);
                                return;
                            case "Tomorrow >":
                                userConfig.setUserConfig("status", "tomorrow");
                                saveUserConfigs();
                                sendLocationMenu(userConfig);
                                return;
                            case "Subscriptions >":
                                userConfig.setUserConfig("status", "subscriptions");
                                saveUserConfigs();
                                sendLocationMenu(userConfig);
                                return;
                            case "Settings >":
                                navigateToSettingsMenu(userConfig);
                                return;
                        }
                        break;
                    case "today":
                        Canteen result = delegateToLocationMenu(userConfig, update.getMessage().getText(), (status.length > 1 ? status[1] : null));
                        if (result != null) {
                            sendBareMessage(userConfig.getChatId(), result.getStyledString(!areEmojisDisabled(userConfig), getAllergies(userConfig), getEatingHabit(userConfig), false));
                            navigateToMainMenu(userConfig);
                        }
                        return;
                    case "tomorrow":
                        Canteen result2 = delegateToLocationMenu(userConfig, update.getMessage().getText(), (status.length > 1 ? status[1] : null));
                        if (result2 != null) {
                            sendBareMessage(userConfig.getChatId(), result2.getStyledString(!areEmojisDisabled(userConfig), getAllergies(userConfig), getEatingHabit(userConfig), true));
                            navigateToMainMenu(userConfig);
                        }
                        return;
                    case "subscriptions":
                        if (status.length > 2) {
                            if (status.length == 3) {
                                // weekday?
                                int weekday = weekDayStringToInt(update.getMessage().getText());

                                if (weekday != -1) {
                                    int canteen = Integer.parseInt(status[2]);
                                    if (hasUserSubscribedWeekday(userConfig, canteen, weekday)) {
                                        removeSubscription(userConfig, weekday, canteen);
                                        sendBareMessage(userConfig.getChatId(), "Your subscription on that day is now deleted!");
                                        sendWeekDayMenu(userConfig, canteen);
                                        return;
                                    } else {
                                        userConfig.setUserConfig("status", userConfig.getUserConfig("status") + "/" + weekday);
                                        saveUserConfigs();
                                        sendHourMenu(userConfig);
                                        return;
                                    }
                                } else {
                                    sendBareMessage(userConfig.getChatId(), "Sorry, I can't understand you. Simply click on the buttons!");
                                    sendWeekDayMenu(userConfig, Integer.parseInt(status[2]));
                                    return;
                                }
                            } else if (status.length == 4) {
                                // hour?
                                int hour;
                                try {
                                    hour = Integer.parseInt(update.getMessage().getText());
                                    if (hour < 7 || hour > 15) {
                                        throw new NumberFormatException();
                                    }
                                } catch (NumberFormatException e) {
                                    sendBareMessage(userConfig.getChatId(), "Sorry, I can't understand you. Simply click on the buttons!");
                                    sendHourMenu(userConfig);
                                    return;
                                }

                                userConfig.setUserConfig("status", userConfig.getUserConfig("status") + "/" + hour);
                                saveUserConfigs();
                                sendMinuteMenu(userConfig, hour);
                                return;
                            } else if (status.length == 5) {
                                // minute?
                                int minute;
                                try {
                                    minute = Integer.parseInt(update.getMessage().getText().split(":")[1]);
                                    if (minute != 0 && minute != 15 && minute != 30 && minute != 45) {
                                        throw new NumberFormatException();
                                    }
                                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                                    sendBareMessage(userConfig.getChatId(), "Sorry, I can't understand you. Simply click on the buttons!");
                                    sendMinuteMenu(userConfig, Integer.parseInt(userConfig.getUserConfig("status").split("/")[4]));
                                    return;
                                }

                                int canteen = Integer.parseInt(status[2]);
                                int day = Integer.parseInt(status[3]);
                                int time = Integer.parseInt(status[4]) * 4 + (minute / 15);
                                addSubscription(userConfig, time, day, canteen);
                                sendBareMessage(userConfig.getChatId(), "Your subscription was set successfully!");
                                userConfig.setUserConfig("status", "subscriptions/" + status[1] + "/" + status[2]);
                                saveUserConfigs();
                                sendWeekDayMenu(userConfig, canteen);
                            }
                        } else {
                            Canteen result3 = delegateToLocationMenu(userConfig, update.getMessage().getText(), (status.length > 1 ? status[1] : null));
                            if (result3 != null) {
                                userConfig.setUserConfig("status", userConfig.getUserConfig("status") + "/" + result3.getUrlId());
                                saveUserConfigs();
                                sendWeekDayMenu(userConfig, result3.getUrlId());
                            }
                        }
                        return;
                    case "settings":
                        if (status.length >= 2) {
                            switch (status[1]) {
                                case "allergies":
                                    sendAllergiesMenu(userConfig, update.getMessage().getText());
                                    return;
                                case "habits":
                                    sendEatingHabitsMenu(userConfig, update.getMessage().getText());
                                    return;
                            }
                        }

                        switch (update.getMessage().getText()) {
                            case "Allergies >":
                                userConfig.setUserConfig("status", "settings/allergies");
                                sendAllergiesMenu(userConfig, null);
                                saveUserConfigs();
                                return;
                            case "Eating habits >":
                                userConfig.setUserConfig("status", "settings/habits");
                                sendEatingHabitsMenu(userConfig, null);
                                saveUserConfigs();
                                return;
                            case "< Back":
                                navigateToMainMenu(userConfig);
                                return;
                        }

                        if (update.getMessage().getText().contains("Emojis")) {
                            if (areEmojisDisabled(userConfig)) {
                                userConfig.setUserConfig("emoji_disabled", "");
                            } else {
                                userConfig.setUserConfig("emoji_disabled", "true");
                            }

                            saveUserConfigs();
                            sendBareMessage(userConfig.getChatId(), "Ok, emojis are now *" + (areEmojisDisabled(userConfig) ? "disabled" : "enabled") + "*!");
                            navigateToSettingsMenu(userConfig);
                            return;
                        }

                        break;
                }
            }

            sendBareMessage(update.getMessage().getChatId(), "Sorry, I can't understand you. To get back to the main menu use /start.");
        }
    }

    boolean areEmojisDisabled(UserConfig userConfig) {
        return userConfig.getUserConfig("emoji_disabled").equals("true");
    }

    void sendAllergiesMenu(UserConfig userConfig, String reply) {
        ArrayList<Allergen> allergies = getAllergies(userConfig);

        if (reply == null) {
            sendBareMessage(userConfig.getChatId(), "Simply click on a button to disable/enable a certain allergen (*scroll* through the buttons).\n\n*Notice: Filtering might not be 100% accurate! This software comes without any warranty of any kind!*");
        } else if (!reply.isEmpty()) {
            boolean containedSomething = false;

            for (Allergen allergen : Allergen.values()) {
                if (reply.contains(AllergenName.getInstance().getAllergenName(allergen))) {
                    if (allergies.contains(allergen)) {
                        removeAllergen(userConfig, allergen);
                        sendBareMessage(userConfig.getChatId(), "You *are not* allergic to " + AllergenName.getInstance().getAllergenName(allergen));
                        containedSomething = true;
                        allergies.remove(allergen);
                        break;
                    } else {
                        addAllergen(userConfig, allergen);
                        sendBareMessage(userConfig.getChatId(), "You *are* allergic to " + AllergenName.getInstance().getAllergenName(allergen));
                        containedSomething = true;
                        allergies.add(allergen);
                        break;
                    }
                }
            }

            if (!containedSomething) {
                sendBareMessage(userConfig.getChatId(), "Sorry, I can't understand you. Simply click on the buttons!");
            }
        }

        ArrayList<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row;

        for (Allergen allergen : Allergen.values()) {
            row = new KeyboardRow();
            row.add((allergies.contains(allergen) ? "✘" : "✔") + "\n" + AllergenName.getInstance().getAllergenName(allergen) + " >");
            rows.add(row);
        }

        row = new KeyboardRow();
        row.add("< Back");
        rows.add(row);

        sendBareMessage(userConfig.getChatId(), "What are you allergic to?", false, rows);
    }

    void sendEatingHabitsMenu(UserConfig userConfig, String reply) {
        if (reply != null) {
            if (reply.contains("Vegan")) {
                userConfig.setUserConfig("eatinghabit", "vegan");
            } else if (reply.contains("Vegetarian")) {
                userConfig.setUserConfig("eatinghabit", "vegetarian");
            } else if (reply.contains("No pig")) {
                userConfig.setUserConfig("eatinghabit", "pig");
            } else if (reply.contains("None")) {
                userConfig.setUserConfig("eatinghabit", "");
            } else {
                sendBareMessage(userConfig.getChatId(), "Sorry, I can't understand you. Simply click on the buttons!");
                sendEatingHabitsMenu(userConfig, null);
            }

            saveUserConfigs();
            sendBareMessage(userConfig.getChatId(), "Alright, your new eating habit is saved."
                    + (getEatingHabit(userConfig).equals(EatingHabit.NONE) ? "" : "\n\n*Notice: Filtering might not be 100% accurate! This software comes without any warranty of any kind!*"));
            navigateToMainMenu(userConfig);
        } else {
            ArrayList<KeyboardRow> rows = new ArrayList<>();

            KeyboardRow row = new KeyboardRow();
            row.add((getEatingHabit(userConfig).equals(EatingHabit.VEGAN) ? "✔\n" : "") + "Vegan >");
            rows.add(row);

            row = new KeyboardRow();
            row.add((getEatingHabit(userConfig).equals(EatingHabit.VEGETARIAN) ? "✔\n" : "") + "Vegetarian >");
            rows.add(row);

            row = new KeyboardRow();
            row.add((getEatingHabit(userConfig).equals(EatingHabit.PIG) ? "✔\n" : "") + "No pig >");
            rows.add(row);

            row = new KeyboardRow();
            row.add((getEatingHabit(userConfig).equals(EatingHabit.NONE) ? "✔\n" : "") + "None >");
            rows.add(row);

            row = new KeyboardRow();
            row.add("< Back");
            rows.add(row);

            sendBareMessage(userConfig.getChatId(), "Which eating habit do you have?", false, rows);
        }
    }

    int weekDayStringToInt(String weekday) {
        if (weekday.contains("Monday")) {
            return Calendar.MONDAY;
        } else if (weekday.contains("Tuesday")) {
            return Calendar.TUESDAY;
        } else if (weekday.contains("Wednesday")) {
            return Calendar.WEDNESDAY;
        } else if (weekday.contains("Thursday")) {
            return Calendar.THURSDAY;
        } else if (weekday.contains("Friday")) {
            return Calendar.FRIDAY;
        }
        return -1;
    }

    boolean hasUserSubscribedWeekday(UserConfig userConfig, int canteenUrlId, int dayId) {
        String[] subscriptions = userConfig.getUserConfig("subscriptions").split("/");

        for (String subscription1 : subscriptions) {
            String[] subscription = subscription1.split(",");

            if (subscription.length == 3) {
                int subscribedDayId = Integer.parseInt(subscription[1]);
                int subscribedCanteenUrlId = Integer.parseInt(subscription[2]);

                if (subscribedDayId == dayId && subscribedCanteenUrlId == canteenUrlId) {
                    return true;
                }
            }
        }
        return false;
    }

    void addSubscription(UserConfig userConfig, int timeId, int dayId, int canteenUrlId) {
        String subscriptions = userConfig.getUserConfig("subscriptions");
        userConfig.setUserConfig("subscriptions", (subscriptions.length() == 0 ? "" : subscriptions + "/") + timeId + "," + dayId + "," + canteenUrlId);
        saveUserConfigs();
    }

    void removeSubscription(UserConfig userConfig, int dayId, int canteenUrlId) {
        String[] subscriptions = userConfig.getUserConfig("subscriptions").split("/");
        StringBuilder filteredSubscriptionString = new StringBuilder();
        boolean isFirst = true;

        for (String currentSubscription : subscriptions) {
            String[] split = currentSubscription.split(",");

            if (!(Integer.parseInt(split[1]) == dayId && Integer.parseInt(split[2]) == canteenUrlId)) {
                if (!isFirst) {
                    filteredSubscriptionString.append("/");
                }
                filteredSubscriptionString.append(currentSubscription);
                isFirst = false;
            }
        }

        userConfig.setUserConfig("subscriptions", filteredSubscriptionString.toString());
        saveUserConfigs();
    }

    void sendWeekDayMenu(UserConfig userConfig, int canteenUrlId) {
        ArrayList<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add((hasUserSubscribedWeekday(userConfig, canteenUrlId, Calendar.MONDAY) ? "✔\n" : "") + "Monday >");
        row.add((hasUserSubscribedWeekday(userConfig, canteenUrlId, Calendar.TUESDAY) ? "✔\n" : "") + "Tuesday >");
        row.add((hasUserSubscribedWeekday(userConfig, canteenUrlId, Calendar.WEDNESDAY) ? "✔\n" : "") + "Wednesday >");
        rows.add(row);

        row = new KeyboardRow();
        row.add((hasUserSubscribedWeekday(userConfig, canteenUrlId, Calendar.THURSDAY) ? "✔\n" : "") + "Thursday >");
        row.add((hasUserSubscribedWeekday(userConfig, canteenUrlId, Calendar.FRIDAY) ? "✔\n" : "") + "Friday >");
        rows.add(row);

        row = new KeyboardRow();
        row.add("< Back");
        rows.add(row);

        sendBareMessage(userConfig.getChatId(), "Which day?\n(Type a already subscribed day to disable the subscription for that day.)", false, rows);
    }

    void sendHourMenu(UserConfig userConfig) {
        ArrayList<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("7");
        row.add("8");
        row.add("9");
        rows.add(row);

        row = new KeyboardRow();
        row.add("10");
        row.add("11");
        row.add("12");
        rows.add(row);

        row = new KeyboardRow();
        row.add("13");
        row.add("14");
        row.add("15");
        rows.add(row);

        row = new KeyboardRow();
        row.add("< Back");
        rows.add(row);

        sendBareMessage(userConfig.getChatId(), "Which hour?", false, rows);
    }

    void sendMinuteMenu(UserConfig userConfig, int hour) {
        ArrayList<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(hour + ":00");
        row.add(hour + ":15");
        rows.add(row);

        row = new KeyboardRow();
        row.add(hour + ":30");
        row.add(hour + ":45");
        rows.add(row);

        row = new KeyboardRow();
        row.add("< Back");
        rows.add(row);

        sendBareMessage(userConfig.getChatId(), "Which exact time?", false, rows);
    }

    Canteen delegateToLocationMenu(UserConfig userConfig, String msg, String category) {
        if (category == null) {
            switch (msg) {
                case "Mensa >":
                    sendPossibleCanteens(userConfig, "mensa");
                    userConfig.setUserConfig("status", userConfig.getUserConfig("status") + "/mensa");
                    saveUserConfigs();
                    return null;
                case "StuBisto Mensa >":
                    sendPossibleCanteens(userConfig, "bistro");
                    userConfig.setUserConfig("status", userConfig.getUserConfig("status") + "/bistro");
                    saveUserConfigs();
                    return null;
                case "StuCafé >":
                    sendPossibleCanteens(userConfig, "cafe");
                    userConfig.setUserConfig("status", userConfig.getUserConfig("status") + "/cafe");
                    saveUserConfigs();
                    return null;
                default:
                    sendBareMessage(userConfig.getChatId(), "Sorry, I can't understand you. Simply click on the buttons!");
                    sendLocationMenu(userConfig);
                    return null;
            }
        } else {
            ArrayList<Canteen> possibleCanteens = getPossibleCanteens(category);
            for (Canteen canteen : possibleCanteens) {
                if ((canteen.getLocation() + " >").equals(msg)) {
                    return canteen;
                }
            }
            sendBareMessage(userConfig.getChatId(), "Sorry, I can't understand you. Simply click on the buttons!");
            sendPossibleCanteens(userConfig, category);
        }

        return null;
    }

    void sendPossibleCanteens(UserConfig userConfig, String category) {
        ArrayList<Canteen> possibleCanteens = getPossibleCanteens(category);

        ArrayList<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row;

        for (int i = 0; i - 2 < possibleCanteens.size(); i += 2) {
            row = new KeyboardRow();
            for (int x = i; x < possibleCanteens.size() && x < i + 2; x++) {
                row.add(possibleCanteens.get(x).getLocation() + " >");
            }
            rows.add(row);
        }

        row = new KeyboardRow();
        row.add("< Back");
        rows.add(row);

        sendBareMessage(userConfig.getChatId(), "Where?", false, rows);
    }

    ArrayList<Canteen> getPossibleCanteens(String category) {
        ArrayList<Canteen> possibleCanteens = new ArrayList<>();
        for (Canteen canteen : Main.getCanteens()) {
            if (canteen.getType().equals(CanteenType.valueOf(category.toUpperCase()))) {
                possibleCanteens.add(canteen);
            }
        }
        return possibleCanteens;
    }

    void sendLocationMenu(UserConfig userConfig) {
        ArrayList<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("Mensa >");
        rows.add(row);

        row = new KeyboardRow();
        row.add("StuBisto Mensa >");
        rows.add(row);

        row = new KeyboardRow();
        row.add("StuCafé >");
        rows.add(row);

        row = new KeyboardRow();
        row.add("< Back");
        rows.add(row);

        sendBareMessage(userConfig.getChatId(), "Which type?", false, rows);
    }

    void navigateToSettingsMenu(UserConfig userConfig) {
        ArrayList<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("Allergies >");
        rows.add(row);

        row = new KeyboardRow();
        row.add("Eating habits >");
        rows.add(row);

        row = new KeyboardRow();
        row.add((areEmojisDisabled(userConfig) ? "✘\n" : "✔\n") + "Highlight with Emojis >");
        rows.add(row);

        // TODO: Add filtering instead of highlighting option

        row = new KeyboardRow();
        row.add("< Back");
        rows.add(row);

        userConfig.setUserConfig("status", "settings");
        saveUserConfigs();
        sendBareMessage(userConfig.getChatId(), "What do you want to set?", false, rows);
    }

    void navigateToMainMenu(UserConfig userConfig) {
        ArrayList<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        // TODO: Change static naming??
        row.add("Today >");
        row.add("Tomorrow >");
        rows.add(row);

        row = new KeyboardRow();
        row.add("Subscriptions >");
        rows.add(row);

        row = new KeyboardRow();
        row.add("Settings >");
        rows.add(row);

        userConfig.setUserConfig("status", "");
        saveUserConfigs();
        sendBareMessage(userConfig.getChatId(), "What do you want to do?", false, rows);
    }

    int userConfigSize() {
        return userConfigs.size();
    }

    private void removeUserConfig(UserConfig userConfig) {
        userConfigs.remove(userConfig);
        saveUserConfigs();
        notifyAdmin("[Info] I lost a user! " + userConfigs.size() + " in total now. :(");
    }

    private UserConfig createUserConfig(long chatID) {
        UserConfig userConfig = getUserConfig(chatID);

        if (userConfig == null) {
            userConfig = new UserConfig(chatID);
            userConfigs.add(userConfig);
            notifyAdmin("[Info] I got a new user! " + userConfigs.size() + " in total now. :)");
            saveUserConfigs();
        }

        return userConfig;
    }

    private UserConfig getUserConfig(long chatID) {
        for (UserConfig userConfig : userConfigs) {
            if (userConfig.getChatId() == chatID) {
                return userConfig;
            }
        }
        return null;
    }

    void notifyAdmin(String msg) {
        if (adminChatID != -1) {
            sendBareMessage(adminChatID, msg, false, null);
        }
    }

    void sendBareMessage(long chatID, String msg) {
        sendBareMessage(chatID, msg, true, null);
    }

    void sendBareMessage(long chatID, String msg, boolean enableMarkdown, List<KeyboardRow> keyboardRows) {
        SendMessage message = new SendMessage()
                .setChatId(chatID)
                .setText(msg)
                .enableMarkdown(enableMarkdown);

        if (keyboardRows != null) {
            ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
            replyKeyboardMarkup.setKeyboard(keyboardRows);
            message.setReplyMarkup(replyKeyboardMarkup);
        }

        try {
            executeAsync(message, new SentCallback<Message>() {
                @Override
                public void onResult(BotApiMethod<Message> botApiMethod, Message message) {

                }

                @Override
                public void onError(BotApiMethod<Message> botApiMethod, TelegramApiRequestException e) {
                    System.out.println("[Error] " + e);
                }

                @Override
                public void onException(BotApiMethod<Message> botApiMethod, Exception e) {
                    System.out.println("[Error] " + e);
                }
            });
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendNotifications(int currentTimeId, int currentDayId) {
        for (UserConfig userConfig : userConfigs) {
            String sub = userConfig.getUserConfig("subscriptions");

            if (!sub.isEmpty()) {
                String[] subscriptions = userConfig.getUserConfig("subscriptions").split("/");

                for (String subscription1 : subscriptions) {
                    String[] subscription = subscription1.split(",");
                    int timeId = Integer.parseInt(subscription[0]);
                    int dayId = Integer.parseInt(subscription[1]);

                    if (timeId == currentTimeId && dayId == currentDayId) {
                        int canteenUrlId = Integer.parseInt(subscription[2]);
                        notifyUser(userConfig, canteenUrlId, false);
                    }
                }
            }
        }

        if (currentTimeId == 42) {
            sendSlackNotification();
        }
    }

    private void notifyUser(UserConfig userConfig, int canteenUrlId, boolean isTomorrow) {
        sendBareMessage(userConfig.getChatId(), Main.getUserNotification(getAllergies(userConfig), getEatingHabit(userConfig), isTomorrow, canteenUrlId, areEmojisDisabled(userConfig)));
    }

    private EatingHabit getEatingHabit(UserConfig userConfig) {
        String eatingHabit = userConfig.getUserConfig("eatinghabit");

        switch (eatingHabit) {
            case "vegan":
                return EatingHabit.VEGAN;
            case "vegetarian":
                return EatingHabit.VEGETARIAN;
            case "pig":
                return EatingHabit.PIG;
            default:
                return EatingHabit.NONE;
        }
    }

    private ArrayList<Allergen> getAllergies(UserConfig userConfig) {
        ArrayList<Allergen> allergens = new ArrayList<>();
        String[] allergies = userConfig.getUserConfig("allergies").split("/");

        for (String allergen : allergies) {
            if (allergen.length() > 0) {
                allergens.add(Allergen.valueOf(allergen.toUpperCase()));
            }
        }

        return allergens;
    }

    private void addAllergen(UserConfig userConfig, Allergen allergen) {
        ArrayList<Allergen> allergens = getAllergies(userConfig);
        allergens.add(allergen);
        saveAllergies(userConfig, allergens);
    }

    private void removeAllergen(UserConfig userConfig, Allergen allergen) {
        ArrayList<Allergen> allergens = getAllergies(userConfig);
        allergens.remove(allergen);
        saveAllergies(userConfig, allergens);
    }

    private void saveAllergies(UserConfig userConfig, ArrayList<Allergen> allergies) {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;

        for (Allergen allergen : allergies) {
            if (!isFirst) {
                sb.append("/");
            }
            sb.append(allergen.toString());
            isFirst = false;
        }

        userConfig.setUserConfig("allergies", sb.toString());
        saveUserConfigs();
    }

    public String sendSlackNotification() {
        if (slackSecret == null) {
            Main.notifyAdminUrgently("[Error] Slack notification could not be sent, because slackSecret is null!");
        }

        try {
            Connection connection = Jsoup.connect("https://hooks.slack.com/services/" + slackSecret)
                    .header("Content-type", "application/json")
                    .requestBody("{\"text\":\"" + Main.getSlackText() + "\"}")
                    .method(Connection.Method.POST);
            Connection.Response response = connection.execute();

            if (response.statusCode() == 200) {
                return "[Info] Sent slack notification!";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "[Error] Failed to send slack notification!";
    }

    String sendBroadcast(String broadcast) {
        for (UserConfig userConfig : userConfigs) {
            sendBareMessage(userConfig.getChatId(), broadcast);
        }
        return "[Info] Sent broadcast: " + broadcast;
    }

    public String getBotUsername() {
        return botName;
    }

    public String getBotToken() {
        return botToken;
    }
}
