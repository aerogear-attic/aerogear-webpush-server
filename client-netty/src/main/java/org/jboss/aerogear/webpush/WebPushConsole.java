package org.jboss.aerogear.webpush;

import org.jboss.aesh.complete.Completion;
import org.jboss.aesh.console.AeshConsoleCallback;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleOperation;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.settings.SettingsBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WebPushConsole {

    public static void main(String[] args) throws IOException {

        final Console console = new Console(new SettingsBuilder().create());
        console.getShell().out().println("WebPush console");
        final Prompt prompt = new Prompt("> ");
        console.setPrompt(prompt);

        final Completion completer = co -> {
            final List<String> commands = new ArrayList<>();
            if(co.getBuffer().startsWith("co")) {
                commands.add("connect localhost:8443");
            }
            if(co.getBuffer().startsWith("re")) {
                commands.add("register");
                co.setOffset(0);
            }
            if(co.getBuffer().startsWith("ch")) {
                commands.add("channel <channel-url>");
            }
            if(co.getBuffer().startsWith("m")) {
                commands.add("monitor <monitor-url>");
            }
            if(co.getBuffer().startsWith("no")) {
                commands.add("notify <notify-url> <payload>");
            }
            if(co.getBuffer().startsWith("q")) {
                commands.add("quit");
            }
            co.setCompletionCandidates(commands);
        };

        console.addCompletion(completer);
        AeshConsoleCallback consoleCallback = new AeshConsoleCallback() {
            WebPushClient client = null;
            @Override
            public int execute(ConsoleOperation output) {

                if (output.getBuffer().startsWith("quit")) {
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
                else if (output.getBuffer().startsWith("connect")) {
                    client = handleConnect(output, console);
                } else if (output.getBuffer().startsWith("register")) {
                    handleRegister(client);
                } else if (output.getBuffer().startsWith("monitor")) {
                    handleMonitor(client, getFirstArg(output.getBuffer()));
                } else if (output.getBuffer().startsWith("channel")) {
                    handleCreateChannel(client, getFirstArg(output.getBuffer()));
                } else if (output.getBuffer().startsWith("notify")) {
                    handleNotification(client, output.getBuffer());
                } else {
                    console.getShell().out().println("Unknown command:" + output.getBuffer());
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
        public void registerResponse(final String channelLink, final String monitorLink, final int streamId) {
            print("ChannelLink: " + channelLink + ", MonitorLink: " + monitorLink, streamId);
        }

        @Override
        public void channelResponse(final String endpoint, final int streamId) {
            print("Notification URL: " + endpoint, streamId);
        }

        @Override
        public void notification(final String data, final int streamId) {
            print(data, streamId);
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

    private static void handleNotification(final WebPushClient client, final String cmd) {
        final String[] args = getFirstTwoArg(cmd);
        try {
            client.notify(args[0], args[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
