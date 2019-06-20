package jpad.core.ex.aircraft.components.cabinconfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import javax.measure.quantity.Length;
import javax.measure.quantity.Mass;
import javax.measure.unit.SI;

import org.apache.commons.lang3.ArrayUtils;
import org.jscience.physics.amount.Amount;

import jpad.configs.ex.MyConfiguration;
import jpad.configs.ex.enumerations.ClassTypeEnum;
import jpad.configs.ex.enumerations.EmergencyExitEnum;
import jpad.configs.ex.enumerations.RelativePositionEnum;
import jpad.core.ex.aircraft.Aircraft;
import jpad.core.ex.standaloneutils.JPADXmlReader;
import jpad.core.ex.standaloneutils.MyXMLReaderUtils;

/**
 * Define the cabin configuration (full economy, economy + business) and current flight configuration
 * (in terms of number of passengers, number of crew members).
 * 
 * Each element in the following lists is relative to a class:
 * index 0: ECONOMY
 * index 1: BUSINESS
 * index 2: FIRST
 * 
 * If a class is missing indexes are decreased by 1, e.g. if first class is
 * missing:
 * index 0: ECONOMY
 * index 1: BUSINESS
 * 
 * The number of passengers and their location is necessary for estimating 
 * the Aircraft Center of Gravity position.
 * 
 * @author Vittorio Trifari
 *
 */
public class CabinConfiguration {

	//------------------------------------------------------------------------------------
	// VARIABLE DECLARATION
	private ICabinConfiguration _theCabinConfigurationBuilder;
	private File _cabinConfigurationPath;

	//....................................................................................
	// Derived Input
	private int _numberOfClasses, _cabinCrewNumber, _totalCrewNumber, _numberOfDecks;

	private List<List<SeatsBlock>> _seatsBlocksList;
	private List<Amount<Length>> _pitchList; 
	private List<Amount<Length>> _widthList; 
	private List<Amount<Length>> _distanceFromWallList;
	private List<Integer> _numberOfPassengersList;
	private List<Integer> _numberOfRowsList;
	private List<int[]> _numberOfColumnsList;
	private List<Integer> _numberOfAislesList;
	private List<Amount<Length>> _seatsActualXCoordinates;
	private List<Amount<Length>> _seatsActualYCoordinates;
	private Map<EmergencyExitEnum, Integer> _emergencyExitMap;
	private Amount<Length> _totalEmergencyExitsWidth;

	private List<Amount<Length>> _seatsCoGFrontToRear;
	private List<Amount<Length>> _seatsCoGRearToFront;
	private List<Amount<Mass>> _currentMassList;

	//------------------------------------------------------------------------------------
	// BUILDER
	public CabinConfiguration(ICabinConfiguration theCabinConfigurationBuilder) {

		this.setTheCabinConfigurationBuilder(theCabinConfigurationBuilder);

		this._currentMassList = new ArrayList<>();
		this.setSeatsCoGFrontToRear(new ArrayList<>());
		this.setSeatsCoGRearToFront(new ArrayList<>());

		this._emergencyExitMap = new HashMap<>();
		this._seatsBlocksList = new ArrayList<>();
		this._pitchList = new ArrayList<>(); 
		this._widthList = new ArrayList<>(); 
		this._distanceFromWallList = new ArrayList<>();
		this._numberOfPassengersList = new ArrayList<>();
		this._numberOfRowsList = new ArrayList<>();
		this._numberOfColumnsList = new ArrayList<>();
		this._numberOfAislesList = new ArrayList<>();

		this.calculateDependentVariables();
	}

