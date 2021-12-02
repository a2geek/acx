package io.github.applecommander.acx;

import java.util.logging.Logger;

import io.github.applecommander.filestreamer.FileTuple;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "delete", description = "Delete file(s) from a disk image.",
        aliases = { "del", "rm" },
        parameterListHeading = "%nParameters:%n",
        descriptionHeading = "%n",
        optionListHeading = "%nOptions:%n")
public class DeleteCommand extends ModifyingCommandWithGlobs {
    private static Logger LOG = Logger.getLogger(DeleteCommand.class.getName());
    
    @Option(names = { "-f", "--force" }, description = "Force delete locked files.")
    private boolean forceFlag;

    public void fileHandler(FileTuple tuple) {
    	if (tuple.fileEntry.isLocked()) {
    		if (forceFlag) {
    			LOG.info(() -> String.format("File '%s' is locked, but 'force' specified; ignoring lock.",
    					tuple.fileEntry.getFilename()));
    		} else {
	    		LOG.warning(() -> String.format("File '%s' is locked.", tuple.fileEntry.getFilename()));
	    		return;
    		}
    	}
    	tuple.fileEntry.delete();
    	LOG.info(() -> String.format("File '%s' deleted.", tuple.fileEntry.getFilename()));
    }
}
