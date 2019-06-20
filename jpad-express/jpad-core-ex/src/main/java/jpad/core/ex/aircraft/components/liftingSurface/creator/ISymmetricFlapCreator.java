package jpad.core.ex.aircraft.components.liftingSurface.creator;

import javax.annotation.Nullable;
import javax.measure.quantity.Angle;

import org.inferred.freebuilder.FreeBuilder;
import org.jscience.physics.amount.Amount;

import jpad.configs.ex.enumerations.FlapTypeEnum;

@FreeBuilder
public interface ISymmetricFlapCreator {

	@Nullable
	String getId();
	FlapTypeEnum getType();
	Double getInnerStationSpanwisePosition();
	Double getOuterStationSpanwisePosition();
	Double getInnerChordRatio();
	Double getOuterChordRatio();
	Amount<Angle> getMinimumDeflection();
	Amount<Angle> getMaximumDeflection();
	
	class Builder extends ISymmetricFlapCreator_Builder {
		public Builder() {
			
		}
	}
}