	//------------------------------------------------------------------------------------
	// METHODS
	public static CabinConfiguration importFromXML(String pathToXML) {

		JPADXmlReader reader = new JPADXmlReader(pathToXML);

		System.out.println("Reading configuration data ...");

		String id = MyXMLReaderUtils
				.getXMLPropertyByPath(
						reader.getXmlDoc(), reader.getXpath(),
						"//@id");

		//------------------------------------------------------------------
		// GLOBAL DATA
		int designPassengerNumber = 0;
		int flightCrewNumber = 0;
		List<ClassTypeEnum> classesType = new ArrayList<>();
		double deltaXCabinStart = 0.0;
		double deltaXCabinFwd = 0.0;
		double deltaXCabinAft = 0.0;

		//..................................................................		
		String designPassengerNumberProperty =  reader.getXMLPropertyByPath("//global_data/design_passengers_number");
		if(designPassengerNumberProperty != null) 
			designPassengerNumber = Integer.valueOf(designPassengerNumberProperty);
		//..................................................................
		String flightCrewNumberProperty =  reader.getXMLPropertyByPath("//global_data/flight_crew_number");
		if(flightCrewNumberProperty != null)
			flightCrewNumber = Integer.valueOf(flightCrewNumberProperty);
		//..................................................................
		List<String> classesTypeProperty = reader.getXMLPropertiesByPath("//global_data/classes_type");
		if(!classesTypeProperty.isEmpty()) {
			List<String> classesTypeList = JPADXmlReader.readArrayFromXML(
					reader.getXMLPropertiesByPath("//global_data/classes_type")
					.get(0));
			classesType = new ArrayList<ClassTypeEnum>();
			for(int i=0; i<classesTypeList.size(); i++) {
				if(classesTypeList.get(i).equalsIgnoreCase("ECONOMY"))
					classesType.add(ClassTypeEnum.ECONOMY);
				else if(classesTypeList.get(i).equalsIgnoreCase("BUSINESS"))
					classesType.add(ClassTypeEnum.BUSINESS);
				else if(classesTypeList.get(i).equalsIgnoreCase("FIRST"))
					classesType.add(ClassTypeEnum.FIRST);
				else {
					System.err.println("\n\tERROR: INVALID CLASS TYPE !!\n");
					return null;
				}
			}
		}
		//..................................................................
		String deltaXCabinStartProperty = reader.getXMLPropertyByPath("//global_data/delta_x_cabin_start");
		if(deltaXCabinStartProperty != null)
			deltaXCabinStart = Double.valueOf(deltaXCabinStartProperty);
		//..................................................................
		String deltaXCabinFwdProperty = reader.getXMLPropertyByPath("//global_data/delta_x_cabin_fwd");
		if(deltaXCabinFwdProperty != null)
			deltaXCabinFwd = Double.valueOf(deltaXCabinFwdProperty);
		//..................................................................
		String deltaXCabinAftProperty = reader.getXMLPropertyByPath("//global_data/delta_x_cabin_aft");
		if(deltaXCabinAftProperty != null)
			deltaXCabinAft = Double.valueOf(deltaXCabinAftProperty);

		//---------------------------------------------------------------
		// DETAILED DATA
		int[] numberOfColumnsEconomyClass = new int[] {0};
		int[] numberOfColumnsBusinessClass = new int[] {0};
		int[] numberOfColumnsFirstClass = new int[] {0};
		double percentageEconomyClass = 0.0;
		double percentageBusinessClass = 0.0;
		double percentageFirstClass = 0.0;
		int numberOfRowsEconomyClass = 0;
		int numberOfRowsBusinessClass = 0;
		int numberOfRowsFirstClass = 0;	
		Amount<Length> pitchEconomyClass = Amount.valueOf(0.0, SI.METER);
		Amount<Length> pitchBusinessClass = Amount.valueOf(0.0, SI.METER);
		Amount<Length> pitchFirstClass = Amount.valueOf(0.0, SI.METER);
		Amount<Length> widthEconomyClass = Amount.valueOf(0.0, SI.METER);
		Amount<Length> widthBusinessClass = Amount.valueOf(0.0, SI.METER);
		Amount<Length> widthFirstClass = Amount.valueOf(0.0, SI.METER);
		Amount<Length> distanceFromWallEconomyClass = Amount.valueOf(0.0, SI.METER);
		Amount<Length> distanceFromWallBusinessClass = Amount.valueOf(0.0, SI.METER);
		Amount<Length> distanceFromWallFirstClass = Amount.valueOf(0.0, SI.METER);

		//..................................................................
		List<String> numberOfColumnsEconomyClassProperty = reader.getXMLPropertiesByPath(
				"//detailed_data/number_of_columns_economy_class"
				);
		if(!numberOfColumnsEconomyClassProperty.isEmpty()) {
			List<String> numberOfColumnsEconomyClassArray = JPADXmlReader
					.readArrayFromXML(
							reader.getXMLPropertiesByPath(
									"//detailed_data/number_of_columns_economy_class"
									)
							.get(0)
							);
			numberOfColumnsEconomyClass = new int[numberOfColumnsEconomyClassArray.size()];
			for(int i=0; i<numberOfColumnsEconomyClassArray.size(); i++)
				numberOfColumnsEconomyClass[i] = Integer.valueOf(
						numberOfColumnsEconomyClassArray.get(i)
						);
		}
		//..................................................................
		if(classesType.contains(ClassTypeEnum.BUSINESS)) {
			List<String> numberOfColumnsBusinessClassProperty = reader.getXMLPropertiesByPath(
					"//detailed_data/number_of_columns_business_class"
					);
			if(!numberOfColumnsBusinessClassProperty.isEmpty()) {
				List<String> numberOfColumnsBusinessClassArray = JPADXmlReader
						.readArrayFromXML(
								reader.getXMLPropertiesByPath(
										"//detailed_data/number_of_columns_business_class"
										)
								.get(0)
								);
				numberOfColumnsBusinessClass = new int[numberOfColumnsBusinessClassArray.size()];
				for(int i=0; i<numberOfColumnsBusinessClassArray.size(); i++)
					numberOfColumnsBusinessClass[i] = Integer.valueOf(
							numberOfColumnsBusinessClassArray.get(i)
							);
			}
		}	
		//..................................................................
		if(classesType.contains(ClassTypeEnum.FIRST)) {
			List<String> numberOfColumnsFirstClassProperty = reader.getXMLPropertiesByPath(
					"//detailed_data/number_of_columns_first_class"
					);
			if(!numberOfColumnsFirstClassProperty.isEmpty()) {
				List<String> numberOfColumnsFirstClassArray = JPADXmlReader
						.readArrayFromXML(
								reader.getXMLPropertiesByPath(
										"//detailed_data/number_of_columns_first_class"
										)
								.get(0)
								);
				numberOfColumnsFirstClass = new int[numberOfColumnsFirstClassArray.size()];
				for(int i=0; i<numberOfColumnsFirstClassArray.size(); i++)
					numberOfColumnsFirstClass[i] = Integer.valueOf(
							numberOfColumnsFirstClassArray.get(i)
							);
			}
		}	
		//..................................................................
		String estimateRowsFromPercentagesString = MyXMLReaderUtils
				.getXMLPropertyByPath(
						reader.getXmlDoc(), reader.getXpath(), 
						"//detailed_data/@estimate_rows_from_percentages"
						);

		if(estimateRowsFromPercentagesString.equalsIgnoreCase("TRUE")) {			
			String estimateClassPercentagesString = MyXMLReaderUtils
					.getXMLPropertyByPath(
							reader.getXmlDoc(), reader.getXpath(), 
							"//detailed_data/@estimate_class_percentages"
							);

			if(estimateClassPercentagesString.equalsIgnoreCase("TRUE")) {
				double[] classPercentages = estimateClassPercentages(
						designPassengerNumber, 
						IntStream.of(numberOfColumnsEconomyClass).sum(), 
						classesType
						);
				percentageEconomyClass = classPercentages[0];
				percentageBusinessClass = classPercentages[1];
				percentageFirstClass = classPercentages[2];
			} else {
				//..................................................................
				String percentageEconomyClassString = reader.getXMLPropertyByPath("//detailed_data/percentage_economy_class");
				if(percentageEconomyClassString != null)
					percentageEconomyClass = Double.valueOf(percentageEconomyClassString);
				//..................................................................
				if(classesType.contains(ClassTypeEnum.BUSINESS)) {
					String percentageBusinessClassString = reader.getXMLPropertyByPath("//detailed_data/percentage_business_class");
					if(percentageBusinessClassString != null)
						percentageBusinessClass = Double.valueOf(percentageBusinessClassString);
				}
				//..................................................................
				if(classesType.contains(ClassTypeEnum.FIRST)) {
					String percentageFirstClassString = reader.getXMLPropertyByPath("//detailed_data/percentage_first_class");
					if(percentageFirstClassString != null)
						percentageFirstClass = Double.valueOf(percentageFirstClassString);
				}
				if(Math.abs((percentageEconomyClass + percentageBusinessClass + percentageFirstClass) - 100) > 0.01 ) {
					System.err.println("\n\tERROR: THE SUM OF CLASS PERCENTAGES MUST BE EQUAL TO 100.0 !!\n");
					return null;
				}
			}		
		} else {
			//..................................................................
			String numberOfRowsEconomyClassProperty = reader.getXMLPropertyByPath("//detailed_data/number_of_rows_economy_class");
			if(numberOfRowsEconomyClassProperty != null)
				numberOfRowsEconomyClass = Integer.valueOf(numberOfRowsEconomyClassProperty);
			//..................................................................
			if(classesType.contains(ClassTypeEnum.BUSINESS)) {
				String numberOfRowsBusinessClassProperty = reader.getXMLPropertyByPath("//detailed_data/number_of_rows_business_class");
				if(numberOfRowsBusinessClassProperty != null)
					numberOfRowsBusinessClass = Integer.valueOf(numberOfRowsBusinessClassProperty);
			}			
			//..................................................................
			if(classesType.contains(ClassTypeEnum.FIRST)) {
				String numberOfRowsFirstClassProperty = reader.getXMLPropertyByPath("//detailed_data/number_of_rows_first_class");
				if(numberOfRowsFirstClassProperty != null)
					numberOfRowsFirstClass = Integer.valueOf(numberOfRowsFirstClassProperty);
			}		
			//..................................................................
			// Check coherence between rows and columns and total number of pax
			int minPaxEconomy = IntStream.of(numberOfColumnsEconomyClass).sum()*(numberOfRowsEconomyClass - 1) + 1;
			int minPaxBusiness = IntStream.of(numberOfColumnsBusinessClass).sum()*(numberOfRowsBusinessClass - 1) + 1;;
			int minPaxFirst = IntStream.of(numberOfColumnsFirstClass).sum()*(numberOfRowsFirstClass - 1) + 1;

			int maxPaxEconomy = IntStream.of(numberOfColumnsEconomyClass).sum()*numberOfRowsEconomyClass;
			int maxPaxBusiness = IntStream.of(numberOfColumnsBusinessClass).sum()*numberOfRowsBusinessClass;
			int maxPaxFirst = IntStream.of(numberOfColumnsFirstClass).sum()*numberOfRowsFirstClass;

			int totalMinPax = minPaxEconomy + minPaxBusiness + minPaxFirst;
			int totalMaxPax = maxPaxEconomy + maxPaxBusiness + maxPaxFirst;

			if(designPassengerNumber < totalMinPax || designPassengerNumber > totalMaxPax) {
				System.err.println("\n\tERROR: ROWS & COLUMNS AND DESIGN PAX NUMBER COHERENCE CHECK FAILED !!\n");
				return null;
			}
		}	
		//..................................................................
		String estimatePitchString = MyXMLReaderUtils
				.getXMLPropertyByPath(
						reader.getXmlDoc(), reader.getXpath(), 
						"//detailed_data/@estimate_pitch"
						);

		if(estimatePitchString.equalsIgnoreCase("TRUE")) {
			double[] seatsPitch = estimateSeatsPitch(
					designPassengerNumber, 
					IntStream.of(numberOfColumnsEconomyClass).sum(), 
					classesType
					);
			pitchEconomyClass = Amount.valueOf(seatsPitch[0], SI.METER);
			pitchBusinessClass = Amount.valueOf(seatsPitch[1], SI.METER);
			pitchFirstClass = Amount.valueOf(seatsPitch[2], SI.METER);
		} else {
			//..................................................................
			String pitchEconomyClassProperty = reader.getXMLPropertyByPath("//detailed_data/pitch_economy_class");
			if(pitchEconomyClassProperty != null)
				pitchEconomyClass = reader.getXMLAmountLengthByPath("//detailed_data/pitch_economy_class");
			//..................................................................
			if(classesType.contains(ClassTypeEnum.BUSINESS)) {
				String pitchBusinessClassProperty = reader.getXMLPropertyByPath("//detailed_data/pitch_business_class");
				if(pitchBusinessClassProperty != null)
					pitchBusinessClass = reader.getXMLAmountLengthByPath("//detailed_data/pitch_business_class");
			}		
			//..................................................................
			if(classesType.contains(ClassTypeEnum.FIRST)) {
				String pitchFirstClassProperty = reader.getXMLPropertyByPath("//detailed_data/pitch_first_class");
				if(pitchFirstClassProperty != null)
					pitchFirstClass = reader.getXMLAmountLengthByPath("//detailed_data/pitch_first_class");
			}		
		}
		//..................................................................
		String estimateWidthString = MyXMLReaderUtils
				.getXMLPropertyByPath(
						reader.getXmlDoc(), reader.getXpath(), 
						"//detailed_data/@estimate_width"
						);

		if(estimateWidthString.equalsIgnoreCase("TRUE")) {
			double[] seatsWidth = estimateSeatsWidth(
					designPassengerNumber, 
					IntStream.of(numberOfColumnsEconomyClass).sum(), 
					classesType
					);
			widthEconomyClass = Amount.valueOf(seatsWidth[0], SI.METER);
			widthBusinessClass = Amount.valueOf(seatsWidth[1], SI.METER);
			widthFirstClass = Amount.valueOf(seatsWidth[2], SI.METER);
		} else {
			//..................................................................
			String widthEconomyClassProperty = reader.getXMLPropertyByPath("//detailed_data/width_economy_class");
			if(widthEconomyClassProperty != null)
				widthEconomyClass = reader.getXMLAmountLengthByPath("//detailed_data/width_economy_class");
			//..................................................................
			if(classesType.contains(ClassTypeEnum.BUSINESS)) {
				String widthBusinessClassProperty = reader.getXMLPropertyByPath("//detailed_data/width_business_class");
				if(widthBusinessClassProperty != null)
					widthBusinessClass = reader.getXMLAmountLengthByPath("//detailed_data/width_business_class");
			}			
			//..................................................................
			if(classesType.contains(ClassTypeEnum.FIRST)) {
				String widthFirstClassProperty = reader.getXMLPropertyByPath("//detailed_data/width_first_class");
				if(widthFirstClassProperty != null)
					widthFirstClass = reader.getXMLAmountLengthByPath("//detailed_data/width_first_class");
			}		
		}	
		//..................................................................
		String distanceFromWallEconomyClassProperty = reader.getXMLPropertyByPath("//detailed_data/distance_from_wall_economy_class");
		if(distanceFromWallEconomyClassProperty != null)
			distanceFromWallEconomyClass = reader.getXMLAmountLengthByPath("//detailed_data/distance_from_wall_economy_class");
		//..................................................................
		if(classesType.contains(ClassTypeEnum.BUSINESS)) {
			String distanceFromWallBusinessClassProperty = reader.getXMLPropertyByPath("//detailed_data/distance_from_wall_business_class");
			if(distanceFromWallBusinessClassProperty != null)
				distanceFromWallBusinessClass = reader.getXMLAmountLengthByPath("//detailed_data/distance_from_wall_business_class");
		}	
		//..................................................................
		if(classesType.contains(ClassTypeEnum.FIRST)) {
			String distanceFromWallFirstClassProperty = reader.getXMLPropertyByPath("//detailed_data/distance_from_wall_first_class");
			if(distanceFromWallFirstClassProperty != null)
				distanceFromWallFirstClass = reader.getXMLAmountLengthByPath("//detailed_data/distance_from_wall_first_class");
		}		
		
		//---------------------------------------------------------------
		// CHECK CLASSES AND ABREAST
		if((classesType.size() == 1 && !classesType.contains(ClassTypeEnum.ECONOMY)) || 
		   (classesType.size() == 2 && !(classesType.contains(ClassTypeEnum.ECONOMY) 
				   && classesType.contains(ClassTypeEnum.BUSINESS))) ||
		   (classesType.size() == 3 && !(classesType.contains(ClassTypeEnum.ECONOMY) 
				   && classesType.contains(ClassTypeEnum.BUSINESS)
				   && classesType.contains(ClassTypeEnum.FIRST))
				   )) {
			System.err.println("\n\tERROR: INVALID CLASSES COMBINATION !!\n");
			return null;
		}
		
		if(numberOfColumnsEconomyClass.length > 3 || numberOfColumnsBusinessClass.length > 3 || numberOfColumnsFirstClass.length > 3) {
			System.err.println("\n\tERROR: INVALID NUMBER OF COLUMNS/AISLES !!\n");
			return null;
		}

		//---------------------------------------------------------------
		// SET DEPENDENT DATA
		int classesNumber = classesType.size();
		int numberOfPassengersEconomyClass = 0;
		int numberOfPassengersBusinessClass = 0;
		int numberOfPassengersFirstClass = 0;

		if(estimateRowsFromPercentagesString.equalsIgnoreCase("TRUE")) {
			//..................................................................
			// Appropriately divide passengers per class and calculate rows
			int[] passengersPerClass = new int[] {
					(int) Math.round(percentageEconomyClass*designPassengerNumber)/100,
					(int) Math.round(percentageBusinessClass*designPassengerNumber)/100,
					(int) Math.round(percentageFirstClass*designPassengerNumber)/100
			};
			int newPassengersTotalNumber = IntStream.of(passengersPerClass).sum();
			double[] numberOfRowsDouble = new double[] {0.0, 0.0, 0.0};
			while(Math.abs(newPassengersTotalNumber - designPassengerNumber) > 0) {
				numberOfRowsDouble[0] = passengersPerClass[0]/(IntStream.of(numberOfColumnsEconomyClass).sum());
				if(classesType.contains(ClassTypeEnum.BUSINESS))
					numberOfRowsDouble[1] = passengersPerClass[1]/(IntStream.of(numberOfColumnsBusinessClass).sum());
				if(classesType.contains(ClassTypeEnum.FIRST))
					numberOfRowsDouble[2] = passengersPerClass[2]/(IntStream.of(numberOfColumnsFirstClass).sum());
				if(newPassengersTotalNumber - designPassengerNumber > 0) {			
					double smallestRowsDouble = Double.MAX_VALUE;
					int idxSmallestRowsDouble = 0;
					for(int idx = 0; idx < numberOfRowsDouble.length; idx++) {
						double flooredRowsDouble = numberOfRowsDouble[idx] - Math.floor(numberOfRowsDouble[idx]);
						if(flooredRowsDouble > 0.0 && flooredRowsDouble < smallestRowsDouble) {
							smallestRowsDouble = flooredRowsDouble;
							idxSmallestRowsDouble = idx;
						}							
					}					
					passengersPerClass[idxSmallestRowsDouble] = passengersPerClass[idxSmallestRowsDouble] - 1;			
				} else {
					double greatestRowsDouble = 0.0;
					int idxGreatestRowsDouble = 0;
					for(int idx = 0; idx < numberOfRowsDouble.length; idx++) {
						double ceiledRowsDouble = Math.ceil(numberOfRowsDouble[idx]) - numberOfRowsDouble[idx];
						if(ceiledRowsDouble < 1.0 && ceiledRowsDouble > greatestRowsDouble) {
							greatestRowsDouble = ceiledRowsDouble;
							idxGreatestRowsDouble = idx;
						}
					}
					passengersPerClass[idxGreatestRowsDouble] = passengersPerClass[idxGreatestRowsDouble] + 1;
				}	
				newPassengersTotalNumber = IntStream.of(passengersPerClass).sum();
			}
			numberOfPassengersEconomyClass = passengersPerClass[0];
			numberOfPassengersBusinessClass = passengersPerClass[1];
			numberOfPassengersFirstClass = passengersPerClass[2];

			numberOfRowsEconomyClass = (int) Math.ceil(
					(float) numberOfPassengersEconomyClass/IntStream.of(numberOfColumnsEconomyClass).sum()
					);
			if(classesType.contains(ClassTypeEnum.BUSINESS))
				numberOfRowsBusinessClass = (int) Math.ceil(
						(float) numberOfPassengersBusinessClass/IntStream.of(numberOfColumnsBusinessClass).sum()
						);
			if(classesType.contains(ClassTypeEnum.FIRST))
				numberOfRowsFirstClass = (int) Math.ceil(
						(float) numberOfPassengersFirstClass/IntStream.of(numberOfColumnsFirstClass).sum()
						);

		} else {
			//..................................................................
			// Appropriately divide passengers per class 
			int[] passengersPerClass = new int[] {
					IntStream.of(numberOfColumnsEconomyClass).sum()*numberOfRowsEconomyClass,
					IntStream.of(numberOfColumnsBusinessClass).sum()*numberOfRowsBusinessClass,
					IntStream.of(numberOfColumnsFirstClass).sum()*numberOfRowsFirstClass
			};

			int newPassengersTotalNumber = IntStream.of(passengersPerClass).sum();

			int iterIdx = 1;
			int iterLimit = classesNumber;
			while(newPassengersTotalNumber > designPassengerNumber) {
				if(iterIdx > iterLimit)
					iterIdx = 1;

				if(iterIdx == 1)
					passengersPerClass[0] = passengersPerClass[0] - 1;

				if(iterIdx == 2 && classesType.contains(ClassTypeEnum.BUSINESS))
					passengersPerClass[1] = passengersPerClass[1] - 1;

				if(iterIdx == 3 && classesType.contains(ClassTypeEnum.FIRST))
					passengersPerClass[2] = passengersPerClass[2] - 1;

				newPassengersTotalNumber = IntStream.of(passengersPerClass).sum();
				iterIdx = iterIdx++;
			}

			numberOfPassengersEconomyClass = passengersPerClass[0];
			numberOfPassengersBusinessClass = passengersPerClass[1];
			numberOfPassengersFirstClass = passengersPerClass[2];
		}

		//---------------------------------------------------------------
		// BUILD THE CABIN CONFIGURATION OBJECT
		CabinConfiguration cabinConfiguration = new CabinConfiguration(
				new ICabinConfiguration.Builder()
				.setId(id)
				.setDesignPassengerNumber(designPassengerNumber)
				.setFlightCrewNumber(flightCrewNumber)
				.addAllClassesType(classesType)
				.setDeltaXCabinStart(deltaXCabinStart)
				.setDeltaXCabinFwd(deltaXCabinFwd)
				.setDeltaXCabinAft(deltaXCabinAft)
				.setNumberOfColumnsEconomyClass(numberOfColumnsEconomyClass)
				.setNumberOfColumnsBusinessClass(numberOfColumnsBusinessClass)
				.setNumberOfColumnsFirstClass(numberOfColumnsFirstClass)
				.setPercentageEconomyClass(percentageEconomyClass)
				.setPercentageBusinessClass(percentageBusinessClass)
				.setPercentageFirstClass(percentageFirstClass)
				.setNumberOfRowsEconomyClass(numberOfRowsEconomyClass)
				.setNumberOfRowsBusinessClass(numberOfRowsBusinessClass)
				.setNumberOfRowsFirstClass(numberOfRowsFirstClass)
				.setNumberOfPassengersEconomyClass(numberOfPassengersEconomyClass)
				.setNumberOfPassengersBusinessClass(numberOfPassengersBusinessClass)
				.setNumberOfPassengersFirstClass(numberOfPassengersFirstClass)
				.setPitchEconomyClass(pitchEconomyClass)
				.setPitchBusinessClass(pitchBusinessClass)
				.setPitchFirstClass(pitchFirstClass)
				.setWidthEconomyClass(widthEconomyClass)
				.setWidthBusinessClass(widthBusinessClass)
				.setWidthFirstClass(widthFirstClass)
				.setDistanceFromWallEconomyClass(distanceFromWallEconomyClass)
				.setDistanceFromWallBusinessClass(distanceFromWallBusinessClass)
				.setDistanceFromWallFirstClass(distanceFromWallFirstClass)
				.build()				
				);

		return cabinConfiguration;

	}

