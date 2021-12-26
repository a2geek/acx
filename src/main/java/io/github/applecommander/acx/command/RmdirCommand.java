package io.github.applecommander.acx.command;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import com.webcodepro.applecommander.storage.DirectoryEntry;
import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DiskException;
import com.webcodepro.applecommander.storage.DiskUnrecognizedException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FormattedDisk;

import io.github.applecommander.acx.converter.DiskConverter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "rmdir", description = "Remove a directory on disk.",
         aliases = { "rd" },
         parameterListHeading = "%nParameters:%n",
         descriptionHeading = "%n",
         optionListHeading = "%nOptions:%n")
public class RmdirCommand implements Callable<Integer> {
    private static Logger LOG = Logger.getLogger(RmdirCommand.class.getName());

    @Option(names = { "-h", "--help" }, description = "Show help for subcommand.", usageHelp = true)
    private boolean helpFlag;

    @Option(names = { "-d", "--disk" }, description = "Image to process.", required = true,
            converter = DiskConverter.class, defaultValue = "${ACX_DISK_NAME}")
    private Disk disk;
    
    @Option(names = { "-r", "--recursive" }, description = "Recursively delete subdirectories.")
    private boolean recursiveFlag;
    
    @Option(names = { "-f", "--force" }, description = "Force files to be deleted as well.")
    private boolean forceFlag;
    
    @Parameters(description = "Directory name to delete (use '/' as divider).")
    private String fullPath;

    @Override
    public Integer call() throws Exception {
        FormattedDisk formattedDisk = disk.getFormattedDisks()[0];
        
        // Locate directory
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
                throw new RuntimeException(String.format("Directory does not exist: '%s'", pathName));
            }
        }
        
        deleteDirectory(directory);
        
        saveDisk(formattedDisk);
        return 0;
    }
    
    public void deleteDirectory(DirectoryEntry directory) throws DiskException {
        for (FileEntry file : directory.getFiles()) {
            if (file.isDeleted()) {
                // skip
            } else if (recursiveFlag && file.isDirectory()) {
                deleteDirectory((DirectoryEntry)file);
            }
            else if (forceFlag && !file.isDirectory()) {
                file.delete();
            }
            else {
                String message = String.format("Encountered %s '%s'",
                        file.isDirectory() ? "directory" : "file",
                        file.getFilename());
                throw new RuntimeException(message);
            }
        }
        
        FileEntry file = (FileEntry)directory;
        file.delete();
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
