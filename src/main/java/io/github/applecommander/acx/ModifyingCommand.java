package io.github.applecommander.acx;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.webcodepro.applecommander.storage.FormattedDisk;

import io.github.applecommander.filestreamer.FileStreamer;
import io.github.applecommander.filestreamer.FileTuple;
import io.github.applecommander.filestreamer.TypeOfFile;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public abstract class ModifyingCommand implements Callable<Integer> {
    private static Logger LOG = Logger.getLogger(ModifyingCommand.class.getName());

    @Option(names = { "-h", "--help" }, description = "Show help for subcommand.", usageHelp = true)
    private boolean helpFlag;
    
    @Parameters(index = "0", description = "Image to process.")
    private File image;
    
    @Parameters(index = "1", arity = "1..*", description = "File glob(s) to unlock (default = '*') - be cautious of quoting!")
    private List<String> globs = Arrays.asList("*");

    @Override
    public Integer call() throws Exception {
        List<FileTuple> files = FileStreamer.forDisk(image)
			        .ignoreErrors(true)
			        .includeTypeOfFile(TypeOfFile.FILE)
			        .matchGlobs(globs)
			        .afterDisk(this::saveDisk)
			        .stream()
			        .collect(Collectors.toList());

        if (files.isEmpty()) {
        	LOG.warning(() -> String.format("No matches found for %s.", String.join(",", globs)));
        } else {
        	files.forEach(this::fileHandler);
        }
        
        return 0;
    }
    
    public abstract void fileHandler(FileTuple tuple);
    
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
