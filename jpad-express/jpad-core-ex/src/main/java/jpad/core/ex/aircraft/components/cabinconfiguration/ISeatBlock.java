package jpad.core.ex.aircraft.components.cabinconfiguration;

import javax.measure.quantity.Length;

import org.inferred.freebuilder.FreeBuilder;
import org.jscience.physics.amount.Amount;

import jpad.configs.ex.enumerations.ClassTypeEnum;
import jpad.configs.ex.enumerations.RelativePositionEnum;

@FreeBuilder
public interface ISeatBlock {

RelativePositionEnum getPosition();
	
	// x-coordinate of first seat of each block
	Amount<Length> getXStart();
	
	// x-coordinate of last seat of each block
	Amount<Length> getXEnd();
	
	// delta_x of cabin forward break
	Amount<Length> getDeltaXCabinFwd();
	
	// delta_x of cabin aft break
	Amount<Length> getDeltaXCabinAft();
	
	// Pitch: the distance between two consecutive seats farther pillars
	Amount<Length> getPitch();
	
	// Seat width
	Amount<Length> getWidth();
	
	// The distance of the nearest seat of the block from the wall
	// This distance is measured at half the height of the seat
	Amount<Length> getDistanceFromWall();
		
	// Number of rows. It is a fixed parameter of the block: we suppose
	// that the user never deletes an entire row of seats but uses a break
	// to insert an empty space.
	int getRowsNumber();
	
	// Columns are given from the leftmost "line" to the rightmost one,
	// looking the aircraft rear to front.
	int getColumnsNumber();
	
	// The actual number of seats to be distributed among rows and columns
	int getBlockActualNumberOfSeats();
	
	// The class of which the seat block is part
	ClassTypeEnum getType();
	
	class Builder extends ISeatBlock_Builder {
		public Builder() {
			
		}
	}
}
