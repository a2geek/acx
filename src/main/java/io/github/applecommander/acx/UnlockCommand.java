package io.github.applecommander.acx;

import java.util.logging.Logger;

import io.github.applecommander.filestreamer.FileTuple;
import picocli.CommandLine.Command;

@Command(name = "unlock", description = "Unlock file(s) on a disk image.",
        parameterListHeading = "%nParameters:%n",
        descriptionHeading = "%n",
        optionListHeading = "%nOptions:%n")
public class UnlockCommand extends ModifyingCommandWithGlobs {
    private static Logger LOG = Logger.getLogger(UnlockCommand.class.getName());

    public void fileHandler(FileTuple tuple) {
    	tuple.fileEntry.setLocked(false);
    	LOG.info(() -> String.format("File '%s' unlocked.", tuple.fileEntry.getFilename()));
    }
}