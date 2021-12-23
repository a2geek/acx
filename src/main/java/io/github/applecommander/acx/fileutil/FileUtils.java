package io.github.applecommander.acx.fileutil;

import java.util.Optional;
import java.util.logging.Logger;

import com.webcodepro.applecommander.storage.DirectoryEntry;
import com.webcodepro.applecommander.storage.DiskException;
import com.webcodepro.applecommander.storage.FileEntry;

import io.github.applecommander.acx.command.CopyFileCommand;

public class FileUtils {
    private static Logger LOG = Logger.getLogger(CopyFileCommand.class.getName());

    public static void copy(DirectoryEntry directory, FileEntry file) throws DiskException {
        LOG.fine(() -> String.format("Copying '%s'", file.getFilename()));
		if (file.isDeleted()) {
			// Skip deleted files
		}
		else if (file.isDirectory()) {
			copyDirectory(directory, (DirectoryEntry)file, file.getFilename());
		} 
		else {
			copyFile(directory, file);
		}
	}
	
	static void copyDirectory(DirectoryEntry targetParent, DirectoryEntry sourceDir, String name) throws DiskException {
	    Optional<FileEntry> targetFile = targetParent.getFiles()
	            .stream()
	            .filter(fileEntry -> name.equals(fileEntry.getFilename()))
	            .findFirst();
	    Optional<DirectoryEntry> targetDir = targetFile
	            .filter(FileEntry::isDirectory)
	            .map(DirectoryEntry.class::cast);

	    if (targetDir.isPresent()) {
	        // Fall through to general logic
	    }
	    else if (targetFile.isPresent()) {
	        // This is an abstract class, so faking it for now.
	        throw new DiskException("Unable to create directory", name) {
                private static final long serialVersionUID = 4726414295404986677L;
	        };
	    }
	    else {
	        targetDir = Optional.of(targetParent.createDirectory(name));
	    }
	    
        for (FileEntry fileEntry : sourceDir.getFiles()) {
            copy(targetDir.get(), fileEntry);
        }
	}
	
	static void copyFile(DirectoryEntry directory, FileEntry sourceFile) throws DiskException {
	    FileEntry targetFile = directory.createFile();
	    FileEntryWriter target = FileEntryWriter.get(targetFile);
	    FileEntryReader source = FileEntryReader.get(sourceFile);
	    
	    source.getFilename().ifPresent(target::setFilename);
	    source.getProdosFiletype().ifPresent(target::setProdosFiletype);
	    source.isLocked().ifPresent(target::setLocked);
	    source.getFileData().ifPresent(target::setFileData);
	    source.getBinaryAddress().ifPresent(target::setBinaryAddress);
	    source.getBinaryLength().ifPresent(target::setBinaryLength);
	    source.getAuxiliaryType().ifPresent(target::setAuxiliaryType);
	    source.getCreationDate().ifPresent(target::setCreationDate);
	    source.getLastModificationDate().ifPresent(target::setLastModificationDate);
	}
}
