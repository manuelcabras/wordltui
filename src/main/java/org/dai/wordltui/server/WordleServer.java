package org.dai.wordltui.server;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class WordleServer {

    /**
     * The max attempts amount.
     */
    private final int MAX_ATTEMPTS = 6;

    /**
     * A collection of words to choose from.
     *
     * All words must be of the same length.
     */
    private static ArrayList<String> dictionary = new ArrayList<>();

    /**
     * The word to guess.
     */
    private String wordToGuess;

    /**
     * The guessed characters that are not in the word.
     */
    private ArrayList<Character> wrongChars = new ArrayList<>();

    /**
     * The guessed characters that are in the word but not in the right position.
     */
    private ArrayList<Character> containedChars = new ArrayList<>();

    /**
     * The correctly guessed chars
     *
     * Chars that have not been guessed are spaces (== ' ')
     */
    private char[] correctChars;

    /**
     * The amount of attempts for the current game.
     */
    private int attempts;

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
    public String startGame() {
        wordToGuess = dictionary.get((int) (Math.random() * dictionary.size()));
        attempts = 0;
        return wordToGuess;
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
    public char handleHint(boolean consonant) {
        for(char c : wordToGuess.toCharArray()) {
            if (containedChars.contains(c)) {
                continue;
            }

            if (consonant && isVowel(c)) {
                continue;
            }

            if (!consonant && !isVowel(c)) {
                continue;
            }

            containedChars.add(c);
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
    public String handleGuess(String guess) {
        attempts++;
        if (guess.equals(wordToGuess)) {
            return "win: " + wordToGuess;
        }

        if(attempts >= MAX_ATTEMPTS) {
            return "lose: " + wordToGuess;
        }

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

        return  "correctChars: " + Arrays.toString(correctChars) +
                "\nwrongChars: " + wrongChars.toString() +
                "\ncontainedChars: " + containedChars.toString() +
                "\nattempts left: " + (MAX_ATTEMPTS - attempts);
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

        if (c == s.charAt(index)) {
            return 1;
        }

        return 0;
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
