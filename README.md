# `ac`, extended

This is a revamp of the venerable `ac` command-line utility with a more modern command-line interface.

# Usage

```
$ acx
Usage: acx [-hV] [--debug] [COMMAND]

'ac' experimental utility

Options:
      --debug     Show detailed stack traces.
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.

Commands:
  help       Displays help information about the specified command
  info, i    Show information on a disk image(s).
  list, ls   List directory of disk image(s).
  export, x  Export file(s) from a disk image.
```

## Info

```
$ acx info --help
Usage: acx info [-h] <paths>...

Show information on a disk image(s).

Parameters:
      <paths>...   Image(s) to process.

Options:
  -h, --help       Show help for subcommand.
```

```
$ acx info "Beagle Graphics.dsk" 
File Name: Beagle Graphics.dsk
Disk Name: DISK VOLUME #254
Physical Size (bytes): 143360
Free Space (bytes): 20480
Used Space (bytes): 122880
Physical Size (KB): 140
Free Space (KB): 20
Used Space (KB): 120
Archive Order: DOS
Disk Format: DOS 3.3
Total Sectors: 560
Free Sectors: 80
Used Sectors: 480
Tracks On Disk: 35
Sectors On Disk: 16
```

## List

```
$ acx list --help
Usage: acx list [[-n] | [-s] | [-l]] [-hr] [--deleted] <paths>...

List directory of disk image(s).

Parameters:
      <paths>...            Image(s) to process.

Options:
      --deleted             Show deleted files.
  -h, --help                Show help for subcommand.
  -r, --[no-]recursive      Display directory recursively.
File display formatting:
  -l, --long, --detail      Use long/detailed directory format.
  -n, --native              Use native directory format (default).
  -s, --short, --standard   Use brief directory format.
```

```
$ acx list --no-recursive ~/Downloads/3476_dimg.bin 
/home/rob/Downloads/3476_dimg.bin /SYSTEM.DISK/
  PRODOS          SYS      004 08/18/1988 08/18/1988      1,452 A=$2000  
  SYSTEM          DIR      001 08/10/0101 08/18/1988        512          
  FINDER.DEF      FND      001 07/07/1988 08/04/1987         82          
  ICONS           DIR      001 07/21/1988 07/21/1988        512          
  FINDER.DATA     FND      001 07/11/2001 06/14/1988        216          
  APPLETALK       DIR      001 07/10/2001 08/18/1988        512          
  BASIC.SYSTEM    SYS      021 12/14/1987 12/14/1987     10,240          
  BASIC.LAUNCHER  SYS      003 07/12/1987 07/12/1987        915 A=$0800  
ProDOS format; 365568 bytes free; 453632 bytes used.
```
