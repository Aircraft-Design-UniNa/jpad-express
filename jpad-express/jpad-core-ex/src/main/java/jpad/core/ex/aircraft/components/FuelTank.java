package jpad.core.ex.aircraft.components;

import java.util.ArrayList;
import java.util.List;

import javax.measure.quantity.Area;
import javax.measure.quantity.Force;
import javax.measure.quantity.Length;
import javax.measure.quantity.Mass;
import javax.measure.quantity.Volume;
import javax.measure.quantity.VolumetricDensity;
import javax.measure.unit.SI;

import org.jscience.physics.amount.Amount;

import jpad.configs.ex.MyConfiguration;
import jpad.core.ex.aircraft.components.liftingSurface.LiftingSurface;
import jpad.core.ex.aircraft.components.liftingSurface.airfoils.Airfoil;
import jpad.core.ex.standaloneutils.MyUnits;
import jpad.core.ex.standaloneutils.atmosphere.AtmosphereCalc;
import jpad.core.ex.standaloneutils.geometry.AirfoilCalc;
import jpad.core.ex.standaloneutils.geometry.LSGeometryCalc;

/** 
 * The fuel tank is supposed to be make up of a series of prismoids from the root station to the 85% of the
 * wing semispan. The separation of each prismoid from the other is make by the kink airfoil stations (more
 * than one on a multi-panel wing). 
 * 
 * Each prismoid is defined from the the inner spanwise section, the outer spanwise section and the 
 * distance between the two airfoil stations. Furthermore, each section of the prismoid is defined by:
 * 
 *    - the airfoil thickness related to the main spar x station
 *    - the airfoil thickness related to the second spar x station
 *    - the distance between the two spar stations. 
 * 
 * The spar stations can be set for a default aircraft (for example 25% - 55%) 
 * or can be read from the wing file.
 * 
 * The fuel tank is supposed to be contained in the wing; 
 * the class defines only half of the fuel tank (the whole tank is symmetric with respect
 * to xz plane).
 *  
 * @author Vittorio Trifari
 * 
 */
public class FuelTank {

	//--------------------------------------------------------------------------------
	// VARIABLE DECLARATION
	private String _id;
	private LiftingSurface _theWing;
	private Amount<Length> _xApexConstructionAxes;
	private Amount<Length> _yApexConstructionAxes;
	private Amount<Length> _zApexConstructionAxes;
	private List<Amount<Length>> _thicknessAtMainSpar;
	private List<Amount<Length>> _thicknessAtSecondarySpar;
	private List<Amount<Length>> _distanceBetweenSpars;
	private List<Amount<Length>> _prismoidsLength;
	private List<Amount<Area>> _prismoidsSectionsAreas;
	private List<Amount<Volume>> _prismoidsVolumes;
	private List<Amount<Length>> _fuelTankStations;
	private List<Amount<Length>> _wingChordsAtFuelTankStations;
	
	private Amount<Mass> _massEstimated, _massReference;

	private Amount<Length> _xCG;
	private Amount<Length> _yCG;
	private Amount<Length> _zCG;
	
	private Amount<Length> _xCGLRF;
	private Amount<Length> _yCGLRF;
	private Amount<Length> _zCGLRF;
	
	// Jet A1 fuel density : the user can set this parameter when necessary
	private Amount<VolumetricDensity> _fuelDensity = Amount.valueOf(804.0, MyUnits.KILOGRAM_PER_CUBIC_METER);
	private Amount<Volume> _fuelVolume = Amount.valueOf(0.0, SI.CUBIC_METRE);
	private Amount<Mass> _fuelMass = Amount.valueOf(0.0, SI.KILOGRAM);
	private Amount<Force> _fuelWeight = Amount.valueOf(0.0, SI.NEWTON);

	//--------------------------------------------------------------------------------------------
	// BUILDER
	
	public FuelTank(String id, LiftingSurface theWing) {
			
		this._theWing = theWing;
		
		_thicknessAtMainSpar = new ArrayList<>();
		_thicknessAtSecondarySpar = new ArrayList<>();
		_distanceBetweenSpars = new ArrayList<>();
		_prismoidsLength = new ArrayList<>();
		_prismoidsSectionsAreas = new ArrayList<>();
		_prismoidsVolumes = new ArrayList<>();
		_fuelTankStations = new ArrayList<>();
		_wingChordsAtFuelTankStations = new ArrayList<>();
		
		calculateGeometry(_theWing);
		calculateFuelMass();
		
	}
	
