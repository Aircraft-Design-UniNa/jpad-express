package jpad.core.ex.standaloneutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import jpad.core.ex.aircraft.Aircraft;
import jpad.core.ex.aircraft.components.fuselage.Fuselage;
import jpad.core.ex.standaloneutils.customdata.MyXmlTree;

public class JPADGlobalData {

	public static MyXmlTree theXmlTree = null;

	private static Aircraft theCurrentAircraft = null;
	public static List<Aircraft> _theAircraftList = new ArrayList<Aircraft>();
	private static String currentAircraftName = "AIRCRAFT";
	public static ObjectProperty<Aircraft> theCurrentAircraftProperty = new SimpleObjectProperty<Aircraft>();
	public static ObjectProperty<Fuselage> theCurrentFuselageProperty = new SimpleObjectProperty<Fuselage>();
	
	public static final Logger log = Logger.getLogger("log");
	
	public static final Map<Object, String> imagesMap = new HashMap<Object, String>();

	public static Map<Object, String> get_imagesMap() {
		return imagesMap;
	}
	
	public static String getCurrentAircraftName() {
		return currentAircraftName;
	}

	public static void setCurrentAircraftName(String currentAircraftName) {
		JPADGlobalData.currentAircraftName = currentAircraftName;
	}

	public static MyXmlTree getTheXmlTree() {
		return theXmlTree;
	}

	public static void setTheXmlTree(MyXmlTree xmlTree) {
		theXmlTree = xmlTree;
	}

	public static Aircraft getTheCurrentAircraft() {
		return theCurrentAircraft;
	}

	public static void setTheCurrentAircraft(Aircraft _theCurrentAircraft) {
		theCurrentAircraft = _theCurrentAircraft;
	}

	public static void deleteTheCurrentAircraft() {
		theCurrentAircraft = null;
	}

	public static void setTheCurrentFuselage(Fuselage fuselage) {
		JPADGlobalData.theCurrentFuselageProperty.set(fuselage);
		if (fuselage != null) {
		}
	}

	public static void setTheCurrentAircraftInMemory(Aircraft ac) {

		// The whole aircraft
		setTheCurrentAircraft(ac);
		JPADGlobalData.theCurrentAircraftProperty.set(getTheCurrentAircraft());

	}

	public static List<Aircraft> get_theAircraftList() {
		return _theAircraftList;
	}

	public static void set_theAircraftList(List<Aircraft> theAircraftList) {
		_theAircraftList = theAircraftList;
	}

	public static Logger getLOG() {
		return log;
	}

}
