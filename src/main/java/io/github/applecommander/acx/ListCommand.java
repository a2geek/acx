package io.github.applecommander.acx;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import com.webcodepro.applecommander.storage.DirectoryEntry;
import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DiskException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.FormattedDisk.FileColumnHeader;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "list", description = "List directory of disk image(s).",
        aliases = { "ls" },
        parameterListHeading = "%nParameters:%n",
        descriptionHeading = "%n",
        optionListHeading = "%nOptions:%n")
public class ListCommand implements Callable<Integer> {
    @ParentCommand
    private Main main;
    
    @Option(names = { "-h", "--help" }, description = "Show help for subcommand.", usageHelp = true)
    private boolean helpFlag;

    @ArgGroup(exclusive = true, multiplicity = "0..1", heading = "File display formatting:%n")
    private FileDisplay fileDisplay = new FileDisplay();
    
    @Option(names = { "-r", "--recursive"}, description = "Display directory recursively.", negatable = true, defaultValue = "false")
    private boolean recursiveFlag;
    
    @Option(names = { "--deleted" }, description = "Show deleted files.")
    private boolean deletedFlag;

    @Parameters(arity = "1..*", description = "Image(s) to process.")
    private List<Path> paths = new ArrayList<Path>();

    @Override
    public Integer call() throws Exception {
        for (Path path : paths) {
            String filename = path.toString();
            try {
                showDisk(filename);
            } catch (DiskException e) {
                main.log(e);
                return 1;
            } catch (RuntimeException e) {
                main.logf("%s: %s\n", filename, e);
            }
        }
        return 0;
    }
    
    /**
     * Show all components for a Disk.
     */
    private void showDisk(String filename) throws IOException, DiskException {
        Disk disk = new Disk(filename);
        FormattedDisk[] formattedDisks = disk.getFormattedDisks();
        List<String> fmtSpec = null;
        for (int i = 0; i < formattedDisks.length; i++) {
            FormattedDisk formattedDisk = formattedDisks[i];
            
            List<FileColumnHeader> headers = formattedDisk.getFileColumnHeaders(fileDisplay.format());
            if (fmtSpec == null) {
                fmtSpec = createFormatSpec(headers);
            }

            System.out.printf("%s %s\n", filename, formattedDisk.getDiskName());
            List<FileEntry> files = formattedDisk.getFiles();
            if (files != null) {
                showFiles(files, "", fmtSpec);
            }
            System.out.printf("%s format; %d bytes free; %d bytes used.\n",
                formattedDisk.getFormat(),
                formattedDisk.getFreeSpace(),
                formattedDisk.getUsedSpace());
            System.out.println();
        }
    }
    
    /**
     * Recursive routine to display directory entries. In the instance of a
     * system with directories (e.g. ProDOS), this really returns the first file
     * with the given filename.
     */
    private void showFiles(List<FileEntry> files, String indent, List<String> fmtSpec) throws DiskException {
        for (FileEntry entry : files) {
            if (!deletedFlag && entry.isDeleted()) {
                continue;
            }
            
            List<String> data = entry.getFileColumnData(fileDisplay.format());
            System.out.print(indent);
            for (int d = 0; d < data.size(); d++) {
                System.out.printf(fmtSpec.get(d), data.get(d));
            }
            if (entry.isDeleted()) {
                System.out.print("[deleted]");
            }
            System.out.println();
            
            if (recursiveFlag && entry.isDirectory()) {
                showFiles(((DirectoryEntry) entry).getFiles(), indent + "  ", fmtSpec);
            }
        }
    }
    
    private List<String> createFormatSpec(List<FileColumnHeader> fileColumnHeaders) {
        List<String> fmtSpec = new ArrayList<>();
        for (FileColumnHeader h : fileColumnHeaders) {
            String spec = String.format("%%%s%ds ", h.isRightAlign() ? "" : "-", 
                    h.getMaximumWidth());
            fmtSpec.add(spec);
        }
        return fmtSpec;
    }
    
    public static class FileDisplay {
        public int format() {
            if (standardFormat) {
                return FormattedDisk.FILE_DISPLAY_STANDARD;
            }
            if (longFormat) {
                return FormattedDisk.FILE_DISPLAY_DETAIL;
            }
            return FormattedDisk.FILE_DISPLAY_NATIVE;
        }
        
        @Option(names = { "-n", "--native" }, description = "Use native directory format (default).")
        private boolean nativeFormat;

        @Option(names = { "-s", "--short", "--standard" }, description = "Use brief directory format.")
        private boolean standardFormat;
        
        @Option(names = { "-l", "--long",  "--detail" }, description = "Use long/detailed directory format.")
        private boolean longFormat;
    }
}
