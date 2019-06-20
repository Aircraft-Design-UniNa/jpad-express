package jpad.core.ex.aircraft.components.powerplant;

import javax.measure.quantity.Force;
import javax.measure.quantity.Length;
import javax.measure.quantity.Mass;
import javax.measure.quantity.Power;
import javax.measure.unit.SI;

import org.inferred.freebuilder.FreeBuilder;
import org.jscience.physics.amount.Amount;

import jpad.configs.ex.enumerations.EngineTypeEnum;

@FreeBuilder
public interface IEngine {
	
	String getId();
	EngineTypeEnum getEngineType();
	String getEngineDatabaseName();
	Amount<Length> getLength();
	//------------------------------------------
	// only for propeller driven engines
	Amount<Length> getPropellerDiameter();
	int getNumberOfBlades();
	double getEtaPropeller();
	Amount<Power> getStaticPower();
	//------------------------------------------
	double getBpr();
	Amount<Force> getStaticThrust();
	Amount<Mass> getDryMassPublicDomain(); 

	class Builder extends IEngine_Builder {
		public Builder () {
			
			// initializing values
			setPropellerDiameter(Amount.valueOf(0.0, SI.METER));
			setNumberOfBlades(0);
			setEtaPropeller(1.0);
			setStaticPower(Amount.valueOf(0.0, SI.WATT));
			setBpr(0.0);
			setStaticThrust(Amount.valueOf(0.0, SI.NEWTON));
			setDryMassPublicDomain(Amount.valueOf(0.0, SI.KILOGRAM));
		}
	}

}
