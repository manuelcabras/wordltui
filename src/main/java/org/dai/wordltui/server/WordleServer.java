package org.dai.wordltui.server;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class WordleServer {

    /**
     * A collection of words to choose from.
     *
     * All words should be of the same length.
     */
    private static ArrayList<String> dictionary = new ArrayList<>();

    private String wordToGuess;

    private int letterCount;

    private ArrayList<Character> wrongChars = new ArrayList<>();
    private ArrayList<Character> containedChars = new ArrayList<>();
    private char[] correctChars;

    /**
     * Constructs a Server instance.
     *
     * @param pathToWords
     *   A collection of same letter words, separated by newLine.
     * @param letterCount
     *   The letter count for each word.
     */
    public WordleServer(String pathToWords, int letterCount) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(pathToWords), StandardCharsets.UTF_8))) {

            dictionary = new ArrayList<String>();
            String line;
            while((line = reader.readLine()) != null) {
                if (line.length() != letterCount) {
                    throw new IOException("Some words don't respect the given " + letterCount + " letter length");
                }

                dictionary.add(line);
            }
        }
        catch(IOException e){
            throw new RuntimeException("The file you just submitted does not exist");
        }

        this.correctChars = new char[letterCount];
        Arrays.fill(this.correctChars, ' ');
        this.letterCount = letterCount;
    }

    /**
     * Constructs a Server instance with default values.
     */
    public WordleServer() {
        final String DEFAULT_FILE = "./src/main/resources/words.txt";
        final int DEFAULT_WORD_LENGTH = 5;

        this(DEFAULT_FILE, DEFAULT_WORD_LENGTH);
    }

    public String startGame() {
        wordToGuess = dictionary.get((int) (Math.random() * dictionary.size()));
        return wordToGuess;
    }

    public String handleGuess(String guess) {
        for(int i = 0; i < guess.length(); i++) {
            char c = guess.charAt(i);

            switch(isInWord(c, wordToGuess, i)) {
                case -1:
                    wrongChars.add(c);
                    break;
                case 0:
                    containedChars.add(c);
                    break;
                case 1:
                    correctChars[i] = c;
            }
        }

        return getResponseString();
    }

    private String getResponseString() {
        // ╦ ╩
        StringBuilder topLine = new StringBuilder();
        StringBuilder letters = new StringBuilder();
        StringBuilder bottomLine = new StringBuilder();

        topLine.append("╔");
        letters.append("║");
        bottomLine.append("╚");
        for(int i = 0; i < (letterCount * 2) - 1; i++) {
            topLine.append(i % 2 == 0 ? "═" : "╦");
            bottomLine.append(i % 2 == 0 ? "═" : "╩");
            letters.append(i % 2 == 0 ? this.correctChars[i/2] : "║");
        }

        topLine.append("╗\n");
        letters.append("║\n");
        bottomLine.append("╝\n");

        return topLine.append(letters.append(bottomLine)).toString();
    }

    /**
     * Checks if a char is in a word, gives a precision about the position.
     *
     * @param c
     *   The char to test for.
     * @param s
     *   The string to search in.
     * @param index
     *   The supposed index of the char.
     * @return
     *   Returns an int from {-1, 0, 1}
     *   -1: The char is not in the string
     *   0: The char is in the string at a different index
     *   1: The char is in the string at the given index
     */
    private int isInWord(char c, String s, int index) {
        if (s.indexOf(c) == -1) {
            return -1;
        }

        if (c == s.charAt(index)) {
            return 1;
        }

        return 0;
    }

}
