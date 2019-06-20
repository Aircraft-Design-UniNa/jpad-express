package jpad.core.ex.aircraft.components.powerplant;

import java.io.IOException;

import javax.measure.quantity.Angle;
import javax.measure.quantity.Force;
import javax.measure.quantity.Length;
import javax.measure.quantity.Mass;
import javax.measure.quantity.Power;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.jscience.physics.amount.Amount;

import jpad.configs.ex.MyConfiguration;
import jpad.configs.ex.enumerations.EngineTypeEnum;
import jpad.configs.ex.enumerations.FoldersEnum;
import jpad.configs.ex.enumerations.PowerPlantMountingPositionEnum;
import jpad.core.ex.standaloneutils.JPADXmlReader;
import jpad.core.ex.standaloneutils.MyXMLReaderUtils;
import jpad.core.ex.standaloneutils.database.DatabaseManager;
import jpad.core.ex.standaloneutils.database.databasefunctions.engine.TurbofanCharacteristicsReader;

public class Engine {

	//------------------------------------------------------------------------------
	// VARIABLES DECLARATAION
	private IEngine _theEngineInterface;
	
	private PowerPlantMountingPositionEnum _mountingPoint;
	private Amount<Length> _deltaXApexConstructionAxes; 
	private Amount<Length> _deltaYApexConstructionAxes; 
	private Amount<Length> _deltaZApexConstructionAxes;
	private Amount<Length> _xApexConstructionAxes; 
	private Amount<Length> _yApexConstructionAxes; 
	private Amount<Length> _zApexConstructionAxes;
	private Amount<Angle> _tiltingAngle;
	
	//------------------------------------------------------------------------------
	// BUILDER
	public Engine (IEngine theEngineInterface) {
		
		this._theEngineInterface = theEngineInterface;
		
		if((this._theEngineInterface.getEngineType().equals(EngineTypeEnum.TURBOPROP))
				|| (this._theEngineInterface.getEngineType().equals(EngineTypeEnum.PISTON))) {
			calculateT0FromP0();
		}
		else
			setTheEngineInterface(
					IEngine.Builder.from(_theEngineInterface)
					.setStaticPower(Amount.valueOf(0.0, SI.WATT))
					.build()
					);
	}
	
	//------------------------------------------------------------------------------
	// METHODS
	
