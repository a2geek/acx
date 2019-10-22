package io.github.applecommander.filters;

import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;

public class RawFileFilter implements FileFilter {

    @Override
    public byte[] filter(FileEntry fileEntry) {
        return fileEntry.getFileData();
    }

    @Override
    public String getSuggestedFileName(FileEntry fileEntry) {
        return fileEntry.getFilename();
    }

}
