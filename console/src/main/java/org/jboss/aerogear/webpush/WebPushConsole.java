package org.jboss.aerogear.webpush;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.util.CharsetUtil;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.terminal.Color;
import org.jboss.aesh.terminal.Color.Intensity;
import org.jboss.aesh.terminal.TerminalColor;
import org.jboss.aesh.terminal.TerminalString;

import java.io.IOException;

import static org.jboss.aesh.terminal.Color.DEFAULT;
import static org.jboss.aesh.terminal.Color.GREEN;
import static org.jboss.aesh.terminal.Color.Intensity.NORMAL;

public class WebPushConsole {

    public static void main(final String[] args) {
        final SettingsBuilder builder = new SettingsBuilder().logging(true);
        builder.enableMan(true).readInputrc(false);
        final ConnectCommand connectCommand = new ConnectCommand();
        final DisconnectCommand disconnectCommand = new DisconnectCommand(connectCommand);
        final RegisterCommand registerCommand = new RegisterCommand(connectCommand);
        final SubscribeCommand subscribeCommand = new SubscribeCommand(connectCommand);
        final MonitorCommand monitorCommand = new MonitorCommand(connectCommand);
        final NotifyCommand notifyCommand = new NotifyCommand(connectCommand);
        final StatusCommand statusCommand = new StatusCommand(connectCommand);
        final DeleteSubCommand deleteSubCommand = new DeleteSubCommand(connectCommand);
        final AggregateCommand aggregateCommand = new AggregateCommand(connectCommand);

        final Settings settings = builder.create();
        final CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(ExitCommand.class)
                .command(connectCommand)
                .command(disconnectCommand)
                .command(registerCommand)
                .command(subscribeCommand)
                .command(monitorCommand)
                .command(notifyCommand)
                .command(statusCommand)
                .command(deleteSubCommand)
                .command(aggregateCommand)
                .create();

        final AeshConsole aeshConsole = new AeshConsoleBuilder()
                .commandRegistry(registry)
                .settings(settings)
                .prompt(new Prompt(new TerminalString("[webpush]$ ", new TerminalColor(GREEN, DEFAULT, NORMAL))))
                .create();
        connectCommand.setConsole(aeshConsole);
        aeshConsole.start();
    }

    @CommandDefinition(name="exit", description = "the program")
    public static class ExitCommand implements Command {

        @Override
        public CommandResult execute(final CommandInvocation commandInvocation) throws IOException, InterruptedException {
            commandInvocation.stop();
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "connect", description = "<url>")
    public static class ConnectCommand implements Command {
        private WebPushClient webPushClient;
        private AeshConsole console;

        @Option(shortName = 'h', hasValue = true, description = "the host to connect to", defaultValue = "localhost")
        private String host;

        @Option(shortName = 'p', hasValue = true, description = "the port to connect to", defaultValue = "8443")
        private int port;

        @Option(hasValue = false, description = "display this help and exit")
        private boolean help;

        public void setConsole(final AeshConsole console) {
            this.console = console;
        }

        @Override
        public CommandResult execute(final CommandInvocation commandInvocation) throws IOException, InterruptedException {
            if(help) {
                commandInvocation.getShell().out().println(commandInvocation.getHelpInfo("connect"));
            } else {
                if (webPushClient != null && webPushClient.isConnected()) {
                    commandInvocation.getShell().out().println("Currently connected. Will disconnect");
                    webPushClient.disconnect();
                }
                commandInvocation.putProcessInBackground();
                webPushClient = WebPushClient.forHost(host)
                        .port(port)
                        .ssl(true)
                        .notificationHandler(new CallbackHandler(console))
                        .build();
                try {
                    webPushClient.connect();
                } catch (Exception e) {
                    e.printStackTrace();
                    return CommandResult.FAILURE;
                }
            }
            return CommandResult.SUCCESS;
        }

        public WebPushClient webPushClient() {
            return webPushClient;
        }
    }

    @CommandDefinition(name = "disconnect", description = "from the connected WebPush Server")
    public static class DisconnectCommand implements Command {
        private final ConnectCommand connectCommand;

        @Option(hasValue = false, description = "display this help and exit")
        private boolean help;

        public DisconnectCommand(final ConnectCommand connectCommand) {
            this.connectCommand = connectCommand;
        }