	@SuppressWarnings("unchecked")
	public static Engine importFromXML (String pathToXML) throws IOException {
		
		JPADXmlReader reader = new JPADXmlReader(pathToXML);

		System.out.println("Reading systems data ...");
		
		Engine theEngine = null;
		
		String id = MyXMLReaderUtils
				.getXMLPropertyByPath(
						reader.getXmlDoc(), reader.getXpath(),
						"//@id");
		
		String typeProperty = MyXMLReaderUtils
				.getXMLPropertyByPath(
						reader.getXmlDoc(), reader.getXpath(),
						"//@type");
		
		String engineDatabaseName = MyXMLReaderUtils
				.getXMLPropertyByPath(
						reader.getXmlDoc(), reader.getXpath(),
						"//@database");
		
		EngineTypeEnum engineType = null;
		if(typeProperty.equalsIgnoreCase("TURBOJET"))
			engineType = EngineTypeEnum.TURBOJET;
		else if(typeProperty.equalsIgnoreCase("TURBOFAN"))
			engineType = EngineTypeEnum.TURBOFAN;
		else if(typeProperty.equalsIgnoreCase("PISTON"))
			engineType = EngineTypeEnum.PISTON;
		else if(typeProperty.equalsIgnoreCase("TURBOPROP"))
			engineType = EngineTypeEnum.TURBOPROP;
		else {
			System.err.println("\tINVALID ENGINE TYPE!!");
			return null;
		}
		
		if((engineType == EngineTypeEnum.TURBOJET)||(engineType == EngineTypeEnum.TURBOFAN)) {
			
			TurbofanCharacteristicsReader turbofanCharacteristicsReader = null;
			try {
				turbofanCharacteristicsReader = DatabaseManager.initializeTurbofanCharacteristicsDatabase(
						new TurbofanCharacteristicsReader(
								MyConfiguration.getDir(FoldersEnum.DATABASE_DIR),
								"TurbofanCharacterstics.h5"
								),
						MyConfiguration.getDir(FoldersEnum.DATABASE_DIR),
						"TurbofanCharacterstics.h5"
						);
			} catch (InvalidFormatException | IOException e) {
				e.printStackTrace();
			}
			
			//..............................................................................
			// STATIC THRUST
			Amount<Force> staticThrust = null; 
			String staticThrustProperty = reader.getXMLPropertyByPath("//specifications/static_thrust");
			if(staticThrustProperty != null)
				staticThrust = (Amount<Force>) reader.getXMLAmountWithUnitByPath("//specifications/static_thrust");
			
			//..............................................................................
			// BRR
			double bpr = 0.0;
			String bprProperty = reader.getXMLPropertyByPath("//specifications/by_pass_ratio");
			if(bprProperty != null)
				bpr = Double.valueOf(bprProperty);
			
			//..............................................................................
			// LENGTH
			Amount<Length> length = null; 
			String calculateLengthString = MyXMLReaderUtils
					.getXMLPropertyByPath(
							reader.getXmlDoc(), reader.getXpath(),
							"//dimensions/length/@calculate");
			
			if(calculateLengthString.equalsIgnoreCase("TRUE"))
				length = turbofanCharacteristicsReader.getEngineMaxLength(staticThrust, bpr);
			else {
				String lengthProperty = reader.getXMLPropertyByPath("//dimensions/length");
				if(lengthProperty != null)
					length = reader.getXMLAmountLengthByPath("//dimensions/length");
			}
			
			//..............................................................................
			// DRY MASS
			Amount<Mass> dryMass = null;
			String calculateDryMassString = MyXMLReaderUtils
					.getXMLPropertyByPath(
							reader.getXmlDoc(), reader.getXpath(),
							"//specifications/dry_mass/@calculate");
			
			if(calculateDryMassString.equalsIgnoreCase("TRUE"))
				dryMass = turbofanCharacteristicsReader.getEngineDryMass(staticThrust, bpr);
			else {
				String dryMassProperty = reader.getXMLPropertyByPath("//specifications/dry_mass");
				if(dryMassProperty != null)
					dryMass = (Amount<Mass>) reader.getXMLAmountWithUnitByPath("//specifications/dry_mass");
			}

			theEngine = new Engine(
					new IEngine.Builder()
					.setId(id)
					.setEngineType(engineType)
					.setEngineDatabaseName(engineDatabaseName)
					.setLength(length)
					.setStaticThrust(staticThrust)
					.setBpr(bpr)
					.setDryMassPublicDomain(dryMass)
					.buildPartial()
					);
		}
		else if(engineType == EngineTypeEnum.TURBOPROP) {

			//..............................................................................
			// PROPELLER DIAMETER
			Amount<Length> propellerDiameter = null; 
			String propellerDiameterProperty = reader.getXMLPropertyByPath("//dimensions/propeller_diameter");
			if(propellerDiameterProperty != null)
				propellerDiameter = reader.getXMLAmountLengthByPath("//dimensions/propeller_diameter");
			
			//..............................................................................
			// STATIC POWER
			Amount<Power> staticPower = null; 
			String staticPowerProperty = reader.getXMLPropertyByPath("//specifications/static_power");
			if(staticPowerProperty != null)
				staticPower = (Amount<Power>) reader.getXMLAmountWithUnitByPath("//specifications/static_power");
			
			//..............................................................................
			// LENGTH
			Amount<Length> length = null; 
			String calculateLengthString = MyXMLReaderUtils
					.getXMLPropertyByPath(
							reader.getXmlDoc(), reader.getXpath(),
							"//dimensions/length/@calculate");
			
			if(calculateLengthString.equalsIgnoreCase("TRUE"))
				length = Amount.valueOf(
						-(1.28*0.00001*Math.pow(staticPower.doubleValue(NonSI.HORSEPOWER),2))
						+ (9.273*0.01*staticPower.doubleValue(NonSI.HORSEPOWER))
						- 8.3456,
						NonSI.INCH).to(SI.METER);
			else {
				String lengthProperty = reader.getXMLPropertyByPath("//dimensions/length");
				if(lengthProperty != null)
					length = reader.getXMLAmountLengthByPath("//dimensions/length");
			}
			
			//..............................................................................
			// DRY MASS
			Amount<Mass> dryMass = null;
			String calculateDryMassString = MyXMLReaderUtils
					.getXMLPropertyByPath(
							reader.getXmlDoc(), reader.getXpath(),
							"//specifications/dry_mass/@calculate");
			
			if(calculateDryMassString.equalsIgnoreCase("TRUE")) {
				if(staticPower.doubleValue(NonSI.HORSEPOWER)*2.8 < 10000)
					dryMass = Amount.valueOf(
							Amount.valueOf(
									Math.pow(
											0.4054*staticPower.doubleValue(NonSI.HORSEPOWER)*2.8,
											0.9255
											),
									NonSI.POUND_FORCE)
							.to(SI.NEWTON)
							.divide(9.81)
							.getEstimatedValue(),
							SI.KILOGRAM
							);
				else
					dryMass = Amount.valueOf(
							Amount.valueOf(
									Math.pow(
											0.616*staticPower.doubleValue(NonSI.HORSEPOWER)*2.8,
											0.886
											),
									NonSI.POUND_FORCE)
							.to(SI.NEWTON)
							.divide(9.81)
							.getEstimatedValue(),
							SI.KILOGRAM
							);
			}
			else {
				String dryMassProperty = reader.getXMLPropertyByPath("//specifications/dry_mass");
				if(dryMassProperty != null)
					dryMass = (Amount<Mass>) reader.getXMLAmountWithUnitByPath("//specifications/dry_mass");
			}
			
			//..............................................................................
			// NUMBER OF PROPERLLER BLADES 
			Integer numberOfPropellerBlades = null;
			String numberOfPropellerBladesProperty = reader.getXMLPropertyByPath("//specifications/number_of_propeller_blades");
			if(numberOfPropellerBladesProperty != null)
				numberOfPropellerBlades = Integer.valueOf(numberOfPropellerBladesProperty);
			
			//..............................................................................
			// PROPERLLER EFFICIENCY 
			Double etaPropeller = null;
			String etaPropellerProperty = reader.getXMLPropertyByPath("//specifications/eta_propeller");
			if(etaPropellerProperty != null)
				etaPropeller = Double.valueOf(etaPropellerProperty);
			
			theEngine = new Engine(
					new IEngine.Builder()
					.setId(id)
					.setEngineType(engineType)
					.setEngineDatabaseName(engineDatabaseName)
					.setLength(length)
					.setPropellerDiameter(propellerDiameter)
					.setNumberOfBlades(numberOfPropellerBlades)
					.setEtaPropeller(etaPropeller)
					.setStaticPower(staticPower)
					.setDryMassPublicDomain(dryMass)
					.buildPartial()
					);
			
		}
		else if(engineType == EngineTypeEnum.PISTON) {

			//..............................................................................
			// PROPELLER DIAMETER
			Amount<Length> propellerDiameter = null; 
			String propellerDiameterProperty = reader.getXMLPropertyByPath("//dimensions/propeller_diameter");
			if(propellerDiameterProperty != null)
				propellerDiameter = reader.getXMLAmountLengthByPath("//dimensions/propeller_diameter");
			
			//..............................................................................
			// STATIC POWER
			Amount<Power> staticPower = null; 
			String staticPowerProperty = reader.getXMLPropertyByPath("//specifications/static_power");
			if(staticPowerProperty != null)
				staticPower = (Amount<Power>) reader.getXMLAmountWithUnitByPath("//specifications/static_power");
			
			//..............................................................................
			// LENGTH
			Amount<Length> length = null; 
			String calculateLengthString = MyXMLReaderUtils
					.getXMLPropertyByPath(
							reader.getXmlDoc(), reader.getXpath(),
							"//dimensions/length/@calculate");

			if(calculateLengthString.equalsIgnoreCase("TRUE"))
				length = Amount.valueOf(
						4*10e-10*Math.pow(staticPower.doubleValue(NonSI.HORSEPOWER),4)
						- 6*10e-7*Math.pow(staticPower.doubleValue(NonSI.HORSEPOWER),3)
						+ 8*10e-5*Math.pow(staticPower.doubleValue(NonSI.HORSEPOWER),2)
						- 0.2193*staticPower.doubleValue(NonSI.HORSEPOWER)
						+ 54.097,
						NonSI.INCH).to(SI.METER);
			else {
				String lengthProperty = reader.getXMLPropertyByPath("//dimensions/length");
				if(lengthProperty != null)
					length = reader.getXMLAmountLengthByPath("//dimensions/length");
			}
			
			//..............................................................................
			// DRY MASS
			Amount<Mass> dryMass = null;
			String calculateDryMassString = MyXMLReaderUtils
					.getXMLPropertyByPath(
							reader.getXmlDoc(), reader.getXpath(),
							"//specifications/dry_mass/@calculate");
			
			if(calculateDryMassString.equalsIgnoreCase("TRUE")) {
				if(staticPower.doubleValue(NonSI.HORSEPOWER)*2.8 < 10000)
					dryMass = Amount.valueOf(
							Amount.valueOf(
									Math.pow(
											0.4054*staticPower.doubleValue(NonSI.HORSEPOWER)*2.8,
											0.9255
											),
									NonSI.POUND_FORCE)
							.to(SI.NEWTON)
							.divide(9.81)
							.getEstimatedValue(),
							SI.KILOGRAM
							);
				else
					dryMass = Amount.valueOf(
							Amount.valueOf(
									Math.pow(
											0.616*staticPower.doubleValue(NonSI.HORSEPOWER)*2.8,
											0.886
											),
									NonSI.POUND_FORCE)
							.to(SI.NEWTON)
							.divide(9.81)
							.getEstimatedValue(),
							SI.KILOGRAM
							);
			}
			else {
				String dryMassProperty = reader.getXMLPropertyByPath("//specifications/dry_mass");
				if(dryMassProperty != null)
					dryMass = (Amount<Mass>) reader.getXMLAmountWithUnitByPath("//specifications/dry_mass");
			}
			
			//..............................................................................
			// NUMBER OF PROPERLLER BLADES 
			Integer numberOfPropellerBlades = null;
			String numberOfPropellerBladesProperty = reader.getXMLPropertyByPath("//specifications/number_of_propeller_blades");
			if(numberOfPropellerBladesProperty != null)
				numberOfPropellerBlades = Integer.valueOf(numberOfPropellerBladesProperty);
			
			//..............................................................................
			// PROPERLLER EFFICIENCY 
			Double etaPropeller = null;
			String etaPropellerProperty = reader.getXMLPropertyByPath("//specifications/eta_propeller");
			if(etaPropellerProperty != null)
				etaPropeller = Double.valueOf(etaPropellerProperty);
			
			theEngine = new Engine(
					new IEngine.Builder()
					.setId(id)
					.setEngineType(engineType)
					.setEngineDatabaseName(engineDatabaseName)
					.setLength(length)
					.setPropellerDiameter(propellerDiameter)
					.setNumberOfBlades(numberOfPropellerBlades)
					.setEtaPropeller(etaPropeller)
					.setStaticPower(staticPower)
					.setDryMassPublicDomain(dryMass)
					.buildPartial()
					);
			
		}
		
		return theEngine;
	}
	
