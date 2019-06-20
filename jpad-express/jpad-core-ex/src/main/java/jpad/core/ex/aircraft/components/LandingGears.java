package jpad.core.ex.aircraft.components;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.analysis.solvers.AllowedSolution;
import org.jscience.physics.amount.Amount;

import jpad.configs.ex.MyConfiguration;
import jpad.configs.ex.enumerations.LandingGearsMountingPositionEnum;
import jpad.core.ex.aircraft.Aircraft;
import jpad.core.ex.standaloneutils.JPADXmlReader;
import jpad.core.ex.standaloneutils.MyArrayUtils;
import jpad.core.ex.standaloneutils.MyMathUtils;
import jpad.core.ex.standaloneutils.MyXMLReaderUtils;

public class LandingGears {

	//------------------------------------------------------------------------------------------
	// VARIABLE DECLARATION
	private ILandingGear _theLandingGearInterface;
	private LandingGearsMountingPositionEnum _mountingPosition;
	private double _deltaXApexConstructionAxesNoseGear;
	private double _deltaXApexConstructionAxesMainGear; 
	private Amount<Length> _xApexConstructionAxesNoseGear; 
	private Amount<Length> _yApexConstructionAxesNoseGear; 
	private Amount<Length> _zApexConstructionAxesNoseGear;
	private Amount<Length> _xApexConstructionAxesMainGear; 
	private Amount<Length> _yApexConstructionAxesMainGear; 
	private Amount<Length> _zApexConstructionAxesMainGear;
	private Amount<Length> _mainLegLength;
	private Amount<Length> _noseLegLength;
	private Amount<Length> _wheelbase;
	
	
	//------------------------------------------------------------------------------------------
	// BUILDER
	public LandingGears (ILandingGear theLandingGearsInterface) {
		
		this._theLandingGearInterface = theLandingGearsInterface;
	}

	//------------------------------------------------------------------------------------------
	// METHODS
	
