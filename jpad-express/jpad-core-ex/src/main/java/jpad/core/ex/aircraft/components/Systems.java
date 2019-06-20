package jpad.core.ex.aircraft.components;

public class Systems {

	//-------------------------------------------------------------------------------------
	// VARIABLE DECLARATION
	private ISystems _theSystemsInterface;
	
	//------------------------------------------------------------------------------------------
	// BUILDER
	public Systems(ISystems theSystemsInterface) {
		
		this._theSystemsInterface = theSystemsInterface;
	}
	
	//-------------------------------------------------------------------------------------
	// GETTERS AND SETTER
	public ISystems getTheSystemsInterface() {
		return _theSystemsInterface;
	}

	public void setTheSystemsInterface(ISystems _theSystemsInterface) {
		this._theSystemsInterface = _theSystemsInterface;
	}

}