	private void calculateT0FromP0 () {
		
		// this is the max. static thrust 
		setT0(
				Amount.valueOf(
						_theEngineInterface.getStaticPower().doubleValue(NonSI.HORSEPOWER)*2.8,
						NonSI.POUND_FORCE
						)
				);
		
	}
	
	@Override
	public String toString() {
		
		MyConfiguration.customizeAmountOutput();

		StringBuilder sb = new StringBuilder()
				.append("\tID: '" + _theEngineInterface.getId() + "'\n")
				.append("\tType: " + _theEngineInterface.getEngineType() + "\n")
				.append("\tEngine database name: " + _theEngineInterface.getEngineDatabaseName() + "\n")
				.append("\t...............................................................................................................\n")
				.append("\tLength: " + _theEngineInterface.getLength() + "\n")
				;
		if(_theEngineInterface.getEngineType() == EngineTypeEnum.TURBOPROP) 
			sb.append("\tPropeller diameter: " + _theEngineInterface.getPropellerDiameter() + "\n")
			.append("\tNumber of blades: " + _theEngineInterface.getNumberOfBlades() + "\n")
			;
		else if(_theEngineInterface.getEngineType() == EngineTypeEnum.PISTON) 
			sb.append("\tPropeller diameter: " + _theEngineInterface.getPropellerDiameter() + "\n")
			.append("\tNumber of blades: " + _theEngineInterface.getNumberOfBlades() + "\n")
			;
		
		sb.append("\t...............................................................................................................\n");
		
		if((_theEngineInterface.getEngineType() == EngineTypeEnum.TURBOFAN) 
				|| (_theEngineInterface.getEngineType() == EngineTypeEnum.TURBOJET))
			sb.append("\tT0: " + _theEngineInterface.getStaticThrust() + "\n")
			.append("\tBPR: " + _theEngineInterface.getBpr() + "\n")
			;
		else if((_theEngineInterface.getEngineType() == EngineTypeEnum.PISTON) 
				|| (_theEngineInterface.getEngineType() == EngineTypeEnum.TURBOPROP))
			sb.append("\tP0: " + _theEngineInterface.getStaticPower().to(NonSI.HORSEPOWER) + "\n")
			.append("\tT0: " + _theEngineInterface.getStaticThrust().to(NonSI.POUND_FORCE) + "\n")
			;
		sb.append("\t...............................................................................................................\n");
		;
		
		return sb.toString();
	}
	
