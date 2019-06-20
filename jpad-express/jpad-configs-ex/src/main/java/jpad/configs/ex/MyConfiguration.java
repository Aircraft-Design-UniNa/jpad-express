package jpad.configs.ex;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import javax.measure.unit.NonSI;
import javax.measure.unit.UnitFormat;

import org.jscience.physics.amount.Amount;
import org.jscience.physics.amount.AmountFormat;

import javolution.text.TypeFormat;
import jpad.configs.ex.enumerations.FoldersEnum;

/**
 * Group together all the settings needed to run the application
 * 
 * @author Lorenzo Attanasio
 *
 */
public class MyConfiguration {

	// Software info
	public final double version = 1.0;
	public final String name = "JPAD";
	public final String description = "none";
	public final String release = "1.0";

	// Files and folders names
	public static final  String outputFolderName = "jpad-output";
	public static final String inputFolderName = "jpad-input";
	public static final String databaseFolderName = "jpad-data";
	public static final String databasePackageName = "database";

	//final String databaseFolderPath = "/" + databasePackageName + "/" + databaseFolderName;
	public static final String databaseFolderPath = databaseFolderName; // <=== NB !!!

	public static final String cadFolderName = "cad";
	public static final String imagesFolderName = "images";

	public static final String tabAsSpaces = "    ";
	public static final String notInitializedWarning = "not_initialized";

	// Directories
	public static String currentImagesDirectory;
	public static final String currentDirectoryString = 
			System.getProperty("user.dir"); // TODO: fix me! points to JPADSandbox
			//System.getenv("JPAD_ROOT") + File.separator + "JPADSandbox_v2";
	public static final File currentDirectory = new File(currentDirectoryString);

	public static final String src_it_unina_adopt_Directory = currentDirectoryString
			+ File.separator + "src"
			+ File.separator + "org"
			+ File.separator + "jpad"
			+ File.separator;

	public static final  String objects3dDirectory = src_it_unina_adopt_Directory	+ "objects3d" + File.separator;

	public static final String runsDirectory = currentDirectoryString + File.separator + "runs" + File.separator;

	public static final String cadDirectory = currentDirectoryString + File.separator + outputFolderName + File.separator + cadFolderName + File.separator;

	public static final String testDirectory = currentDirectoryString + File.separator + "test" + File.separator;

	public static final String inputDirectory = currentDirectoryString + File.separator + inputFolderName + File.separator;

	public static final String outputDirectory = currentDirectoryString + File.separator + outputFolderName + File.separator;

	public static final String imagesDirectory = outputDirectory + imagesFolderName + File.separator;

	public static final String databaseDirectory = currentDirectoryString + File.separator + databaseFolderName + File.separator;
 
	// Serialized database interpolating functions
	public static final String interpolaterVeDSCDatabaseSerializedDirectory = databaseDirectory + File.separator + "serializedDatabase" 
			+ File.separator; 
	public static final String interpolaterVeDSCDatabaseSerializedName = "interpolaterVeDSCDatabase.xml"; 
	public static final String interpolaterVeDSCDatabaseSerializedFullName = 
			interpolaterVeDSCDatabaseSerializedDirectory + File.separator + interpolaterVeDSCDatabaseSerializedName; 

	//	final String interpolaterFusDesDatabaseSerializedDirectory = currentDirectoryString + File.separator + "database" + File.separator; 
	public static final String interpolaterFusDesDatabaseSerializedDirectory = databaseDirectory + File.separator + "serializedDatabase" 
			+ File.separator; 
	public static final String interpolaterFusDesDatabaseSerializedName = "interpolaterFusDesDatabase.xml"; 
	public static final String interpolaterFusDesatabaseSerializedFullName = 
			interpolaterFusDesDatabaseSerializedDirectory + File.separator + interpolaterFusDesDatabaseSerializedName;
	
	public static final String interpolaterAerodynamicDatabaseSerializedDirectory = databaseDirectory + File.separator + "serializedDatabase" 
			+ File.separator; 
	public static final String interpolaterAerodynamicDatabaseSerializedName = "interpolaterAerodynamicDatabase.xml"; 
	public static final String interpolaterAerodynamicDatabaseSerializedFullName = 
			interpolaterAerodynamicDatabaseSerializedDirectory + File.separator + interpolaterAerodynamicDatabaseSerializedName;
	
	public static final String interpolaterHighLiftDatabaseSerializedDirectory = databaseDirectory + File.separator + "serializedDatabase" 
			+ File.separator; 
	public static final String interpolaterHighLiftDatabaseSerializedName = "interpolaterHighLiftDatabase.xml"; 
	public static final String interpolaterHighLiftDatabaseSerializedFullName = 
			interpolaterHighLiftDatabaseSerializedDirectory + File.separator + interpolaterHighLiftDatabaseSerializedName;
	
