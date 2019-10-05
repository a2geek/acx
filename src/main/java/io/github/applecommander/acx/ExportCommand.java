package io.github.applecommander.acx;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "export", description = "Export file(s) from a disk image.",
        aliases = "x",
        parameterListHeading = "%nParameters:%n",
        descriptionHeading = "%n",
        optionListHeading = "%nOptions:%n")
public class ExportCommand implements Callable<Integer> {
    @ParentCommand
    private Main main;
    
    @Option(names = { "-h", "--help" }, description = "Show help for subcommand.", usageHelp = true)
    private boolean helpFlag;
    
    @ArgGroup(exclusive = true)
    private ExtractionStrategy strategy;
    
    @Option(names = { "-a", "--all" }, description = "Extract all files (requires an output directory).")
    private boolean allFlag;
    
    @Option(names = { "-o", "--output" }, description = "Extract to file or to directory.")
    private Path outputName;
    
    @Parameters(arity = "1", description = "Image to process.")
    private Path image;
    
    @Parameters(arity = "1..*", description = "File(s) to extract.")
    private List<Path> paths;

    @Override
    public Integer call() throws Exception {
        return 0;
    }
    
    public class ExtractionStrategy {
        @Option(names = "--raw", description = "Extract raw data (including any OS specific bytes).")
        private boolean rawFlag;
        
        @Option(names = "--binary", description = "Extract binary file.")
        private boolean binaryFlag;
    }
}
