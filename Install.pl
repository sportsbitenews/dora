#!/bin/perl
#
# Installation Script for GitNsfFilters
#
# Copies the GNF script to the ~/bin directory
# Copies the XSL file to the ~/GitFilter/xsl directory
# TODO Use FTP to download latest libxslt
# TODO Unzip libxslt packages
# TODO MD5 checksum the libxslt packages after download

use strict;

package GitFiltersForNSF;

use File::Basename 'dirname';
use File::Copy 'copy';
use File::Spec;
use Term::ANSIColor;

our $projname   = "Git Filters For NSF";

our $useColours = 1;
our $verbose    = 0;

our $thisAbs = File::Spec->rel2abs(__FILE__);
our ($thisVol, $thisDir, $thisFile) = File::Spec->splitpath($thisAbs);

our $installScriptDir     = $thisDir;
$installScriptDir =~ s:/$::;

our $homeDir              = $ENV{"HOME"};

our $setupSourceFilename  = "Setup.pl";
our $setupSource          = "$installScriptDir/$setupSourceFilename";

our $setupTargetDir = "$homeDir/bin";
our $setupTargetFilename  = "dogh.pl";
our $setupTarget          = "$setupTargetDir/$setupTargetFilename";

our $binTargetDir					= $setupTargetDir;

our $xslSourceFilename    = "xsl/NonBinaryDXL.xsl";
our $xslSource            = "$installScriptDir/$xslSourceFilename";

our $xslTargetDir  = "$homeDir/GitFilters";
our $xslTargetFilename   = "NonBinaryDxl.xsl";
our $xslTarget           = "$xslTargetDir/$xslTargetFilename";

our $libxsltDir		= "$installScriptDir/libxslt";
our @libxsltBins  = (
	'iconv-1.9.2.win32/bin/iconv.dll', 
	'iconv-1.9.2.win32/bin/iconv.exe', 
	'libxml2-2.7.8.win32/bin/libxml2.dll', 
	'libxml2-2.7.8.win32/bin/xmlcatalog.exe', 
	'libxml2-2.7.8.win32/bin/xmllint.exe', 
	'libxslt-1.1.26.win32/bin/libexslt.dll', 
	'libxslt-1.1.26.win32/bin/libxslt.dll', 
	'libxslt-1.1.26.win32/bin/xsltproc.exe', 
	'zlib-1.2.5/bin/minigzip.exe',
	'zlib-1.2.5/bin/zlib1.dll'
);

# Variables used to check current configuration
our $chkHelper 	= 0;
our $chkXSL			= 0;
our $chkLibxslt = 0;

sub installEverything {

  installHelper();
  installXSL();
	installLibxslt();

}

sub uninstallEverything {

	uninstallHelper();
	uninstallXSL();
	uninstallLibxslt();

}


sub installHelper {


	heading("Install the Helper");

	print "This step will install the $setupTargetFilename script into $setupTargetDir\n";
	print "The helper script is used to help set up and configure git repositories for nsf use\n";

  if (-e $setupTarget) {
		colorSet("bold yellow");
    print "\nThe helper script is already installed, if you continue it will overwrite the existing copy.\n";
		colorReset();
  }

	return 0 if !confirmContinue();
	
  # Check if the Home Bin directory exists
  if (-d $setupTargetDir) {
		printFileResult($setupTargetDir,"directory already exists",0);
  } else {
    mkdir $setupTargetDir or die "Could not create Directory: $!\n";
		printFileResult($setupTargetDir,"directory created", 1);
  }

  # Copy the Setup script to the Target Directory
  use File::Copy;
  copy($setupSource, $setupTarget) or die "...Failed Copying: $!\n";
	printFileResult($setupTarget,"Installed",1);

}

