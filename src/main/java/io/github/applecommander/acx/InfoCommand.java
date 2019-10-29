package io.github.applecommander.acx;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.FormattedDisk.DiskInformation;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "info", description = "Show information on a disk image(s).",
        aliases = "i",
        parameterListHeading = "%nParameters:%n",
        descriptionHeading = "%n",
        optionListHeading = "%nOptions:%n")
public class InfoCommand implements Callable<Integer> {
    private static Logger LOG = Logger.getLogger(InfoCommand.class.getName());

    @Option(names = { "-h", "--help" }, description = "Show help for subcommand.", usageHelp = true)
    private boolean helpFlag;
    
    @Parameters(arity = "1..*", description = "Image(s) to process.")
    private List<Path> paths;

    @Override
    public Integer call() throws Exception {
        for (Path path : paths) {
            LOG.info(() -> "Path: " + path);
            Disk disk = new Disk(path.toString());
            FormattedDisk[] formattedDisks = disk.getFormattedDisks();
            for (int i = 0; i < formattedDisks.length; i++) {
                FormattedDisk formattedDisk = formattedDisks[i];
                LOG.info(() -> String.format("Disk: %s (%s)", formattedDisk.getDiskName(), formattedDisk.getFormat()));
                for (DiskInformation diskinfo : formattedDisk.getDiskInformation()) {
                    System.out.printf("%s: %s\n", diskinfo.getLabel(), diskinfo.getValue());
                }
                System.out.println();
            }
        }
        return 0;
    }
}
