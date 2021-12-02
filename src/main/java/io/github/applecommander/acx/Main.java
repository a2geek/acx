package io.github.applecommander.acx;

import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;

/**
 * Primary entry point into the 'acx' utility. 
 */
@Command(name = "acx", mixinStandardHelpOptions = true, versionProvider = VersionProvider.class,
    descriptionHeading = "%n",
    commandListHeading = "%nCommands:%n",
    optionListHeading = "%nOptions:%n",
    description = "'ac' experimental utility", 
    subcommands = {
    		DeleteCommand.class,
            ExportCommand.class,
            HelpCommand.class,
            InfoCommand.class,
            ListCommand.class,
            LockCommand.class,
            RenameCommand.class,
            UnlockCommand.class
    })
public class Main {
    private static Logger LOG = Logger.getLogger(Main.class.getName());
    private static final Level LOG_LEVELS[] = { Level.OFF, Level.SEVERE, Level.WARNING, Level.INFO, 
            Level.CONFIG, Level.FINE, Level.FINER, Level.FINEST };
    
    static {
    	System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s%n");
    	setAllLogLevels(Level.WARNING);
    }
    private static void setAllLogLevels(Level level) {
    	Logger rootLogger = LogManager.getLogManager().getLogger("");
		rootLogger.setLevel(level);
		for (Handler handler : rootLogger.getHandlers()) {
		    handler.setLevel(level);
		}
    }
    
    @Option(names = { "--debug" }, description = "Show detailed stack traces.")
    private void enableStackTrace(boolean flag) {
        Main.showError = t -> t.printStackTrace(System.err);
    }
    private static Consumer<Throwable> showError = t -> System.err.println(t.getLocalizedMessage()); 
    
    @Option(names = { "-v", "--verbose" }, description = "Be verbose. Multiple occurrences increase logging.")
    public void setVerbosity(boolean[] flag) {
    	// The "+ 2" is due to the default of the levels
        int loglevel = Math.min(flag.length + 2, LOG_LEVELS.length);
        Level level = LOG_LEVELS[loglevel-1];
        setAllLogLevels(level);
    }
    
    @Option(names = { "--quiet" }, description = "Turn off all logging.")
    public void setQuiet(boolean flag) {
    	setAllLogLevels(Level.OFF);
    }

    public static void main(String[] args) {
        CommandLine cmd = new CommandLine(new Main());
        if (args.length == 0) {
            cmd.usage(System.out);
            System.exit(1);
        }
        
        try {
            LOG.info(() -> String.format("Log level set to %s.", Logger.getGlobal().getLevel()));
            int exitCode = cmd.execute(args);
            LOG.fine("Exiting with code " + exitCode);
            System.exit(exitCode);
        } catch (Throwable t) {
            showError.accept(t);
            System.exit(-1);
        }
    }
}
