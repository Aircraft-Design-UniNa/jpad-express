package jpad.configs.ex.enumerations;

import javax.measure.quantity.Length;
import javax.measure.unit.SI;

import org.jscience.physics.amount.Amount;

public enum EmergencyExitEnum {

	TYPE_I {
		@Override
		public Amount<Length> getEmergencyExitWidth() {			
			return Amount.valueOf(0.6096, SI.METER);
		}
		@Override
		public Amount<Length> getEmergencyExitHeight() {
			return Amount.valueOf(1.2192, SI.METER);
		}
	},
	TYPE_II {
		@Override
		public Amount<Length> getEmergencyExitWidth() {
			return Amount.valueOf(0.508, SI.METER);
		}
		@Override
		public Amount<Length> getEmergencyExitHeight() {
			return Amount.valueOf(1.1176, SI.METER);
		}
	},
	TYPE_III {
		@Override
		public Amount<Length> getEmergencyExitWidth() {
			return Amount.valueOf(0.508, SI.METER);
		}
		@Override
		public Amount<Length> getEmergencyExitHeight() {
			return Amount.valueOf(0.9144, SI.METER);
		}
	},
	TYPE_IV {
		@Override
		public Amount<Length> getEmergencyExitWidth() {
			return Amount.valueOf(0.4826, SI.METER);
		}
		@Override
		public Amount<Length> getEmergencyExitHeight() {
			return Amount.valueOf(0.6604, SI.METER);
		}
	},
	TYPE_A {
		@Override
		public Amount<Length> getEmergencyExitWidth() {
			return Amount.valueOf(1.0668, SI.METER);
		}
		@Override
		public Amount<Length> getEmergencyExitHeight() {
			return Amount.valueOf(1.8288, SI.METER);
		}
	},
	TAILCONE {
		@Override
		public Amount<Length> getEmergencyExitWidth() {
			return Amount.valueOf(0.508, SI.METER);
		}
		@Override
		public Amount<Length> getEmergencyExitHeight() {
			return Amount.valueOf(1.524, SI.METER);
		}
	},
	VENTRAL {
		@Override
		public Amount<Length> getEmergencyExitWidth() {			
			return Amount.valueOf(0.6096, SI.METER);
		}
		@Override
		public Amount<Length> getEmergencyExitHeight() {
			return Amount.valueOf(1.2192, SI.METER);
		}
	};
	
	public abstract Amount<Length> getEmergencyExitWidth();
	public abstract Amount<Length> getEmergencyExitHeight();
}