	//--------------------------------------------------------------------------------------------
	// METHODS
	
	/************************************************************************************ 
	 * Estimates dimensions of the fuel tank.
	 * 
	 * The first section is at the root station while the other ones, except the last,
	 * are at the kink stations (may be more than one).
	 * 
	 * The last section is at 85% of the semispan so that it has to be defined separately.
	 * 
	 */
	private void estimateDimensions(LiftingSurface theWing) {

		for (int i=0; i<theWing.getAirfoilList().size()-1; i++) {
			this._thicknessAtMainSpar.add(
					Amount.valueOf(
							AirfoilCalc.calculateThicknessRatioAtXNormalizedStation(
									theWing.getMainSparDimensionlessPosition(),
									theWing.getAirfoilList().get(i).getThicknessToChordRatio()
									)
							* theWing.getChordsBreakPoints().get(i).doubleValue(SI.METER),
							SI.METER)
					);
			this._thicknessAtSecondarySpar.add(
					Amount.valueOf(
							AirfoilCalc.calculateThicknessRatioAtXNormalizedStation(
									theWing.getSecondarySparDimensionlessPosition(),
									theWing.getAirfoilList().get(i).getThicknessToChordRatio()
									)
							* theWing.getChordsBreakPoints().get(i).doubleValue(SI.METER),
							SI.METER)
					);
			this._distanceBetweenSpars.add(
					Amount.valueOf(
							theWing.getSecondarySparDimensionlessPosition()
							* theWing.getChordsBreakPoints().get(i).doubleValue(SI.METER)
							- (theWing.getMainSparDimensionlessPosition()
							* theWing.getChordsBreakPoints().get(i).doubleValue(SI.METER)),
							SI.METER
							)
					);
			this._fuelTankStations.add(theWing.getYBreakPoints().get(i));
			this._wingChordsAtFuelTankStations.add(theWing.getChordsBreakPoints().get(i));
		}
		for(int i=1; i<theWing.getYBreakPoints().size()-1; i++)
			this._prismoidsLength.add(
					theWing.getYBreakPoints().get(i)
					.minus(theWing.getYBreakPoints().get(i-1))
					);
		
		Airfoil airfoilAt85Percent = LSGeometryCalc.calculateAirfoilAtY(
				theWing,
				theWing.getSemiSpan().times(0.85).doubleValue(SI.METER)
				);
		Amount<Length> chordAt85Percent = Amount.valueOf(
				theWing.getChordAtYActual(
						theWing.getSemiSpan().times(0.85).doubleValue(SI.METER)
						),
				SI.METER
				);
		
		this._thicknessAtMainSpar.add(
				Amount.valueOf(
						AirfoilCalc.calculateThicknessRatioAtXNormalizedStation(
								theWing.getMainSparDimensionlessPosition(),
								airfoilAt85Percent.getThicknessToChordRatio()
								)
						* chordAt85Percent.doubleValue(SI.METER),
						SI.METER)
				);
		this._thicknessAtSecondarySpar.add(
				Amount.valueOf(
						AirfoilCalc.calculateThicknessRatioAtXNormalizedStation(
								theWing.getSecondarySparDimensionlessPosition(),
								airfoilAt85Percent.getThicknessToChordRatio()
								)
						* chordAt85Percent.doubleValue(SI.METER),
						SI.METER)
				);
		this._distanceBetweenSpars.add(
				Amount.valueOf(
						(theWing.getSecondarySparDimensionlessPosition()*chordAt85Percent.doubleValue(SI.METER))
						-(theWing.getMainSparDimensionlessPosition()*chordAt85Percent.doubleValue(SI.METER)),
						SI.METER
						)
				);
		this._fuelTankStations.add(theWing.getSemiSpan().times(0.85));
		
		this._wingChordsAtFuelTankStations.add(chordAt85Percent);
		
		this._prismoidsLength.add(
				theWing.getSemiSpan().times(0.85)
				.minus(theWing.getYBreakPoints().get(
						theWing.getYBreakPoints().size()-2)
						)
				);
	}
	