	public void updateConfiguration() {

		setPitchList(new ArrayList<Amount<Length>>());
		getPitchList().add(getTheCabinConfigurationBuilder().getPitchEconomyClass());
		getPitchList().add(getTheCabinConfigurationBuilder().getPitchBusinessClass());
		getPitchList().add(getTheCabinConfigurationBuilder().getPitchFirstClass());

		setWidthList(new ArrayList<Amount<Length>>());
		getWidthList().add(getTheCabinConfigurationBuilder().getWidthEconomyClass());
		getWidthList().add(getTheCabinConfigurationBuilder().getWidthBusinessClass());
		getWidthList().add(getTheCabinConfigurationBuilder().getWidthFirstClass());

		setDistanceFromWallList(new ArrayList<Amount<Length>>());
		getDistanceFromWallList().add(getTheCabinConfigurationBuilder().getDistanceFromWallEconomyClass());
		getDistanceFromWallList().add(getTheCabinConfigurationBuilder().getDistanceFromWallBusinessClass());
		getDistanceFromWallList().add(getTheCabinConfigurationBuilder().getDistanceFromWallFirstClass());

		setNumberOfRowsList(new ArrayList<Integer>());
		getNumberOfRowsList().add(getTheCabinConfigurationBuilder().getNumberOfRowsEconomyClass());
		getNumberOfRowsList().add(getTheCabinConfigurationBuilder().getNumberOfRowsBusinessClass());
		getNumberOfRowsList().add(getTheCabinConfigurationBuilder().getNumberOfRowsFirstClass());

		setNumberOfColumnsList(new ArrayList<int[]>());
		getNumberOfColumnsList().add(getTheCabinConfigurationBuilder().getNumberOfColumnsEconomyClass());
		getNumberOfColumnsList().add(getTheCabinConfigurationBuilder().getNumberOfColumnsBusinessClass());
		getNumberOfColumnsList().add(getTheCabinConfigurationBuilder().getNumberOfColumnsFirstClass());

		setNumberOfPassengersList(new ArrayList<>());
		getNumberOfPassengersList().add(getTheCabinConfigurationBuilder().getNumberOfPassengersEconomyClass());
		getNumberOfPassengersList().add(getTheCabinConfigurationBuilder().getNumberOfPassengersBusinessClass());
		getNumberOfPassengersList().add(getTheCabinConfigurationBuilder().getNumberOfPassengersFirstClass());

	}

