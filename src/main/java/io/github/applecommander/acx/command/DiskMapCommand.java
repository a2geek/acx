package io.github.applecommander.acx.command;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.function.Function;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.FormattedDisk.DiskUsage;

import io.github.applecommander.acx.converter.DiskConverter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "diskmap", description = "Show disk usage map.",
         aliases = { "map" },
         parameterListHeading = "%nParameters:%n",
         descriptionHeading = "%n",
         optionListHeading = "%nOptions:%n")
public class DiskMapCommand implements Callable<Integer> {
    @Option(names = { "-h", "--help" }, description = "Show help for subcommand.", usageHelp = true)
    private boolean helpFlag;

    @Option(names = { "-d", "--disk" }, description = "Image to process.", required = true,
            converter = DiskConverter.class, defaultValue = "${ACX_DISK_NAME}")
    private Disk disk;
    
    @Override
    public Integer call() throws Exception {
        Arrays.asList(disk.getFormattedDisks()).forEach(this::showDiskMap);
        return 0;
    }
    
    public void showDiskMap(FormattedDisk formattedDisk) {
        final int[] dimensions = formattedDisk.getBitmapDimensions();
        final int length = formattedDisk.getBitmapLength();
        final int width,height;
        final Function<Integer,Integer> leftNumFn, rightNumFn; 
        if (dimensions != null && dimensions.length == 2) {
            width = dimensions[0];
            height = dimensions[1];
            // This is expected to be Track, so same number on left and right.
            leftNumFn = rightNumFn = i -> i;
        } else {
            width = 70;
            height= (length + width - 1) / width;
            // This is expected to be blocks, so show start of range of 
            // left and end of range on right.
            leftNumFn = i -> i * width;
            rightNumFn = i -> (i + 1) * width - 1;
        }
        
        header1(width); // 10's position
        header2(width); // 1's position
        header3(width); // divider
        
        DiskUsage diskUsage = formattedDisk.getDiskUsage();
        for (int y=0; y<height; y++) {
            System.out.printf("%5d|", leftNumFn.apply(y));
            for (int x=0; x<width; x++) {
                if (diskUsage.hasNext()) {
                    diskUsage.next();
                    System.out.print(diskUsage.isUsed() ? '*' : '.');
                } else {
                    System.out.print(" ");
                }
            }
            System.out.printf("|%d", rightNumFn.apply(y));
            System.out.println();
        }
        
        header3(width);
        header2(width);
        header1(width);
    }
    
    void header1(final int width) {
        System.out.print("     ");
        for (int i=0; i<width; i++) {
            System.out.print(i % 10 == 0 ? Character.forDigit(i%10, 10) : ' ');
        }
        System.out.println();
    }
    void header2(final int width) {
        System.out.print("     ");
        for (int i=0; i<width; i++) {
            System.out.print(i%10);
        }
        System.out.println();
    }
    void header3(final int width) {
        System.out.print("     ");
        for (int i=0; i<width; i++) {
            System.out.print(i%5 == 0 ? '+' : '-');
        }
        System.out.println();
    }
}
