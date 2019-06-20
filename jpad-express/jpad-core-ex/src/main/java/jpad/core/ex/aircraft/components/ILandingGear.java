package jpad.core.ex.aircraft.components;

import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;

import org.inferred.freebuilder.FreeBuilder;
import org.jscience.physics.amount.Amount;

@FreeBuilder
public interface ILandingGear {

	String getId();
	boolean isRetractable();
	double getGearCompressionFactor();
	Amount<Length> getWheeltrack();
	int getNumberOfFrontalWheels();
	int getNumberOfRearWheels();
	Amount<Angle> getNoseWheelSteeringAngle();
	Amount<Length> getFrontalWheelsHeight();
	Amount<Length> getFrontalWheelsWidth();
	Amount<Length> getRearWheelsHeight();
	Amount<Length> getRearWheelsWidth();

	class Builder extends ILandingGear_Builder {
		public Builder () {
			
		}
	}
}