	//------------------------------------------------------------------------------
	// GETTERS & SETTERS
	public IEngine getTheEngineInterface() {
		return _theEngineInterface;
	}
	
	public void setTheEngineInterface (IEngine theEngineInterface) {
		this._theEngineInterface = theEngineInterface;
	}
	
	public String getId() {
		return _theEngineInterface.getId();
	}
	
	public void setId(String id) {
		setTheEngineInterface(IEngine.Builder.from(_theEngineInterface).setId(id).build());
	}

	public EngineTypeEnum getEngineType() {
		return _theEngineInterface.getEngineType();
	}
	
	public Amount<Length> getXApexConstructionAxes() {
		return _xApexConstructionAxes;
	}

	public void setXApexConstructionAxes(Amount<Length> _X0) {
		this._xApexConstructionAxes = _X0;
	}

	public Amount<Length> getYApexConstructionAxes() {
		return _yApexConstructionAxes;
	}

	public void setYApexConstructionAxes(Amount<Length> _Y0) {
		this._yApexConstructionAxes = _Y0;
	}

	public Amount<Length> getZApexConstructionAxes() {
		return _zApexConstructionAxes;
	}
	
	public void setZApexConstructionAxes(Amount<Length> _Z0) {
		this._zApexConstructionAxes = _Z0;
	}

