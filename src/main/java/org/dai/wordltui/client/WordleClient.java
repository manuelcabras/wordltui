package org.dai.wordltui.client;

import java.util.Arrays;

public class WordleClient {

    /**
     * Constructs a client instance.
     */
    public WordleClient() {}

    /**
     * Builds a failed attempt string.
     *
     * @param correctChars
     *   The guessed characters in the word in the right order.
     * @param wrongChars
     *   The guessed characters that are not in the word.
     * @param containedChars
     *   The guessed characters that are in the word but not in the right position.
     * @param attempts
     *   The amount of attempts for the current game.
     *
     * @return
     *   The string to display to the user.
     */
    private String buildAttemptString(char[] correctChars, char[] wrongChars, char[] containedChars, int attempts) {
        return this.buildGuessedWordString(correctChars) +
                "\nEliminated letters : " + Arrays.toString(wrongChars) +
                "\nGuessed, wrong position : " + Arrays.toString(containedChars) +
                "\nAttempts left : " + attempts;
    }

    /**
     * Builds guessed word display string.
     *
     * @param correctChars
     *   The guessed characters.
     * @return
     *   The display string for the word to guess progress.
     */
    private String buildGuessedWordString(char[] correctChars) {
        StringBuilder topLine = new StringBuilder();
        StringBuilder letters = new StringBuilder();
        StringBuilder bottomLine = new StringBuilder();

        topLine.append("╔");
        letters.append("║");
        bottomLine.append("╚");

        for(int i = 0; i < (correctChars.length * 2) - 1; i++) {
            topLine.append(i % 2 == 0 ? "═" : "╦");
            bottomLine.append(i % 2 == 0 ? "═" : "╩");
            letters.append(i % 2 == 0 ? correctChars[i/2] : "║");
        }

        topLine.append("╗\n");
        letters.append("║\n");
        bottomLine.append("╝\n");

        return topLine.append(letters.append(bottomLine)).toString();
    }

}
