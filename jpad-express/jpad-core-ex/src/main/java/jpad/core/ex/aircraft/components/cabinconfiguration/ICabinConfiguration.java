package jpad.core.ex.aircraft.components.cabinconfiguration;

import java.util.List;

import javax.annotation.Nullable;
import javax.measure.quantity.Length;

import org.inferred.freebuilder.FreeBuilder;
import org.jscience.physics.amount.Amount;

import jpad.configs.ex.enumerations.ClassTypeEnum;

@FreeBuilder
public interface ICabinConfiguration {

	//.....................
	// Global data
	String getId();
	int getDesignPassengerNumber();
	int getFlightCrewNumber();
	List<ClassTypeEnum> getClassesType();
	double getDeltaXCabinStart();
	double getDeltaXCabinFwd();
	double getDeltaXCabinAft();
	
	//.....................
	// Detailed data	
	@Nullable
	int[] getNumberOfColumnsEconomyClass();
	@Nullable
	int[] getNumberOfColumnsBusinessClass();
	@Nullable
	int[] getNumberOfColumnsFirstClass();
	
	double getPercentageEconomyClass();
	double getPercentageBusinessClass();
	double getPercentageFirstClass();
	int getNumberOfRowsEconomyClass();
	int getNumberOfRowsBusinessClass();
	int getNumberOfRowsFirstClass();
	int getNumberOfPassengersEconomyClass();
	int getNumberOfPassengersBusinessClass();
	int getNumberOfPassengersFirstClass();
	Amount<Length> getPitchEconomyClass();
	Amount<Length> getPitchBusinessClass();
	Amount<Length> getPitchFirstClass();
	Amount<Length> getWidthEconomyClass();
	Amount<Length> getWidthBusinessClass();
	Amount<Length> getWidthFirstClass();
	Amount<Length> getDistanceFromWallEconomyClass();
	Amount<Length> getDistanceFromWallBusinessClass();
	Amount<Length> getDistanceFromWallFirstClass();

	class Builder extends ICabinConfiguration_Builder {
		public Builder() {
			
		}
	}
}