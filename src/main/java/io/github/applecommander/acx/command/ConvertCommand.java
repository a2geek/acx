package io.github.applecommander.acx.command;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DiskUnrecognizedException;
import com.webcodepro.applecommander.storage.FormattedDisk;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "convert", description = {
            "Uncompress a ShrinkIt or Binary II file; ",
            "or convert a DiskCopy 4.2 image into a ProDOS disk image." },
        parameterListHeading = "%nParameters:%n",
        descriptionHeading = "%n",
        optionListHeading = "%nOptions:%n")
public class ConvertCommand implements Callable<Integer> {
    private static Logger LOG = Logger.getLogger(ConvertCommand.class.getName());

    @Spec
    private CommandSpec spec;
    
    @Option(names = { "-h", "--help" }, description = "Show help for subcommand.", usageHelp = true)
    private boolean helpFlag;

    @Option(names = { "-d", "--disk" }, description = "Image to create.", required = true,
            defaultValue = "${ACX_DISK_NAME}")
    private String diskName;
    
    @Option(names = { "-f", "--force" }, description = "Allow existing disk image to be replaced.")
    private boolean overwriteFlag;

    @Parameters(description = "Archive to convert.", arity = "1")
    private String archiveName;

    @Override
    public Integer call() throws Exception {
        File targetFile = new File(diskName);
        if (targetFile.exists() && !overwriteFlag) {
            throw new RuntimeException("File exists and overwriting not enabled.");
        }
        
        Disk disk = new Disk(archiveName);
        disk.setFilename(diskName);
        saveDisk(disk);
                    
        return 0;
    }
    
    public void saveDisk(Disk disk) throws DiskUnrecognizedException {
        saveDisk(disk.getFormattedDisks()[0]);
    }
    
    public void saveDisk(FormattedDisk disk) {
        try {
            // Only save if there are changes.
            if (disk.getDiskImageManager().hasChanged()) {
                LOG.fine(() -> String.format("Saving disk '%s'", disk.getFilename()));
                disk.save();
            } else {
                LOG.fine(() -> String.format("Disk '%s' has not changed; not saving.", disk.getFilename()));
            }
        } catch (IOException e) {
            LOG.severe(e.getMessage());
        }
    }
}
