package io.github.applecommander.acx;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.github.applecommander.filestreamer.FileStreamer;
import io.github.applecommander.filestreamer.FileTuple;
import io.github.applecommander.filestreamer.TypeOfFile;
import picocli.CommandLine.Parameters;

public abstract class ModifyingCommandWithGlobs extends ModifyingCommand {
    private static Logger LOG = Logger.getLogger(ModifyingCommandWithGlobs.class.getName());

    @Parameters(index = "1", arity = "1..*", description = "File glob(s) to unlock (default = '*') - be cautious of quoting!")
    private List<String> globs = Arrays.asList("*");

    @Override
    public Integer call() throws Exception {
        List<FileTuple> files = FileStreamer.forDisk(image)
			        .ignoreErrors(true)
			        .includeTypeOfFile(TypeOfFile.FILE)
			        .matchGlobs(globs)
			        .stream()
			        .collect(Collectors.toList());

        if (files.isEmpty()) {
        	LOG.warning(() -> String.format("No matches found for %s.", String.join(",", globs)));
        } else {
        	files.forEach(this::fileHandler);
        	files.forEach(this::saveDisk);
        }
        
        return 0;
    }
    
    public abstract void fileHandler(FileTuple tuple);
}