	public Amount<Angle> getTiltingAngle() {
		return _tiltingAngle;
	}
	
	public void setTiltingAngle(Amount<Angle> _muT) {
		this._tiltingAngle = _muT;
	}
	
	public void setEngineType(EngineTypeEnum _engineType) {
		setTheEngineInterface(IEngine.Builder.from(_theEngineInterface).setEngineType(_engineType).build());
	}
	
	public PowerPlantMountingPositionEnum getMountingPosition() {
		return _mountingPoint;
	}
	
	public void setMountingPosition(PowerPlantMountingPositionEnum _position) {
		this._mountingPoint = _position;
	}
	
	public Amount<Length> getLength() {
		return _theEngineInterface.getLength();
	}
	
	public void setLength(Amount<Length> _length) {
		setTheEngineInterface(IEngine.Builder.from(_theEngineInterface).setLength(_length).build());
	}

	public Amount<Power> getP0() {
		return _theEngineInterface.getStaticPower();
	}

	public void setP0(Amount<Power> _p0) {
		setTheEngineInterface(IEngine.Builder.from(_theEngineInterface).setStaticPower(_p0).build());
	}

	public Amount<Force> getT0() {
		return _theEngineInterface.getStaticThrust();
	}
	
	public void setT0(Amount<Force> _t0) {
		setTheEngineInterface(IEngine.Builder.from(_theEngineInterface).setStaticThrust(_t0).build());
	}
	