	public static final String interpolaterFuelFractionDatabaseSerializedDirectory = databaseDirectory + File.separator + "serializedDatabase" 
			+ File.separator; 
	public static final String interpolaterFuelFractionDatabaseSerializedName = "interpolaterFuelFractionsDatabase.xml"; 
	public static final String interpolaterFuelFractionDatabaseSerializedFullName = 
			interpolaterFuelFractionDatabaseSerializedDirectory + File.separator + interpolaterFuelFractionDatabaseSerializedName;
	
	private static HashMap<FoldersEnum, String>  mapPaths = new HashMap<FoldersEnum, String>();

	
	public static String createNewFolder(String path) {
		File folder = new File(path);
		try{
			if(folder.mkdir() && !folder.exists()) return path;
			else return path;

		} catch(Exception e){
			e.printStackTrace();
			return "";
		}
	}

	
	/**
	 * 
	 * Customize output format of Amount variables
	 * 
	 * @author Agostino De Marco
	 */

	public static void customizeAmountOutput(){
		
		//============================================================================
		// Trick to write the ".getEstimatedValue() + unit" format
		// http://stackoverflow.com/questions/8514293/is-there-a-way-to-make-jscience-output-in-a-more-human-friendly-format
		UnitFormat uf = UnitFormat.getInstance();
		
		// Customize labels
		uf.label(NonSI.DEGREE_ANGLE, "deg"); // instead of default '�' symbol
		
		
		AmountFormat.setInstance(new AmountFormat() {
		    @Override
		    public Appendable format(Amount<?> m, Appendable a) throws IOException {
		        TypeFormat.format(m.getEstimatedValue(), -1, false, false, a);
		        a.append(" ");
		        return uf.format(m.getUnit(), a);
		    }

		    @Override
		    public Amount<?> parse(CharSequence csq, Cursor c) throws IllegalArgumentException {
		        throw new UnsupportedOperationException("Parsing not supported.");
		    }
		});
	}	
	
	
	/**
	 * Initialize the working directory tree and fill the map of folders
	 * @return mapPaths HashMap<MyConfiguration.FoldersEnum, String>
	 * 
	 * @author Vincenzo Cusati 
	 */
	public static HashMap<FoldersEnum, String> initWorkingDirectoryTree(){

		mapPaths.put(FoldersEnum.CURRENT_DIR, currentDirectoryString);
		mapPaths.put(FoldersEnum.DATABASE_DIR, databaseDirectory);
		mapPaths.put(FoldersEnum.INPUT_DIR, inputDirectory);
		mapPaths.put(FoldersEnum.OUTPUT_DIR, outputDirectory);
		mapPaths.put(FoldersEnum.RUNS_DIR, runsDirectory);
		mapPaths.put(FoldersEnum.CAD_DIR, cadDirectory);
		mapPaths.put(FoldersEnum.IMAGE_DIR, imagesDirectory);
		mapPaths.put(FoldersEnum.TEST_DIR, testDirectory);
		mapPaths.put(FoldersEnum.OBJECTS3D_DIR, objects3dDirectory);

		// Create the folder from map values
		mapPaths.entrySet().stream().forEach(
				//				e -> System.out.println(e.getKey() + ": " + e.getValue())
				e -> createNewFolder(e.getValue())
				);

		return mapPaths;
	}	


	/**
	 * 
	 * @param str  
	 * @return
	 * 
	 * @author Vincenzo Cusati
	 */

	public static HashMap<FoldersEnum, String> initWorkingDirectoryTree(String ... str){

		if(Arrays.asList(str).contains(currentDirectoryString)) mapPaths.put(FoldersEnum.CURRENT_DIR, currentDirectoryString);
		if(Arrays.asList(str).contains(databaseDirectory)) mapPaths.put(FoldersEnum.DATABASE_DIR, databaseDirectory);
		if(Arrays.asList(str).contains(inputDirectory)) mapPaths.put(FoldersEnum.INPUT_DIR, inputDirectory);
		if(Arrays.asList(str).contains(outputDirectory)) mapPaths.put(FoldersEnum.OUTPUT_DIR, outputDirectory);
		if(Arrays.asList(str).contains(runsDirectory)) mapPaths.put(FoldersEnum.RUNS_DIR, runsDirectory);
		if(Arrays.asList(str).contains(cadDirectory)) mapPaths.put(FoldersEnum.CAD_DIR, cadDirectory);
		if(Arrays.asList(str).contains(imagesDirectory)) mapPaths.put(FoldersEnum.IMAGE_DIR, imagesDirectory);

		return mapPaths;
	}


	public static String getDir(FoldersEnum dir){
		return mapPaths.get(dir);
	}

	public static void setDir(FoldersEnum dir, String folderPath) {
		mapPaths.put(dir, folderPath);
	}


}