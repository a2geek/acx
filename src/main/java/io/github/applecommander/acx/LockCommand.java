package io.github.applecommander.acx;

import java.util.logging.Logger;

import io.github.applecommander.filestreamer.FileTuple;
import picocli.CommandLine.Command;

@Command(name = "lock", description = "Lock file(s) on a disk image.",
        parameterListHeading = "%nParameters:%n",
        descriptionHeading = "%n",
        optionListHeading = "%nOptions:%n")
public class LockCommand extends ModifyingCommand {
    private static Logger LOG = Logger.getLogger(LockCommand.class.getName());

    public void fileHandler(FileTuple tuple) {
    	tuple.fileEntry.setLocked(true);
    	LOG.info(() -> String.format("File '%s' locked.", tuple.fileEntry.getFilename()));
    }
}
