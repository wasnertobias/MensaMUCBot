package me.wasnertobias.mensamucbot;

import java.util.ArrayList;
import java.util.List;

class UserConfig {
    private long chatId;

    private ArrayList<String> configKeys = new ArrayList<>();
    private ArrayList<String> configValues = new ArrayList<>();

    UserConfig(long chatId) {
        this.chatId = chatId;
    }

    long getChatId() {
        return chatId;
    }

    private void encodeUserConfig(StringBuilder sb) {
        sb.append(chatId);
        sb.append(";");

        for (int i = 0; i < configKeys.size(); i++) {
            sb.append(configKeys.get(i)).append('=').append(configValues.get(i)).append(';');
        }

        sb.append('\n');
    }

    void setUserConfig(String key, String value) {
        int index = configKeys.indexOf(key);

        if (index < 0) {
            configKeys.add(key);
            configValues.add(value);
        } else {
            configValues.set(index, value);
        }
    }

    String getUserConfig(String key) {
        int index = configKeys.indexOf(key);

        if (index < 0) {
            return "";
        }

        return configValues.get(index);
    }

    static String encodeUserConfigs(ArrayList<UserConfig> list) {
        StringBuilder sb = new StringBuilder();

        for (UserConfig config : list) {
            config.encodeUserConfig(sb);
        }

        return sb.toString();
    }

    private static UserConfig decodeUserConfig(String string) {
        String[] split = string.split(";");
        UserConfig userConfig = new UserConfig(Long.parseLong(split[0]));

        for (int i = 1; i < split.length; i++) {
            String[] split2 = split[i].split("=");
            userConfig.setUserConfig(split2[0], split2[1]);
        }

        return userConfig;
    }

    static ArrayList<UserConfig> decodeUserConfigs(List<String> strings) {
        ArrayList<UserConfig> list = new ArrayList<>();

        for (String string : strings) {
            list.add(decodeUserConfig(string));
        }

        return list;
    }
}
