package io.github.applecommander.acx.command;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import com.webcodepro.applecommander.storage.DirectoryEntry;
import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DiskUnrecognizedException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FormattedDisk;

import io.github.applecommander.acx.converter.DiskConverter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "mkdir", description = "Create a directory on disk.",
         aliases = { "md" },
         parameterListHeading = "%nParameters:%n",
         descriptionHeading = "%n",
         optionListHeading = "%nOptions:%n")
public class MkdirCommand implements Callable<Integer> {
    private static Logger LOG = Logger.getLogger(MkdirCommand.class.getName());

    @Option(names = { "-h", "--help" }, description = "Show help for subcommand.", usageHelp = true)
    private boolean helpFlag;

    @Option(names = { "-d", "--disk" }, description = "Image to process.", required = true,
            converter = DiskConverter.class, defaultValue = "${ACX_DISK_NAME}")
    private Disk disk;
    
    @Option(names = { "-p" }, description = "Create intermediate subdirectories.")
    private boolean prefixFlag;
    
    @Parameters(description = "Directory name to create (use '/' as divider).")
    private String fullPath;

    @Override
    public Integer call() throws Exception {
        FormattedDisk formattedDisk = disk.getFormattedDisks()[0];
        DirectoryEntry directory = formattedDisk;
        
        String[] paths = fullPath.split("/");
        for (int i=0; i<paths.length; i++) {
            final String pathName = formattedDisk.getSuggestedFilename(paths[i]);
            Optional<FileEntry> optEntry = directory.getFiles().stream()
                    .filter(entry -> entry.getFilename().equalsIgnoreCase(pathName))
                    .findFirst();
            
            if (optEntry.isPresent()) {
                FileEntry fileEntry = optEntry.get();
                if (fileEntry instanceof DirectoryEntry) {
                    directory = (DirectoryEntry)fileEntry;
                }
                else {
                    throw new RuntimeException(String.format("Not a directory: '%s'", pathName));
                }
            }
            else {
                if (prefixFlag || i == paths.length-1) {
                    directory = directory.createDirectory(pathName);
                } 
                else {
                    throw new RuntimeException(String.format("Directory does not exist: '%s'", pathName));
                }
            }
        }
        
        saveDisk(formattedDisk);
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