	public void calculateDependentVariables() {

		setNumberOfClasses(getTheCabinConfigurationBuilder().getClassesType().size());
		setCabinCrewNumber((int) Math.ceil(getTheCabinConfigurationBuilder().getDesignPassengerNumber()/35));
		setTotalCrewNumber(getCabinCrewNumber() + getTheCabinConfigurationBuilder().getFlightCrewNumber());
		setNumberOfDecks((getDesignPassengerNumber() < 500) ? 1 : 2);

		setNumberOfAislesList(new ArrayList<>());
		getNumberOfAislesList().add(getTheCabinConfigurationBuilder().getNumberOfColumnsEconomyClass().length - 1);
		if(getTheCabinConfigurationBuilder().getClassesType().contains(ClassTypeEnum.BUSINESS)) {
			getNumberOfAislesList().add(getTheCabinConfigurationBuilder().getNumberOfColumnsBusinessClass().length - 1);
		} else {
			getNumberOfAislesList().add(0);
		}
		if(getTheCabinConfigurationBuilder().getClassesType().contains(ClassTypeEnum.FIRST)) {
			getNumberOfAislesList().add(getTheCabinConfigurationBuilder().getNumberOfColumnsFirstClass().length - 1);
		} else {
			getNumberOfAislesList().add(0);
		}

		buildEmergencyExitMap();
		updateConfiguration();

	}

	/**
	 * Build a simplified cabin layout: the user has to define the x coordinate
	 * at which the layout starts, the number of classes and, for each class,
	 * number of rows and of abreast, pitch, width, and eventually breaks
	 * (empty spaces between seats). The method does not consider missing seats
	 * and differences between two groups of seats (a group is such when 
	 * separated from another one by an aisle).
	 * 
	 * @param aircraft
	 * 
	 */
	public void buildSimpleLayout(Aircraft aircraft) {

		updateConfiguration();	
		Amount<Length> xCabinStart = aircraft.getFuselage().getXApexConstructionAxes().plus(
				aircraft.getFuselage().getFuselageLength().times(getTheCabinConfigurationBuilder().getDeltaXCabinStart())
				);	
		Amount<Length> deltaXCabinFwd = aircraft.getFuselage().getFuselageLength().times(
				getTheCabinConfigurationBuilder().getDeltaXCabinFwd()
				);
		Amount<Length> deltaXCabinAft = aircraft.getFuselage().getFuselageLength().times(
				getTheCabinConfigurationBuilder().getDeltaXCabinAft()
				);
		Amount<Length> xCoordsBreaks = xCabinStart;	 

		List<List<Amount<Length>>> cabinDeltas = new ArrayList<>();
		for(int i = 0; i < getNumberOfClasses(); i++) {
			List<Amount<Length>> deltas = new ArrayList<>();
			if(i == 0) {
				deltas.add(deltaXCabinFwd);
				deltas.add(Amount.valueOf(0.0, SI.METER));
			} else if(i == getNumberOfClasses() - 1) {
				deltas.add(Amount.valueOf(0.0, SI.METER));
				deltas.add(deltaXCabinAft);
			} else {
				deltas.add(Amount.valueOf(0.0, SI.METER));
				deltas.add(Amount.valueOf(0.0, SI.METER));
			}
			cabinDeltas.add(deltas);	
		}
		Collections.reverse(cabinDeltas);

		List<List<Amount<Length>>> cabinBreaks = new ArrayList<>();
		for(int i = (getNumberOfClasses() - 1); i > -1; i--) {
			List<Amount<Length>> breaks = new ArrayList<>();
			breaks.add(xCoordsBreaks);
			xCoordsBreaks = xCoordsBreaks
					.plus(getPitchList().get(i).times(getNumberOfRowsList().get(i)))
					.plus(cabinDeltas.get(i).get(0))
					.plus(cabinDeltas.get(i).get(1));
			breaks.add(xCoordsBreaks);
			cabinBreaks.add(breaks);
		}
		Collections.reverse(cabinBreaks);

		List<List<SeatsBlock>> seatsBlocksList = new ArrayList<>();
		for(int i = (getNumberOfClasses() - 1); i > -1; i--) {			
			//..................................................................
			// Divide empty spaces between available columns 
			int[] fwdRowAvailableSeats = new int[] {0, 0, 0}; // left, center, right
			int classAislesNumber = getNumberOfColumnsList().get(i).length - 1;

			if(classAislesNumber > 1) {
				fwdRowAvailableSeats[0] = getNumberOfColumnsList().get(i)[0];
				fwdRowAvailableSeats[1] = getNumberOfColumnsList().get(i)[1];
				fwdRowAvailableSeats[2] = getNumberOfColumnsList().get(i)[2];
			} else {
				fwdRowAvailableSeats[0] = getNumberOfColumnsList().get(i)[0];
				fwdRowAvailableSeats[2] = getNumberOfColumnsList().get(i)[1];
			}

			int numClassEmptySpaces = (getNumberOfPassengersList().get(i) % (IntStream.of(getNumberOfColumnsList().get(i)).sum()) != 0) ?
					IntStream.of(getNumberOfColumnsList().get(i)).sum() - 
					getNumberOfPassengersList().get(i) % (IntStream.of(getNumberOfColumnsList().get(i)).sum()) :
						0;	

					if(numClassEmptySpaces < Collections.min(Arrays.asList(ArrayUtils.toObject(getNumberOfColumnsList().get(i))))) {
						if(classAislesNumber > 1) {
							fwdRowAvailableSeats[1] = fwdRowAvailableSeats[1] - numClassEmptySpaces;
						} else {
							fwdRowAvailableSeats[0] = fwdRowAvailableSeats[0] - numClassEmptySpaces;
						}
					} else {
						int remainingEmptySpaces = numClassEmptySpaces;
						if(classAislesNumber > 1) {				
							if(getNumberOfColumnsList().get(i)[1] >= numClassEmptySpaces) {
								fwdRowAvailableSeats[1] = fwdRowAvailableSeats[1] - numClassEmptySpaces;
							} else {
								remainingEmptySpaces = remainingEmptySpaces - fwdRowAvailableSeats[1];
								fwdRowAvailableSeats[1] = 0;
								if(fwdRowAvailableSeats[0] >= remainingEmptySpaces) {
									fwdRowAvailableSeats[0] = fwdRowAvailableSeats[0] - remainingEmptySpaces;
								} else {
									remainingEmptySpaces = remainingEmptySpaces - fwdRowAvailableSeats[0];
									fwdRowAvailableSeats[0] = 0;
									fwdRowAvailableSeats[2] = fwdRowAvailableSeats[2] - remainingEmptySpaces;
								}
							}
						} else {
							if(getNumberOfColumnsList().get(i)[0] >= numClassEmptySpaces) {
								fwdRowAvailableSeats[0] = fwdRowAvailableSeats[0] - numClassEmptySpaces;
							} else {
								remainingEmptySpaces = remainingEmptySpaces - fwdRowAvailableSeats[0];
								fwdRowAvailableSeats[0] = 0;
								fwdRowAvailableSeats[2] = fwdRowAvailableSeats[2] - remainingEmptySpaces;
							}
						}
					}

					int[] seatBlockActualSeats = new int[3];
					if(classAislesNumber > 1) {
						seatBlockActualSeats[0] = getNumberOfColumnsList().get(i)[0]*(getNumberOfRowsList().get(i) - 1) + fwdRowAvailableSeats[0];
						seatBlockActualSeats[1] = getNumberOfColumnsList().get(i)[1]*(getNumberOfRowsList().get(i) - 1) + fwdRowAvailableSeats[1];
						seatBlockActualSeats[2] = getNumberOfColumnsList().get(i)[2]*(getNumberOfRowsList().get(i) - 1) + fwdRowAvailableSeats[2];
					} else {
						seatBlockActualSeats[0] = getNumberOfColumnsList().get(i)[0]*(getNumberOfRowsList().get(i) - 1) + fwdRowAvailableSeats[0];
						seatBlockActualSeats[2] = getNumberOfColumnsList().get(i)[1]*(getNumberOfRowsList().get(i) - 1) + fwdRowAvailableSeats[2];
					}

					//..................................................................
					// Generate seat blocks	
					List<SeatsBlock> seatsBlockList = new ArrayList<>();

					SeatsBlock seatsBlockLeft = new SeatsBlock(
							new ISeatBlock.Builder()
							.setPosition(RelativePositionEnum.LEFT)
							.setXStart(cabinBreaks.get(i).get(0))
							.setXEnd(cabinBreaks.get(i).get(1))
							.setDeltaXCabinFwd(cabinDeltas.get(i).get(0))
							.setDeltaXCabinAft(cabinDeltas.get(i).get(1))
							.setPitch(getPitchList().get(i))
							.setWidth(getWidthList().get(i))
							.setDistanceFromWall(getDistanceFromWallList().get(i))
							.setBlockActualNumberOfSeats(seatBlockActualSeats[0])
							.setRowsNumber(getNumberOfRowsList().get(i))
							.setColumnsNumber(getNumberOfColumnsList().get(i)[0])
							.setType(_theCabinConfigurationBuilder.getClassesType().get(i))
							.build()
							);

					if(getNumberOfAislesList().get(i) < 2) {

						SeatsBlock seatsBlockRight = new SeatsBlock(
								new ISeatBlock.Builder()
								.setPosition(RelativePositionEnum.RIGHT)
								.setXStart(cabinBreaks.get(i).get(0))
								.setXEnd(cabinBreaks.get(i).get(1))
								.setDeltaXCabinFwd(cabinDeltas.get(i).get(0))
								.setDeltaXCabinAft(cabinDeltas.get(i).get(1))
								.setPitch(getPitchList().get(i))
								.setWidth(getWidthList().get(i))
								.setDistanceFromWall(getDistanceFromWallList().get(i))
								.setBlockActualNumberOfSeats(seatBlockActualSeats[2])
								.setRowsNumber(getNumberOfRowsList().get(i))
								.setColumnsNumber(getNumberOfColumnsList().get(i)[1])
								.setType(_theCabinConfigurationBuilder.getClassesType().get(i))
								.build()
								);

						seatsBlockList.add(seatsBlockLeft);	
						seatsBlockList.add(seatsBlockRight);

					} else {

						SeatsBlock seatsBlockRight = new SeatsBlock(
								new ISeatBlock.Builder()
								.setPosition(RelativePositionEnum.RIGHT)
								.setXStart(cabinBreaks.get(i).get(0))
								.setXEnd(cabinBreaks.get(i).get(1))
								.setDeltaXCabinFwd(cabinDeltas.get(i).get(0))
								.setDeltaXCabinAft(cabinDeltas.get(i).get(1))
								.setPitch(getPitchList().get(i))
								.setWidth(getWidthList().get(i))
								.setDistanceFromWall(getDistanceFromWallList().get(i))
								.setBlockActualNumberOfSeats(seatBlockActualSeats[2])
								.setRowsNumber(getNumberOfRowsList().get(i))
								.setColumnsNumber(getNumberOfColumnsList().get(i)[2])
								.setType(_theCabinConfigurationBuilder.getClassesType().get(i))
								.build()
								);

						SeatsBlock seatsBlockCenter = new SeatsBlock(
								new ISeatBlock.Builder()
								.setPosition(RelativePositionEnum.CENTER)
								.setXStart(cabinBreaks.get(i).get(0))
								.setXEnd(cabinBreaks.get(i).get(1))
								.setDeltaXCabinFwd(cabinDeltas.get(i).get(0))
								.setDeltaXCabinAft(cabinDeltas.get(i).get(1))
								.setPitch(getPitchList().get(i))
								.setWidth(getWidthList().get(i))
								.setDistanceFromWall(getDistanceFromWallList().get(i))
								.setBlockActualNumberOfSeats(seatBlockActualSeats[1])
								.setRowsNumber(getNumberOfRowsList().get(i))
								.setColumnsNumber(getNumberOfColumnsList().get(i)[1])
								.setType(_theCabinConfigurationBuilder.getClassesType().get(i))
								.build()
								);

						seatsBlockList.add(seatsBlockLeft);
						seatsBlockList.add(seatsBlockCenter);
						seatsBlockList.add(seatsBlockRight);
					}

					seatsBlocksList.add(seatsBlockList);
		}
		Collections.reverse(seatsBlocksList);
		setSeatsBlockList(seatsBlocksList);

		// Calculate seats actual x and y coordinates
		List<Amount<Length>> seatsActualXCoordinates = new ArrayList<>();
		List<Amount<Length>> seatsActualYCoordinates = new ArrayList<>();	
		for(int i = 0; i < getNumberOfClasses(); i++) {
			Amount<Length> blockXStart = getSeatsBlocksList().get(i).get(0).getXStart();

			Amount<Length> aisleWidth = Amount.valueOf(0.0, SI.METER);
			Amount<Length> startingYPosition = Amount.valueOf(0.0, SI.METER)
					.minus(Amount.valueOf(
							aircraft.getFuselage().getWidthAtX(blockXStart.doubleValue(SI.METER)),
							SI.METER)
							.divide(2)
							)
					.plus(getDistanceFromWallList().get(i));

			double blocksWidth = getSeatsBlocksList().get(i).stream().mapToDouble(s -> s.getMaxYCoord()).sum();
			aisleWidth = (Amount.valueOf(aircraft.getFuselage().getWidthAtX(blockXStart.doubleValue(SI.METER)), SI.METER)
					.minus(getDistanceFromWallList().get(i).times(2))
					.minus(Amount.valueOf(blocksWidth, SI.METER)))
					.divide(getNumberOfAislesList().get(i));

			Amount<Length> prevBlockWidth = Amount.valueOf(0.0, SI.METER);
			for(int j = 0; j < getSeatsBlocksList().get(i).size(); j++) {			
				SeatsBlock block = getSeatsBlocksList().get(i).get(j);
				Amount<Length> currentYPosition = startingYPosition
						.plus(prevBlockWidth)
						.plus(block.getWidth().divide(2));

				for(int r = 0; r < block.getRowsNumber(); r++) {				
					for(int c = 0; c < block.getColumnsNumber(); c++) {					
						if(block.getSeatsMatrix().getEntry(r, c) == 1.0) {
							seatsActualXCoordinates.add(
									Amount.valueOf(block.getSeatsXCoordsMatrix().getEntry(r, c), SI.METER).minus(
											block.getWidth().divide(2)
											)
									);
							seatsActualYCoordinates.add(
									currentYPosition.plus(
											Amount.valueOf(block.getSeatsYCoordsMatrix().getEntry(r, c), SI.METER)
											)
									);								
						}
					}
				}
				prevBlockWidth = prevBlockWidth.plus(Amount.valueOf(block.getMaxYCoord(), SI.METER));				
				if(j < getSeatsBlocksList().get(i).size() - 1) {
					prevBlockWidth = prevBlockWidth.plus(aisleWidth);
				}
			}
		}
		setSeatsActualXCoordinates(seatsActualXCoordinates);
		setSeatsActualYCoordinates(seatsActualYCoordinates);

		System.out.println("----- CABIN LAYOUT CREATION FINISHED -----");

	}

