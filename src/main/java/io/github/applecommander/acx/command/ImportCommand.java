package io.github.applecommander.acx.command;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.logging.Logger;

import com.webcodepro.applecommander.storage.DirectoryEntry;
import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DiskUnrecognizedException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFormatDisk;
import com.webcodepro.applecommander.util.AppleUtil;
import com.webcodepro.applecommander.util.StreamUtil;
import com.webcodepro.applecommander.util.TranslatorStream;
import com.webcodepro.shrinkit.HeaderBlock;
import com.webcodepro.shrinkit.NuFileArchive;
import com.webcodepro.shrinkit.ThreadRecord;

import io.github.applecommander.acx.converter.DiskConverter;
import io.github.applecommander.acx.converter.IntegerTypeConverter;
import io.github.applecommander.acx.fileutil.FileEntryReader;
import io.github.applecommander.acx.fileutil.FileUtils;
import io.github.applecommander.acx.fileutil.OverrideFileEntryReader;
import io.github.applecommander.applesingle.AppleSingle;
import io.github.applecommander.applesingle.FileDatesInfo;
import io.github.applecommander.applesingle.ProdosFileInfo;
import io.github.applecommander.bastools.api.Configuration;
import io.github.applecommander.bastools.api.Parser;
import io.github.applecommander.bastools.api.TokenReader;
import io.github.applecommander.bastools.api.Visitors;
import io.github.applecommander.bastools.api.model.Program;
import io.github.applecommander.bastools.api.model.Token;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "import", description = "Import file onto disk.",
         aliases = { "put" },
         parameterListHeading = "%nParameters:%n",
         descriptionHeading = "%n",
         optionListHeading = "%nOptions:%n")
public class ImportCommand implements Callable<Integer> {
    private static Logger LOG = Logger.getLogger(ImportCommand.class.getName());

    @Option(names = { "-h", "--help" }, description = "Show help for subcommand.", usageHelp = true)
    private boolean helpFlag;

    @Option(names = { "-d", "--disk" }, description = "Image to process.", required = true,
            converter = DiskConverter.class, defaultValue = "${ACX_DISK_NAME}")
    private Disk disk;
    
    @ArgGroup(heading = "%nInput source:%n", multiplicity = "1")
    private InputData inputData;

    @ArgGroup(heading = "%nProcessing options:%n")
    private Processor processor;
    
    @ArgGroup(heading = "%nGeneral overrides:%n", exclusive = false)
    private Overrides overrides = new Overrides();
    
    @Option(names = { "--dir" }, description = "Write file(s) to directory.")
    private Optional<String> directoryName;
    
    @Option(names = { "-f", "--force" }, description = "Over-write existing files.")
    private boolean overwriteFlag;

