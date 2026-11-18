package org.dai.wordltui.server;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class WordleServer {

    /**
     * The max attempts amount.
     */
    private final int MAX_ATTEMPTS = 6;

    /**
     * The length of words in the dictionary.
     */
    private int letterCount;

    /**
     * A collection of words to choose from.
     *
     * All words must be of the same length.
     */
    private static final ArrayList<String> dictionary = new ArrayList<String>();

    /**
     * The word to guess. Keyed by client.
     */
    private final ConcurrentHashMap<String, String> wordToGuess = new ConcurrentHashMap<>();

    /**
     * The guessed characters that are not in the word. Keyed by client.
     */
    private final ConcurrentHashMap<String, ArrayList<Character>> wrongChars = new ConcurrentHashMap<>();

    /**
     * The guessed characters that are in the word but not in the right position. Keyed by client.
     */
    private final ConcurrentHashMap<String, ArrayList<Character>> containedChars = new ConcurrentHashMap<>();

    /**
     * The correctly guessed chars. Keyed by client.
     *
     * Chars that have not been guessed are spaces (== ' ')
     */
    private final ConcurrentHashMap<String, char[]> correctChars = new ConcurrentHashMap<>();

    /**
     * The amount of attempts for the current game. Keyed by client.
     */
    private final ConcurrentHashMap<String, Integer> attempts = new ConcurrentHashMap<>();

    /**
     * Constructs a Server instance.
     *
     * @param pathToWords
     *   A collection of same letter words, separated by newLine.
     * @param letterCount
     *   The letter count for each word.
     */
    public WordleServer(String pathToWords, int letterCount) {
        // Populate dictionary from given file.
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(pathToWords), StandardCharsets.UTF_8))) {

            String line;
            while((line = reader.readLine()) != null) {
                if (line.length() != letterCount) {
                    System.out.println("[Skipped] Word" + line + " does not respect the given " + letterCount + " letter length");
                    continue;
                }

                dictionary.add(line);
            }
        }
        catch(IOException e){
            throw new RuntimeException("The file you just submitted does not exist");
        }

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

    /**
     * Initializes a game.
     *
     * @return
     *   The new word to guess.
     */
    public String startGame(String clientName) {
        wordToGuess.put(clientName, dictionary.get((int) (Math.random() * dictionary.size())));
        attempts.put(clientName, 0);
        wrongChars.put(clientName, new ArrayList<>());
        containedChars.put(clientName, new ArrayList<>());
        correctChars.put(clientName, new char[letterCount]);
        Arrays.fill(correctChars.get(clientName), ' ');
        return wordToGuess.get(clientName);
    }

    /**
     * Handles a hint request.
     *
     * @param consonant
     *   Whether the hint requested is a consonant.
     *
     * @return
     *   A character contained in the word or ' ' if none can be given.
     */
    public char handleHint(boolean consonant, String clientName) {
        for(char c : wordToGuess.get(clientName).toCharArray()) {
            if (containedChars.get(clientName).contains(c)) {
                continue;
            }

            if (consonant && isVowel(c)) {
                continue;
            }

            if (!consonant && !isVowel(c)) {
                continue;
            }

            containedChars.get(clientName).add(c);
            return c;
        }

        return ' ';
    }

    /**
     * Handles a guessed word.
     *
     * @param guess
     *   The guessed word to process.
     *
     * @return
     *   The full response string after a guess.
     */
    public String handleGuess(String guess, String clientName) {
        String targetWord = wordToGuess.get(clientName);
        attempts.put(clientName, attempts.get(clientName) + 1);
        int attemptNumber = attempts.get(clientName);
        if (guess.equalsIgnoreCase(targetWord)) {
            return "win: " + targetWord;
        }

        if(attemptNumber >= MAX_ATTEMPTS) {
            return "lose: " + targetWord;
        }

        for(int i = 0; i < guess.length(); i++) {
            char c = guess.charAt(i);

            switch(isInWord(c, targetWord, i)) {
                case -1:
                    wrongChars.get(clientName).add(c);
                    break;
                case 0:
                    containedChars.get(clientName).add(c);
                    break;
                case 1:
                    correctChars.get(clientName)[i] = c;
            }
        }

        return  "correctChars: " + Arrays.toString(correctChars.get(clientName)) +
                "\nwrongChars: " + wrongChars.get(clientName).toString() +
                "\ncontainedChars: " + containedChars.get(clientName).toString() +
                "\nattempts left: " + (MAX_ATTEMPTS - attemptNumber);
    }

    /**
     * Calculates a highscore.
     *
     * @param attempts
     *   The number of attempts
     *
     * @param duration
     *   The duration of the game in milliseconds.
     *
     * @return
     *   The calculated score.
     */
    public int calculateHighscore(int attempts, int duration) {
        return duration * 100 / attempts;
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

        return c == s.charAt(index) ? 1 : 0;
    }

    /**
     * Checks whether a character is a vowel
     *
     * Found on https://stackoverflow.com/questions/19160921/how-do-i-check-if-a-char-is-a-vowel
     *
     * @param c
     *   The character to check.
     * @return
     *   TRUE if c is a vowel.
     */
    private boolean isVowel(char c) {
        return "AEIOUYaeiouy".indexOf(c) != -1;
    }

}
