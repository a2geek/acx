package io.github.applecommander.acx;

import java.util.Collections;
import java.util.function.Consumer;
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
            HelpCommand.class,
            InfoCommand.class,
            ListCommand.class,
            ExportCommand.class
    })
public class Main {
    private static Logger LOG = Logger.getLogger(Main.class.getName());
    private static final Level LOG_LEVELS[] = { Level.OFF, Level.SEVERE, Level.WARNING, Level.INFO, 
            Level.CONFIG, Level.FINE, Level.FINER, Level.FINEST };
    
    static {
        Collections.list(LogManager.getLogManager().getLoggerNames())
            .forEach(name -> LogManager.getLogManager().getLogger(name).setLevel(Level.OFF));
    }
    
    @Option(names = { "--debug" }, description = "Show detailed stack traces.")
    private void enableStackTrace(boolean flag) {
        Main.showError = t -> t.printStackTrace(System.err);
    }
    private static Consumer<Throwable> showError = t -> System.err.println(t.getLocalizedMessage()); 
    
    @Option(names = { "-v", "--verbose" }, description = "Be verbose. Multiple occurrences increase logging.")
    public void setVerbosity(boolean[] flag) {
        int loglevel = Math.min(flag.length, LOG_LEVELS.length);
        Level level = LOG_LEVELS[loglevel-1];
        Collections.list(LogManager.getLogManager().getLoggerNames())
            .forEach(name -> LogManager.getLogManager().getLogger(name).setLevel(level));
    }

    public static void main(String[] args) {
        CommandLine cmd = new CommandLine(new Main());
        if (args.length == 0) {
            cmd.usage(System.out);
            System.exit(1);
        }
        
        try {
            LOG.fine("Command-line arguments: " + args);
            int exitCode = cmd.execute(args);
            LOG.info("Exiting with code " + exitCode);
            LOG.fine("Log level was " + Logger.getGlobal().getLevel());
            System.exit(exitCode);
        } catch (Throwable t) {
            showError.accept(t);
            System.exit(-1);
        }
    }
}
