package org.jboss.aerogear.webpush;

import io.netty.handler.codec.http2.Http2Headers;
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
                "Registers with the WebPush server and displays the subscribe-url and monitor-url."),
        SUBSCRIBE("subscribe",
                Optional.of("<subscribe-url>"),
                "Creates a new subscription and displays the endpoint-url the subscription. This can be used with the notify command "),
        STATUS("status",
                Optional.of("<endpoint-url>"),
                "Checks the status of the subscription and returns the latest message if the server has any undelivered messages for the subscription"),
        DELETE("delete",
                Optional.of("<endpoint-url>"),
                "Deletes the specified subscription."),
        AGGREGATE("aggregate-subscription",
                Optional.of("<aggregate-url> <endpoint-url>[,<endpoint-url>]"),
                "Creates a new aggregate subscription with the passed in endpoint-urls being part for the aggregate."),
        MONITOR("monitor",
                Optional.of("<reg-url> nowait"),
                "Starts monitoring for notifications. Adding nowait will send a Prefer: wait=0 header."),
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
            if(co.getBuffer().startsWith("su")) {
                commands.add(Command.SUBSCRIBE.example());
            }
            if(co.getBuffer().startsWith("st")) {
                commands.add(Command.STATUS.example());
            }
            if(co.getBuffer().startsWith("de")) {
                commands.add(Command.DELETE.example());
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
                    handleMonitor(client, buffer);
                } else if (buffer.startsWith(Command.SUBSCRIBE.toString())) {
                    handleSubscribe(client, getFirstArg(buffer));
                } else if (buffer.startsWith(Command.STATUS.toString())) {
                    handleStatus(client, getFirstArg(buffer));
                } else if (buffer.startsWith(Command.DELETE.toString())) {
                    handleSubscriptionDelete(client, getFirstArg(buffer));
                } else if (buffer.startsWith(Command.AGGREGATE.toString())) {
                    handleAggregateSubscription(client, buffer, console);
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

    private static class CallbackHandler implements EventHandler {

        private final Console console;
        private final Prompt inbound = new Prompt("< ");
        private final Prompt outbound = new Prompt(">");

        CallbackHandler(final Console console) {
            this.console = console;
        }

        @Override
        public void outbound(final Http2Headers headers) {
            printOutbound(headers);
        }

        @Override
        public void inbound(Http2Headers headers, int streamId) {
            printInbound(headers.toString(), streamId);
        }

        @Override
        public void notification(final String data, final int streamId) {
            printInbound(data, streamId);
        }

        private void printOutbound(final Http2Headers headers) {
            final Prompt current = console.getPrompt();
            console.setPrompt(outbound);
            console.getShell().out().println(" " + headers.toString());
            console.setPrompt(current);
        }

        private void printInbound(final String message, final int streamId) {
            final Prompt current = console.getPrompt();
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

    private static void handleMonitor(final WebPushClient client, final String buffer) {
        final String[] args = getArgs(buffer);
        final boolean nowait = args.length == 3 && args[2] != null;
        try {
            client.monitor(args[1], nowait);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void handleSubscribe(final WebPushClient client, final String url) {
        try {
            client.createSubscription(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void handleStatus(final WebPushClient client, final String url) {
        try {
            client.status(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void handleSubscriptionDelete(final WebPushClient client, final String url) {
        try {
            client.deleteSubscription(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void handleAggregateSubscription(final WebPushClient client,
                                                   final String cmd,
                                                   final Console console) {
        final String[] args = getArgs(cmd);
        try {
            final String json = JsonMapper.toJson(new DefaultAggregateSubscription(WebPushClient.asEntries(args[2].split(","))));
            console.getShell().out().println(JsonMapper.pretty(json));
            client.createAggregateSubscription(args[1], json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleNotification(final WebPushClient client, final String cmd) {
        final String[] args = getArgs(cmd);
        try {
            client.notify(args[1], args[2]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void displayHelp(final Console console) {
        final PrintStream out = console.getShell().out();
        out.println("WebPushConsole commands (tab completion available)");
        out.println();
        Command.CONNECT.printHelp(out);
        Command.SUBSCRIBE.printHelp(out);
        Command.HELP.printHelp(out);
        Command.NOTIFY.printHelp(out);
        Command.MONITOR.printHelp(out);
        Command.REGISTER.printHelp(out);
        Command.QUIT.printHelp(out);
    }

    private static String getFirstArg(final String cmd) {
        return cmd.substring(cmd.indexOf(" ") + 1, cmd.length());
    }

    private static String[] getArgs(final String cmd) {
        return cmd.split(" ");
    }

}
