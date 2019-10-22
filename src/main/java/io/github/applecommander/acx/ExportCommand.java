package io.github.applecommander.acx;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.storage.filters.BinaryFileFilter;
import com.webcodepro.applecommander.storage.filters.HexDumpFileFilter;

import io.github.applecommander.filestreamer.FileStreamer;
import io.github.applecommander.filestreamer.FileTuple;
import io.github.applecommander.filters.RawFileFilter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

@Command(name = "export", description = "Export file(s) from a disk image.",
        aliases = "x",
        parameterListHeading = "%nParameters:%n",
        descriptionHeading = "%n",
        optionListHeading = "%nOptions:%n")
public class ExportCommand implements Callable<Integer> {
    @ParentCommand
    private Main main;
    @Spec
    private CommandSpec spec;
    
    @Option(names = { "-h", "--help" }, description = "Show help for subcommand.", usageHelp = true)
    private boolean helpFlag;
    
// FIXME vvv Should be in subclass; waiting on Picocli 4.0.5 to fix bug (?!)
//    @ArgGroup(exclusive = true, heading = "File extract methods:")
//    private FileExtractMethods extraction = new FileExtractMethods();
    private Function<FileEntry,FileFilter> extractFunction = this::asSuggestedFile; 

    @Option(names = { "--raw", "--binary" }, description = "Extract file in native format.")
    public void setBinaryExtraction(boolean flag) {
        this.extractFunction = this::asRawFile;
    }
    @Option(names = { "--hex", "--dump" }, description = "Extract file in hex dump format.")
    public void setHexDumpExtraction(boolean flag) {
        this.extractFunction = this::asHexDumpFile;
    }
    @Option(names = { "--suggested" }, description = "Extract file as suggested by AppleCommander (default)")
    public void setSuggestedExtraction(boolean flag) {
        this.extractFunction = this::asSuggestedFile;
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
    
    @Option(names = { "--deleted" }, description = "Include deleted files (at your own risk!)")
    private boolean deletedFlag;
    
    @Option(names = { "-o", "--output" }, description = "Extract to file or to directory (default is stdout).")
    private File outputFile;
    
    @Parameters(index = "0", description = "Image to process.")
    private File image;
    
    @Parameters(index = "1", arity = "*", description = "File(s) to extract (default = '*').")
    private List<String> fileNames; // = Arrays.asList("*");

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
                    .stream()
                    .filter(this::fileFilter)
                    .forEach(fileHandler);
                    
        return 0;
    }
    
    public boolean hasFiles() {
        return fileNames != null && fileNames.size() > 1;
    }
    public boolean isAllFiles() {
        return fileNames == null || fileNames.isEmpty();
    }
    public boolean isMultipleFiles() {
        return hasFiles() || isAllFiles();
    }
    
    public boolean fileFilter(FileTuple tuple) {
        if (tuple.fileEntry.isDirectory()) {
            return false;
        }
        if (tuple.fileEntry.isDeleted() && !deletedFlag) {
            return false;
        }
        if (fileNames != null && !fileNames.isEmpty()) {
            return fileNames.stream().anyMatch(tuple.fileEntry.getFilename()::equalsIgnoreCase);
        }
        return true;
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
                if (created) main.logf("Directory created: %s\n", file.getPath());
            }
            file = new File(file, ff.getSuggestedFileName(tuple.fileEntry));
        }
        main.logf("Writing to '%s'\n", file.getPath());
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
