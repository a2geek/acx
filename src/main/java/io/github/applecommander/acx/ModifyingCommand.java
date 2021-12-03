package io.github.applecommander.acx;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import com.webcodepro.applecommander.storage.FormattedDisk;

import io.github.applecommander.filestreamer.FileTuple;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public abstract class ModifyingCommand implements Callable<Integer> {
    private static Logger LOG = Logger.getLogger(ModifyingCommand.class.getName());

    @Option(names = { "-h", "--help" }, description = "Show help for subcommand.", usageHelp = true)
    private boolean helpFlag;

    @Parameters(index = "0", description = "Image to process.")
    protected File image;
    
    public void saveDisk(FileTuple tuple) {
    	saveDisk(tuple.formattedDisk);
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
