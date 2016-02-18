package repotools.repotool;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;

import repotools.manifest.Manifest;
import repotools.manifest.ManifestConsole;
import repotools.utilities.ArgUtilities;
import repotools.utilities.StreamUtilities;


public class RepoToolApp
{
	public static void main(String[] argsArg) throws IOException
	{
		// Initialize some things
		console = new ManifestConsole();
		ArgUtilities args = new ArgUtilities(argsArg, console);

		RepoTool tool = new RepoTool();
		tool.setConsole(console);
		
		Date startTime = new Date();
		int exitCode = 0;
		boolean manifestInfoChanged = false;
		
		
		// Give the user some help if they need it
		String commandArg = "help";
		if (args.getLength() > 0)
		{
			commandArg = args.getArg(0);
		}

		
		// Initial screen for valid command
		switch (commandArg)
		{
			case "clear":
			case "create":
			case "edit":
			case "groom":
			case "info":
			case "status":
			case "update":
			case "validate":
				break;

			case "help":
				ClassLoader cl = RepoToolApp.class.getClassLoader();
				InputStream in = cl.getResourceAsStream("help.txt");
				console.write(StreamUtilities.getStringFromInputStream(in));
				System.exit(exitCode);
				break;
			
			default:
				console.writeLine("Unrecognized command \"" + commandArg + "\"");
				exitCode = 1;
				System.exit(exitCode);
				break;
		}
		
		
		// Set up options and parameters
		boolean all					= args.hasOption("all");
		boolean cascade				= args.hasOption("cascade");
		boolean confirmUpdate		= args.hasOption("confirmUpdate");
		boolean force 				= args.hasOption("force");		
		boolean ignoreDate 			= args.hasOption("ignoreDate");
		boolean ignoreDefault		= args.hasOption("ignoreDefault");
		boolean ignoreNew 			= args.hasOption("ignoreNew");
		boolean noTouch				= args.hasOption("noTouch");
		boolean recursive			= args.hasOption("recursive");
		boolean time 				= args.hasOption("time");

		String repositoryName 				= args.getParameterForOption("name");
		String repositoryDescription		= args.getParameterForOption("description");
		String hashMethod					= args.getParameterForOption("hashMethod");
		String manifestFilePathNotRecursive = args.getParameterForOption("manifestFile");

		ArrayList<String> ignoreList 		= args.getParametersForOption("ignore");
		ArrayList<String> dontIgnoreList	= args.getParametersForOption("dontIgnore");
		
		
		// Tool options
		tool.setBackDate(args.hasOption("backDate"));
		tool.setMakeNewHash(args.hasOption("newHash"));
		tool.setAlwaysCheckHash(args.hasOption("reHash"));
		tool.setShowProgress(args.hasOption("showProgress"));
		tool.setTrackDuplicates(args.hasOption("trackDuplicates"));
		tool.setTrackMoves(args.hasOption("trackMoves"));

		
		// Console options
		console.setDetail(args.hasOption("detail"));	
		console.setSilent(args.hasOption("silent"));


		// Exit and show error for any unrecognized arguments
		args.exitWithErrorWhenUncheckedArgs();
		
		
		// Begin doing the work
		if (time)
		{
			console.writeLine("Started: " + startTime.toString());
		}

		// Prepare a list of paths to be processed
		ArrayList<String> manifestFilePaths = new ArrayList<String>();
		if (recursive)
		{
			findManifests(
				new File(System.getProperty("user.dir")),
				cascade,
				manifestFilePaths);
		}
		else
		{
			if (manifestFilePathNotRecursive == null)
			{
				// Default manifest file name located in current directory.
				manifestFilePathNotRecursive =
					System.getProperty("user.dir") +
					File.separator +
					Manifest.defaultManifestStandardFilePath;	
			}
		
			manifestFilePaths.add(manifestFilePathNotRecursive);
		}

		
		// Outer loop over manifests
		for (String manifestFilePath : manifestFilePaths)
		{
			if (recursive)
			{
				console.writeLine(new File(manifestFilePath).getParentFile().getCanonicalPath() + ":");
			}

			// Initialize the tool for this manifest
			tool.clear();
			tool.setManifest(null);

			// Second copy of the manifest which will remain unmodified
			// and possibly rewritten after a validate.
			Manifest manifestForValidateDateUpdate = null;

			File fileInfo = new File(manifestFilePath);
			tool.setRootDirectory(fileInfo.getParentFile());

			// Command-specific code to initialize tool object and
			// manifest object, and then execute command using tool.
			switch (commandArg)
			{
				case "create":
				{
					boolean doCreate = true;
					
					if (force == false)
					{
						if (new File(manifestFilePath).exists())
						{
							doCreate = false;
							console.write("Replace existing manifest file? ");
							doCreate = console.checkConfirm();
						}
					}
					
					if (doCreate == true)
					{
						tool.setManifest(tool.makeManifest());
					}
					
					break;
				}

				case "validate":
				case "status":
				case "update":
				case "edit":
				case "groom":
				{
					if (commandArg == "validate")
					{
						tool.setAlwaysCheckHash(true);
					}
					else if (commandArg == "update")
					{
						tool.setUpdate(true);
					}
					
					boolean different = false;
				
					try
					{
						tool.setManifest(Manifest.readManifestFile(manifestFilePath));
					
						if (commandArg == "validate" && noTouch == false)
						{
							// Read a second copy which will remain unmodified
							manifestForValidateDateUpdate =
							Manifest.readManifestFile(manifestFilePath);
						}
					}
					catch (Exception ex)
					{
						console.reportException(ex);
						console.writeLine("Could not read manifest.");
						System.exit(1);
					}

					if (tool.getManifest() == null)
					{
						exitCode = 1;
					}
					else if (commandArg != "edit")
					{
						tool.doUpdate();

						if (tool.getMissingFiles().size() > 0)
						{
							console.writeLine(tool.getMissingFiles().size() + " files are missing.");
							console.detailFiles(tool.getMissingFiles());
							different = true;
						}
						
						if (tool.getChangedFiles().size() > 0)
						{
							console.writeLine(tool.getChangedFiles().size() + " files have changed content.");
							console.detailFiles(tool.getChangedFiles());
							different = true;
						}
						
						if (tool.getNewFiles().size() > 0)
						{
							console.writeLine(tool.getNewFiles().size() + " files are new.");
							console.detailFiles(tool.getNewFiles());
						
							if (ignoreNew == false)
							{
								different = true;
							}
						}

						if (tool.getLastModifiedDateFiles().size() > 0)
						{
							console.writeLine(tool.getLastModifiedDateFiles().size() +
								" files have last-modified dates which are different.");
							
							console.detailFiles(tool.getLastModifiedDateFiles());
						
							if (ignoreDate == false)
							{
								different = true;
							}
						}
						
						if (tool.getErrorFiles().size() > 0)
						{
							console.writeLine(tool.getErrorFiles().size() + " files have errors.");
							console.detailFiles(tool.getErrorFiles());
							different = true;
						}
						
						if (tool.getMovedFiles().size() > 0)
						{
							console.writeLine(tool.getMovedFiles().size() + " files were moved.");
							console.detailFiles(tool.getMovedFileOrder(), tool.getMovedFiles(), false);
							different = true;
						}
						
						if (tool.getDuplicateFiles().size() > 0)
						{
							console.writeLine(tool.getDuplicateFiles().size() + " file hashes were duplicates.");
							console.detailFiles(tool.getDuplicateFiles());
						}

						if (tool.getNewlyIgnoredFiles().size() > 0)
						{
							console.writeLine(tool.getNewlyIgnoredFiles().size() + " files are newly ignored.");
							console.detailFiles(tool.getNewlyIgnoredFiles());
						}
						
						if (tool.getIgnoredFiles().size() > 1)
						{
							console.writeLine(tool.getIgnoredFiles().size() + " files were ignored.");
							
							if (all == true)
							{
								console.detailFiles(tool.getIgnoredFiles());
							}
						}

						console.writeLine(tool.getFileCheckedCount() + " files were checked.");
						
						if (commandArg == "validate")
						{
							if (different)
							{
								console.writeLine("Problems found.");
								exitCode = 1;
							}
							else
							{
								console.writeLine("No problems.");
							}
						}
					}
					
					break;
				}

				case "clear":
				{
					try
					{
						tool.setManifest(Manifest.readManifestFile(manifestFilePath));
					}
					catch (Exception ex)
					{
						console.reportException(ex);
						console.writeLine("Could not read manifest.");
					}
					
					if (tool.getManifest() != null)
					{
						boolean doClear = true;
						
						if (force == false)
						{
							console.write("Clear " +
								tool.getManifest().countFiles() +
								" files from the manifest? ");
							
							doClear = console.checkConfirm();
						}
						
						if (doClear == true)
						{
							if (tool.getManifest() == null)
							{
								exitCode = 1;
							}
							else
							{
								tool.getManifest().getRootDirectory().getFiles().clear();
								tool.getManifest().getRootDirectory().getSubdirectories().clear();
							}
						}
					}
				
					break;
				}
				
				case "info":
				{
					try
					{
						tool.setManifest(Manifest.readManifestFile(manifestFilePath));
					}
					catch (Exception ex)
					{
						console.reportException(ex);
						console.writeLine("Could not read manifest.");
					}
					
					if (tool.getManifest() == null)
					{
						exitCode = 1;
					}
					else
					{
						if (tool.getManifest().getName() != null)
						{
							console.writeLine("Name:                          " + tool.getManifest().getName());
						}
					
						console.writeLine("GUID:                          " + tool.getManifest().getGuid().toString());
						
						if (tool.getManifest().getDefaultHashMethod() != null)
						{
							console.writeLine("Default hash method:           " + tool.getManifest().getDefaultHashMethod());
						}
						
						console.writeLine("Date of creation:              " +
							(tool.getManifest().getInceptionDateUtc().toString()));
						
						console.writeLine("Date of last update:           " +
							(tool.getManifest().getLastUpdateDateUtc().toString()));
						
						console.writeLine("Last change of manifest info:  " +
							(tool.getManifest().getManifestInfoLastModifiedUtc().toString()));
						
						console.writeLine("Date of last validation:       " +
							(tool.getManifest().getLastValidateDateUtc().toString()));

						console.writeLine("Total number of files:         " + tool.getManifest().countFiles());
						console.writeLine("Total number of bytes:         " + tool.getManifest().countBytes());
						if (tool.getManifest().getIgnoreList().size() > 0)
						{
							console.writeLine("Ignoring these file patterns:");
							for (String nextIgnore : tool.getManifest().getIgnoreList())
							{
								console.writeLine("   " + nextIgnore);
							}
						}
					}

					if (tool.getManifest().getDescription() != null)
					{
						console.writeLine();
						console.writeLine("Description: ");
						console.writeLine(tool.getManifest().getDescription());
					}

					break;
				}
			}

			// Command-specific code to write the manifest, and possibly
			// delete some files if grooming.
			switch (commandArg)
			{
				case "create":
				case "update":
				case "edit":
				case "clear":
				{
					if (tool.getManifest() != null)
					{
						if (repositoryName != null)
						{
							tool.getManifest().setName(repositoryName);
							manifestInfoChanged = true;
						}

						if (repositoryDescription != null)
						{
							tool.getManifest().setDescription(repositoryDescription);
							manifestInfoChanged = true;
						}
						
						if (hashMethod != null)
						{
							tool.getManifest().setDefaultHashMethod(hashMethod);
							manifestInfoChanged = true;
						}
						
						if (ignoreList.size() > 0)
						{
							for (String nextIgnore : ignoreList)
							{
								if (tool.getManifest().getIgnoreList().contains(nextIgnore) == false)
								{
									tool.getManifest().getIgnoreList().add(nextIgnore);
								}
							}
							manifestInfoChanged = true;
						}
						
						if (dontIgnoreList.size() > 0)
						{
							for (String nextIgnore : dontIgnoreList)
							{
								if (tool.getManifest().getIgnoreList().contains(nextIgnore) == true)
								{
									tool.getManifest().getIgnoreList().remove(nextIgnore);
								}
							}
							manifestInfoChanged = true;
						}

						if (ignoreDefault == true)
						{
							tool.getManifest().getIgnoreList().clear();
							
							Manifest defaultPrototype = tool.makeManifest();
							for (String nextIgnore : defaultPrototype.getIgnoreList())
							{
								tool.getManifest().getIgnoreList().add(nextIgnore);
							}
							manifestInfoChanged = true;
						}
						
						if (manifestInfoChanged)
						{
							tool.getManifest().setManifestInfoLastModifiedUtc(new Date());
						}
						
						if (confirmUpdate)
						{
							console.write("Update manifest? ");
							
							boolean writeManifest = console.checkConfirm();
							if (writeManifest == false)
							{
								break;
							}
						}

						try
						{
							tool.getManifest().writeManifestFile(manifestFilePath);
						}
						catch (Exception ex)
						{
							console.reportException(ex);
							console.writeLine("Could not write manifest.");
							exitCode = 1;
						}
					}
					
					break;
				}
				
				case "validate":
				{
					if (noTouch == false)
					{
						manifestForValidateDateUpdate.setLastValidateDateUtc(new Date());
					
						try
						{
							manifestForValidateDateUpdate.writeManifestFile(
								manifestFilePath);
						}
						catch (Exception ex)
						{
							console.reportException(ex);
							console.writeLine("Could not write manifest.");
							exitCode = 1;
						}	
					}

					break;
				}
				
				case "groom":
				{
					if (tool.getNewFilesForGroom().size() > 0)
					{
						boolean doGroom = true;
						
						if (force == false)
						{
							console.write("Delete " +
								tool.getNewFilesForGroom().size() +
								" new files? ");
							
							doGroom = console.checkConfirm();
						}
						
						if (doGroom == true)
						{
							for (File delFile : tool.getNewFilesForGroom())
							{
								delFile.delete();
							}
						}
					}

					if (all == true && tool.getIgnoredFilesForGroom().size() > 0)
					{
						boolean doGroomAll = true;
						
						if (force == false)
						{
							console.write("Delete " +
								tool.getIgnoredFilesForGroom().size() +
								" ignored files? ");
							
							doGroomAll = console.checkConfirm();
						}
						
						if (doGroomAll == true)
						{
							for (File delFile : tool.getIgnoredFilesForGroom())
							{
								delFile.delete();
							}
						}
					}

					break;
				}
			}

			if (recursive)
			{
				console.writeLine();
			}
		}

		if (time)
		{
			Date finishTime = new Date();
			console.writeLine("Finished: " + finishTime.toString());
			
			ZonedDateTime startZdt = ZonedDateTime.ofInstant(
				startTime.toInstant(),
				ZoneId.systemDefault());
		
			ZonedDateTime finishZdt = ZonedDateTime.ofInstant(
				startTime.toInstant(),
				ZoneId.systemDefault());
		
			Duration duration = Duration.between(startZdt, finishZdt);
			console.writeLine("Duration: " + duration.toString());
		}
		
		System.exit(exitCode);
	}
	
	/*
	System.out.println("Running...");
	Manifest manifest = Manifest.ReadManifestFile(args[0]);
	System.out.println("Done.");
	manifest.WriteManifestFile(args[1]);
	*/
	
	// Helper methods

	static ManifestConsole console = null;
	
	static void findManifests(
		File nextDirectory,
		boolean cascade,
		ArrayList<String> filePaths)
	{
		String checkManifestPath;
		try
		{
			checkManifestPath =	
				nextDirectory.getCanonicalPath() +
				File.separator +
				Manifest.defaultManifestFileName;
		}
		catch (IOException ex)
		{
			console.reportException(ex);
			return;
		}

		boolean foundManifest = false;
		if (new File(checkManifestPath).exists())
		{
			foundManifest = true;
			filePaths.add(checkManifestPath);
		}
		
		if (cascade == true || foundManifest == false)
		{
			for (File nextSubDirectory :
				nextDirectory.listFiles())
			{
				if (nextDirectory.isDirectory())
				{
					findManifests(
						nextSubDirectory,
						cascade,
						filePaths);
				}
			}
		}
	}
}
