package org.example.babich;

import org.apache.commons.cli.*;
import org.example.babich.domain.News;
import org.example.babich.filter.PositiveNewsFilter;
import org.example.babich.newsfeed.NewsPublisher;
import org.example.babich.server.Server;
import org.example.babich.server.SocketDataConsumer;
import org.example.babich.statistic.SimpleStatistics;
import org.example.babich.statistic.SimpleStatisticsResult;

import java.io.PrintStream;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.System.exit;
import static java.lang.System.out;

public class NewsSubscriber {

    private static final String USAGE_STRING = "java -jar news-subscriber-{version}.jar [-help]"
            + "[-newsFeedMode] [-port] [-host] [-analyserIntervalSec] [-frequencyNews]";

    private static final Options options;

    static {
        options = new Options();

        options.addOption(Option.builder("help")
                .required(false)
                .hasArg(false)
                .desc("print this message")
                .build());

        options.addOption(Option.builder("newsFeedMode")
                .required(false)
                .hasArg(false)
                .desc("Launch the app as a new mock news feed")
                .build());

        options.addOption(Option.builder("port")
                .required(false)
                .hasArg(true)
                .desc("Listener port, this port is used to establish connections by publishers.")
                .build()
        );

        options.addOption(Option.builder("host")
                .required(false)
                .hasArg(true)
                .desc("Binding server host.")
                .build()
        );

        options.addOption(Option.builder("analyserIntervalSec")
                .required(false)
                .hasArg(true)
                .desc("Every {analyserIntervalSec} seconds, the news analyser should output to the console.")
                .build()
        );

        options.addOption(Option.builder("frequencyNews")
                .required(false)
                .hasArg(true)
                .desc("The delay in ms between of news items being emitted by the feed. default - 100ms")
                .build()
        );

    }

    static int port;
    static String host;
    static int interval;

    static boolean newsFeedMode;
    static int frequencyNews;

    public static void main(String[] args) {

        try {
            setupShutdownHook(out);

            init(args);

            if (newsFeedMode) {
                runMockNewsFeed();
            } else {
                runNewsAnalyser();
            }

            exit(0);
        } catch (Exception e) {
            printUsage(e.getMessage());
            exit(1);
        }
    }

    static Consumer<SimpleStatisticsResult> getStatisticsConsumer(PrintStream printStream) {
        return value -> {
            printStream.println(value.getTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    + "\nThe count of positive news items seen during the last 10 seconds: "
                    + value.getTotalPositiveNews());

            String outputMessage = value.getSortedNews().stream()
                    .map(item -> item.getPriority() + " - " + item.getHeadline())
                    .collect(Collectors.joining("\n\t"
                            , "the unique headlines of up to three of the highest-priority positive news items " +
                                    "seen during the last 10 seconds:\n\t"
                            , "\n"));

            printStream.println(outputMessage);
        };
    }

    private static void runMockNewsFeed() {
        NewsPublisher.newNewsPublisher(configurer -> configurer.withDaley(frequencyNews)
                .withHost(host)
                .withPort(port)
                .withHeadlineWords(new String[]{"up", "down", "rise", "fall", "good", "bad", "success", "failure"
                        , "high", "low", "über", "unter"}))
                .run();
    }

    private static void runNewsAnalyser() {
        SimpleStatistics analyzer = SimpleStatistics.newSimpleStatistics(statConf ->
                statConf.withConsumer(getStatisticsConsumer(out)));

        Predicate<News> messageFilter =
                new PositiveNewsFilter("up", "rise", "good", "success", "high", "über");

        Server.newServer(serverBuilder ->
                serverBuilder.withPort(port)
                        .withHost(host)
                        .withDataConsumer(SocketDataConsumer
                                .newSocketDataConsumer(conf -> conf.messageFilter(messageFilter), analyzer))
        )
                .doStart();
    }

    private static void init(String... args) {
        out.println("initializing..");

        CommandLine commandLine;
        try {
            commandLine = new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        if (commandLine.hasOption("help")) {
            printUsage();
            exit(0);
        }

        initPort(commandLine);

        initHost(commandLine);

        initAnalyser(commandLine);

        newsFeedMode(commandLine);

        frequencyNews(commandLine);
    }

    static void initPort(CommandLine commandLine) {
        String value = commandLine.getOptionValue("port", "80");
        port = Integer.parseInt(value);
    }

    static void initHost(CommandLine commandLine) {
        host = commandLine.getOptionValue("host", "localhost");
    }

    static void initAnalyser(CommandLine commandLine) {
        String value = commandLine.getOptionValue("analyserIntervalSec", "10");
        interval = Integer.parseInt(value);
    }

    static void newsFeedMode(CommandLine commandLine) {
        newsFeedMode = commandLine.hasOption("newsFeedMode");
    }

    static void frequencyNews(CommandLine commandLine) {
        String value = commandLine.getOptionValue("frequencyNews", "100");
        frequencyNews = Integer.parseInt(value);
    }

    private static void printUsage(String message) {
        out.println();
        out.println(message);
        printUsage();
    }

    private static void printUsage() {
        out.println();

        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp(120, USAGE_STRING, "", options, "");
        out.println();
    }

    private static void setupShutdownHook(PrintStream printStream) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Thread.sleep(400);
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
            }
            printStream.println("\nShutting down ...");
        }));
    }
}
