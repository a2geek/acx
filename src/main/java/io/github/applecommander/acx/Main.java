package io.github.applecommander.acx;

import com.webcodepro.applecommander.ui.UiBundle;
import com.webcodepro.applecommander.util.TextBundle;

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
    TextBundle textBundle = UiBundle.getInstance();
    
    @Option(names = { "--debug" }, description = "Show detailed stack traces.")
    private boolean debug;

    public static void main(String[] args) {
        CommandLine cmd = new CommandLine(new Main());
        if (args.length == 0) {
            cmd.usage(System.out);
            System.exit(1);
        }
        
        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }

    public void log(Throwable t) {
        if (debug) {
            t.printStackTrace(System.err);
        } else {
            System.err.println(t.getLocalizedMessage());
        }
    }
    public void logf(String format, String arg1, Throwable t) {
        System.err.printf(format, arg1, t.getLocalizedMessage());
        if (debug) {
            t.printStackTrace(System.err);
        }
    }
}