	public static LandingGears importFromXML (String pathToXML) {
		
		JPADXmlReader reader = new JPADXmlReader(pathToXML);

		System.out.println("Reading landing gears data ...");
		
		String id = MyXMLReaderUtils
				.getXMLPropertyByPath(
						reader.getXmlDoc(), reader.getXpath(),
						"//@id");
		
		String isRetractableProperty = MyXMLReaderUtils
				.getXMLPropertyByPath(
						reader.getXmlDoc(), reader.getXpath(),
						"//@retractable");
		boolean isRetractable = false;
		if(isRetractableProperty.equalsIgnoreCase("TRUE"))
			isRetractable = true;
		
		//---------------------------------------------------------------
		// GLOBAL DATA
		Amount<Length> wheeltrack= Amount.valueOf(0.0, SI.METER);  // default value
		double gearCompressionFactor = 0.0; // default value
		Amount<Angle> noseWheelSteeringAngle = Amount.valueOf(60.0, NonSI.DEGREE_ANGLE);  // default value
		int numberOfFrontalWheels = 0;
		int numberOfRearWheels = 0;
		
		String gearCompressionFactorProperty = reader.getXMLPropertyByPath("//global_data/gear_compression_factor");
		if(gearCompressionFactorProperty != null)
			gearCompressionFactor = Double.valueOf(gearCompressionFactorProperty);
		
		String wheeltrackProperty = reader.getXMLPropertyByPath("//global_data/wheeltrack");
		if(wheeltrackProperty != null)
			wheeltrack = reader.getXMLAmountLengthByPath("//global_data/wheeltrack");
		
		String noseWheelSteeringAngleProperty = reader.getXMLPropertyByPath("//global_data/nose_wheel_steering_angle");
		if(noseWheelSteeringAngleProperty != null)
			noseWheelSteeringAngle = reader.getXMLAmountAngleByPath("//global_data/nose_wheel_steering_angle");
		
		String numberOfFrontalWheelsProperty = reader.getXMLPropertyByPath("//global_data/number_of_frontal_wheels");
		if(numberOfFrontalWheelsProperty != null)
			numberOfFrontalWheels = Integer.valueOf(numberOfFrontalWheelsProperty);
		
		String numberOfRearWheelsProperty = reader.getXMLPropertyByPath("//global_data/number_of_rear_wheels");
		if(numberOfRearWheelsProperty != null)
			numberOfRearWheels = Integer.valueOf(numberOfRearWheelsProperty);
		
		//---------------------------------------------------------------
		// FRONTAL WHEEL DATA
		Amount<Length> frontalWheelsHeight = Amount.valueOf(0.0, SI.METER);
		Amount<Length> frontalWheelsWidth = Amount.valueOf(0.0, SI.METER);
		
		String frontalWheelsHeightProperty = reader.getXMLPropertyByPath("//frontal_wheels_data/wheel_height");
		if(frontalWheelsHeightProperty != null)
			frontalWheelsHeight = reader.getXMLAmountLengthByPath("//frontal_wheels_data/wheel_height");
		
		String frontalWheelsWidthProperty = reader.getXMLPropertyByPath("//frontal_wheels_data/wheel_width");
		if(frontalWheelsWidthProperty != null)
			frontalWheelsWidth = reader.getXMLAmountLengthByPath("//frontal_wheels_data/wheel_width");
		
		//---------------------------------------------------------------
		// REAR WING DATA
		Amount<Length> rearWheelsHeight = Amount.valueOf(0.0, SI.METER);
		Amount<Length> rearWheelsWidth = Amount.valueOf(0.0, SI.METER);
		
		String rearWheelsHeightProperty = reader.getXMLPropertyByPath("//rear_wheels_data/wheel_height");
		if(rearWheelsHeightProperty != null)
			rearWheelsHeight = reader.getXMLAmountLengthByPath("//rear_wheels_data/wheel_height");
		
		String rearWheelsWidthProperty = reader.getXMLPropertyByPath("//rear_wheels_data/wheel_width");
		if(rearWheelsWidthProperty != null)
			rearWheelsWidth = reader.getXMLAmountLengthByPath("//rear_wheels_data/wheel_width");
		
		LandingGears landingGears = new LandingGears(
				new ILandingGear.Builder()
				.setId(id)
				.setRetractable(isRetractable)
				.setGearCompressionFactor(gearCompressionFactor)
				.setWheeltrack(wheeltrack)
				.setNoseWheelSteeringAngle(noseWheelSteeringAngle)
				.setNumberOfFrontalWheels(numberOfFrontalWheels)
				.setNumberOfRearWheels(numberOfRearWheels)
				.setFrontalWheelsHeight(frontalWheelsHeight)
				.setFrontalWheelsWidth(frontalWheelsWidth)
				.setRearWheelsHeight(rearWheelsHeight)
				.setRearWheelsWidth(rearWheelsWidth)
				.build()
				);
		
		return landingGears;
		
	}
	
	@Override
	public String toString() {
		
		MyConfiguration.customizeAmountOutput();

		StringBuilder sb = new StringBuilder()
				.append("\t-------------------------------------\n")
				.append("\tLanding gears\n")
				.append("\t-------------------------------------\n")
				.append("\tID: '" + _theLandingGearInterface.getId() + "'\n")
				.append("\t·····································\n")
				.append("\tRetractable: " + _theLandingGearInterface.isRetractable() + "\n")
				.append("\t·····································\n")
				.append("\tWheel base: " + _wheelbase + "\n")
				.append("\tWheel track: " + _theLandingGearInterface.getWheeltrack() + "\n")
				.append("\tMain gear legs length: " + _mainLegLength + "\n")
				.append("\tNose gear legs length: " + _noseLegLength + "\n")
				.append("\tMain gear compression factor : " + _theLandingGearInterface.getGearCompressionFactor() + "\n")
				.append("\tNose wheel steering angle: " + _theLandingGearInterface.getNoseWheelSteeringAngle() + "\n")
				.append("\tNumber of frontal wheels: " + _theLandingGearInterface.getNumberOfFrontalWheels() + "\n")
				.append("\tNumber of rear wheels: " + _theLandingGearInterface.getNumberOfRearWheels() + "\n")
				.append("\t·····································\n")
				.append("\tFrontal wheels height: " + _theLandingGearInterface.getFrontalWheelsHeight() + "\n")
				.append("\tFrontal wheels width: " + _theLandingGearInterface.getFrontalWheelsWidth() + "\n")
				.append("\t·····································\n")
				.append("\tRear wheels height: " + _theLandingGearInterface.getRearWheelsHeight() + "\n")
				.append("\tRear wheels width: " + _theLandingGearInterface.getRearWheelsWidth() + "\n")
				.append("\t·····································\n")
				;
		
		return sb.toString();
		
	}
	
