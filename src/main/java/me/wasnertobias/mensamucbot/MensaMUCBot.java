package me.wasnertobias.mensamucbot;

import me.wasnertobias.mensamucbot.menu.Allergen;
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
import java.util.List;

public class MensaMUCBot extends TelegramLongPollingBot {
    private ArrayList<UserConfig> userConfigs;
    private File userDatFile;
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

    String saveUserConfigs() {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(userDatFile));
            bufferedWriter.write(UserConfig.encodeUserConfigs(userConfigs));
            bufferedWriter.close();
            return "[Info] Saved user configs!";
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "[Error] Failed to save user configs!";
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

            String[] status = userConfig.getUserConfig("status").split("|");

            if (status.length > 0) {

                switch (status[0]) {
                    case "start":

                        return;
                    case "today":

                        return;
                    case "subscriptions":

                        return;
                    case "settings":

                        return;
                }
            }


            sendBareMessage(update.getMessage().getChatId(), "Sorry, I can't understand you. To get back to the main menu use /start.");
        }
    }

    void sendLocationMenu(UserConfig userConfig) {
        // TODO!
        ArrayList<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("Allergies >");
        rows.add(row);

        row = new KeyboardRow();
        row.add("Eating habits >");
        rows.add(row);

        row = new KeyboardRow();
        row.add("< Back");
        rows.add(row);

        userConfig.setUserConfig("status", "settings");
        sendBareMessage(userConfig.getChatId(), "Where?", false, rows);
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
        row.add("< Back");
        rows.add(row);

        userConfig.setUserConfig("status", "settings");
        sendBareMessage(userConfig.getChatId(), "What do you want to set?", false, rows);
    }

    void navigateToMainMenu(UserConfig userConfig) {
        ArrayList<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("Today >");
        row.add("Tomorrow >");
        rows.add(row);

        row = new KeyboardRow();
        row.add("Subscriptions >");
        rows.add(row);

        row = new KeyboardRow();
        row.add("Settings >");
        rows.add(row);

        userConfig.setUserConfig("status", "start");
        sendBareMessage(userConfig.getChatId(), "What do you want to do?", false, rows);
    }

    int userConfigSize() {
        return userConfigs.size();
    }

    private void removeUserConfig(UserConfig userConfig) {
        userConfigs.remove(userConfig);
        saveUserConfigs();
        System.out.println("[Info] I lost a user! " + userConfigs.size() + " in total now. :(");
    }

    private UserConfig createUserConfig(long chatID) {
        UserConfig userConfig = getUserConfig(chatID);

        if (userConfig == null) {
            userConfig = new UserConfig(chatID);
            userConfig.setUserConfig("status", "start");
            userConfigs.add(userConfig);
            System.out.println("[Info] I got a new user! " + userConfigs.size() + " in total now. :)");
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
            String[] subscriptions = userConfig.getUserConfig("subscriptions").split("|");

            for (String subscription1 : subscriptions) {
                String[] subscription = subscription1.split(",");
                int timeId = Integer.parseInt(subscription[0]);
                int dayId = Integer.parseInt(subscription[1]);

                if (timeId == currentTimeId && dayId == currentDayId) {
                    int canteenId = Integer.parseInt(subscription[2]);
                    notifyUser(userConfig, canteenId, false);
                }
            }
        }

        if (currentTimeId == 12) {
            sendSlackNotification();
        }
    }

    private void notifyUser(UserConfig userConfig, int canteenId, boolean isTomorrow) {
        sendBareMessage(userConfig.getChatId(), Main.getUserNotification(getAllergies(userConfig), getEatingHabit(userConfig), isTomorrow, canteenId));
    }

    private EatingHabit getEatingHabit(UserConfig userConfig) {
        String eatingHabit = userConfig.getUserConfig("eatinghabit");

        switch (eatingHabit) {
            case "vegan":
                return EatingHabit.VEGAN;
            case "vegeterian":
                return EatingHabit.VEGETARIAN;
            case "pig":
                return EatingHabit.PIG;
            default:
                return EatingHabit.NONE;
        }
    }

    private ArrayList<Allergen> getAllergies(UserConfig userConfig) {
        ArrayList<Allergen> allergens = new ArrayList<>();
        String[] allergies = userConfig.getUserConfig("allergies").split("|");

        for (String allergen : allergies) {
            allergens.add(Allergen.valueOf(allergen));
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
                sb.append("|");
            }
            sb.append(allergen.toString());
            isFirst = false;
        }

        userConfig.setUserConfig("allergies", sb.toString());
    }

    String sendSlackNotification() {
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