sub uninstallHelper {

	heading("Uninstall the Helper");

	if (-e $setupTarget) {

		print "This step will remove the Helper script located at: \n";
		colorSet("bold white");
		print "\n$setupTarget\n";
		colorReset();

		return 0 if !confirmContinue();

		unlink $setupTarget or warn "Could not remove $setupTarget: $!\n";
		printFileResult($setupTarget, "removed", -1);

	} else {

		print "The helper script is not installed, no action taken\n";

	}

}

sub checkHelper {

	return (-e $setupTarget);

}


sub installXSL {

	heading("Install the XSL File");

	print "This step will install the XSL Stylesheet to:\n\n";
	colorSet("bold white");
	print "  $xslTarget\n\n";
	colorReset();
	print "The XSL file is used by the NSF Metadata filter\n";
	print "When you use the helper script to setup a repository for NSF Metadata filtering,,\n";
	print "the helper script will copy the above file to the ";
	colorSet("bold white");
	print "xsl/";
	colorReset();
	print " folder of the repository\n";

	if (checkXSL()) {
		colorSet("bold yellow");
		print "\nThe XSL File is already installed, if you continue it will be overwritten.\n";
		colorReset();
	}

	return 0 if !confirmContinue();

  # Check if the Home Bin directory exists
  if (-d $xslTargetDir) {
		printFileResult($xslTargetDir, "directory already exists", 0);
  } else {
    mkdir $xslTargetDir or die "Could not create directory$xslTargetDir: $!";
		printFileResult($xslTargetDir, "directory created", 1);
  }

  # Copy the xsl file to the Target Directory
  use File::Copy;

  copy($xslSource, $xslTarget) or die "Failed Copying: $!\n";
	printFileResult($xslTarget, "Installed", 1);

  print "\n";

}

sub uninstallXSL {

	heading("Uninstall the XSL File");

	if (-e $xslTarget) {

		print "This step will remove the xsl script located at:\n";
		colorSet("bold white");
		print "\n$xslTarget\n";
		colorReset();

		return 0 if !confirmContinue();

		unlink $xslTarget or warn "Could not remove $xslTarget: $!\n";
		printFileResult($xslTarget,"removed",-1);

	} else {

		print "The XSL file is not currently installed, no action taken\n";

	}

}

sub checkXSL {

	return (-e $xslTarget);

}

sub getLibxsltTarget {

	# get input parameter
	my ($srcFile) = @_;

	# get full path of Source Binary
	my $binSource = "$libxsltDir/$srcFile";

	# Determin the FileName of the binary
	my ($volume, $directories, $file) = File::Spec->splitpath($binSource);

	# Return the Target Source Path of the binary
	return "$binTargetDir/$file";

} 

sub installLibxslt {

	my $binSource = '';
	my $binTarget	= '';
	my $binsExist = 0;

	heading("Install libxslt win 32 binaries");

	print ("\nThis step will install the binaries required to run xsltproc\n\n");
	print ("xsltproc is the program used to filter the DXL using an xsl file\n");


	foreach (@libxsltBins) {

		$binTarget = getLibxsltTarget($_);

		if (-e $binTarget) {
	
			colorSet("bold yellow");
			if (!$binsExist) {
				print "\nThe Following binaries are already installed and will be overwritten if you continue:\n";
				$binsExist = 1;
			}
			print "$binTarget\n";
			colorReset();
	
		}	

	}

	return 0 if !confirmContinue();

	# Set up bin directory if not there
	if (-d $binTargetDir) {
		printFileResult($binTargetDir, "directory already exists", 0);
  } else {
    mkdir $binTargetDir or die "Could not create directory $binTargetDir: $!";
		printFileResult($binTargetDir, "directory created", 1);
  }

	# for each binary in the folder
	foreach (@libxsltBins)  {
	
		$binSource = "$libxsltDir/$_";

		my ($volume, $directories, $file) = File::Spec->splitpath( $binSource );

		$binTarget = "$binTargetDir/$file";

		use File::Copy;
		my $cpResult = copy($binSource, $binTarget) or warn "Failed copying\n$binSource to \n$binTarget: $!\n\n";

		printFileResult($binTarget,"copied successfully",1) if $cpResult;

	}	

}