	public void calcuateDependentData(Aircraft aircraft) {
		
		calculateMainLegLength(aircraft);
		calculateNosePosition(aircraft);
		calculateNoseLegLength(aircraft);
		
	}
	
	private void calculateMainLegLength(Aircraft aircraft) {
		
		// calculation of the initial point (xu,zu) from intersection of:
		// - horiz. line at 0.26 of fuselage height (d_C) - taken from the bottom-line
		// - lower profile of the tail sideview
		//
		// Using Java 8 features

		// x at l_N + l_C
		double x0 = aircraft.getFuselage().getNoseLength().doubleValue(SI.METER) 
				+ aircraft.getFuselage().getCylinderLength().doubleValue(SI.METER);

		// values filtered as x >= l_N + l_C
		List<Double> vX = new ArrayList<Double>();
		aircraft.getFuselage().getOutlineXZLowerCurveX().stream().filter(x -> x > x0 ).distinct().forEach(vX::add);

		// index of first x in _outlineXZLowerCurveX >= x0
		int idxX0 = IntStream.range(0, aircraft.getFuselage().getOutlineXZLowerCurveX().size())
	            .reduce((i,j) -> aircraft.getFuselage().getOutlineXZLowerCurveX().get(i) > x0 ? i : j)
	            .getAsInt();  // or throw

		// the coupled z-values
		List<Double> vZ = new ArrayList<Double>();
		vZ = IntStream.range(0, aircraft.getFuselage().getOutlineXZLowerCurveZ().size()).filter(i -> i >= idxX0)
			 .mapToObj(i -> aircraft.getFuselage().getOutlineXZLowerCurveZ().get(i)).distinct()
	         .collect(Collectors.toList());

		// generate a vector of constant z = z_min + 0.26*d_C, same size of vZ, or vX
		double z1 = vZ.get(0) + 0.26* aircraft.getFuselage().getSectionCylinderHeight().doubleValue(SI.METER);
		List<Double> vZ1 = new ArrayList<Double>();
		vZ.stream().map(z -> z1).forEach(vZ1::add);

		double xu = MyArrayUtils.intersectArraysBrent(
				ArrayUtils.toPrimitive(vX.toArray(new Double[vX.size()])),
				ArrayUtils.toPrimitive(vZ.toArray(new Double[vZ.size()])),
				ArrayUtils.toPrimitive(vZ1.toArray(new Double[vZ1.size()])),
				vX.get(0), vX.get(vX.size()-1),
				AllowedSolution.ANY_SIDE);

		//-------------------------------------------------------------------------------------------------------
		// calculation of the main leg length based on upsweep angle
		_mainLegLength = Amount.valueOf( 
				(xu - _xApexConstructionAxesMainGear.doubleValue(SI.METER)) 
				* Math.tan(aircraft.getFuselage().getUpsweepAngle().doubleValue(SI.RADIAN)),
				SI.METER
				).times(1 - _theLandingGearInterface.getGearCompressionFactor());
		
	}
	
	private void calculateNosePosition(Aircraft aircraft) {
		
		_xApexConstructionAxesNoseGear = aircraft.getFuselage().getFuselageLength().to(SI.METER).times(_deltaXApexConstructionAxesNoseGear);
		_yApexConstructionAxesNoseGear = _yApexConstructionAxesMainGear;
		_zApexConstructionAxesNoseGear = Amount.valueOf( 
				MyMathUtils.getInterpolatedValue1DLinear(
						aircraft.getFuselage().getOutlineXZLowerCurveAmountX().stream().mapToDouble(x -> x.doubleValue(SI.METER)).toArray(),
						aircraft.getFuselage().getOutlineXZLowerCurveAmountZ().stream().mapToDouble(z -> z.doubleValue(SI.METER)).toArray(),
						_xApexConstructionAxesNoseGear.doubleValue(SI.METER)
						),
				SI.METER
				);
		
		_wheelbase = _xApexConstructionAxesMainGear.to(SI.METER).minus(_xApexConstructionAxesNoseGear.to(SI.METER));
		
	}
	
