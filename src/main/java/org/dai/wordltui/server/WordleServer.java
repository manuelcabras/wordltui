package org.dai.wordltui.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

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
     * The guessed characters that are in the word but not in the right position.
     * Keyed by client.
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
     * The amount of points attributed to each 
     */
    private record HighScore(String username, int points){}          // from https://medium.com/@toimrank/java-records-the-end-of-boilerplate-code-37cd8b439b0a and https://www.baeldung.com/java-record-keyword, to avoid multiple users by word and only keep the user with the biggest score for each word
    private static final ConcurrentHashMap<String,HighScore> highscores = new ConcurrentHashMap<>();

    /**
     * Constructs a Server instance.
     *
     * @param pathToWords
     *                    A collection of same letter words, separated by newLine.
     * @param letterCount
     *                    The letter count for each word.
     */
    public WordleServer(String pathToWords, int letterCount) {
        // Populate dictionary from given file.
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(pathToWords), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.length() != letterCount) {
                    System.out.println(
                            "[Skipped] Word" + line + " does not respect the given " + letterCount + " letter length");
                    continue;
                }

                dictionary.add(line);
            }
        } catch (IOException e) {
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

    private HighScore getHighScore(){
        HighScore max = null;
        for(HighScore var : highscores.values()){
            if(max == null || max.points() < var.points()){
                max = var;
            }
        }
        return max;
    }

    private HighScore getHighScore(String word){
        return highscores.get(word);
    }

    /**
     * Initializes a game.
     *
     * @return
     *         The new word to guess.
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
     *                  Whether the hint requested is a consonant.
     *
     * @return
     *         A character contained in the word or ' ' if none can be given.
     */
    public char handleHint(boolean consonant, String clientName) {
        for (char c : wordToGuess.get(clientName).toCharArray()) {
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
     *              The guessed word to process.
     *
     * @return
     *         The full response string after a guess.
     */
    public String handleGuess(String guess, String clientName) {
        String targetWord = wordToGuess.get(clientName);
        attempts.put(clientName, attempts.get(clientName) + 1);
        int attemptNumber = attempts.get(clientName);
        if (guess.equalsIgnoreCase(targetWord)) {
            return "Won";
        }

        if (attemptNumber >= MAX_ATTEMPTS) {
            return "Lost";
        }

        for (int i = 0; i < guess.length(); i++) {
            char c = guess.charAt(i);

            switch (isInWord(c, targetWord, i)) {
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

        return "correctChars: " + Arrays.toString(correctChars.get(clientName)) +
                "\nwrongChars: " + wrongChars.get(clientName).toString() +
                "\ncontainedChars: " + containedChars.get(clientName).toString() +
                "\nattempts left: " + (MAX_ATTEMPTS - attemptNumber);
    }

    /**
     * Calculates a highscore.
     *
     * @param attempts
     *                 The number of attempts
     *
     * @param duration
     *                 The duration of the game in milliseconds.
     *
     * @return
     *         The calculated score.
     */
    public int calculateHighscore(int attempts, int duration) {
        return duration * 100 / attempts;
    }

    /**
     * Checks if a char is in a word, gives a precision about the position.
     *
     * @param c
     *              The char to test for.
     * @param s
     *              The string to search in.
     * @param index
     *              The supposed index of the char.
     * @return
     *         Returns an int from {-1, 0, 1}
     *         -1: The char is not in the string
     *         0: The char is in the string at a different index
     *         1: The char is in the string at the given index
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
     * Found on
     * https://stackoverflow.com/questions/19160921/how-do-i-check-if-a-char-is-a-vowel
     *
     * @param c
     *          The character to check.
     * @return
     *         TRUE if c is a vowel.
     */
    private boolean isVowel(char c) {
        return "AEIOUYaeiouy".indexOf(c) != -1;
    }

    private void playgame(BufferedReader in, BufferedWriter out) throws IOException {
        out.write(
                "Welcome to Wordle ! Guess a random 5 letter word as fast and in the least amount of tries as possible\n");
        out.write("\n");
        out.write("Choose a username\n");
        out.flush();

        String username = in.readLine();
        if (username == null) {
            return;
        }

        String wordtoguess = startGame(username);
        long startTime = System.nanoTime();
        int hintsUsed = 0;
        String input;

        while ((input = in.readLine()) != null) {
            switch (input) {
                case "HELP" -> {
                    out.write("------------------------------------------- Game commands -------------------------------------------\n");
                    out.write("GUESS <word>     : Guess a word\n");
                    out.write("GIVEUP           : You give up and instantly loose the game, the correct word is revealed\n");
                    out.write("HINT             : ask for a hint (you've the right to ask ONE hint per game, so make good use of it)\n");
                    out.write("   => VOWEL      : Gives you a vowel from the correct word (if it contains any)\n");
                    out.write("   => CONSONANT  : Gives you a consonant from the correct word (if it contains any)\n");
                    out.flush();
                }
                case "HINT" -> {
                    String hintType = in.readLine();
                    if (hintType == null) {
                        return;
                    }
                    if (hintsUsed > 0) {
                        out.write("You've already wasted your only hint for this game\n");
                    } else if (hintType.equals("VOWEL") || hintType.equals("CONSONANT")) {
                        hintsUsed++;
                        boolean consonant = hintType.equals("CONSONANT");
                        char hint = handleHint(consonant, username);
                        out.write(hint == ' '
                                ? "There are no " + (consonant ? "consonants" : "vowels") + " in this word\n"
                                : "=> " + hint + "\n");
                    } else {
                        out.write("Invalid hint type; choose VOWEL or CONSONANT\n");
                    }
                    out.flush();
                }
                case "GIVEUP" -> {
                    out.write("You've lost the game. The correct word was : " + wordtoguess + "\n");
                    out.flush();
                    return;
                }
                case "QUIT" -> {
                    return;
                }
                default -> {
                    if (input.startsWith("GUESS ")) {
                        int gameAttempts = attempts.get(username);
                        String guess = input.substring("GUESS ".length()).trim();
                        String result = handleGuess(guess, username);
                        switch (result) {
                            case "Won" -> {
                                int duration = (int) ((System.nanoTime() - startTime) / 1_000_000L);
                                int score = calculateHighscore(gameAttempts + 1, duration);
                                out.write("You've won, your score is + " + score + "\n");
                                out.flush();
                                highscores.compute(wordtoguess, (word, best) ->
                                        best == null || score > best.points()
                                                ? new HighScore(username, score)
                                                : best);
                                return;
                            }
                            case "Lost" -> {
                                out.write("You've lost the game. The correct word was : " + wordtoguess + "\n");
                                out.flush();
                                return;
                            }
                            default -> {
                                out.write(result + "\n\n");
                                out.flush();
                            }
                        }
                    } else {
                        out.write("Invalid command, type HELP to see all the game commands\n");
                        out.flush();
                    }
                }
            }
        }
    }

    private void handle(Socket s) throws IOException {
        try (Socket client = s;
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
                BufferedWriter out = new BufferedWriter(
                        new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8))) {
            String input;
            out.write("WELCOME [HELP FOR COMMANDS]\n");
            out.flush();
            while ((input = in.readLine()) != null && !input.equals("QUIT")) {  
                switch (input) {
                    case "PLAY" -> playgame(in, out);
                    case "HELP" ->{
                        out.write(
                                "----------------------------------- General commands -----------------------------------\n");
                        out.write("HIGHSCORE        : Gives the best score over all games played\n");
                        out.write(
                                "HIGHSCORE <word> : Gives the best score for a given word if it's ever been registered\n");
                        out.write("PLAY             : Start a game of Wordle\n");
                        out.write("QUIT             : Close the connection with the server\n");
                        out.write("\n");
                        out.flush();
                    }
                    case "GUESS", "HINT","GIVEUP" ->{
                        out.write("This command is only valid during a game, start one in order to use them\n");
                        out.flush();
                    }
                    case "HIGHSCORE" -> {
                        HighScore best = getHighScore();
                        if (best == null) {
                            out.write("No player has won a game since the server started\n");
                        } else {
                            out.write("Best score so far: " + best.points() + " points by " + best.username() + "\n");
                        }
                        out.flush();
                    }
                    default ->{
                        if (input.contains("HIGHSCORE")){
                            String word = input.substring(input.indexOf(" ")+1, input.length());
                            HighScore best = getHighScore(word);
                            if(best == null){
                                out.write("No one has guessed that word since the server started\n");
                            }else{
                                out.write("Best score for " + word +" so far: " + best.points() + " points by " + best.username() + "\n");
                            }
                        }else{
                            out.write("Invalid command\n");
                            out.write("\n");
                            out.flush();
                        }
                    }
                }
            }
            out.write("Connection closed : BYE\n");
            out.flush();
        } catch (IOException e) {
            throw new IOException("[Server] Client connexion failed abruptly, " + e.getMessage());
        }
    }

    public void openConnection(int num_port, int max_threads) throws IOException {
        try (ServerSocket ss = new ServerSocket(num_port);
                ExecutorService pool = Executors.newFixedThreadPool(max_threads)) {
            while (!ss.isClosed()) {
                Socket s = ss.accept();
                pool.submit(() -> {
                    try {
                        handle(s);
                    } catch (IOException e) {
                        System.out.println("[Server] Client connection failed abruptly, " + e.getMessage());
                    }
                });
            }
        } catch (IOException e) {
            throw new IOException("[Server] Client connexion failed abruptly, " + e.getMessage());
        }
    }

}