    @Override
    public Integer call() throws Exception {
        DirectoryEntry directory = disk.getFormattedDisks()[0];
        if (directoryName.isPresent()) {
            String[] dirs = directoryName.get().split("/");
            for (String dir : dirs) {
                Optional<FileEntry> fileEntry = directory.getFiles().stream()
                        .filter(f -> dir.equalsIgnoreCase(f.getFilename()))
                        .findFirst();
                Optional<DirectoryEntry> dirEntry = fileEntry
                        .filter(FileEntry::isDirectory)
                        .map(DirectoryEntry.class::cast);
                directory = dirEntry.orElseThrow(() -> 
                    new IOException(String.format("Directory '%s' not found.", dir)));
            }
        }
        
        FileUtils copier = new FileUtils(overwriteFlag);
        FileEntryReader inputReader = inputData.get();
        for (FileEntryReader processorReader : processor.apply(inputReader)) {
            FileEntryReader reader = OverrideFileEntryReader.builder()
                    .filename(overrides.fileName)
                    .prodosFiletype(overrides.fileType)
                    .binaryAddress(overrides.fileAddress)
                    .build(processorReader);
            
            copier.copyFile(directory, reader);
        }
        
        saveDisk(disk);
        
        return 0;
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

    public static class InputData {
        private FileEntryReader fileEntryReader;
        
        public FileEntryReader get() {
            return fileEntryReader;
        }

        @Option(names = { "--stdin" }, description = "Import from standard input.")
        public void stdinFlag(boolean flag) {
            try {
                byte[] data = System.in.readAllBytes();
                fileEntryReader = OverrideFileEntryReader.builder()
                        .fileData(data)
                        .filename("UNKNOWN")
                        .prodosFiletype("BIN")
                        .build();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        
        @Parameters(description = "File to import.")
        public void fromFile(final String filename) {
            try {
                Path path = Path.of(filename);
                byte[] data = Files.readAllBytes(path);
                fileEntryReader = OverrideFileEntryReader.builder()
                        .fileData(data)
                        .filename(filename)
                        .prodosFiletype("BIN")
                        .build();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
    
    public static class Processor {
        private Function<FileEntryReader,List<FileEntryReader>> fileEntryReaderFn;
        
        public List<FileEntryReader> apply(FileEntryReader reader) {
            return fileEntryReaderFn.apply(reader);
        }
        
        @Option(names = { "--text", "--text-high" }, description = { 
                "Import as a text file, setting high bit and ",
                "replacing newline characters with $8D." })
        public void setTextModeSetHighBit(boolean textFlag) {
            fileEntryReaderFn = this::handleTextModeSetHighBit;
        }

        @Option(names = { "--text-low" }, description = { 
                "Import as a text file, clearing high bit and ",
                "replacing newline characters with $8D." })
        public void setTextModeClearHighBit(boolean textFlag) {
            fileEntryReaderFn = this::handleTextModeClearHighBit;
        }
        
        @Option(names = { "--dos" }, description = "Use standard 4-byte DOS header.")
        public void setDosMode(boolean dosFlag) {
            fileEntryReaderFn = this::handleDosMode;
        }
        
        @Option(names = { "--geos" }, description = "Interpret as a GEOS conversion file.")
        public void setGeosMode(boolean geosFlag) {
            fileEntryReaderFn = this::handleGeosMode;
        }
        
        @Option(names = { "--basic" }, description = "Tokenize an AppleSoft BASIC program.")
        public void setApplesoftTokenizerMode(boolean tokenizeMode) {
            fileEntryReaderFn = this::handleApplesoftTokenizeMode;
        }
        
        @Option(names = { "--as", "--applesingle" }, description = "Import Apple Single file.")
        public void setAppleSingleMode(boolean applesingleMode) {
            fileEntryReaderFn = this::handleAppleSingleMode;
        }
 
        @Option(names = { "--shk", "--nufx", "--shrinkit", "--bxy" }, 
                description = "Import files from SHK archive.")
        public void setShrinkitMode(boolean shrinkitMode) {
            fileEntryReaderFn = this::handleShrinkitMode;
        }

        private List<FileEntryReader> handleTextModeSetHighBit(FileEntryReader reader) {
            InputStream inputStream = new ByteArrayInputStream(reader.getFileData().get());
            return handleTextMode(TranslatorStream.builder(inputStream)
                    .lfToCr().setHighBit().get(), reader);
        }
        private List<FileEntryReader> handleTextModeClearHighBit(FileEntryReader reader) {
            InputStream inputStream = new ByteArrayInputStream(reader.getFileData().get());
            return handleTextMode(TranslatorStream.builder(inputStream)
                    .lfToCr().clearHighBit().get(), reader);
        }
        private List<FileEntryReader> handleTextMode(InputStream inputStream, FileEntryReader reader) {
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                StreamUtil.copy(inputStream, outputStream);
                final byte[] translatedData = outputStream.toByteArray();
                return OverrideFileEntryReader.builder()
                        .fileData(translatedData)
                        .prodosFiletype("TXT")
                        .buildList(reader);
            } catch (IOException cause) {
                throw new UncheckedIOException(cause);
            }
        }

        private List<FileEntryReader> handleDosMode(FileEntryReader reader) {
            try {
                ByteArrayInputStream inputStream = new ByteArrayInputStream(reader.getFileData().get());
                byte[] header = new byte[4];
                if (inputStream.read(header) != 4) {
                    throw new IOException("Unable to read DOS header.");
                }
                int address = AppleUtil.getWordValue(header, 0);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                inputStream.transferTo(outputStream);
                return OverrideFileEntryReader.builder()
                        .fileData(outputStream.toByteArray())
                        .binaryAddress(address)
                        .buildList(reader);
            } catch (IOException cause) {
                throw new UncheckedIOException(cause);
            }
        }

        private List<FileEntryReader> handleGeosMode(FileEntryReader reader) {
            return OverrideFileEntryReader.builder()
                    .prodosFiletype("GEO")
                    .binaryAddress(0)
                    .buildList(reader);
        }
        
        private List<FileEntryReader> handleApplesoftTokenizeMode(FileEntryReader reader) {
            try {
                File fakeTempSource = File.createTempFile("ac-", "bas");
                fakeTempSource.deleteOnExit();
                Configuration config = Configuration.builder().sourceFile(fakeTempSource).build();
                Queue<Token> tokens = TokenReader.tokenize(new ByteArrayInputStream(reader.getFileData().get()));
                Parser parser = new Parser(tokens);
                Program program = parser.parse();
                byte[] tokenData = Visitors.byteVisitor(config).dump(program);
                return OverrideFileEntryReader.builder()
                        .fileData(tokenData)
                        .prodosFiletype("BAS")
                        .binaryAddress(config.startAddress)
                        .buildList(reader);
            } catch (IOException cause) {
                throw new UncheckedIOException(cause);
            }
        }
        
        private List<FileEntryReader> handleAppleSingleMode(FileEntryReader reader) {
            try {
                AppleSingle as = AppleSingle.read(reader.getFileData().get());
                if (as.getProdosFileInfo() == null) {
                    throw new IOException("This AppleSingle does not contain a ProDOS file.");
                }
                if (as.getDataFork() == null || as.getDataFork().length == 0) {
                    throw new IOException("This AppleSingle does not contain a data fork.");
                }
                ProdosFileInfo info = as.getProdosFileInfo();
                String fileType = ProdosFormatDisk.getFiletype(info.getFileType());
                
                OverrideFileEntryReader.Builder builder = OverrideFileEntryReader.builder();
                builder.filename(Optional.ofNullable(as.getRealName()));
                builder.fileData(as.getDataFork());
                builder.resourceData(Optional.ofNullable(as.getResourceFork()));
                builder.prodosFiletype(fileType);
                builder.locked(info.getAccess() == 0xc3);
                builder.auxiliaryType(info.getAuxType());
                
                if (as.getFileDatesInfo() != null) {
                    FileDatesInfo dates = as.getFileDatesInfo();
                    builder.creationDate(Date.from(dates.getCreationInstant()));
                    builder.lastModificationDate(Date.from(dates.getModificationInstant()));
                }
                
                return builder.buildList(reader);
            } catch (IOException cause) {
                throw new UncheckedIOException(cause);
            }
        }
        
        private List<FileEntryReader> handleShrinkitMode(FileEntryReader reader) {
            try {
                List<FileEntryReader> files = new ArrayList<>();
                ByteArrayInputStream inputStream = new ByteArrayInputStream(reader.getFileData().get());
                NuFileArchive nufx = new NuFileArchive(inputStream);
                for (HeaderBlock header : nufx.getHeaderBlocks()) {
                    OverrideFileEntryReader.Builder builder = OverrideFileEntryReader.builder()
                            .filename(header.getFilename())
                            .prodosFiletype(ProdosFormatDisk.getFiletype((int)header.getFileType()))
                            .auxiliaryType((int)header.getExtraType())
                            .creationDate(header.getCreateWhen())
                            .lastModificationDate(header.getModWhen());
                    
                    ThreadRecord dataFork = header.getDataForkThreadRecord();
                    ThreadRecord resourceFork = header.getResourceForkThreadRecord();
                    if (dataFork == null) {
                        LOG.info(() -> String.format("No data fork for '%s', skipping it.", 
                                header.getFilename()));
                        continue;
                    }
                    
                    builder.fileData(dataFork.getBytes());
                    if (resourceFork != null) {
                        builder.resourceData(resourceFork.getBytes());
                    }
                    files.add(builder.build());
                }
                return files;
            } catch (IOException cause) {
                throw new UncheckedIOException(cause);
            }
        }
    }
    
    public static class Overrides {
        @Option(names = { "-t", "--type" }, description = "File type.")
        private Optional<String> fileType;
        
        @Option(names = { "-a", "--addr" }, description = "File address.", 
                converter = IntegerTypeConverter.class)
        private Optional<Integer> fileAddress;
        
        @Option(names = { "-n", "--name" }, description = "File name.")
        private Optional<String> fileName;
    }
}