	/***********************************************************************************
	 * Calculates areas of each prismoid section (spanwise) from base size
	 * 
	 * @param theAircraft
	 */
	private void calculateAreas() {

		/*
		 * Each section is a trapezoid, so that the area is given by:
		 * 
		 *  (thicknessAtMainSpar + thicknessAtSecondarySpar)*distanceBetweenSpars*0.5
		 *  
		 */
		int nSections = this._thicknessAtMainSpar.size();
		for(int i=0; i<nSections; i++)
			this._prismoidsSectionsAreas.add(
					Amount.valueOf(
							(this._thicknessAtMainSpar.get(i).plus(this._thicknessAtSecondarySpar.get(i)))
							.times(this._distanceBetweenSpars.get(i)).times(0.5).getEstimatedValue(),
							SI.SQUARE_METRE
							)
					);
	}
	
	/*********************************************************************************
	 * Calculates the fuel tank volume using the section areas. 
	 * Each prismoid has a volume given by:
	 * 
	 *  (prismoidLength/3)*
	 *  	((prismoidSectionAreas(inner)) + (prismoidSectionAreas(outer)) 
	 *  		+ sqrt((prismoidSectionAreas(inner)) * (prismoidSectionAreas(outer))))
	 *  
	 * The total volume is the double of the sum of all prismoid volumes 
	 */
	private void calculateVolume() {

		/*

		 */
		for(int i=0; i<this._prismoidsLength.size(); i++) 
			this._prismoidsVolumes.add(
					Amount.valueOf(
							this._prismoidsLength.get(i).divide(3)
							.times(
									this._prismoidsSectionsAreas.get(i).getEstimatedValue()
									+ this._prismoidsSectionsAreas.get(i+1).getEstimatedValue()
									+ Math.sqrt(
											this._prismoidsSectionsAreas.get(i)
											.times(this._prismoidsSectionsAreas.get(i+1))
											.getEstimatedValue()
											)
									).getEstimatedValue(),
							SI.CUBIC_METRE
							)
					);
		
		for(int i=0; i<this._prismoidsVolumes.size(); i++)
			this._fuelVolume = this._fuelVolume
										.plus(this._prismoidsVolumes.get(i));
		this._fuelVolume = this._fuelVolume.times(2);
		
	}
	
	public void calculateGeometry(LiftingSurface theWing) {
		
		estimateDimensions(theWing);
		calculateAreas();
		calculateVolume();
	}
	
	public void calculateFuelMass() {
		_fuelMass = Amount.valueOf(_fuelDensity.times(_fuelVolume).getEstimatedValue(), SI.KILOGRAM);
		_fuelWeight = _fuelMass.times(AtmosphereCalc.g0).to(SI.NEWTON);
	}

	@Override
	public String toString() {
		
		MyConfiguration.customizeAmountOutput();

		StringBuilder sb = new StringBuilder()
				.append("\t-------------------------------------\n")
				.append("\tFuel tank\n")
				.append("\t-------------------------------------\n")
				.append("\tMain spar position (% local chord): " + _theWing.getMainSparDimensionlessPosition() + "\n")
				.append("\tSecondary spar position (% local chord): " + _theWing.getSecondarySparDimensionlessPosition() + "\n")
				.append("\t�������������������������������������\n")
				.append("\tAirfoils thickness at main spar stations: " + _thicknessAtMainSpar + "\n")
				.append("\tAirfoils thickness at secondary spar stations: " + _thicknessAtSecondarySpar + "\n")
				.append("\tSpar distance at each spanwise station: " + _distanceBetweenSpars + "\n")
				.append("\tPrismoids length: " + _prismoidsLength + "\n")
				.append("\t�������������������������������������\n")
				.append("\tPrismoids spanwise sections areas: " + _prismoidsSectionsAreas + "\n")
				.append("\tPrismoids volumes: " + _prismoidsVolumes + "\n")
				.append("\t�������������������������������������\n")
				.append("\tTotal tank volume: " + _fuelVolume + "\n")
				.append("\tFuel density: " + _fuelDensity + "\n")
				.append("\tTotal fuel mass: " + _fuelMass + "\n")
				.append("\tTotal fuel weight: " + _fuelWeight + "\n")
				.append("\t�������������������������������������\n")
				;
		
		return sb.toString();
		
	}
	