        @Override
        public CommandResult execute(final CommandInvocation commandInvocation) throws IOException, InterruptedException {
            if(help) {
                commandInvocation.getShell().out().println(commandInvocation.getHelpInfo("disconnect"));
            } else {
                commandInvocation.putProcessInBackground();
                final WebPushClient client = connectCommand.webPushClient();
                if (client != null && client.isConnected()) {
                    client.disconnect();
                    commandInvocation.getShell().out().println("Disconnected from ["
                            + client.host() + ":" + client.port() + "]");
                } else {
                    commandInvocation.getShell().out().println("Not currently connected");
                    return CommandResult.FAILURE;
                }
            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "register", description = "this device with the WebPush Server")
    public static class RegisterCommand implements Command {
        private final ConnectCommand connectCommand;

        @Option(shortName = 'p',
                hasValue = true,
                description = "the path that that the WebPush server exposes for registrations",
                defaultValue = "/webpush/register")
        private String path;

        @Option(hasValue = false, description = "display this help and exit")
        private boolean help;

        public RegisterCommand(final ConnectCommand connectCommand) {
            this.connectCommand = connectCommand;
        }

        @Override
        public CommandResult execute(final CommandInvocation commandInvocation) throws IOException, InterruptedException {
            if(help) {
                commandInvocation.getShell().out().println(commandInvocation.getHelpInfo("register"));
            } else {
                commandInvocation.putProcessInBackground();
                final WebPushClient client = connectCommand.webPushClient();
                if (!isConnected(client, commandInvocation)) {
                    return CommandResult.FAILURE;
                }
                try {
                    client.register(path);
                } catch (Exception e) {
                    e.printStackTrace();
                    return CommandResult.FAILURE;
                }
            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "subscribe", description = "and displays the endpoint-url for the created subscription")
    public static class SubscribeCommand implements Command {
        private final ConnectCommand connectCommand;

        @Option(hasValue = false, description = "display this help and exit")
        private boolean help;

        @Option(hasValue = true, description = "the subscription WebLink URL, of rel type 'urn:ietf:params:push:sub', from the register command response", required = true)
        private String url;

        public SubscribeCommand(final ConnectCommand connectCommand) {
            this.connectCommand = connectCommand;
        }

        @Override
        public CommandResult execute(final CommandInvocation commandInvocation) throws IOException, InterruptedException {
            if(help) {
                commandInvocation.getShell().out().println(commandInvocation.getHelpInfo("subscribe"));
            } else {
                commandInvocation.putProcessInBackground();
                final WebPushClient client = connectCommand.webPushClient();
                if (!isConnected(client, commandInvocation)) {
                    return CommandResult.FAILURE;
                }
                try {
                    client.createSubscription(url);
                } catch (Exception e) {
                    e.printStackTrace();
                    return CommandResult.FAILURE;
                }
            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "monitor", description = "for notifications")
    public static class MonitorCommand implements Command {
        private final ConnectCommand connectCommand;

        @Option(hasValue = false, description = "display this help and exit")
        private boolean help;

        @Option(hasValue = true, description = "the monitor WebLink URL, of rel type 'urn:ietf:params:push:reg',  from the register command response", required = true)
        private String url;

        @Option(hasValue = false, description = "returns any existing notifications that the server might have")
        private boolean nowait;

        public MonitorCommand(final ConnectCommand connectCommand) {
            this.connectCommand = connectCommand;
        }

        @Override
        public CommandResult execute(final CommandInvocation commandInvocation) throws IOException, InterruptedException {
            if(help) {
                commandInvocation.getShell().out().println(commandInvocation.getHelpInfo("monitor"));
            } else {
                commandInvocation.putProcessInBackground();
                final WebPushClient client = connectCommand.webPushClient();
                if (!isConnected(client, commandInvocation)) {
                    return CommandResult.FAILURE;
                }
                try {
                    client.monitor(url, nowait);
                } catch (Exception e) {
                    e.printStackTrace();
                    return CommandResult.FAILURE;
                }
            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "notify", description = "a channel")
    public static class NotifyCommand implements Command {
        private final ConnectCommand connectCommand;

        @Option(hasValue = false, description = "display this help and exit")
        private boolean help;

        @Option(hasValue = true, description = "the endpoint url from an earlier 'subscribe' commands location response", required = true)
        private String url;

        @Option(hasValue = true, description = "the body/payload of the notification")
        private String payload;

        public NotifyCommand(final ConnectCommand connectCommand) {
            this.connectCommand = connectCommand;
        }

        @Override
        public CommandResult execute(final CommandInvocation commandInvocation) throws IOException, InterruptedException {
            if(help) {
                commandInvocation.getShell().out().println(commandInvocation.getHelpInfo("notify"));
            } else {
                commandInvocation.putProcessInBackground();
                final WebPushClient client = connectCommand.webPushClient();
                if (!isConnected(client, commandInvocation)) {
                    return CommandResult.FAILURE;
                }
                try {
                    client.notify(url, payload);
                } catch (Exception e) {
                    e.printStackTrace();
                    return CommandResult.FAILURE;
                }
            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition( name = "status", description = "of the subscription and returns the latest message if the server has any undelivered messages for the subscription")
    public static class StatusCommand implements Command {
        private final ConnectCommand connectCommand;

        @Option(hasValue = false, description = "display this help and exit")
        private boolean help;

        @Option(hasValue = true, description = "the endpoint url from an earlier 'subscribe' commands location response", required = true)
        private String url;

        public StatusCommand(final ConnectCommand connectCommand) {
            this.connectCommand = connectCommand;
        }

        @Override
        public CommandResult execute(final CommandInvocation commandInvocation) throws IOException, InterruptedException {
            if(help) {
                commandInvocation.getShell().out().println(commandInvocation.getHelpInfo("status"));
            } else {
                commandInvocation.putProcessInBackground();
                final WebPushClient client = connectCommand.webPushClient();
                if (!isConnected(client, commandInvocation)) {
                    return CommandResult.FAILURE;
                }
                try {
                    client.status(url);
                } catch (Exception e) {
                    e.printStackTrace();
                    return CommandResult.FAILURE;
                }
            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "delete", description = "subscription")
    public static class DeleteSubCommand implements Command {
        private final ConnectCommand connectCommand;

        @Option(hasValue = false, description = "display this help and exit")
        private boolean help;

        @Option(hasValue = true, description = "the endpoint url for the subscription to be deleted", required = true)
        private String url;

        public DeleteSubCommand(final ConnectCommand connectCommand) {
            this.connectCommand = connectCommand;
        }

        @Override
        public CommandResult execute(final CommandInvocation commandInvocation) throws IOException, InterruptedException {
            if(help) {
                commandInvocation.getShell().out().println(commandInvocation.getHelpInfo("delete"));
            } else {
                commandInvocation.putProcessInBackground();
                final WebPushClient client = connectCommand.webPushClient();
                if (!isConnected(client, commandInvocation)) {
                    return CommandResult.FAILURE;
                }
                try {
                    client.deleteSubscription(url);
                } catch (Exception e) {
                    e.printStackTrace();
                    return CommandResult.FAILURE;
                }
            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "aggregate", description = "multiple channels so they are handled as one")
    public static class AggregateCommand implements Command {
        private final ConnectCommand connectCommand;

        @Option(hasValue = false, description = "display this help and exit")
        private boolean help;

        @Option(hasValue = true, description = "the aggreagate url from an earlier 'subscribe' commands location response", required = true)
        private String url;

        @Option(hasValue = true, description = "comma separated list of channels that should be part of this aggreagate channel")
        private String channels;

        public AggregateCommand(final ConnectCommand connectCommand) {
            this.connectCommand = connectCommand;
        }

        @Override
        public CommandResult execute(final CommandInvocation commandInvocation) throws IOException, InterruptedException {
            if(help) {
                commandInvocation.getShell().out().println(commandInvocation.getHelpInfo("aggregate"));
            } else {
                commandInvocation.putProcessInBackground();
                final WebPushClient client = connectCommand.webPushClient();
                if (!isConnected(client, commandInvocation)) {
                    return CommandResult.FAILURE;
                }
                try {
                    final String json = JsonMapper.toJson(AggregateSubscription.from(channels));
                    client.createAggregateSubscription(url, json);
                } catch (Exception e) {
                    e.printStackTrace();
                    return CommandResult.FAILURE;
                }
            }
            return CommandResult.SUCCESS;
        }
    }

    private static boolean isConnected(final WebPushClient client, CommandInvocation inv) {
        if (client == null || !client.isConnected()) {
            inv.getShell().out().println("Please use the connect command to connect to the server");
            return false;
        }
        return true;
    }

    private static class CallbackHandler implements EventHandler {

        private final AeshConsole console;
        private final Prompt inbound =  new Prompt(new TerminalString("< ",
                new TerminalColor(Color.YELLOW, DEFAULT, Intensity.BRIGHT)));
        private final Prompt outbound =  new Prompt(new TerminalString("> ",
                new TerminalColor(GREEN, DEFAULT, Intensity.BRIGHT)));

        CallbackHandler(final AeshConsole console) {
            this.console = console;
        }

        @Override
        public void outbound(final Http2Headers headers) {
            printOutbound(headers);
        }

        @Override
        public void outbound(final Http2Headers headers, final ByteBuf byteBuf) {
            printOutbound(headers, byteBuf);
        }

        @Override
        public void inbound(Http2Headers headers, int streamId) {
            printInbound(headers.toString(), streamId);
        }

        @Override
        public void notification(final String data, final int streamId) {
            printInbound(data, streamId);
        }

        @Override
        public void message(final String message) {
            printOutbound(message);
        }

        private void printOutbound(final Http2Headers headers) {
            printOutbound(headers.toString());
        }

        private void printOutbound(final String msg) {
            final Prompt current = console.getPrompt();
            console.setPrompt(outbound);
            console.getShell().out().println(msg);
            console.setPrompt(current);
        }

        private void printOutbound(final Http2Headers headers, final ByteBuf byteBuf) {
            final Prompt current = console.getPrompt();
            console.setPrompt(outbound);
            console.getShell().out().println(headers);
            console.getShell().out().println(JsonMapper.pretty(byteBuf.toString(CharsetUtil.UTF_8)));
            console.setPrompt(current);
        }

        private void printInbound(final String message, final int streamId) {
            final Prompt current = console.getPrompt();
            console.setPrompt(inbound);
            console.getShell().out().println("[streamid:" + streamId + "] " + message);
            console.setPrompt(current);
        }

    }

}
