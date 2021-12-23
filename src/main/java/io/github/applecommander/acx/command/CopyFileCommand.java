package io.github.applecommander.acx.command;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DiskException;
import com.webcodepro.applecommander.storage.DiskUnrecognizedException;
import com.webcodepro.applecommander.storage.FormattedDisk;

import io.github.applecommander.acx.converter.DiskConverter;
import io.github.applecommander.acx.fileutil.FileUtils;
import io.github.applecommander.filestreamer.FileStreamer;
import io.github.applecommander.filestreamer.FileTuple;
import io.github.applecommander.filestreamer.TypeOfFile;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "copy", description = "Copy files between disks.",
         aliases = { "cp" },
         parameterListHeading = "%nParameters:%n",
         descriptionHeading = "%n",
         optionListHeading = "%nOptions:%n")
public class CopyFileCommand implements Callable<Integer> {
    private static Logger LOG = Logger.getLogger(CopyFileCommand.class.getName());

    @Option(names = { "-h", "--help" }, description = "Show help for subcommand.", usageHelp = true)
    private boolean helpFlag;

    @Option(names = { "-d", "--disk" }, description = "Image to process.", required = true,
            converter = DiskConverter.class, defaultValue = "${ACX_DISK_NAME}")
    private Disk disk;
    
    @Option(names = { "-r", "--recursive" }, description = "Copy files recursively.")
    private boolean recursiveFlag;
    
    @Option(names = { "--to", "--directory" }, description = "Specify which directory to place files.")
    private String targetPath;
    
    @Option(names = { "-f", "--from", "--source" }, description = "Source disk for files.", 
            converter = DiskConverter.class, required = true)
    private Disk sourceDisk;
    
    @Parameters(arity = "*", description = "File glob(s) to copy (default = '*')", 
            defaultValue = "*")
    private List<String> globs;

    @Override
    public Integer call() throws Exception {
        List<FileTuple> files = FileStreamer.forDisk(sourceDisk)
                .ignoreErrors(true)
                .includeTypeOfFile(TypeOfFile.BOTH)
                .recursive(recursiveFlag)
                .matchGlobs(globs)
                .stream()
                .collect(Collectors.toList());

        if (files.isEmpty()) {
            LOG.warning(() -> String.format("No matches found for %s.", String.join(",", globs)));
        } else {
            files.forEach(this::fileHandler);
            saveDisk(disk);
        }
        
        return 0;
    }

    private void fileHandler(FileTuple tuple) {
        try {
            FormattedDisk formattedDisk = disk.getFormattedDisks()[0];
            if (!recursiveFlag && tuple.fileEntry.isDirectory()) {
                formattedDisk.createDirectory(tuple.fileEntry.getFilename());
            } else {
                FileUtils.copy(formattedDisk, tuple.fileEntry);
            }
        } catch (DiskException ex) {
            LOG.severe(ex.getMessage());
            throw new RuntimeException(ex);
        }
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
