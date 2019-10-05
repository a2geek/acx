package io.github.applecommander.acx;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.FormattedDisk.DiskInformation;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "info", description = "Show information on a disk image(s).",
        aliases = "i",
        parameterListHeading = "%nParameters:%n",
        descriptionHeading = "%n",
        optionListHeading = "%nOptions:%n")
public class InfoCommand implements Callable<Integer> {
    @ParentCommand
    private Main main;
    
    @Option(names = { "-h", "--help" }, description = "Show help for subcommand.", usageHelp = true)
    private boolean helpFlag;
    
    @Parameters(arity = "1..*", description = "Image(s) to process.")
    private List<Path> paths;

    @Override
    public Integer call() throws Exception {
        try {
            for (Path path : paths) {
                Disk disk = new Disk(path.toString());
                FormattedDisk[] formattedDisks = disk.getFormattedDisks();
                for (int i = 0; i < formattedDisks.length; i++) {
                    FormattedDisk formattedDisk = formattedDisks[i];
                    for (DiskInformation diskinfo : formattedDisk.getDiskInformation()) {
                        System.out.printf("%s: %s\n", diskinfo.getLabel(), diskinfo.getValue());
                    }
                    System.out.println();
                }
            }
        } catch (IOException ex) {
            main.log(ex);
            return 1;
        }
        return 0;
    }
}
