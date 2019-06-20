package jpad.core.ex.aircraft.components.fuselage;

import java.util.List;

import javax.measure.quantity.Length;

import org.inferred.freebuilder.FreeBuilder;
import org.jscience.physics.amount.Amount;

import jpad.configs.ex.enumerations.WindshieldTypeEnum;
import jpad.core.ex.aircraft.components.liftingSurface.creator.SpoilerCreator;

@FreeBuilder
public interface IFuselage {
	
	String getId();
	boolean isPressurized();
	Amount<Length> getFuselageLength();
	double getNoseLengthRatio();
	double getCylinderLengthRatio();
	Amount<Length> getSectionCylinderWidth();
	Amount<Length> getSectionCylinderHeight();
	Amount<Length> getRoughness();
	Amount<Length> getNoseTipOffset();
	Amount<Length> getTailTipOffest();
	double getNoseCapOffsetPercent();
	double getTailCapOffsetPercent();
	WindshieldTypeEnum getWindshieldType();
	Amount<Length> getWindshieldHeight();
	Amount<Length> getWindshieldWidth();
	
	// how lower part is different from half diameter
	double getSectionNoseMidLowerToTotalHeightRatio();
	double getSectionCylinderLowerToTotalHeightRatio();
	double getSectionTailMidLowerToTotalHeightRatio();
	// shape index, 1 --> close to a rectangle; 0 --> close to a circle
	double getSectionCylinderRhoUpper();
	double getSectionCylinderRhoLower();
	double getSectionMidNoseRhoUpper();
	double getSectionMidTailRhoUpper();
	double getSectionMidNoseRhoLower();
	double getSectionMidTailRhoLower();
	
	List<SpoilerCreator> getSpoilers();
	
	class Builder extends IFuselage_Builder  { 
		 public Builder() {
			 			 
		 }
	}
}
