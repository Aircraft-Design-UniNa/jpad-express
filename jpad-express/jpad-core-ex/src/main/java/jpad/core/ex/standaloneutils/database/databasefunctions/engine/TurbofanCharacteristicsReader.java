package jpad.core.ex.standaloneutils.database.databasefunctions.engine;

import javax.measure.quantity.Force;
import javax.measure.quantity.Length;
import javax.measure.quantity.Mass;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.jscience.physics.amount.Amount;

import jpad.core.ex.standaloneutils.MyInterpolatingFunction;
import jpad.core.ex.standaloneutils.database.databasefunctions.DatabaseReader;

public class TurbofanCharacteristicsReader extends DatabaseReader {

	//-----------------------------------------------------------------------------------------
	// VARIABLE DECLARATION
	private MyInterpolatingFunction dryMassFunction, maxLengthFunction, maxWidthFunction;
	
	//-----------------------------------------------------------------------------------------
	// BUILDER
	public TurbofanCharacteristicsReader(String databaseFolderPath, String databaseFileName) {
		
		super(databaseFolderPath, databaseFileName);
		
		dryMassFunction = database.interpolate2DFromDatasetFunction("EngineDryMass");
		maxLengthFunction = database.interpolate2DFromDatasetFunction("EngineMaxLength");
		maxWidthFunction = database.interpolate2DFromDatasetFunction("EngineMaxWidth");
		
	}

	//-----------------------------------------------------------------------------------------
	// GETTES & SETTERS
	
	/**
	 * @author Vittorio Trifari
	 * @param t0, the static thrust
	 * @param bpr, the bypass ratio
	 * @return the dry mass of the engine in kg
	 */
	public Amount<Mass> getEngineDryMass(Amount<Force> t0, double bpr) { 
		return  Amount.valueOf(
					dryMassFunction.valueBilinear(t0.doubleValue(NonSI.POUND_FORCE), bpr),
					NonSI.POUND).to(SI.KILOGRAM);
	}
	
	/**
	 * @author Vittorio Trifari
	 * @param t0, the static thrust
	 * @param bpr, the bypass ratio
	 * @return the overall engine length in meters
	 */
	public Amount<Length> getEngineMaxLength(Amount<Force> t0, double bpr) { 
		return  Amount.valueOf(
					maxLengthFunction.valueBilinear(t0.doubleValue(NonSI.POUND_FORCE), bpr),
					NonSI.INCH).to(SI.METER);
	}
	
	/**
	 * @author Vittorio Trifari
	 * @param t0, the static thrust
	 * @param bpr, the bypass ratio
	 * @return the maximum width of the engine (with nacelle) in meters
	 */
	public Amount<Length> getEngineMaxWidth(Amount<Force> t0, double bpr) { 
		return  Amount.valueOf(
					maxWidthFunction.valueBilinear(t0.doubleValue(NonSI.POUND_FORCE), bpr),
					NonSI.INCH).to(SI.METER);
	}
	
}
