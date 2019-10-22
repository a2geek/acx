package io.github.applecommander.filestreamer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DiskException;
import com.webcodepro.applecommander.storage.DiskUnrecognizedException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FormattedDisk;

/**
 * FileStreamer is utility class that will (optionally) recurse through all directories and
 * feed a Java Stream of useful directory walking detail (disk, directory, file, and the 
 * textual path to get there).
 * <p>
 * Sample usage:
 * <pre>
 * FileStreamer.forDisk(image)
 *             .ignoreErrors(true)
 *             .stream()
 *             .filter(this::fileFilter)
 *             .forEach(fileHandler);
 * </pre>
 * 
 * @author rob
 */
public class FileStreamer {
    public static FileStreamer forDisk(File file) throws IOException, DiskUnrecognizedException {
        return forDisk(file.getPath());
    }
    public static FileStreamer forDisk(String fileName) throws IOException, DiskUnrecognizedException {
        return new FileStreamer(new Disk(fileName));
    }
    public static FileStreamer forDisk(Disk disk) throws DiskUnrecognizedException {
        return new FileStreamer(disk);
    }
    
    private FormattedDisk[] formattedDisks = null;
    private boolean ignoreErrorsFlag = false;
    private boolean recursiveFlag = true;
    
    private FileStreamer(Disk disk) throws DiskUnrecognizedException {
        this.formattedDisks = disk.getFormattedDisks();
    }
    
    public FileStreamer ignoreErrors(boolean flag) {
        this.ignoreErrorsFlag = flag;
        return this;
    }
    public FileStreamer recursive(boolean flag) {
        this.recursiveFlag = flag;
        return this;
    }
    
    public Stream<FileTuple> stream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator(), 0), false);
    }
    public Iterator<FileTuple> iterator() {
        return new FileTupleIterator();
    }
    
    private class FileTupleIterator implements Iterator<FileTuple> {
        private LinkedList<FileTuple> files = new LinkedList<>();
        
        private FileTupleIterator() {
            for (FormattedDisk formattedDisk : formattedDisks) {
                files.addAll(toTupleList(FileTuple.of(formattedDisk)));
            }
        }

        @Override
        public boolean hasNext() {
            return !files.isEmpty();
        }

        @Override
        public FileTuple next() {
            if (hasNext()) {
                FileTuple tuple = files.removeFirst();
                if (recursiveFlag && tuple.fileEntry.isDirectory()) {
                    FileTuple newTuple = tuple.pushd(tuple.fileEntry);
                    files.addAll(0, toTupleList(newTuple));
                }
                return tuple;
            } else {
                throw new NoSuchElementException();
            }
        }
        
        private List<FileTuple> toTupleList(FileTuple tuple) {
            List<FileTuple> list = new ArrayList<>();
            try {
                for (FileEntry fileEntry : tuple.directoryEntry.getFiles()) {
                    list.add(tuple.of(fileEntry));
                }
            } catch (DiskException e) {
                if (!ignoreErrorsFlag) {
                    throw new RuntimeException(e);
                }
            }
            return list;
        }
    }
}
