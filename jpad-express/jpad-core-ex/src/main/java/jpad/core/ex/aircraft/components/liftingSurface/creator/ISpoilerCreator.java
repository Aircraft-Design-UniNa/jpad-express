package jpad.core.ex.aircraft.components.liftingSurface.creator;

import javax.annotation.Nullable;
import javax.measure.quantity.Angle;

import org.inferred.freebuilder.FreeBuilder;
import org.jscience.physics.amount.Amount;

@FreeBuilder
public interface ISpoilerCreator {

	@Nullable
	String getId();
	double getInnerStationSpanwisePosition();
	double getOuterStationSpanwisePosition();
	double getInnerStationChordwisePosition();
	double getOuterStationChordwisePosition();
	double getInnerStationChordRatio();
	double getOuterStationChordRatio();
	Amount<Angle> getMinimumDeflection();
	Amount<Angle> getMaximumDeflection();
	
	class Builder extends ISpoilerCreator_Builder {
		public Builder() {
			
		}
	}
}
