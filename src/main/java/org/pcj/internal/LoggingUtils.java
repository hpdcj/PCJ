package org.pcj.internal;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Queue;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LoggingUtils {

    private static final String NODE_NAME;

    static {
        Queue<InetAddress> interfacesAddresses = Networker.getHostAllNetworkInterfaces();
        String hostname = guessCurrentHostName(interfacesAddresses);

        if (hostname == null) hostname = "*unknown*";

        NODE_NAME = String.format("%s:%d", hostname, Configuration.DEFAULT_PORT);
    }

    private static String guessCurrentHostName(Queue<InetAddress> interfacesAddresses) {
        InetAddress localHost = null;
        try {
            localHost = InetAddress.getLocalHost();
            if (!localHost.isLoopbackAddress()) {
                return localHost.getHostName();
            }
        } catch (UnknownHostException e) {
            // skip exception, try another method
        }

        for (InetAddress inetAddress : interfacesAddresses) {
            if (!inetAddress.isLoopbackAddress()) {
                return inetAddress.getHostName();
            }
        }

        return localHost == null ? null : localHost.getHostName();
    }

    public static void prepareLoggers() {
        Level level = Level.FINEST;
        Arrays.stream(Logger.getLogger("").getHandlers())
                .forEach(handler -> handler.setLevel(level));
        Logger.getLogger("").setLevel(level);

        for (Handler handler : Logger.getLogger("").getHandlers()) {
            Formatter formatter = handler.getFormatter();
            if (!(formatter instanceof PcjLogFormatter)) {
                handler.setFormatter(new PcjLogFormatter(formatter));
            }
        }
    }

    private static class PcjLogFormatter extends Formatter {
        private final Formatter formatter;

        public PcjLogFormatter(Formatter formatter) {
            this.formatter = formatter;
        }

        @Override
        public String format(LogRecord record) {
            record.setMessage("[" + NODE_NAME + "] " + record.getMessage());
            return formatter.format(record);
        }
    }

}