sub uninstallLibxslt {

	my $binSource = '';
	my $binTarget	= '';
	my @binsExist = ();

	heading("Uninstall libxslt win 32 binaries");

	foreach(@libxsltBins) {
		$binTarget = getLibxsltTarget($_);
		push(@binsExist, $binTarget) if (-e $binTarget);
	}

	if (!@binsExist) {
		print "No Libxslt binaries are currently installed, no action taken\n";
		return 0;
	}

	print "This step will remove the following Libxslt binaries\n\n";

	colorSet("bold white");
	foreach(@binsExist) {
		print "$_\n";
	}
	colorReset();
	

	if (!confirmContinue()) {
		print "aborting un-installation libxslt binaries\n";
		return 0;
	}

	# for each binary in the folder
	foreach (@libxsltBins)  {
	
		$binTarget = getLibxsltTarget($_);

		if (-e $binTarget) {

			my $noDelete = unlink $binTarget or warn "Could not remove $binTarget: $!\n";

			if ($noDelete == 1) {
				printFileResult($binTarget,"removed",-1);
			}

		} else {

			printFileResult($binTarget,"not there anyway",0);

		}

	}	

}

sub checkLibxslt {

	foreach (@libxsltBins) {

		my $binTarget = getLibxsltTarget($_);

		if (!-e $binTarget) {
			return 0;
		}

	}

	return 1;

}

sub processArgs {

  my $numArgs = $#ARGV + 1;

  foreach my $argnum (0 .. $#ARGV) {

    if ($ARGV[$argnum] eq '--no-color') {
      $useColours = 0;
    }

    if ($ARGV[$argnum] eq '--remove') {
      uninstall();
      exit 0;
    }

    if ($ARGV[$argnum] eq '-v') {
      $verbose = 1;
    }

    print "$ARGV[$argnum]\n";
  }

}

sub checkSetup {

	$chkHelper	= checkHelper();
	$chkXSL 		= checkXSL();
	$chkLibxslt	= checkLibxslt();

}


sub main {

	processArgs();

	my $opt = "";
	my $invalidOpt = 0;


	while (1) {

		checkSetup();

		mycls();

		heading("$projname Installation");

		print "Current Status:\n\n";

		printInstallStatus("Git Helper Script", $chkHelper);
		printInstallStatus("XSL Stylesheet",		$chkXSL);
		printInstallStatus("libxslt binaries", 	$chkLibxslt);

		print "\nChoose a Menu Option\n\n";

		menuOption("1", "Install Everything");
		menuOption("2", "Uninstall Everything");
		menuSeparator();
		menuOption("3", "Install Git Helper Script");
		menuOption("4", "Install xsl stylesheet");
		menuOption("5", "Install libxslt binaries");
		menuOption("6", "Uninstall Git Helper Script");
		menuOption("7", "Uninstall xsl stylesheet");
		menuOption("8", "Uninstall libxslt binaries");
		menuSeparator();
		menuOption("q", "Quit");

		if ($invalidOpt) {
			printf ("%s is an invalid option\n", $opt);
		} else {
			print "\n";
		}

		print "\nEnter Menu Option: ";

		$invalidOpt = 0;
		my $opt = <>;

		chomp($opt);

		exit 0 if $opt =~ m/^q/i;

		if ($opt eq "1") {

			mycls();
			installEverything();

		} elsif ($opt eq "2") {

			mycls();
			uninstallEverything();

		} elsif ($opt eq "3") {

			mycls();
			installHelper();	

		} elsif ($opt eq "4") {

			mycls();
			installXSL();

		} elsif ($opt eq "5") {

			mycls();
			installLibxslt();

		} elsif ($opt eq "6") {

			mycls();
			uninstallHelper();

		} elsif ($opt eq "7") {

			mycls();
			uninstallXSL();

		} elsif ($opt eq "8") {

			mycls();
			uninstallLibxslt();

		} else {
			$invalidOpt = 1;
		}

		confirmAnyKey() if !$invalidOpt;

	}

}

