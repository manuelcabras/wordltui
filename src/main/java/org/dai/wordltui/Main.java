package org.dai.wordltui;

import org.dai.wordltui.server.WordleServer;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(name = "wordlTUI",
        mixinStandardHelpOptions = true,
        version = "wordlTUI 1.0",
        description = "Play Wordle in you terminle",
        subcommands = {Main.Client.class, Main.Server.class})
public class Main implements Runnable {

    /**
     * The default port for the application.
     */
    private final static int DEFAULT_PORT = 42069;

    @Override
    public void run() {
        // Invoked when no subcommand is given: show usage instead of doing nothing.
        new CommandLine(this).usage(System.out);
    }

    @Command(name = "client", mixinStandardHelpOptions = true, version = "wordlTUI 1.0",
            description = "Launches a client for wordlTUI")
    static class Client implements Callable<Integer> {

        @Option(names = {"-p", "--port"}, description = "The port to send a connection request to.")
        private int port = DEFAULT_PORT;

        @Override
        public Integer call() throws Exception {
            System.out.println("Connecting to port " + port);
            return 0;
        }
    }

    @Command(name = "server", mixinStandardHelpOptions = true, version = "wordlTUI 1.0",
            description = "Launches a wordlTUI server")
    static class Server implements Callable<Integer> {

        @Option(names = {"-p", "--port"}, description = "The port to listen for connections on.")
        private int port = DEFAULT_PORT;

        @Override
        public Integer call() throws Exception {
            System.out.println("Listening on port " + port);
            WordleServer s = new WordleServer();
            String wordToGuess = s.startGame();
            System.out.println("Word to guess: " + wordToGuess);
            System.out.println("Test guess:\n" + s.handleGuess("aa" + wordToGuess.substring(2)));
            return 0;
        }
    }

    public static void main(String... args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

}