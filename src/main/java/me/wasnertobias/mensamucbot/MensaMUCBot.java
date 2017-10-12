package me.wasnertobias.mensamucbot;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.telegram.telegrambots.api.methods.BotApiMethod;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
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

            if (update.getMessage().getText().equals("/stop")) {
                UserConfig userConfig = getUserConfig(update.getMessage().getChatId());

                if (userConfig != null) {
                    removeUserConfig(userConfig);
                }

                sendBareMessage(update.getMessage().getChatId(), "Sorry, but what have I done wrong? I will shut up now. :(");
                return;
            }

            if (update.getMessage().getText().equals("/start")) {
                UserConfig userConfig = getUserConfig(update.getMessage().getChatId());

                if (userConfig != null) {
                    removeUserConfig(userConfig);
                }

                StringBuilder sb = new StringBuilder("*Please choose a canteen* (not all are supported yet)\n\n");

                for (int i = 0; i < Main.canteens.length; i++) {
                    sb.append("/choose").append(i + 1).append(" ").append(Main.canteens[i]).append('\n');
                }

                sendBareMessage(update.getMessage().getChatId(), sb.toString());

                if (userConfig == null) {
                    sendBareMessage(update.getMessage().getChatId(), "Welcome human. If you find any bugs please refer to my master @wasnertob24.");
                }
            } else if (update.getMessage().getText().startsWith("/choose")) {
                int temp;
                try {
                    temp = Integer.parseInt(update.getMessage().getText().substring(7));
                } catch (NumberFormatException e) {
                    sendBareMessage(update.getMessage().getChatId(), "Please directly click at the command.");
                    return;
                }
                temp--;

                if (temp >= 0 && temp < Main.canteens.length) {
                    if (Main.urls[temp].length() == 0) {
                        sendBareMessage(update.getMessage().getChatId(), "Sorry, due to technical reasons this one isn't (yet) supported. You may want to choose another one?");
                        return;
                    }

                    createUserConfig(update.getMessage().getChatId()).setUserConfig("mensaID", temp + "");
                    saveUserConfigs();

                    if (Main.cache[temp] != null) {
                        sendBareMessage(update.getMessage().getChatId(), Main.cache[temp]);
                    }

                    sendBareMessage(update.getMessage().getChatId(), "Alright, you will get notified daily at 10:30 a.m.! To stop simply use /stop.");
                }
            } else if (update.getMessage().getChatId() == adminChatID) {
                Main.notifyAdminUrgently(Main.processAdminInput(update.getMessage().getText()));
            } else {
                sendBareMessage(update.getMessage().getChatId(), "Sorry, I can't understand you. If you want to choose another canteen simply use the command /start.");
            }
        }
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
            userConfigs.add(userConfig);
            System.out.println("[Info] I got a new user! " + userConfigs.size() + " in total now. :)");
        }

        return userConfig;
    }

    private UserConfig getUserConfig(long chatID) {
        for (UserConfig userConfig : userConfigs) {
            if (userConfig.getChatID() == chatID) {
                return userConfig;
            }
        }
        return null;
    }

    void notifyAdmin(String msg) {
        if (adminChatID != -1) {
            sendBareMessage(adminChatID, msg, false);
        }
    }

    void sendBareMessage(long chatID, String msg) {
        sendBareMessage(chatID, msg, true);
    }

    void sendBareMessage(long chatID, String msg, boolean enableMarkdown) {
        SendMessage message = new SendMessage()
                .setChatId(chatID)
                .setText(msg)
                .enableMarkdown(enableMarkdown);
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

    String sendNotifications() {
        for (UserConfig userConfig : userConfigs) {
            String msg = Main.cache[Integer.parseInt(userConfig.getUserConfig("mensaID"))];
            if (msg != null) {
                sendBareMessage(userConfig.getChatID(), msg);
            }
        }

        sendSlackNotification();

        return "[Info] Mahlzeit!";
    }

    String sendSlackNotification() {
        if (slackSecret == null) {
            Main.notifyAdminUrgently("[Error] Slack notification could not be sent, because slackSecret is null!");
        }

        try {
            Connection connection = Jsoup.connect("https://hooks.slack.com/services/" + slackSecret)
                    .header("Content-type", "application/json")
                    .requestBody("{\"text\":\"" + Main.cache[1] + "\"}")
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
            sendBareMessage(userConfig.getChatID(), broadcast);
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