	private void buildEmergencyExitMap() {

		//........................................
		// Build the emergency exits map
		if(getDesignPassengerNumber() >= 1 & getDesignPassengerNumber() <= 9) {
			getEmergencyExitMap().put(EmergencyExitEnum.TYPE_IV, 2);
		} else if(getDesignPassengerNumber() >= 10 & getDesignPassengerNumber() <= 19) {
			getEmergencyExitMap().put(EmergencyExitEnum.TYPE_III, 2);
		} else if(getDesignPassengerNumber() >= 20 & getDesignPassengerNumber() <= 39) {
			getEmergencyExitMap().put(EmergencyExitEnum.TYPE_II, 2);
			getEmergencyExitMap().put(EmergencyExitEnum.TYPE_III, 2);
		} else if(getDesignPassengerNumber() >= 40 & getDesignPassengerNumber() <= 79) {
			getEmergencyExitMap().put(EmergencyExitEnum.TYPE_I, 2);
			getEmergencyExitMap().put(EmergencyExitEnum.TYPE_III, 2);
		} else if(getDesignPassengerNumber() >= 80 & getDesignPassengerNumber() <= 109) {
			getEmergencyExitMap().put(EmergencyExitEnum.TYPE_I, 2);
			getEmergencyExitMap().put(EmergencyExitEnum.TYPE_III, 4);
		} else if(getDesignPassengerNumber() >= 110 & getDesignPassengerNumber() <= 139) {
			getEmergencyExitMap().put(EmergencyExitEnum.TYPE_I, 4);
			getEmergencyExitMap().put(EmergencyExitEnum.TYPE_III, 2);
		} else if(getDesignPassengerNumber() >= 140 & getDesignPassengerNumber() <= 179) {
			getEmergencyExitMap().put(EmergencyExitEnum.TYPE_I, 4);
			getEmergencyExitMap().put(EmergencyExitEnum.TYPE_III, 4);
		} else if(getDesignPassengerNumber() >= 180 & getDesignPassengerNumber() <= 299) {
			if(getDesignPassengerNumber() >= 180 & getDesignPassengerNumber() <= 192) {
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_I, 4);
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_III, 4);
				getEmergencyExitMap().put(EmergencyExitEnum.VENTRAL, 1);
			} else if(getDesignPassengerNumber() >= 193 & getDesignPassengerNumber() <= 195) {
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_I, 4);
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_III, 4);
				getEmergencyExitMap().put(EmergencyExitEnum.TAILCONE, 1);
			} else if(getDesignPassengerNumber() >= 196 & getDesignPassengerNumber() <= 215) {
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_I, 4);
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_III, 6);
			} else if(getDesignPassengerNumber() >= 216 & getDesignPassengerNumber() <= 220) {
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_I, 4);
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_II, 2);
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_III, 4);			
			} else if(getDesignPassengerNumber() >= 221 & getDesignPassengerNumber() <= 225) {
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_I, 6);
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_III, 4);
			} else if(getDesignPassengerNumber() >= 226 & getDesignPassengerNumber() <= 230) {
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_I, 4);
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_III, 6);
				getEmergencyExitMap().put(EmergencyExitEnum.TAILCONE, 1);
			} else if(getDesignPassengerNumber() >= 231 & getDesignPassengerNumber() <= 235) {
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_I, 4);
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_II, 2);
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_III, 4);
				getEmergencyExitMap().put(EmergencyExitEnum.TAILCONE, 1);
			} else if(getDesignPassengerNumber() >= 231 & getDesignPassengerNumber() <= 235) {
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_I, 6);
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_III, 4);
				getEmergencyExitMap().put(EmergencyExitEnum.TAILCONE, 1);
			} else if(getDesignPassengerNumber() >= 236 & getDesignPassengerNumber() <= 255) {
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_I, 4);
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_II, 2);
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_III, 6);
			} else if(getDesignPassengerNumber() >= 256 & getDesignPassengerNumber() <= 260) {
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_I, 4);
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_II, 4);
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_III, 4);
			} else if(getDesignPassengerNumber() >= 261 & getDesignPassengerNumber() <= 270) {
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_I, 6);
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_III, 6);
			} else if(getDesignPassengerNumber() >= 271 & getDesignPassengerNumber() <= 290) {
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_I, 4);
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_III, 4);
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_A, 2);
			} else {
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_I, 4);
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_III, 4);
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_A, 2);
				getEmergencyExitMap().put(EmergencyExitEnum.TAILCONE, 1);
			}		
		} else {
			if(getDesignPassengerNumber() >= 300 & getDesignPassengerNumber() <= 330) {
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_A, 6);
			} else if(getDesignPassengerNumber() >= 331 & getDesignPassengerNumber() <= 375) {
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_I, 2);
				getEmergencyExitMap().put(EmergencyExitEnum.TYPE_A, 6);
			} else {
				getEmergencyExitMap().put(
						EmergencyExitEnum.TYPE_A, 
						Math.round((float) getDesignPassengerNumber()/110)*2
						);
			}
		}

		//................................................................................
		// Evaluate total emergency exits width (ventral and tailcone types not included)
		setTotalEmergencyExitsWidth(
				Amount.valueOf(
						getEmergencyExitMap().keySet().stream()
						.filter(e -> (!e.equals(EmergencyExitEnum.VENTRAL) & !e.equals(EmergencyExitEnum.TAILCONE)))
						.map(e -> e.getEmergencyExitWidth())
						.mapToDouble(w -> w.doubleValue(SI.METER))
						.sum(), 
						SI.METER
						)
				);					
	}

	private static double[] estimateClassPercentages(
			int designPassengerNumber, 
			int economyAbreast, 
			List<ClassTypeEnum> classesType
			) {

		double[] percentages = new double[3];

		if(designPassengerNumber <= 100) {
			if(economyAbreast < 5) {
				if(classesType.size() == 1) { // Full economy
					percentages[0] = 100.0;
					percentages[1] = 0.0;
					percentages[2] = 0.0;
				} else if(classesType.size() == 2) { // Economy + Business
					percentages[0] = 83.0;
					percentages[1] = 17.0;
					percentages[2] = 0.0;
				} else if(classesType.size() == 3) { // Economy + Business + First
					percentages[0] = 67.5;
					percentages[1] = 20.0;
					percentages[2] = 12.5;
				}
			} else {
				if(classesType.size() == 1) { // Full economy
					percentages[0] = 100.0;
					percentages[1] = 0.0;
					percentages[2] = 0.0;
				} else if(classesType.size() == 2) { // Economy + Business
					percentages[0] = 86.0;
					percentages[1] = 14.0;
					percentages[2] = 0.0;
				} else if(classesType.size() == 3) { // Economy + Business + First
					percentages[0] = 67.5;
					percentages[1] = 20.0;
					percentages[2] = 12.5;
				}
			}
		} else if(designPassengerNumber > 100 && designPassengerNumber <= 250) {
			if(economyAbreast < 7) {
				if(classesType.size() == 1) { // Full economy
					percentages[0] = 100.0;
					percentages[1] = 0.0;
					percentages[2] = 0.0;
				} else if(classesType.size() == 2) { // Economy + Business
					percentages[0] = 87.5;
					percentages[1] = 12.5;
					percentages[2] = 0.0;
				} else if(classesType.size() == 3) { // Economy + Business + First
					percentages[0] = 69.5;
					percentages[1] = 20.5;
					percentages[2] = 10.0;
				}
			} else {
				if(classesType.size() == 1) { // Full economy
					percentages[0] = 100.0;
					percentages[1] = 0.0;
					percentages[2] = 0.0;
				} else if(classesType.size() == 2) { // Economy + Business
					percentages[0] = 88.0;
					percentages[1] = 12.0;
					percentages[2] = 0.0;
				} else if(classesType.size() == 3) { // Economy + Business + First
					percentages[0] = 80.0;
					percentages[1] = 16.0;
					percentages[2] = 4.0;
				}
			}
		} else if(designPassengerNumber > 250) {
			if(economyAbreast < 9) {
				if(classesType.size() == 1) { // Full economy
					percentages[0] = 100.0;
					percentages[1] = 0.0;
					percentages[2] = 0.0;
				} else if(classesType.size() == 2) { // Economy + Business
					percentages[0] = 89.5;
					percentages[1] = 10.5;
					percentages[2] = 0.0;
				} else if(classesType.size() == 3) { // Economy + Business + First
					percentages[0] = 84.0;
					percentages[1] = 13.0;
					percentages[2] = 3.0;
				}
			} else {
				if(classesType.size() == 1) { // Full economy
					percentages[0] = 100.0;
					percentages[1] = 0.0;
					percentages[2] = 0.0;
				} else if(classesType.size() == 2) { // Economy + Business
					percentages[0] = 88.5;
					percentages[1] = 11.5;
					percentages[2] = 0.0;
				} else if(classesType.size() == 3) { // Economy + Business + First
					percentages[0] = 84.0;
					percentages[1] = 13.0;
					percentages[2] = 3.0;
				}
			}
		}

		return percentages;
	}

	private static double[] estimateSeatsWidth(int designPassengerNumber, int economyAbreast, List<ClassTypeEnum> classesType) {

		double[] seatsWidth = new double[3];

		if(designPassengerNumber <= 100) {
			if(economyAbreast < 5) {
				if(classesType.size() == 1) { // Full economy
					seatsWidth[0] = 0.444;
					seatsWidth[1] = 0.0;
					seatsWidth[2] = 0.0;
				} else if(classesType.size() == 2) { // Economy + Business
					seatsWidth[0] = 0.435;
					seatsWidth[1] = 0.468;
					seatsWidth[2] = 0.0;
				} else if(classesType.size() == 3) { // Economy + Business + First
					seatsWidth[0] = 0.447;
					seatsWidth[1] = 0.447;
					seatsWidth[2] = 0.519;
				}
			} else {
				if(classesType.size() == 1) { // Full economy
					seatsWidth[0] = 0.445;
					seatsWidth[1] = 0.0;
					seatsWidth[2] = 0.0;
				} else if(classesType.size() == 2) { // Economy + Business
					seatsWidth[0] = 0.435;
					seatsWidth[1] = 0.465;
					seatsWidth[2] = 0.0;
				} else if(classesType.size() == 3) { // Economy + Business + First
					seatsWidth[0] = 0.447;
					seatsWidth[1] = 0.447;
					seatsWidth[2] = 0.519;
				}
			}
		} else if(designPassengerNumber > 100 && designPassengerNumber <= 250) {
			if(economyAbreast < 7) {
				if(classesType.size() == 1) { // Full economy
					seatsWidth[0] = 0.442;
					seatsWidth[1] = 0.0;
					seatsWidth[2] = 0.0;
				} else if(classesType.size() == 2) { // Economy + Business
					seatsWidth[0] = 0.450;
					seatsWidth[1] = 0.486;
					seatsWidth[2] = 0.0;
				} else if(classesType.size() == 3) { // Economy + Business + First
					seatsWidth[0] = 0.443;
					seatsWidth[1] = 0.444;
					seatsWidth[2] = 0.529;
				}
			} else {
				if(classesType.size() == 1) { // Full economy
					seatsWidth[0] = 0.432;
					seatsWidth[1] = 0.0;
					seatsWidth[2] = 0.0;
				} else if(classesType.size() == 2) { // Economy + Business
					seatsWidth[0] = 0.445;
					seatsWidth[1] = 0.488;
					seatsWidth[2] = 0.527;
				} else if(classesType.size() == 3) { // Economy + Business + First
					seatsWidth[0] = 0.445;
					seatsWidth[1] = 0.529;
					seatsWidth[2] = 0.601;
				}
			}
		} else if(designPassengerNumber > 250) {
			if(economyAbreast < 9) {
				if(classesType.size() == 1) { // Full economy
					seatsWidth[0] = 0.435;
					seatsWidth[1] = 0.0;
					seatsWidth[2] = 0.0;
				} else if(classesType.size() == 2) { // Economy + Business
					seatsWidth[0] = 0.447;
					seatsWidth[1] = 0.540;
					seatsWidth[2] = 0.0;
				} else if(classesType.size() == 3) { // Economy + Business + First
					seatsWidth[0] = 0.444;
					seatsWidth[1] = 0.508;
					seatsWidth[2] = 0.559;
				}
			} else {
				if(classesType.size() == 1) { // Full economy
					seatsWidth[0] = 0.435;
					seatsWidth[1] = 0.0;
					seatsWidth[2] = 0.0;
				} else if(classesType.size() == 2) { // Economy + Business
					seatsWidth[0] = 0.448;
					seatsWidth[1] = 0.559;
					seatsWidth[2] = 0.0;
				} else if(classesType.size() == 3) { // Economy + Business + First
					seatsWidth[0] = 0.432;
					seatsWidth[1] = 0.521;
					seatsWidth[2] = 0.542;
				}
			}
		}

		return seatsWidth;
	}

	private static double[] estimateSeatsPitch(int designPassengerNumber, int economyAbreast, List<ClassTypeEnum> classesType) {

		double[] seatsPitch = new double[3];

		if(designPassengerNumber <= 100) {
			if(economyAbreast < 5) {
				if(classesType.size() == 1) { // Full economy
					seatsPitch[0] = 0.784;
					seatsPitch[1] = 0.0;
					seatsPitch[2] = 0.0;
				} else if(classesType.size() == 2) { // Economy + Business
					seatsPitch[0] = 0.775;
					seatsPitch[1] = 0.830;
					seatsPitch[2] = 0.0;
				} else if(classesType.size() == 3) { // Economy + Business + First
					seatsPitch[0] = 0.784;
					seatsPitch[1] = 0.860;
					seatsPitch[2] = 0.933;
				}
			} else {
				if(classesType.size() == 1) { // Full economy
					seatsPitch[0] = 0.825;
					seatsPitch[1] = 0.0;
					seatsPitch[2] = 0.0;
				} else if(classesType.size() == 2) { // Economy + Business
					seatsPitch[0] = 0.787;
					seatsPitch[1] = 0.914;
					seatsPitch[2] = 0.0;
				} else if(classesType.size() == 3) { // Economy + Business + First
					seatsPitch[0] = 0.784;
					seatsPitch[1] = 0.860;
					seatsPitch[2] = 0.933;
				}
			}
		} else if(designPassengerNumber > 100 && designPassengerNumber <= 250) {
			if(economyAbreast < 7) {
				if(classesType.size() == 1) { // Full economy
					seatsPitch[0] = 0.779;
					seatsPitch[1] = 0.0;
					seatsPitch[2] = 0.0;
				} else if(classesType.size() == 2) { // Economy + Business
					seatsPitch[0] = 0.791;
					seatsPitch[1] = 0.913;
					seatsPitch[2] = 0.0;
				} else if(classesType.size() == 3) { // Economy + Business + First
					seatsPitch[0] = 0.783;
					seatsPitch[1] = 0.879;
					seatsPitch[2] = 1.008;
				}
			} else {
				if(classesType.size() == 1) { // Full economy
					seatsPitch[0] = 0.787;
					seatsPitch[1] = 0.0;
					seatsPitch[2] = 0.0;
				} else if(classesType.size() == 2) { // Economy + Business
					seatsPitch[0] = 0.814;
					seatsPitch[1] = 1.613;
					seatsPitch[2] = 0.0;
				} else if(classesType.size() == 3) { // Economy + Business + First
					seatsPitch[0] = 0.826;
					seatsPitch[1] = 1.770;
					seatsPitch[2] = 2.125;
				}
			}
		} else if(designPassengerNumber > 250) {
			if(economyAbreast < 9) {
				if(classesType.size() == 1) { // Full economy
					seatsPitch[0] = 0.775;
					seatsPitch[1] = 0.0;
					seatsPitch[2] = 0.0;
				} else if(classesType.size() == 2) { // Economy + Business
					seatsPitch[0] = 0.821;
					seatsPitch[1] = 1.606;
					seatsPitch[2] = 0.0; 
				} else if(classesType.size() == 3) { // Economy + Business + First
					seatsPitch[0] = 0.813;
					seatsPitch[1] = 1.524;
					seatsPitch[2] = 2.007;
				}
			} else {
				if(classesType.size() == 1) { // Full economy
					seatsPitch[0] = 0.775;
					seatsPitch[1] = 0.0;
					seatsPitch[2] = 0.0;
				} else if(classesType.size() == 2) { // Economy + Business
					seatsPitch[0] = 0.814;
					seatsPitch[1] = 1.820;
					seatsPitch[2] = 0.0;
				} else if(classesType.size() == 3) { // Economy + Business + First
					seatsPitch[0] = 0.826;
					seatsPitch[1] = 1.524;
					seatsPitch[2] = 1.897;
				}
			}
		}

		return seatsPitch;
	}

	@Override
	public String toString() {

		MyConfiguration.customizeAmountOutput();

		StringBuilder sb = new StringBuilder()
				.append("\t-------------------------------------\n")
				.append("\tConfiguration\n")
				.append("\t-------------------------------------\n")
				.append("\tID: '" + getTheCabinConfigurationBuilder().getId() + "'\n")
				.append("\t.....................................\n")
				.append("\tDesign number of passengers: " + getTheCabinConfigurationBuilder().getDesignPassengerNumber() + "\n")
				.append("\tFlight crew number: " + getTheCabinConfigurationBuilder().getFlightCrewNumber() + "\n")
				.append("\tCabin crew number: " + getCabinCrewNumber() + "\n")
				.append("\tTotal crew number: " + getTotalCrewNumber() + "\n")
				.append("\tClasses type: " + getTheCabinConfigurationBuilder().getClassesType() + "\n")
				.append("\tClasses number: " + getNumberOfClasses() + "\n")
				.append("\tDelta X cabin start: " + getTheCabinConfigurationBuilder().getDeltaXCabinStart() + "\n")
				.append("\tDelta X cabin forward: " + getTheCabinConfigurationBuilder().getDeltaXCabinFwd() + "\n")
				.append("\tDelta X cabin aft: " + getTheCabinConfigurationBuilder().getDeltaXCabinAft() + "\n");

		sb.append("\n\t.....................................\n")
		.append("\tNumber of decks: " + getNumberOfDecks() + "\n")
		.append("\tNumber of columns economy class: " + Arrays.toString(getTheCabinConfigurationBuilder().getNumberOfColumnsEconomyClass()) + "\n")
		.append("\tNumber of columns business class: " + Arrays.toString(getTheCabinConfigurationBuilder().getNumberOfColumnsBusinessClass()) + "\n")
		.append("\tNumber of columns first class: " + Arrays.toString(getTheCabinConfigurationBuilder().getNumberOfColumnsFirstClass()) + "\n")
		.append("\tNumber of rows economy class: " + getTheCabinConfigurationBuilder().getNumberOfRowsEconomyClass() + "\n")
		.append("\tNumber of rows business class: " + getTheCabinConfigurationBuilder().getNumberOfRowsBusinessClass() + "\n")
		.append("\tNumber of rows first class: " + getTheCabinConfigurationBuilder().getNumberOfRowsFirstClass() + "\n")	
		.append("\tNumber of passengers in economy class: " + getTheCabinConfigurationBuilder().getNumberOfPassengersEconomyClass() + "\n")
		.append("\tNumber of passengers in business class: " + getTheCabinConfigurationBuilder().getNumberOfPassengersBusinessClass() + "\n")
		.append("\tNumber of passengers in first class: " + getTheCabinConfigurationBuilder().getNumberOfPassengersFirstClass() + "\n")	
		.append("\tPitch economy class: " + getTheCabinConfigurationBuilder().getPitchEconomyClass() + "\n")
		.append("\tPitch business class: " + getTheCabinConfigurationBuilder().getPitchBusinessClass() + "\n")
		.append("\tPitch first class: " + getTheCabinConfigurationBuilder().getPitchFirstClass() + "\n")
		.append("\tWidth economy class: " + getTheCabinConfigurationBuilder().getWidthEconomyClass() + "\n")
		.append("\tWidth business class: " + getTheCabinConfigurationBuilder().getWidthBusinessClass() + "\n")
		.append("\tWidth first class: " + getTheCabinConfigurationBuilder().getWidthFirstClass() + "\n")
		.append("\tDistance from wall economy class: " + getTheCabinConfigurationBuilder().getDistanceFromWallEconomyClass() + "\n")
		.append("\tDistance from wall business class: " + getTheCabinConfigurationBuilder().getDistanceFromWallBusinessClass() + "\n")
		.append("\tDistance from wall first class: " + getTheCabinConfigurationBuilder().getDistanceFromWallFirstClass() + "\n")
		.append("\t.....................................\n");

		return sb.toString();
	}

	//---------------------------------------------------------------------------------------------------
	// GETTERS & SETTERS

	public File getCabinConfigurationPath() {
		return _cabinConfigurationPath;
	}

	public void setCabinConfigurationPath(File _cabinConfigurationPath) {
		this._cabinConfigurationPath = _cabinConfigurationPath;
	}

	public ICabinConfiguration getTheCabinConfigurationBuilder() {
		return _theCabinConfigurationBuilder;
	}

	public void setTheCabinConfigurationBuilder(ICabinConfiguration _theCabinConfigurationBuilder) {
		this._theCabinConfigurationBuilder = _theCabinConfigurationBuilder;
	}

	public String getId() {
		return _theCabinConfigurationBuilder.getId();
	}

	public void setId (String id) {
		setTheCabinConfigurationBuilder(ICabinConfiguration.Builder.from(_theCabinConfigurationBuilder).setId(id).build());
	}

	public int getDesignPassengerNumber() {
		return _theCabinConfigurationBuilder.getDesignPassengerNumber();
	}

	public void setDesignPassengerNumber (int designPassengerNumber) {
		setTheCabinConfigurationBuilder(ICabinConfiguration.Builder.from(_theCabinConfigurationBuilder).setDesignPassengerNumber(designPassengerNumber).build());
	}

	public int getFlightCrewNumber(){
		return _theCabinConfigurationBuilder.getFlightCrewNumber();
	}

	public void setFlightCrewNumber (int flightCrewNumber) {
		setTheCabinConfigurationBuilder(ICabinConfiguration.Builder.from(_theCabinConfigurationBuilder).setFlightCrewNumber(flightCrewNumber).build());
	}

	public List<ClassTypeEnum> getClassesType(){
		return _theCabinConfigurationBuilder.getClassesType();
	}

	public void setClassesType (List<ClassTypeEnum> classesType) {
		setTheCabinConfigurationBuilder(ICabinConfiguration.Builder.from(_theCabinConfigurationBuilder).addAllClassesType(classesType).build());
	}

	public double getDeltaXCabinStart() {
		return _theCabinConfigurationBuilder.getDeltaXCabinStart();
	}

	public void setDeltaXCabinStart (double deltaXCabinStart) {
		setTheCabinConfigurationBuilder(ICabinConfiguration.Builder.from(_theCabinConfigurationBuilder).setDeltaXCabinStart(deltaXCabinStart).build());
	}

	public double getDeltaXCabinFwd() {
		return _theCabinConfigurationBuilder.getDeltaXCabinFwd();
	}

	public void setDeltaXCabinFwd(double deltaXCabinFwd) {
		setTheCabinConfigurationBuilder(ICabinConfiguration.Builder.from(_theCabinConfigurationBuilder).setDeltaXCabinFwd(deltaXCabinFwd).build());
	}

	public double getDeltaXCabinAft() {
		return _theCabinConfigurationBuilder.getDeltaXCabinAft();
	}

	public void setDeltaXCabinAft(double deltaXCabinAft) {
		setTheCabinConfigurationBuilder(ICabinConfiguration.Builder.from(_theCabinConfigurationBuilder).setDeltaXCabinAft(deltaXCabinAft).build());
	}

	public int getNumberOfDecks() {
		return _numberOfDecks;
	}

	public void setNumberOfDecks(int _numberOfDecks) {
		this._numberOfDecks = _numberOfDecks;
	}

	public int getNumberOfRowsEconomyClass() {
		return _theCabinConfigurationBuilder.getNumberOfRowsEconomyClass();
	}

	public void setNumberOfRowsEconomyClass (int numberOfRowsEconomyClass) {
		setTheCabinConfigurationBuilder(ICabinConfiguration.Builder.from(_theCabinConfigurationBuilder).setNumberOfRowsEconomyClass(numberOfRowsEconomyClass).build());
	}

	public int getNumberOfRowsBusinessClass(){
		return _theCabinConfigurationBuilder.getNumberOfRowsBusinessClass();
	}

	public void setNumberOfRowsBusinessClass (int numberOfRowsBusinessClass) {
		setTheCabinConfigurationBuilder(ICabinConfiguration.Builder.from(_theCabinConfigurationBuilder).setNumberOfRowsBusinessClass(numberOfRowsBusinessClass).build());
	}

	public int getNumberOfRowsFirstClass(){
		return _theCabinConfigurationBuilder.getNumberOfRowsFirstClass();
	}

	public void setNumberOfRowsFirstClass (int numberOfRowsFirstClass) {
		setTheCabinConfigurationBuilder(ICabinConfiguration.Builder.from(_theCabinConfigurationBuilder).setNumberOfRowsFirstClass(numberOfRowsFirstClass).build());
	}

	public int[] getNumberOfColumnsEconomyClass(){
		return _theCabinConfigurationBuilder.getNumberOfColumnsEconomyClass();
	}

	public void setNumberOfColumnsEconomyClass (int[] numberOfColumnsEconomyClass) {
		setTheCabinConfigurationBuilder(ICabinConfiguration.Builder.from(_theCabinConfigurationBuilder).setNumberOfColumnsEconomyClass(numberOfColumnsEconomyClass).build());
	}

	public int[] getNumberOfColumnsBusinessClass(){
		return _theCabinConfigurationBuilder.getNumberOfColumnsBusinessClass();
	}

	public void setNumberOfColumnsBusinessClass (int[] numberOfColumnsBusinessClass) {
		setTheCabinConfigurationBuilder(ICabinConfiguration.Builder.from(_theCabinConfigurationBuilder).setNumberOfColumnsBusinessClass(numberOfColumnsBusinessClass).build());
	}

	public int[] getNumberOfColumnsFirstClass() {
		return _theCabinConfigurationBuilder.getNumberOfColumnsFirstClass();
	}

	public void setNumberOfColumnsFirstClass (int[] numberOfColumnsFirstClass) {
		setTheCabinConfigurationBuilder(ICabinConfiguration.Builder.from(_theCabinConfigurationBuilder).setNumberOfColumnsFirstClass(numberOfColumnsFirstClass).build());
	}

	public double getPercentageEconomyClass() {
		return _theCabinConfigurationBuilder.getPercentageEconomyClass();
	}

	public void setPercentageEconomyClass(double percentageEconomyClass) {
		setTheCabinConfigurationBuilder(ICabinConfiguration.Builder.from(_theCabinConfigurationBuilder).setPercentageEconomyClass(percentageEconomyClass).build());
	}

	public double getPercentageBusinessClass() {
		return _theCabinConfigurationBuilder.getPercentageBusinessClass();
	}

	public void setPercentageBusinessClass(double percentageBusinessClass) {
		setTheCabinConfigurationBuilder(ICabinConfiguration.Builder.from(_theCabinConfigurationBuilder).setPercentageBusinessClass(percentageBusinessClass).build());
	}

	public double getPercentageFirstClass() {
		return _theCabinConfigurationBuilder.getPercentageFirstClass();
	}

	public void setPercentageFirstClass(double percentageFirstClass) {
		setTheCabinConfigurationBuilder(ICabinConfiguration.Builder.from(_theCabinConfigurationBuilder).setPercentageFirstClass(percentageFirstClass).build());
	}

	public Amount<Length> getPitchEconomyClass(){
		return _theCabinConfigurationBuilder.getPitchEconomyClass();
	}

	public void setPitchEconomyClass (Amount<Length> pitchEconomyClass) {
		setTheCabinConfigurationBuilder(ICabinConfiguration.Builder.from(_theCabinConfigurationBuilder).setPitchEconomyClass(pitchEconomyClass).build());
	}

	public Amount<Length> getPitchBusinessClass(){
		return _theCabinConfigurationBuilder.getPitchBusinessClass();
	}

	public void setPitchBusinessClass (Amount<Length> pitchBusinessClass) {
		setTheCabinConfigurationBuilder(ICabinConfiguration.Builder.from(_theCabinConfigurationBuilder).setPitchBusinessClass(pitchBusinessClass).build());
	}

	public Amount<Length> getPitchFirstClass(){
		return _theCabinConfigurationBuilder.getPitchFirstClass();
	}

	public void setPitchFirstClass (Amount<Length> pitchFirstClass) {
		setTheCabinConfigurationBuilder(ICabinConfiguration.Builder.from(_theCabinConfigurationBuilder).setPitchFirstClass(pitchFirstClass).build());
	}

	public Amount<Length> getWidthEconomyClass(){
		return _theCabinConfigurationBuilder.getWidthEconomyClass();
	}

	public void setWidthEconomyClass (Amount<Length> widthEconomyClass) {
		setTheCabinConfigurationBuilder(ICabinConfiguration.Builder.from(_theCabinConfigurationBuilder).setWidthEconomyClass(widthEconomyClass).build());
	}

	public Amount<Length> getWidthBusinessClass(){
		return _theCabinConfigurationBuilder.getWidthBusinessClass();
	}

	public void setWidthBusinessClass (Amount<Length> widthBusinessClass) {
		setTheCabinConfigurationBuilder(ICabinConfiguration.Builder.from(_theCabinConfigurationBuilder).setWidthBusinessClass(widthBusinessClass).build());
	}

	public Amount<Length> getWidthFirstClass(){
		return _theCabinConfigurationBuilder.getWidthFirstClass();
	}

	public void setWidthFirstClass (Amount<Length> widthFirstClass) {
		setTheCabinConfigurationBuilder(ICabinConfiguration.Builder.from(_theCabinConfigurationBuilder).setWidthFirstClass(widthFirstClass).build());
	}

	public Amount<Length> getDistanceFromWallEconomyClass(){
		return _theCabinConfigurationBuilder.getDistanceFromWallEconomyClass();
	}

	public void setDistanceFromWallEconomyClass (Amount<Length> distanceFromWallEconomyClass) {
		setTheCabinConfigurationBuilder(ICabinConfiguration.Builder.from(_theCabinConfigurationBuilder).setDistanceFromWallEconomyClass(distanceFromWallEconomyClass).build());
	}

	public Amount<Length> getDistanceFromWallBusinessClass(){
		return _theCabinConfigurationBuilder.getDistanceFromWallBusinessClass();
	}

	public void setDistanceFromWallBusinessClass (Amount<Length> distanceFromWallBusinessClass) {
		setTheCabinConfigurationBuilder(ICabinConfiguration.Builder.from(_theCabinConfigurationBuilder).setDistanceFromWallBusinessClass(distanceFromWallBusinessClass).build());
	}

	public Amount<Length> getDistanceFromWallFirstClass(){
		return _theCabinConfigurationBuilder.getDistanceFromWallFirstClass();
	}

	public void setDistanceFromWallFirstClass (Amount<Length> distanceFromWallFirstClass) {
		setTheCabinConfigurationBuilder(ICabinConfiguration.Builder.from(_theCabinConfigurationBuilder).setDistanceFromWallFirstClass(distanceFromWallFirstClass).build());
	}

	public List<Amount<Mass>> getCurrentMassList() {
		return _currentMassList;
	}

	public void setCurrentMassList(List<Amount<Mass>> currentMassList) {
		this._currentMassList = currentMassList;
	}

	public int getCabinCrewNumber() {
		return _cabinCrewNumber;
	}

	public void setCabinCrewNumber(int _cabinCrewNumber) {
		this._cabinCrewNumber = _cabinCrewNumber;
	}

	public int getTotalCrewNumber() {
		return _totalCrewNumber;
	}

	public void setTotalCrewNumber(int _totalCrewNumber) {
		this._totalCrewNumber = _totalCrewNumber;
	}

	public List<Amount<Length>> getSeatsCoGFrontToRear() {
		return _seatsCoGFrontToRear;
	}

	public void setSeatsCoGFrontToRear(List<Amount<Length>> _seatsCoGFrontToRear) {
		this._seatsCoGFrontToRear = _seatsCoGFrontToRear;
	}

	public List<Amount<Length>> getSeatsCoGRearToFront() {
		return _seatsCoGRearToFront;
	}

	public void setSeatsCoGRearToFront(List<Amount<Length>> _seatsCoGRearToFront) {
		this._seatsCoGRearToFront = _seatsCoGRearToFront;
	}

	public List<Amount<Length>> getPitchList() {
		return _pitchList;
	}

	public void setPitchList(List<Amount<Length>> _pitchList) {
		this._pitchList = _pitchList;
	}

	public List<Amount<Length>> getWidthList() {
		return _widthList;
	}

	public void setWidthList(List<Amount<Length>> _widthList) {
		this._widthList = _widthList;
	}

	public List<Amount<Length>> getDistanceFromWallList() {
		return _distanceFromWallList;
	}

	public void setDistanceFromWallList(List<Amount<Length>> _distanceFromWallList) {
		this._distanceFromWallList = _distanceFromWallList;
	}

	public List<Integer> getNumberOfPassengersList() {
		return _numberOfPassengersList;
	}

	public void setNumberOfPassengersList(List<Integer> _numberOfPassengersList) {
		this._numberOfPassengersList = _numberOfPassengersList;
	}

	public List<Integer> getNumberOfRowsList() {
		return _numberOfRowsList;
	}

	public void setNumberOfRowsList(List<Integer> _numberOfRowsList) {
		this._numberOfRowsList = _numberOfRowsList;
	}

	public List<int[]> getNumberOfColumnsList() {
		return _numberOfColumnsList;
	}

	public void setNumberOfColumnsList(List<int[]> _numberOfColumnsList) {
		this._numberOfColumnsList = _numberOfColumnsList;
	}

	public List<Integer> getNumberOfAislesList() {
		return _numberOfAislesList;
	}

	public void setNumberOfAislesList(List<Integer> _numberOfAislesList) {
		this._numberOfAislesList = _numberOfAislesList;
	}

	public List<List<SeatsBlock>> getSeatsBlocksList() {
		return _seatsBlocksList;
	}

	public void setSeatsBlockList(List<List<SeatsBlock>> _seatsBlocksList) {
		this._seatsBlocksList = _seatsBlocksList;
	}

	public List<Amount<Length>> getSeatsActualXCoordinates() {
		return _seatsActualXCoordinates;
	}

	public void setSeatsActualXCoordinates(List<Amount<Length>> _seatsActualXCoordinates) {
		this._seatsActualXCoordinates = _seatsActualXCoordinates;
	}

	public List<Amount<Length>> getSeatsActualYCoordinates() {
		return _seatsActualYCoordinates;
	}

	public void setSeatsActualYCoordinates(List<Amount<Length>> _seatsActualYCoordinates) {
		this._seatsActualYCoordinates = _seatsActualYCoordinates;
	}

	public Map<EmergencyExitEnum, Integer> getEmergencyExitMap() {
		return _emergencyExitMap;
	}

	public void setEmergencyExitMap(Map<EmergencyExitEnum, Integer> _emergencyExitMap) {
		this._emergencyExitMap = _emergencyExitMap;
	}

	public Amount<Length> getTotalEmergencyExitsWidth() {
		return _totalEmergencyExitsWidth;
	}

	public void setTotalEmergencyExitsWidth(Amount<Length> _totalEmergencyExitsWidth) {
		this._totalEmergencyExitsWidth = _totalEmergencyExitsWidth;
	}

	public int getNumberOfClasses() {
		return _numberOfClasses;
	}

	public void setNumberOfClasses(int _numberOfClasses) {
		this._numberOfClasses = _numberOfClasses;
	}

}