package jpad.express;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import javafx.application.Application;
import javafx.stage.Stage;
import jpad.configs.ex.MyConfiguration;
import jpad.configs.ex.enumerations.FoldersEnum;
import jpad.core.ex.aircraft.Aircraft;
import jpad.core.ex.standaloneutils.aircraft.AircraftAndComponentsViewPlotUtils;
import jpad.core.ex.writers.JPADStaticWriteUtils;
import ncsa.hdf.hdf5lib.exceptions.HDF5LibraryException;

public class JPAD extends Application {

	// declaration necessary for Concrete Object usage
	public static CmdLineParser theCmdLineParser;

	//-------------------------------------------------------------

	public static Aircraft theAircraft;

	//-------------------------------------------------------------

	@Override
	public void start(Stage primaryStage) throws Exception {

		//--------------------------------------------------
		// get the aircraft object
		System.out.println("\n\n##################");
		System.out.println("jpad-suite :: START");

		Aircraft aircraft = JPAD.theAircraft;
		if (aircraft == null) {
			System.out.println("aircraft object null, returning!");
			return;
		}

	}; // end-of-Runnable

	/**
	 * Main
	 *
	 * @param args
	 * @throws InvalidFormatException 
	 * @throws HDF5LibraryException 
	 */
	public static void main(String[] args) throws InvalidFormatException, HDF5LibraryException {

		// TODO: check out this as an alternative
		// https://blog.codecentric.de/en/2015/09/javafx-how-to-easily-implement-application-preloader-2/

		final PrintStream originalOut = System.out;
		PrintStream filterStream = new PrintStream(new OutputStream() {
		    public void write(int b) {
		         // write nothing
		    }
		});
		long startTime = System.currentTimeMillis();        
		
		System.out.println("-------------------");
		System.out.println("Complete Analysis Test");
		System.out.println("-------------------");
		
		JPADArguments va = new JPADArguments();
		JPAD.theCmdLineParser = new CmdLineParser(va);

		// populate the wing static object in the class-> start ...)
		try {
		// before launching the JavaFX application thread (launch -
			JPAD.theCmdLineParser.parseArgument(args);
			
			String pathToXML = va.getInputFile().getAbsolutePath();
			System.out.println("AIRCRAFT INPUT ===> " + pathToXML);

			String pathToAnalysesXML = va.getInputFileAnalyses().getAbsolutePath();
			System.out.println("ANALYSES INPUT ===> " + pathToAnalysesXML);
			
			String pathToOperatingConditionsXML = va.getOperatingConditionsInputFile().getAbsolutePath();
			System.out.println("OPERATING CONDITIONS INPUT ===> " + pathToOperatingConditionsXML);
			
			String dirAirfoil = va.getAirfoilDirectory().getCanonicalPath();
			System.out.println("AIRFOILS ===> " + dirAirfoil);

			String dirFuselages = va.getFuselagesDirectory().getCanonicalPath();
			System.out.println("FUSELAGES ===> " + dirFuselages);
			
			String dirLiftingSurfaces = va.getLiftingSurfacesDirectory().getCanonicalPath();
			System.out.println("LIFTING SURFACES ===> " + dirLiftingSurfaces);
			
			String dirEngines = va.getEnginesDirectory().getCanonicalPath();
			System.out.println("ENGINES ===> " + dirEngines);
			
			String dirNacelles = va.getNacellesDirectory().getCanonicalPath();
			System.out.println("NACELLES ===> " + dirNacelles);
			
			String dirLandingGears = va.getLandingGearsDirectory().getCanonicalPath();
			System.out.println("LANDING GEARS ===> " + dirLandingGears);
			
			String dirCabinConfiguration = va.getCabinConfigurationDirectory().getCanonicalPath();
			System.out.println("CABIN CONFIGURATIONS ===> " + dirCabinConfiguration);
			
			System.out.println("--------------");

			//------------------------------------------------------------------------------------
			// Setup database(s)
			MyConfiguration.initWorkingDirectoryTree(
					MyConfiguration.databaseDirectory,
					MyConfiguration.inputDirectory,
					MyConfiguration.outputDirectory
					);
			
			////////////////////////////////////////////////////////////////////////
			// Aircraft creation
			System.out.println("\n\n\tCreating the Aircraft ... \n\n");
			
			// deactivating system.out
			System.setOut(filterStream);
			
			long aircraftStartTime = System.currentTimeMillis();
			
			// reading aircraft from xml ... 
			theAircraft = Aircraft.importFromXML(
					pathToXML,
					dirLiftingSurfaces,
					dirFuselages,
					dirEngines,
					dirNacelles,
					dirLandingGears,
					dirCabinConfiguration,
					dirAirfoil
					);
			
			// activating system.out
			System.setOut(originalOut);			
			System.out.println(theAircraft.toString());
			System.setOut(filterStream);
			
			long aircraftEndTime = System.currentTimeMillis();
			
			////////////////////////////////////////////////////////////////////////
			// Set the folders tree
			long folderCleaningStartTime = System.currentTimeMillis();
			String folderPath = MyConfiguration.getDir(FoldersEnum.OUTPUT_DIR); 
			String aircraftFolder = JPADStaticWriteUtils.createNewFolder(folderPath + theAircraft.getId() + File.separator);
			String subfolderPath = JPADStaticWriteUtils.createNewFolder(aircraftFolder);
			FileUtils.cleanDirectory(new File(subfolderPath)); 
			long folderCleaningEndTime = System.currentTimeMillis();
			
			long aircraftViewsStartTime = System.currentTimeMillis();
			String subfolderViewPath = JPADStaticWriteUtils.createNewFolder(aircraftFolder + "VIEWS" + File.separator);
			String subfolderViewComponentsPath = JPADStaticWriteUtils.createNewFolder(subfolderViewPath + "COMPONENTS");
			if(theAircraft != null) {
				AircraftAndComponentsViewPlotUtils.createAircraftTopView(theAircraft, subfolderViewPath);
				AircraftAndComponentsViewPlotUtils.createAircraftSideView(theAircraft, subfolderViewPath);
				AircraftAndComponentsViewPlotUtils.createAircraftFrontView(theAircraft, subfolderViewPath);
			}
			if(theAircraft.getFuselage() != null) {
				AircraftAndComponentsViewPlotUtils.createFuselageTopView(theAircraft, subfolderViewComponentsPath);
				AircraftAndComponentsViewPlotUtils.createFuselageSideView(theAircraft, subfolderViewComponentsPath);
				AircraftAndComponentsViewPlotUtils.createFuselageFrontView(theAircraft, subfolderViewComponentsPath);
			}
			if(theAircraft.getWing() != null) {
				AircraftAndComponentsViewPlotUtils.createWingPlanformView(theAircraft, subfolderViewComponentsPath);
				AircraftAndComponentsViewPlotUtils.createEquivalentLiftingSurfaceView(theAircraft.getWing(), subfolderViewComponentsPath);
			}
			if(theAircraft.getHTail() != null) {
				AircraftAndComponentsViewPlotUtils.createHTailPlanformView(theAircraft, subfolderViewComponentsPath);
				AircraftAndComponentsViewPlotUtils.createEquivalentLiftingSurfaceView(theAircraft.getHTail(), subfolderViewComponentsPath);
			}
			if(theAircraft.getVTail() != null) {
				AircraftAndComponentsViewPlotUtils.createVTailPlanformView(theAircraft, subfolderViewComponentsPath);
				AircraftAndComponentsViewPlotUtils.createEquivalentLiftingSurfaceView(theAircraft.getVTail(), subfolderViewComponentsPath);
			}
			if(theAircraft.getCanard() != null) {
				AircraftAndComponentsViewPlotUtils.createCanardPlanformView(theAircraft, subfolderViewComponentsPath);
				AircraftAndComponentsViewPlotUtils.createEquivalentLiftingSurfaceView(theAircraft.getCanard(), subfolderViewComponentsPath);
			}
			if(theAircraft.getNacelles() != null) {
				AircraftAndComponentsViewPlotUtils.createNacelleTopView(theAircraft, subfolderViewComponentsPath);
				AircraftAndComponentsViewPlotUtils.createNacelleSideView(theAircraft, subfolderViewComponentsPath);
				AircraftAndComponentsViewPlotUtils.createNacelleFrontView(theAircraft, subfolderViewComponentsPath);
			}
			long aircraftViewsEndTime = System.currentTimeMillis();
			
			////////////////////////////////////////////////////////////////////////
			long aircraftEstimatedTime = aircraftEndTime - aircraftStartTime;
			long foldersCleaningEstimatedTime = folderCleaningEndTime - folderCleaningStartTime;
			long aircraftViewsEstimatedTime = aircraftViewsEndTime - aircraftViewsStartTime;
			long estimatedTime = System.currentTimeMillis() - startTime;
			
			System.out.println("\n\t TIME ESTIMATED FOR AIRCRAFT CREATION = " + TimeUnit.MILLISECONDS.toSeconds(aircraftEstimatedTime) + " seconds");
			System.out.println("\n\t TIME ESTIMATED FOR FOLDERS CLEANING = " + TimeUnit.MILLISECONDS.toSeconds(foldersCleaningEstimatedTime) + " seconds");
			System.out.println("\n\t TIME ESTIMATED FOR AIRCRAFT VIEWS CREATION = " + TimeUnit.MILLISECONDS.toSeconds(aircraftViewsEstimatedTime) + " seconds");
			System.out.println("\n\t TOTAL TIME ESTIMATED = " + TimeUnit.MILLISECONDS.toSeconds(estimatedTime) + " seconds");
			
			System.setOut(filterStream);
			
		} catch (CmdLineException | IOException e) {
			System.err.println("Error: " + e.getMessage());
			JPAD.theCmdLineParser.printUsage(System.err);
			System.err.println();
			System.err.println("  Must launch this app with proper command line arguments.");
			System.exit(1);
		}	
		
		System.exit(1);
	}
}
