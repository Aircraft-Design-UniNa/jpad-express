package jpad.core.ex.standaloneutils.database;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import jpad.configs.ex.MyConfiguration;
import jpad.core.ex.standaloneutils.MyXMLReaderUtils;
import jpad.core.ex.standaloneutils.database.databasefunctions.engine.TurbofanCharacteristicsReader;
import jpad.core.ex.writers.JPADStaticWriteUtils;

public class DatabaseManager {

	public static  TurbofanCharacteristicsReader initializeTurbofanCharacteristicsDatabase(
			TurbofanCharacteristicsReader databaseManager, 
			String databaseDirectory, 
			String databaseName
			) throws InvalidFormatException, IOException {
		
		String databaseNameXML = databaseName.replace(".h5", ".xml");
		
		String serializedDatabaseDirectory = databaseDirectory + File.separator + "serializedDatabase";
		String serializedDatabaseFullName = serializedDatabaseDirectory + File.separator + databaseNameXML;
		
		File serializedTurbofanCharacteristicsDatabaseFile = new File(serializedDatabaseFullName);
		
		if (serializedTurbofanCharacteristicsDatabaseFile.exists()) {
			
			System.out.println("De-serializing file: " + serializedTurbofanCharacteristicsDatabaseFile.getAbsolutePath() + " ...");
			databaseManager = (TurbofanCharacteristicsReader) 
			MyXMLReaderUtils.deserializeObject(
					databaseManager,
					serializedDatabaseFullName,
					StandardCharsets.UTF_8
					);
		} else {
			
			System.out.println(	"Serializing file " + "==> " + databaseName + " ==> "+ 
					serializedTurbofanCharacteristicsDatabaseFile.getAbsolutePath() + " ...");
			databaseManager = new TurbofanCharacteristicsReader(
					MyConfiguration.databaseDirectory,
					"TurbofanCharacterstics.h5"
					);

			File dir = new File(serializedDatabaseDirectory);
			if (!dir.exists()){
				dir.mkdirs(); 
			} else {
				JPADStaticWriteUtils.serializeObject(
						databaseManager, 
						serializedDatabaseDirectory,
						databaseNameXML
						);
			}
		}
		
		return databaseManager;
	}
}

