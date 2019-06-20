package jpad.core.ex.aircraft.components.powerplant;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.measure.quantity.Force;
import javax.measure.quantity.Power;
import javax.measure.unit.SI;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.jscience.physics.amount.Amount;

import jpad.configs.ex.MyConfiguration;
import jpad.configs.ex.enumerations.EngineTypeEnum;
import jpad.configs.ex.enumerations.FoldersEnum;
import jpad.configs.ex.enumerations.PowerPlantMountingPositionEnum;
import jpad.core.ex.standaloneutils.database.DatabaseManager;
import jpad.core.ex.standaloneutils.database.databasefunctions.engine.TurbofanCharacteristicsReader;

/** 
 * The Propulsion System includes engines, engine exhaust, 
 * reverser, starting, controls, lubricating, and fuel systems.
 * The output of this class is the entire propulsion system (that
 * is, all engines are included) 
 */

public class PowerPlant {

	//--------------------------------------------------------------------------------------------------
	// VARIABLES DECLARATION
	private int _engineNumber;
	private List<Engine> _engineList;
	private List<EngineTypeEnum> _engineType;
	private List<PowerPlantMountingPositionEnum> _mountingPosition;
	private Amount<Force> _t0Total;
	private Amount<Power> _p0Total;
	private TurbofanCharacteristicsReader _turbofanCharacteristicsReader;
	
	//--------------------------------------------------------------------------------------------------
	// BUILDER
	public PowerPlant (List<Engine> engineList) {
		
		this._engineList = engineList;
		this._engineNumber = engineList.size();
		this._mountingPosition = new ArrayList<>();
		this._engineType = new ArrayList<>();
		this._engineList.stream().forEach(engine -> {
			_mountingPosition.add(engine.getMountingPosition());
			_engineType.add(engine.getEngineType());
		});
		
		this._t0Total = Amount.valueOf(
				engineList.stream().mapToDouble(eng -> eng.getT0().doubleValue(SI.NEWTON)).sum(),
				SI.NEWTON
				);
		this._p0Total = Amount.valueOf(
				engineList.stream().mapToDouble(eng -> eng.getP0().doubleValue(SI.WATT)).sum(),
				SI.WATT
				);

		for (Engine engine : engineList) {
			if(engine.getEngineType() == EngineTypeEnum.TURBOFAN || engine.getEngineType() == EngineTypeEnum.TURBOJET) {
				try {
					_turbofanCharacteristicsReader = DatabaseManager.initializeTurbofanCharacteristicsDatabase(
							_turbofanCharacteristicsReader,
							MyConfiguration.getDir(FoldersEnum.DATABASE_DIR),
							"TurbofanCharacterstics.h5"
							);
				} catch (InvalidFormatException | IOException e) {
					e.printStackTrace();
				}
				
				// at least one engine must be a turbofan/turbojet to trigger database initializaion. It is useless to continue the engine iteration.
				break;  
			}
		}
	}
	
	//--------------------------------------------------------------------------------------------------
	// METHODS
	@Override
	public String toString() {
		
		MyConfiguration.customizeAmountOutput();

		StringBuilder sb = new StringBuilder();
		
		sb.append("\t-------------------------------------\n")
		  .append("\tThe Power Plant\n")
		  .append("\t-------------------------------------\n")
		  .append("\tNumber of engines: " + _engineNumber + "\n")
		  ;
		for(int i=0; i<this._engineList.size(); i++)
			sb.append("\t-------------------------------------\n")
			  .append("\tEngine no. " + (i+1) + "\n")
			  .append("\t-------------------------------------\n")
			  .append(this._engineList.get(i).toString())
			  ;
		
		
		return sb.toString();
		
	}
	
	//--------------------------------------------------------------------------------------------------
	// GETTERS & SETTERS
	
	public int getEngineNumber() {
		return _engineNumber;
	}
	
	public void setEngineNumber(int _engineNumber) {
		this._engineNumber = _engineNumber;
	}
	
	public List<Engine> getEngineList() {
		return _engineList;
	}
	
	public void setEngineList(List<Engine> _engineList) {
		this._engineList = _engineList;
	}
	
	public Amount<Force> getT0Total() {
		return _t0Total;
	}
	
	public Amount<Power> getP0Total() {
		return _p0Total;
	}
	
	public List<EngineTypeEnum> getEngineType() {
		return _engineType;
	}

	public void setEngineType(List<EngineTypeEnum> _engineType) {
		this._engineType = _engineType;
	}

	public List<PowerPlantMountingPositionEnum> getMountingPosition() {
		return _mountingPosition;
	}

	public void setMountingPosition(List<PowerPlantMountingPositionEnum> _mountingPosition) {
		this._mountingPosition = _mountingPosition;
	}

	public TurbofanCharacteristicsReader getTurbofanCharacteristicsReader() {
		return _turbofanCharacteristicsReader;
	}

	public void setTurbofanCharacteristicsReader(TurbofanCharacteristicsReader _turbofanCharacteristicsReader) {
		this._turbofanCharacteristicsReader = _turbofanCharacteristicsReader;
	}

}