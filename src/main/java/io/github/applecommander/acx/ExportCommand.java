package io.github.applecommander.acx;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.storage.filters.BinaryFileFilter;
import com.webcodepro.applecommander.storage.filters.HexDumpFileFilter;

import io.github.applecommander.filestreamer.FileStreamer;
import io.github.applecommander.filestreamer.FileTuple;
import io.github.applecommander.filestreamer.TypeOfFile;
import io.github.applecommander.filters.RawFileFilter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "export", description = "Export file(s) from a disk image.",
        aliases = "x",
        parameterListHeading = "%nParameters:%n",
        descriptionHeading = "%n",
        optionListHeading = "%nOptions:%n")
public class ExportCommand implements Callable<Integer> {
    private static Logger LOG = Logger.getLogger(ExportCommand.class.getName());

    @Spec
    private CommandSpec spec;
    
    @Option(names = { "-h", "--help" }, description = "Show help for subcommand.", usageHelp = true)
    private boolean helpFlag;
    
// FIXME vvv Should be in @ArgGroup subclass; waiting on Picocli 4.0.5 to fix bug (?!)
//    @ArgGroup(exclusive = true, heading = "File extract methods:")
//    private FileExtractMethods extraction = new FileExtractMethods();
    private Function<FileEntry,FileFilter> extractFunction = this::asSuggestedFile; 

    @Option(names = { "--raw", "--binary" }, description = "Extract file in native format.")
    public void setBinaryExtraction(boolean flag) {
        this.extractFunction = this::asRawFile;
        LOG.fine("Toggling binary output.");
    }
    @Option(names = { "--hex", "--dump" }, description = "Extract file in hex dump format.")
    public void setHexDumpExtraction(boolean flag) {
        this.extractFunction = this::asHexDumpFile;
        LOG.fine("Toggling hex dump.");
    }
    @Option(names = { "--suggested" }, description = "Extract file as suggested by AppleCommander (default)")
    public void setSuggestedExtraction(boolean flag) {
        this.extractFunction = this::asSuggestedFile;
        LOG.fine("Toggling file filter extraction.");
    }
    
    public FileFilter asRawFile(FileEntry entry) {
        return new RawFileFilter();
    }
    public FileFilter asSuggestedFile(FileEntry entry) {
        FileFilter ff = entry.getSuggestedFilter();
        if (ff instanceof BinaryFileFilter) {
            ff = new HexDumpFileFilter();
        }
        return ff;
    }
    public FileFilter asHexDumpFile(FileEntry entry) {
        return new HexDumpFileFilter();
    }
// FIXME ^^^ Should be in subclass; waiting on Picocli 4.0.5 to fix bug (?!)
    
    @Option(names = { "--deleted" }, description = "Include deleted files (use at your own risk!)")
    private boolean deletedFlag;
    
    @Option(names = { "-o", "--output" }, description = "Extract to file or to directory (default is stdout).")
    private File outputFile;
    
    @Parameters(index = "0", description = "Image to process.")
    private File image;
    
    @Parameters(index = "1", arity = "*", description = "File glob(s) to extract (default = '*') - be cautious of quoting!")
    private List<String> globs = Arrays.asList("*");

    public void validate() {
        List<String> errors = new ArrayList<>();
        // multiple files require --output
        if (isMultipleFiles()) {
            if (outputFile == null) {
                errors.add("--output directory must be specified with multiple files");
            } else if (!outputFile.isDirectory()) {
                errors.add("--output must be a directory");
            }
        }
        if (!errors.isEmpty()) {
            throw new ParameterException(spec.commandLine(), String.join(", ", errors));
        }
    }

    @Override
    public Integer call() throws Exception {
        validate();
        
        Consumer<FileTuple> fileHandler = 
                (outputFile == null) ? this::writeToStdout : this::writeToOutput;
        
        FileStreamer.forDisk(image)
                    .ignoreErrors(true)
                    .includeDeleted(deletedFlag)
                    .includeTypeOfFile(TypeOfFile.FILE)
                    .matchGlobs(globs)
                    .stream()
                    .forEach(fileHandler);
                    
        return 0;
    }
    
    public boolean hasFiles() {
        return globs != null && globs.size() > 1;
    }
    public boolean isAllFiles() {
        return globs == null || globs.isEmpty();
    }
    public boolean isMultipleFiles() {
        return hasFiles() || isAllFiles();
    }

    public void writeToStdout(FileTuple tuple) {
        try {
            FileFilter ff = extractFunction.apply(tuple.fileEntry);
            System.out.write(ff.filter(tuple.fileEntry));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    public void writeToOutput(FileTuple tuple) {
        File file = outputFile;
        FileFilter ff = extractFunction.apply(tuple.fileEntry);
        if (file.isDirectory()) {
            if (!tuple.paths.isEmpty()) {
                file = new File(outputFile, String.join(File.pathSeparator, tuple.paths));
                boolean created = file.mkdirs();
                if (created) LOG.info(String.format("Directory created: %s", file.getPath()));
            }
            file = new File(file, ff.getSuggestedFileName(tuple.fileEntry));
        }
        LOG.info(String.format("Writing to '%s'", file.getPath()));
        try (OutputStream out = new FileOutputStream(file)) {
            out.write(ff.filter(tuple.fileEntry));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

//    private static class FileExtractMethods {
//        private Function<FileEntry,FileFilter> extractFunction = this::asSuggestedFile; 
//
//        @Option(names = { "--raw", "--binary" }, description = "Extract file in native format.")
//        public void setBinaryExtraction(boolean flag) {
//            this.extractFunction = this::asRawFile;
//        }
//        @Option(names = { "--hex", "--dump" }, description = "Extract file in hex dump format.")
//        public void setHexDumpExtraction(boolean flag) {
//            this.extractFunction = this::asHexDumpFile;
//        }
//        @Option(names = { "--suggested" }, description = "Extract file as suggested by AppleCommander (default)")
//        public void setSuggestedExtraction(boolean flag) {
//            this.extractFunction = this::asSuggestedFile;
//        }
//        
//        public FileFilter asRawFile(FileEntry entry) {
//            return new RawFileFilter();
//        }
//        public FileFilter asSuggestedFile(FileEntry entry) {
//            FileFilter ff = entry.getSuggestedFilter();
//            if (ff instanceof BinaryFileFilter) {
//                ff = new HexDumpFileFilter();
//            }
//            return ff;
//        }
//        public FileFilter asHexDumpFile(FileEntry entry) {
//            return new HexDumpFileFilter();
//        }
//    }
}
