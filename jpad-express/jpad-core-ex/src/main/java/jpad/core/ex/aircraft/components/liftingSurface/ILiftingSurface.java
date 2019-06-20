package jpad.core.ex.aircraft.components.liftingSurface;

import java.util.List;

import javax.annotation.Nullable;
import javax.measure.quantity.Length;

import org.inferred.freebuilder.FreeBuilder;
import org.jscience.physics.amount.Amount;

import jpad.configs.ex.enumerations.ComponentEnum;
import jpad.core.ex.aircraft.components.liftingSurface.creator.AsymmetricFlapCreator;
import jpad.core.ex.aircraft.components.liftingSurface.creator.IEquivalentLiftingSurface;
import jpad.core.ex.aircraft.components.liftingSurface.creator.LiftingSurfacePanelCreator;
import jpad.core.ex.aircraft.components.liftingSurface.creator.SlatCreator;
import jpad.core.ex.aircraft.components.liftingSurface.creator.SpoilerCreator;
import jpad.core.ex.aircraft.components.liftingSurface.creator.SymmetricFlapCreator;

@FreeBuilder
public interface ILiftingSurface {

	// GENERAL INFORMATION
	String getId();
	boolean isMirrored();
	ComponentEnum getType();

	// GLOBAL DATA
	double getMainSparDimensionlessPosition();
	double getSecondarySparDimensionlessPosition();
	Amount<Length> getRoughness();
	Amount<Length> getWingletHeight();
	
	// EQUIVALENT WING
	@Nullable
	IEquivalentLiftingSurface getEquivalentWing();	
	
	// PANELS AND CONTROL SURFACE LISTS
	List<LiftingSurfacePanelCreator> getPanels();
	List<SymmetricFlapCreator> getSymmetricFlaps();
	List<SlatCreator> getSlats();
	List<AsymmetricFlapCreator> getAsymmetricFlaps();
	List<SpoilerCreator> getSpoilers();
	
	class Builder extends ILiftingSurface_Builder {
		public Builder() {
			
		}
	};
	
}
