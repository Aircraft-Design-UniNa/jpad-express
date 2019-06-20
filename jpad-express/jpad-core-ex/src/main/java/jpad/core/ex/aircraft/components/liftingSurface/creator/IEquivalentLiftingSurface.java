package jpad.core.ex.aircraft.components.liftingSurface.creator;

import java.util.List;

import org.inferred.freebuilder.FreeBuilder;

@FreeBuilder
public interface IEquivalentLiftingSurface {

	double getRealWingDimensionlessXOffsetRootChordLE();
	double getRealWingDimensionlessXOffsetRootChordTE();
	List<LiftingSurfacePanelCreator> getPanels();
	
	class Builder extends IEquivalentLiftingSurface_Builder {
		public Builder() {
			
		}
	}
}
