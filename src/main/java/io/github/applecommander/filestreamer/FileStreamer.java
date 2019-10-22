package io.github.applecommander.filestreamer;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DiskException;
import com.webcodepro.applecommander.storage.DiskUnrecognizedException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FormattedDisk;

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
        private Queue<FormattedDisk> disks = new LinkedList<>();
        private Queue<FileTuple> directories = new LinkedList<>();
        private Queue<FileTuple> files = new LinkedList<>();
        
        private FileTupleIterator() {
            for (FormattedDisk formattedDisk : formattedDisks) disks.add(formattedDisk);
        }

        @Override
        public boolean hasNext() {
            if (directories.isEmpty() && !disks.isEmpty()) {
                FormattedDisk formattedDisk = disks.poll();
                directories.add(FileTuple.of(formattedDisk));
            }
            if (files.isEmpty() && !directories.isEmpty()) {
                FileTuple tuple = directories.poll();
                try {
                    for (FileEntry fileEntry : tuple.directoryEntry.getFiles()) {
                        if (fileEntry.isDirectory()) {
                            if (recursiveFlag || fileEntry instanceof FormattedDisk) {
                                directories.add(tuple.pushd(fileEntry));
                            }
                        }
                        files.add(tuple.of(fileEntry));
                    }
                } catch (DiskException e) {
                    if (!ignoreErrorsFlag) {
                        throw new RuntimeException(e);
                    }
                }
            }
            return !files.isEmpty();
        }

        @Override
        public FileTuple next() {
            if (hasNext()) {
                return files.poll();
            } else {
                throw new NoSuchElementException();
            }
        }
    }
}