	private void calculateNoseLegLength(Aircraft aircraft) {
	
		Amount<Length> fuselageHeightFromGround = Amount.valueOf( 
				Math.abs(
						Math.abs(aircraft.getLandingGears().getZApexConstructionAxesMainGear().doubleValue(SI.METER)
								- _mainLegLength.doubleValue(SI.METER)
								- _theLandingGearInterface.getRearWheelsHeight().divide(2).doubleValue(SI.METER)
								)
						+ MyMathUtils.getInterpolatedValue1DLinear(
								aircraft.getFuselage().getOutlineXZLowerCurveAmountX().stream().mapToDouble(x -> x.doubleValue(SI.METER)).toArray(),
								aircraft.getFuselage().getOutlineXZLowerCurveAmountZ().stream().mapToDouble(z -> z.doubleValue(SI.METER)).toArray(),
								_xApexConstructionAxesMainGear.doubleValue(SI.METER)
								)
						),
				SI.METER
				);
		
		aircraft.getFuselage().setHeightFromGround(fuselageHeightFromGround);
		
		_noseLegLength = fuselageHeightFromGround.to(SI.METER).minus(
				_theLandingGearInterface.getFrontalWheelsHeight().divide(2).to(SI.METER)
				);
		
	}
	
	//------------------------------------------------------------------------------------------
	// GETTERS & SETTERS
	
	public ILandingGear getTheLandingGearsInterface() {
		return _theLandingGearInterface;
	}
	
	public boolean isRetractable() {
		return _theLandingGearInterface.isRetractable();
	}
	
	public Amount<Angle> getNoseWheelSteeringAngle() {
		return _theLandingGearInterface.getNoseWheelSteeringAngle();
	}
	
	public void setNoseWheelSteeringAngle(Amount<Angle> noseWheelSteeringAngle) {
		setTheLandingGearsInterface(ILandingGear.Builder.from(_theLandingGearInterface).setNoseWheelSteeringAngle(noseWheelSteeringAngle).build());
	}
	
	public void setTheLandingGearsInterface (ILandingGear theLandingGearsInterface) {
		this._theLandingGearInterface = theLandingGearsInterface;
	}
	
	public String getId() {
		return _theLandingGearInterface.getId();
	}

	public void setId (String id) {
		setTheLandingGearsInterface(ILandingGear.Builder.from(_theLandingGearInterface).setId(id).build());
	}
	
	public int getNumberOfFrontalWheels() {
		return _theLandingGearInterface.getNumberOfFrontalWheels();
	}

	public int getNumberOfRearWheels() {
		return _theLandingGearInterface.getNumberOfRearWheels();
	}

	public Amount<Length> getFrontalWheelsHeight() {
		return _theLandingGearInterface.getFrontalWheelsHeight();
	}

	public Amount<Length> getFrontalWheelsWidth() {
		return _theLandingGearInterface.getFrontalWheelsWidth();
	}

	public Amount<Length> getRearWheelsHeight() {
		return _theLandingGearInterface.getRearWheelsHeight();
	}

	public Amount<Length> getRearWheelsWidth() {
		return _theLandingGearInterface.getRearWheelsWidth();
	}

	public void setNumberOfFrontalWheels(int _numberOfFrontalWheels) {
		setTheLandingGearsInterface(ILandingGear.Builder.from(_theLandingGearInterface).setNumberOfFrontalWheels(_numberOfFrontalWheels).build());
	}

	public void setNumberOfRearWheels(int _numberOfRearWheels) {
		setTheLandingGearsInterface(ILandingGear.Builder.from(_theLandingGearInterface).setNumberOfRearWheels(_numberOfRearWheels).build());
	}

	public void setFrontalWheelsHeight(Amount<Length> _frontalWheelsHeight) {
		setTheLandingGearsInterface(ILandingGear.Builder.from(_theLandingGearInterface).setFrontalWheelsHeight(_frontalWheelsHeight).build());
	}

	public void setFrontalWheelsWidth(Amount<Length> _frontalWheelsWidth) {
		setTheLandingGearsInterface(ILandingGear.Builder.from(_theLandingGearInterface).setFrontalWheelsWidth(_frontalWheelsWidth).build());
	}

