package io.github.applecommander.acx;

import java.util.logging.Logger;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.os.pascal.PascalFormatDisk;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFormatDisk;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "rename-disk", description = "Rename volume of a disk image.",
        parameterListHeading = "%nParameters:%n",
        descriptionHeading = "%n",
        optionListHeading = "%nOptions:%n")
public class RenameDiskCommand extends ModifyingCommand {
    private static Logger LOG = Logger.getLogger(RenameDiskCommand.class.getName());
    
    @Parameters(index = "1", description = "Disk name.")
    private String diskName;

    @Override
    public Integer call() throws Exception {
    	Disk disk = new Disk(image.getPath());
		FormattedDisk[] formattedDisks = disk.getFormattedDisks();
		FormattedDisk formattedDisk = formattedDisks[0];
    	if (formattedDisk instanceof ProdosFormatDisk || formattedDisk instanceof PascalFormatDisk) {
			formattedDisk.setDiskName(diskName);
			saveDisk(formattedDisk);
	    	return 0;
    	} else {
    		LOG.warning("Disk must be ProDOS or Pascal.");
    		return 1;
    	}
    }
}
