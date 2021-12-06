package io.github.applecommander.acx;

import java.util.logging.Logger;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.os.dos33.DosFormatDisk;
import com.webcodepro.applecommander.storage.os.dos33.OzDosFormatDisk;
import com.webcodepro.applecommander.storage.os.dos33.UniDosFormatDisk;
import com.webcodepro.applecommander.storage.os.pascal.PascalFormatDisk;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFormatDisk;
import com.webcodepro.applecommander.storage.physical.ImageOrder;

import io.github.applecommander.acx.converter.DataSizeConverter;
import io.github.applecommander.acx.converter.SystemTypeConverter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "create", description = "Rename volume of a disk image.",
		 aliases = { "mkdisk" },
         parameterListHeading = "%nParameters:%n",
         descriptionHeading = "%n",
         optionListHeading = "%nOptions:%n")
public class CreateDiskCommand extends ModifyingCommand {
    private static Logger LOG = Logger.getLogger(CreateDiskCommand.class.getName());
    
    @Option(names = { "-t", "--type" }, required = true, converter = SystemTypeConverter.class, description = "Select system type (DOS, ProDOS, Pascal.")
    private SystemType type;
    
    @Option(names = { "-s", "--size" }, defaultValue = "140kb", converter = DataSizeConverter.class, description = "Select disk size (140K, 800K, 10M).")
    private int size;
    
    @Option(names = { "-f", "--format" }, description = "Disk to copy system files from.")
    private String formatSource;
    
    @Option(names = { "-n", "--name" }, defaultValue = "NEW.DISK", description = "Disk Volume name (ProDOS/Pascal).")
    private String diskName;

    @Override
    public Integer call() throws Exception {
    	LOG.info(() -> String.format("Creating %s image of type %s.", DataSizeConverter.format(size), type));

    	ImageOrder order = type.createImageOrder(size);
    	FormattedDisk[] disks = null;
    	switch (type) {
    	case DOS:		
    		disks = DosFormatDisk.create(image.getName(), order);
    		break;
    	case OZDOS:		
    		disks = OzDosFormatDisk.create(image.getName(), order);
    		break;
    	case UNIDOS:
    		disks = UniDosFormatDisk.create(image.getName(), order);
    		break;
    	case PRODOS:
    		disks = ProdosFormatDisk.create(image.getName(), diskName, order);
    		break;
    	case PASCAL:
    		disks = PascalFormatDisk.create(image.getName(), diskName, order);
    		break;
    	}
    	
    	if (diskName != null) {
    		Disk systemSource = new Disk(formatSource);
    		type.copySystem(disks[0], systemSource.getFormattedDisks()[0]);
    	}
    	
    	saveDisk(disks[0]);
    	
        return 0;
    }
}
