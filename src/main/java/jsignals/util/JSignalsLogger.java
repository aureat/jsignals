package jsignals.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class for managing logging within the JSignals library.
 * It provides a centralized way to obtain SLF4J loggers and configure the logging level.
 */
public final class JSignalsLogger {

    private JSignalsLogger() { }

    /**
     * Gets a logger for the specified class.
     *
     * @param clazz The class to get the logger for.
     * @return An SLF4J Logger instance.
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }

    /**
     * Gets a logger for the specified name.
     *
     * @param name The name of the logger.
     * @return An SLF4J Logger instance.
     */
    public static Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }

    /**
     * Sets the global logging level for the entire JSignals library.
     * This affects all loggers obtained through this utility.
     *
     * @param level The desired logging level (e.g., Level.DEBUG, Level.INFO).
     */
    public static void setLogLevel(Level level) {
        // This requires the Logback backend to be on the classpath.
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        // Set the level for the root logger of the library's package.
        ch.qos.logback.classic.Logger logger = loggerContext.getLogger("jsignals");
        logger.setLevel(level);
        System.out.println("[JSignals] Log level set to " + level);
    }

}