	public void setRearWheelsHeight(Amount<Length> _rearWheelsHeight) {
		setTheLandingGearsInterface(ILandingGear.Builder.from(_theLandingGearInterface).setRearWheelsHeight(_rearWheelsHeight).build());
	}

	public void setRearWheelsWidth(Amount<Length> _rearWheelsWidth) {
		setTheLandingGearsInterface(ILandingGear.Builder.from(_theLandingGearInterface).setRearWheelsWidth(_rearWheelsWidth).build());
	}
	
	public Amount<Length> getXApexConstructionAxesMainGear() {
		return _xApexConstructionAxesMainGear;
	};
	
	public void setXApexConstructionAxesMainGear (Amount<Length> xApexConstructionAxes) {
		this._xApexConstructionAxesMainGear = xApexConstructionAxes;
	};
	
	public Amount<Length> getYApexConstructionAxesMainGear() {
		return _yApexConstructionAxesMainGear;
	};
	
	public void setYApexConstructionAxesMainGear (Amount<Length> yApexConstructionAxes) {
		this._yApexConstructionAxesMainGear = yApexConstructionAxes; 
	};
	
	public Amount<Length> getZApexConstructionAxesMainGear() {
		return _zApexConstructionAxesMainGear;
	};
	
	public void setZApexConstructionAxesMainGear (Amount<Length> zApexConstructionAxes) {
		this._zApexConstructionAxesMainGear = zApexConstructionAxes;
	}

	public LandingGearsMountingPositionEnum getMountingPosition() {
		return _mountingPosition;
	}

	public Amount<Length> getXApexConstructionAxesNoseGear() {
		return _xApexConstructionAxesNoseGear;
	}

	public void setXApexConstructionAxesNoseGear(Amount<Length> _xApexConstructionAxesNoseGear) {
		this._xApexConstructionAxesNoseGear = _xApexConstructionAxesNoseGear;
	}

	public Amount<Length> getYApexConstructionAxesNoseGear() {
		return _yApexConstructionAxesNoseGear;
	}

	public void setYApexConstructionAxesNoseGear(Amount<Length> _yApexConstructionAxesNoseGear) {
		this._yApexConstructionAxesNoseGear = _yApexConstructionAxesNoseGear;
	}

	public Amount<Length> getZApexConstructionAxesNoseGear() {
		return _zApexConstructionAxesNoseGear;
	}

	public void setZApexConstructionAxesNoseGear(Amount<Length> _zApexConstructionAxesNoseGear) {
		this._zApexConstructionAxesNoseGear = _zApexConstructionAxesNoseGear;
	}

	public void setMountingPosition(LandingGearsMountingPositionEnum _mountingPosition) {
		this._mountingPosition = _mountingPosition;
	}

	public Amount<Length> getMainLegLength() {
		return _mainLegLength;
	}

	public void setMainLegLength(Amount<Length> _mainLegLength) {
		this._mainLegLength = _mainLegLength;
	}

	public Amount<Length> getNoseLegLength() {
		return _noseLegLength;
	}

	public void setNoseLegLength(Amount<Length> _noseLegLength) {
		this._noseLegLength = _noseLegLength;
	}

	public Amount<Length> getWheelTrack() {
		return _theLandingGearInterface.getWheeltrack();
	}

	public void setWheelTrack(Amount<Length> _wheelTrack) {
		setTheLandingGearsInterface(ILandingGear.Builder.from(_theLandingGearInterface).setWheeltrack(_wheelTrack).build());
	}

	public double getDeltaXApexConstructionAxesMainGear() {
		return _deltaXApexConstructionAxesMainGear;
	}

	public void setDeltaXApexConstructionAxesMainGear(double _deltaXApexConstructionAxesMainGear) {
		this._deltaXApexConstructionAxesMainGear = _deltaXApexConstructionAxesMainGear;
	}

	public Amount<Length> getWheelbase() {
		return _wheelbase;
	}

	public void setWheelbase(Amount<Length> _wheelBase) {
		this._wheelbase = _wheelBase;
	}

	public double getDeltaXApexConstructionAxesNoseGear() {
		return _deltaXApexConstructionAxesNoseGear;
	}

	public void setDeltaXApexConstructionAxesNoseGear(double _deltaXApexConstructionAxesNoseGear) {
		this._deltaXApexConstructionAxesNoseGear = _deltaXApexConstructionAxesNoseGear;
	}

}