	public double getBPR() {
		return _theEngineInterface.getBpr();
	}
	
	public void setBPR(double _BPR) {
		setTheEngineInterface(IEngine.Builder.from(_theEngineInterface).setBpr(_BPR).build());
	}

	public Amount<Mass> getDryMassPublicDomain() {
		return _theEngineInterface.getDryMassPublicDomain();
	}

	public void setDryMassPublicDomain (Amount<Mass> dryMassPublicDomain) {
		setTheEngineInterface(IEngine.Builder.from(_theEngineInterface).setDryMassPublicDomain(dryMassPublicDomain).build());
	}
	
	public Amount<Length> getPropellerDiameter() {
		return _theEngineInterface.getPropellerDiameter();
	}
	
	public void setPropellerDiameter(Amount<Length> _propellerDiameter) {
		setTheEngineInterface(IEngine.Builder.from(_theEngineInterface).setPropellerDiameter(_propellerDiameter).build());
	}
	
	public int getNumberOfBlades() {
		return _theEngineInterface.getNumberOfBlades();
	}
	
	public void setNumberOfBlades(int _nBlades) {
		setTheEngineInterface(IEngine.Builder.from(_theEngineInterface).setNumberOfBlades(_nBlades).build());
	}

	public double getEtaPropeller() {
		return _theEngineInterface.getEtaPropeller();
	}

	public void setEtaPropeller(double _etaPropeller) {
		setTheEngineInterface(IEngine.Builder.from(_theEngineInterface).setEtaPropeller(_etaPropeller).build());
	}
	
	public String getEngineDatabaseName() {
		return _theEngineInterface.getEngineDatabaseName();
	}
	
	public void setEngineDatabaseName(String _engineDatabaseName) {
		setTheEngineInterface(IEngine.Builder.from(_theEngineInterface).setEngineDatabaseName(_engineDatabaseName).build());
	}

	public Amount<Length> getDeltaXApexConstructionAxes() {
		return _deltaXApexConstructionAxes;
	}

	public void setDeltaXApexConstructionAxes(Amount<Length> _deltaXApexConstructionAxes) {
		this._deltaXApexConstructionAxes = _deltaXApexConstructionAxes;
	}

	public Amount<Length> getDeltaYApexConstructionAxes() {
		return _deltaYApexConstructionAxes;
	}

	public void setDeltaYApexConstructionAxes(Amount<Length> _deltaYApexConstructionAxes) {
		this._deltaYApexConstructionAxes = _deltaYApexConstructionAxes;
	}

	public Amount<Length> getDeltaZApexConstructionAxes() {
		return _deltaZApexConstructionAxes;
	}

	public void setDeltaZApexConstructionAxes(Amount<Length> _deltaZApexConstructionAxes) {
		this._deltaZApexConstructionAxes = _deltaZApexConstructionAxes;
	}

}