# Terminal Helper Functions

sub colorSet {
  my ($color) = @_;
  print Term::ANSIColor::color($color) if $useColours;
}

sub colorReset {
  print Term::ANSIColor::color("reset") if $useColours;
}

sub menuOption {

  my ($num, $text) = @_;

  printf("%4s. %s\n", $num, $text);


}

sub menuSeparator {
	print "  ---------------------\n";
}

sub printFileResult {

  my ($filename, $resultdesc, $indicator) = @_;

  printf("%-50s ...", $filename);

  colorSet("bold green")  if ($indicator == 1);
	colorSet("bold white") 	if ($indicator == 0);
  colorSet("bold red")    if ($indicator == -1);

  print("$resultdesc\n");

  colorReset();

}

sub printInstallStatus {

  my ($element, $status) = @_;

  my $statusText = ($status) ? "Installed" : "Not Installed";

  printf("%-25s : ",$element);

  colorSet("bold green")  if ($status);
  colorSet("bold")        if (!$status);

  print("$statusText\n");

  colorReset();

}

sub installRemoveOption {

  my ($num, $text, $enabled, $installed) = @_;

  # Set up install or remove color
  if ($useColours) {
    if ($enabled) {
      if ($installed) {
        print Term::ANSIColor::color("bold red");
      } else {
        print Term::ANSIColor::color("bold green");
      }
    }
  }

  # Print option number
  print "$num. ";

  # Print the Action
  if (!$enabled) {
    print "n/a     ";
  } elsif ($installed) {
    print "Remove  ";
  } else {   
    print "Install ";
  }

  if ($useColours) {   
    if (!$enabled) {
      print Term::ANSIColor::color("reset");
    } else {
      print Term::ANSIColor::color("bold white");
    }  
  };

  # Print the text of the menu option
  print $text;

  # Reset the colours
  if ($useColours) {  
    print Term::ANSIColor::color("reset");
  };

  print "\n";

}

sub heading {

  my $maxwidth  = 50;
  my $fillerChar = "*";

  # Get the Title from the sub arguments
  my ($title) = @_;;

  # Determine number of Asterixes either side
  my $tlength = length($title);
  my $totFillers = $maxwidth - $tlength - 4;
  if ($totFillers < 0) { print "Error: Title too long... exiting";exit -1; };
  my $fillers = int($totFillers / 2);

  # Give me some space
  print "\n";

  # If we are using colours, Set up the colour
  if ($useColours) {
    print Term::ANSIColor::color("bold white");
    print Term::ANSIColor::color("on_blue");
  }

  # Print first asterixes
  for (my $i = 0; $i < $fillers; $i++) { print $fillerChar; }

  # print Heading with space either side
  print " $title ";

  # Print last asterixes
  for (my $i = 0; $i < $fillers; $i++) { print $fillerChar; }
  # Print an extra one if there was an odd number
  if (($totFillers % 2) > 0) { print $fillerChar; }

  # If we are using colours, reset them
  if($useColours) {
    print Term::ANSIColor::color("reset");
  }

  # Print new line
  print "\n\n";

}


sub mycls {

  system("clear");
  
}


sub confirmAnyKey {

  print "\nPress enter to continue ...";
  my $tmp = <STDIN>;

}

sub confirmContinue {

  my $opt       = "";
  my $invalid   = 0;
  my $noanswer  = 1;

  while ($noanswer) {

    print("\nInvalid option: $opt, please choose y/n/q\n\n") if $invalid;

    print "\nContinue? y/n/q: ";
    $opt = <STDIN>;
    chomp($opt);

		print "\n";

    exit 0    if ($opt =~ m/^q/i);
    return 1  if ($opt =~ m/^y/i || $opt eq "");
    return 0  if ($opt =~ m/^n/i);

    $invalid = 1;

  }

}

# END TERMINAL HELPER FUNCTIONS

main();