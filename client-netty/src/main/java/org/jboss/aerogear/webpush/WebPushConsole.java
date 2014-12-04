package org.jboss.aerogear.webpush;

import org.jboss.aesh.complete.Completion;
import org.jboss.aesh.console.AeshConsoleCallback;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleOperation;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.settings.SettingsBuilder;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WebPushConsole {

    private enum Command {
        CONNECT("connect",
                Optional.of("localhost:8443"),
                "Connects to specified WebPush server on the specificed host/port."),
        REGISTER("register",
                Optional.<String>empty(),
                "Registers with the WebPush server and displays the channel-url and monitor-url."),
        CHANNEL("channel",
                Optional.of("<channel-url>"),
                "Creates a new channel and displays the endpoint-url the channel. This can be used with the notify command "),
        STATUS("status",
                Optional.of("<channel-url>"),
                "Checks to see if a channel exist for the specified channel-url"),
        AGGREGATE("aggregate-channel",
                Optional.of("<aggregate-url> <channel-url>[,<channel-url>]"),
                "Creates a new aggregate channel with the passed in channel-urls being part for the aggregate."),
        MONITOR("monitor",
                Optional.of("<monitor-url>"),
                "Starts monitoring for notifications"),
        NOTIFY("notify",
                Optional.of("<endpoint-url> <payload>"),
                "Sends a notification to the specified endpoint-url."),
        HELP("help",
                Optional.<String>empty(),
                "Displays the help for the WebPushConsole."),
        QUIT("quit",
                Optional.<String>empty(),
                "Quits the WebPushConsole.");

        private final String name;
        private final Optional<String> parameters;
        private final String description;

        private Command(String name, final Optional<String> parameters, String description) {
            this.name = name;
            this.parameters = parameters;
            this.description = description;
        }

        @Override
        public String toString() {
            return name;
        }

        public Optional<String> parameters() {
            return parameters;
        }

        public String example() {
            return parameters.isPresent() ? name + " " +  parameters.get() : name;
        }

        public void printHelp(final PrintStream out) {
            out.println(example());
            out.println(description);
            out.println();
        }

    }

    public static void main(String[] args) throws IOException {

        final Console console = new Console(new SettingsBuilder().create());
        console.getShell().out().println("WebPush console");
        final Prompt prompt = new Prompt("> ");
        console.setPrompt(prompt);

        final Completion completer = co -> {
            final List<String> commands = new ArrayList<>();
            if(co.getBuffer().startsWith("co")) {
                commands.add(Command.CONNECT.example());
            }
            if(co.getBuffer().startsWith("re")) {
                commands.add(Command.REGISTER.example());
                co.setOffset(0);
            }
            if(co.getBuffer().startsWith("ch")) {
                commands.add(Command.CHANNEL.example());
            }
            if(co.getBuffer().startsWith("st")) {
                commands.add(Command.STATUS.example());
            }
            if(co.getBuffer().startsWith("ag")) {
                commands.add(Command.AGGREGATE.example());
            }
            if(co.getBuffer().startsWith("m")) {
                commands.add(Command.MONITOR.example());
            }
            if(co.getBuffer().startsWith("no")) {
                commands.add(Command.NOTIFY.example());
            }
            if(co.getBuffer().startsWith("q")) {
                commands.add(Command.QUIT.example());
            }
            if(co.getBuffer().equals("h") || co.getBuffer().startsWith("he")) {
                commands.add(Command.HELP.example());
            }
            co.setCompletionCandidates(commands);
        };

        console.addCompletion(completer);
        AeshConsoleCallback consoleCallback = new AeshConsoleCallback() {
            WebPushClient client = null;
            @Override
            public int execute(ConsoleOperation output) {
                final String buffer = output.getBuffer();

                if (buffer.startsWith("quit")) {
                    try {
                        if (client != null) {
                            client.disconnect();
                            client.shutdown();
                        }
                        console.stop();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else if (buffer.startsWith(Command.CONNECT.toString())) {
                    client = handleConnect(output, console);
                } else if (buffer.startsWith(Command.REGISTER.toString())) {
                    handleRegister(client);
                } else if (buffer.startsWith(Command.MONITOR.toString())) {
                    handleMonitor(client, getFirstArg(buffer));
                } else if (buffer.startsWith(Command.CHANNEL.toString())) {
                    handleCreateChannel(client, getFirstArg(buffer));
                } else if (buffer.startsWith(Command.STATUS.toString())) {
                    handleChannelStatus(client, getFirstArg(buffer));
                } else if (buffer.startsWith(Command.AGGREGATE.toString())) {
                    handleAggregateChannel(client, buffer, console);
                } else if (buffer.startsWith(Command.NOTIFY.toString())) {
                    handleNotification(client, buffer);
                } else if (buffer.startsWith(Command.HELP.toString())) {
                    displayHelp(console);
                } else {
                    console.getShell().out().println("Unknown command:" + buffer);
                }
                return 0;
            }
        };
        console.setConsoleCallback(consoleCallback);
        console.start();
    }

    private static class CallbackHandler implements ResponseHandler {

        private final Console console;
        private Prompt inbound = new Prompt("< ");

        CallbackHandler(final Console console) {
            this.console = console;
        }

        @Override
        public void registerResponse(final String channelLink,
                                     final String monitorLink,
                                     final String aggregateLink,
                                     final int streamId) {
            print("channelLink: " + channelLink + ", monitorLink: " + monitorLink + ", aggregateLink: " + aggregateLink
                    , streamId);
        }

        @Override
        public void channelResponse(final String endpoint, final int streamId) {
            print("Endpoint: " + endpoint, streamId);
        }

        @Override
        public void notification(final String data, final int streamId) {
            print(data, streamId);
        }

        @Override
        public void channelStatus(final String statusCode, final int streamId) {
            print("ChannelStatus: " + statusCode, streamId);
        }

        private void print(final String message, final int streamId) {
            final Prompt current =  console.getPrompt();
            console.setPrompt(inbound);
            console.getShell().out().println("[streamid:" + streamId + "] " + message);
            console.setPrompt(current);
        }

    }

    private static WebPushClient handleConnect(final ConsoleOperation output, final Console console) {
        final String cmd = output.getBuffer();
        final String[] hostPort = cmd.substring(cmd.indexOf(" "), cmd.length()).trim().split(":");
        final WebPushClient client = WebPushClient.forHost(hostPort[0])
                .port(hostPort[1])
                .ssl(true)
                .notificationHandler(new CallbackHandler(console))
                .build();
        try {
            client.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return client;
    }

    private static void handleRegister(final WebPushClient client) {
        try {
            client.register();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleMonitor(final WebPushClient client, final String url) {
        try {
            client.monitor(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void handleCreateChannel(final WebPushClient client, final String url) {
        try {
            client.createChannel(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void handleChannelStatus(final WebPushClient client, final String url) {
        try {
            client.channelStatus(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void handleAggregateChannel(final WebPushClient client,
                                              final String cmd,
                                              final Console console) {
        final String[] args = getFirstTwoArg(cmd);
        try {
            final String json = JsonMapper.toJson(new DefaultAggregateChannel(WebPushClient.asEntries(args[1].split(","))));
            console.getShell().out().println(JsonMapper.pretty(json));
            client.createAggregateChannel(args[0], json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleNotification(final WebPushClient client, final String cmd) {
        final String[] args = getFirstTwoArg(cmd);
        try {
            client.notify(args[0], args[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void displayHelp(final Console console) {
        final PrintStream out = console.getShell().out();
        out.println("WebPushConsole commands (tab completion available)");
        out.println();
        Command.CONNECT.printHelp(out);
        Command.CHANNEL.printHelp(out);
        Command.HELP.printHelp(out);
        Command.NOTIFY.printHelp(out);
        Command.MONITOR.printHelp(out);
        Command.REGISTER.printHelp(out);
        Command.QUIT.printHelp(out);
    }

    private static String getFirstArg(final String cmd) {
        return cmd.substring(cmd.indexOf(" ") + 1, cmd.length());
    }

    private static String[] getFirstTwoArg(final String cmd) {
        final String[] args = new String[2];
        String tmp = cmd.substring(cmd.indexOf(" ") + 1, cmd.length());
        args[0] = tmp.substring(0, tmp.indexOf(" "));
        args[1] = tmp.substring(tmp.indexOf(" ") + 1, tmp.length());
        return args;
    }

}