	//--------------------------------------------------------------------------------------------
	// GETTERS & SETTERS
	
	public String getId() {
		return _id;
	}

	public void setId(String _id) {
		this._id = _id;
	}

	public LiftingSurface getTheWing() {
		return _theWing;
	}
	
	public void setTheWing (LiftingSurface theWing) {
		this._theWing = theWing;
	}
	
	public Amount<Length> getXApexConstructionAxes() {
		return _xApexConstructionAxes;
	}

	public void setXApexConstructionAxes(Amount<Length> _xApexConstructionAxes) {
		this._xApexConstructionAxes = _xApexConstructionAxes;
	}

	public Amount<Length> getYApexConstructionAxes() {
		return _yApexConstructionAxes;
	}

	public void setYApexConstructionAxes(Amount<Length> _yApexConstructionAxes) {
		this._yApexConstructionAxes = _yApexConstructionAxes;
	}

	public Amount<Length> getZApexConstructionAxes() {
		return _zApexConstructionAxes;
	}

	public void setZApexConstructionAxes(Amount<Length> _zApexConstructionAxes) {
		this._zApexConstructionAxes = _zApexConstructionAxes;
	}

	public List<Amount<Length>> getDistanceBetweenSpars() {
		return _distanceBetweenSpars;
	}

	public void setDistanceBetweenSpars(List<Amount<Length>> _distanceBetweenSpars) {
		this._distanceBetweenSpars = _distanceBetweenSpars;
	}

	public List<Amount<Length>> getPrismoidsLength() {
		return _prismoidsLength;
	}

	public void setPrismoidsLength(List<Amount<Length>> _prismoidsLength) {
		this._prismoidsLength = _prismoidsLength;
	}

	public Amount<Length> getXCG() {
		return _xCG;
	}

	public void setXCG(Amount<Length> _xCG) {
		this._xCG = _xCG;
	}

	public Amount<Length> getYCG() {
		return _yCG;
	}

	public void setYCG(Amount<Length> _yCG) {
		this._yCG = _yCG;
	}

	public Amount<Length> getZCG() {
		return _zCG;
	}

	public void setZCG(Amount<Length> _zCG) {
		this._zCG = _zCG;
	}

	public Amount<VolumetricDensity> getFuelDensity() {
		return _fuelDensity;
	}

	public void setFuelDensity(Amount<VolumetricDensity> _fuelDensity) {
		this._fuelDensity = _fuelDensity;
	}

	public Amount<Mass> getMassEstimated() {
		return _massEstimated;
	}

	public List<Amount<Length>> getThicknessAtMainSpar() {
		return _thicknessAtMainSpar;
	}

	public List<Amount<Length>> getThicknessAtSecondarySpar() {
		return _thicknessAtSecondarySpar;
	}

	public List<Amount<Area>> getPrismoidsSectionsAreas() {
		return _prismoidsSectionsAreas;
	}

	public List<Amount<Volume>> getPrismoidsVolumes() {
		return _prismoidsVolumes;
	}

	public Amount<Volume> getFuelVolume() {
		return _fuelVolume;
	}

	public Amount<Mass> getFuelMass() {
		return _fuelMass;
	}

	public void setFuelMass(Amount<Mass> fuelMass) {
		this._fuelMass = fuelMass;
	}
	
	public Amount<Mass> getMassReference() {
		return _massReference;
	}

	public void setMassReference(Amount<Mass> _massReference) {
		this._massReference = _massReference;
	}

	public Amount<Force> getFuelWeight() {
		return _fuelWeight;
	}

	public Amount<Length> getXCGLRF() {
		return _xCGLRF;
	}

	public void setXCGLRF(Amount<Length> _xCGLRF) {
		this._xCGLRF = _xCGLRF;
	}

	public Amount<Length> getYCGLRF() {
		return _yCGLRF;
	}

	public void setYCGLRF(Amount<Length> _yCGLRF) {
		this._yCGLRF = _yCGLRF;
	}

	public Amount<Length> getZCGLRF() {
		return _zCGLRF;
	}

	public void setZCGLRF(Amount<Length> _zCGLRF) {
		this._zCGLRF = _zCGLRF;
	}

}
