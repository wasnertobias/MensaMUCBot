package me.wasnertobias.mensamucbot;

import com.vdurmont.emoji.EmojiParser;

import java.io.*;

public class EmojiMapping {
    private static EmojiMapping instance;
    private static String[] emojiMapping = {};
    private File emojiMappingFile = null;

    private EmojiMapping() {
        emojiMappingFile = new File("smile.txt");

        if (!emojiMappingFile.exists()) {
            try {
                if (emojiMappingFile.createNewFile()) {
                    System.out.println("[Info] Created new file smile.txt!");
                } else {
                    System.out.println("[Error] Failed to create new file smile.txt!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            BufferedReader br = new BufferedReader(new FileReader(emojiMappingFile));
            String line = br.readLine();
            emojiMapping = line.split(", ");
            System.out.println("[Info] Read " + emojiMapping.length/2 + " emojiMappings!");
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int i = 1; i < emojiMapping.length; i += 2) {
            String emoji = emojiMapping[i];
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

    public String addMapping(String search, String replace) {
        search = search.toLowerCase();
        replace = replace.toLowerCase();
        String emoji = ":" + replace + ":";

        for (int i = 0; i < emojiMapping.length - 1; i += 2) {
            if (search.equals(emojiMapping[i])) {
                return "[Error] search already existing!";
            }
        }

        if (EmojiParser.parseToUnicode(emoji).equals(emoji)) {
            return "[Error] Invalid emoji-replace mapping!";
        }

        String[] tmp = new String[emojiMapping.length + 2];

        for (int i = 0; i < emojiMapping.length; i++) {
            tmp[i] = emojiMapping[i];
        }

        tmp[emojiMapping.length] = search;
        tmp[emojiMapping.length + 1] = emoji;

        emojiMapping = tmp;

        return saveAllMappings();
    }

    public String appendEmojis(String meal) {
        StringBuilder sb = new StringBuilder();
        meal = meal.toLowerCase();

        for (int i = 0; i < emojiMapping.length - 1; i += 2) {
            if (meal.contains(emojiMapping[i])) {
                sb.append(EmojiParser.parseToUnicode(emojiMapping[i + 1]));
            }
        }

        return EmojiParser.parseToUnicode(sb.toString());
    }

    private String saveAllMappings() {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(emojiMappingFile));

            StringBuilder sb = new StringBuilder();

            if (emojiMapping.length != 0) {
                sb.append(emojiMapping[0]);
            }

            for (int i = 1; i < emojiMapping.length; i++) {
                sb.append(", ").append(emojiMapping[i]);
            }

            bufferedWriter.write(sb.toString());
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            return "[Error] while saving!";
        }
        return "[Info] Successfully added and saved in file! :)";
    }
}
