package jpad.core.ex.aircraft.components.liftingSurface;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.measure.quantity.Angle;
import javax.measure.quantity.Area;
import javax.measure.quantity.Length;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jscience.physics.amount.Amount;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.Tuple5;
import jpad.configs.ex.MyConfiguration;
import jpad.configs.ex.enumerations.ComponentEnum;
import jpad.configs.ex.enumerations.LiftingSurfaceAdjustCriteriaEnum;
import jpad.core.ex.aircraft.components.liftingSurface.airfoils.Airfoil;
import jpad.core.ex.aircraft.components.liftingSurface.creator.AsymmetricFlapCreator;
import jpad.core.ex.aircraft.components.liftingSurface.creator.IEquivalentLiftingSurface;
import jpad.core.ex.aircraft.components.liftingSurface.creator.ILiftingSurfacePanelCreator;
import jpad.core.ex.aircraft.components.liftingSurface.creator.LiftingSurfacePanelCreator;
import jpad.core.ex.aircraft.components.liftingSurface.creator.SlatCreator;
import jpad.core.ex.aircraft.components.liftingSurface.creator.SpoilerCreator;
import jpad.core.ex.aircraft.components.liftingSurface.creator.SymmetricFlapCreator;
import jpad.core.ex.standaloneutils.GeometryCalc;
import jpad.core.ex.standaloneutils.JPADXmlReader;
import jpad.core.ex.standaloneutils.MyArrayUtils;
import jpad.core.ex.standaloneutils.MyMathUtils;
import jpad.core.ex.standaloneutils.MyXMLReaderUtils;
import jpad.core.ex.standaloneutils.geometry.LSGeometryCalc;

/**
 * The LiftingSurface class manages all the geometrical data of generic lifting surface.
 * It reads from the component XML file, calculates all the derived geometrical parameters and builds the lifiting surface.
 * It also read and populate the airfoil List.
 * This class is contained within the LiftingSurface class which containes the components position, mass and center of gravity position. 
 * 
 * @author Vittorio Trifari
 *
 */
public class LiftingSurface {

	//-----------------------------------------------------------------------------------------
	// VARIABLE DECLARATION
	ILiftingSurface _theLiftingSurfaceInterface;
	private int _numberOfSpanwisePoints = 30;
	
	private double _positionRelativeToAttachment;
	private Amount<Length> _xApexConstructionAxes = Amount.valueOf(0.0, SI.METER); 
	private Amount<Length> _yApexConstructionAxes = Amount.valueOf(0.0, SI.METER); 
	private Amount<Length> _zApexConstructionAxes = Amount.valueOf(0.0, SI.METER);
	private Amount<Angle> _riggingAngle = Amount.valueOf(0.0, NonSI.DEGREE_ANGLE);
	private int _numberOfEngineOverTheWing = 0; 
	
	private LiftingSurface _exposedLiftingSurface;
	private boolean equivalentWingFlag;
	
	//-----------------------------------------------------------------------------------------
	// DERIVED INPUT DATA
	private Amount<Angle> _dihedralMean;
	private double _xTransitionUpper;
	private double _xTransitionLower;
	private Amount<Area> _surfaceWettedExposed;
	private Amount<Area> _totalControlSurfaceArea;
	private Amount<Area> _symmetricFlapsControlSurfaceArea;
	private Amount<Area> _asymmetricFlapsControlSurfaceArea;
	private Amount<Area> _slatsControlSurfaceArea;
	private Amount<Area> _spoilersControlSurfaceArea;
	private List<Amount<Area>> _symmetricFlapsControlSurfaceAreaList;
	private List<Amount<Area>> _asymmetricFlapsControlSurfaceAreaList;
	private List<Amount<Area>> _slatsControlSurfaceAreaList;
	private List<Amount<Area>> _spoilersControlSurfaceAreaList;
	private double _volumetricRatio;
	private Double _kExcr; 
	private Amount<Length> _liftingSurfaceACToWingACDistance;
	private Amount<Length> _liftingSurfaceArm;
	private Amount<Length> _meanAerodynamicChord;
	private Amount<Length> _meanAerodynamicChordLeadingEdgeX;
	private Amount<Length> _meanAerodynamicChordLeadingEdgeY;
	private Amount<Length> _meanAerodynamicChordLeadingEdgeZ;
	private Amount<Length> _semiSpan, _span;
	private Amount<Area> _surfacePlanform;
	private Amount<Area> _surfaceWetted;
	private double _aspectRatio;
	private double _taperRatio;
	private double _thicknessMean;
	private double _formFactor;

	// airfoil span-wise characteristics : 
	private Airfoil _meanAirfoil;
	private List<Airfoil> _airfoilList;
	private List<String> _airfoilPathList;
 	private List<Double> _maxThicknessVsY;
	private List<Double> _radiusLEVsY;
	private List<Double> _camberRatioVsY;
	private List<Amount<Angle>> _alpha0VsY;
	private List<Amount<Angle>> _alphaStarVsY;
	private List<Amount<Angle>>_alphaStallVsY;
	private List<Amount<?>> _clAlphaVsY; 
	private List<Double> _cdMinVsY;
	private List<Double> _clAtCdMinVsY;
	private List<Double> _cl0VsY;
	private List<Double> _clStarVsY;
	private List<Double> _clMaxVsY;
	private List<Double> _clMaxSweepVsY;
	private List<Double> _kFactorDragPolarVsY;
	private List<Double> _mExponentDragPolarVsY;
	private List<Double> _cmAlphaQuarteChordVsY;
	private List<Double> _xAcAirfoilVsY;
	private List<Double> _cmACVsY;
	private List<Double> _cmACStallVsY;
	private List<Double> _criticalMachVsY;

	private List<Double> _etaBreakPoints;
	private List<Amount<Length>> _yBreakPoints;
	private List<Amount<Length>> _xLEBreakPoints;
	private List<Amount<Length>> _zLEBreakPoints;
	private List<Amount<Length>> _chordsBreakPoints;
	private List<Amount<Angle>> _twistsBreakPoints;
	private List<Amount<Angle>> _dihedralsBreakPoints;
	
	private List<Double> _etaStations;
	private List<Amount<Length>> _yStations;
	
	private List<
				Tuple2<
					LiftingSurfacePanelCreator,
					Tuple5<
						List<Amount<Length>>, // Ys
						List<Amount<Length>>, // chords
						List<Amount<Length>>, // Xle
						List<Amount<Length>>, // Zle
						List<Amount<Angle>>   // twist
						> 
					>
				> _panelToSpanwiseDiscretizedVariables;
	
	private List<
				Tuple5<
					Amount<Length>, // Ys
					Amount<Length>, // chords
					Amount<Length>, // Xle
					Amount<Length>, // Zle
					Amount<Angle>   // twist
					> 
				> _spanwiseDiscretizedVariables;
	
	// data to eventually add to the equivalent wing attributes TODO
	private double _xOffsetEquivalentWingRootLE;
	private double _xOffsetEquivalentWingRootTE;
	
	//-----------------------------------------------------------------------------------------
	// BUILDER
	public LiftingSurface(ILiftingSurface theLiftingSurfaceInterface) {
		
		this._theLiftingSurfaceInterface = theLiftingSurfaceInterface;
		initializeData();
		calculateGeometry(_theLiftingSurfaceInterface.getType(), _theLiftingSurfaceInterface.isMirrored());
		
	}

	//-----------------------------------------------------------------------------------------
	// METHODS
	private void initializeData() {

		_etaBreakPoints = new ArrayList<>();
		_yBreakPoints =  new ArrayList<Amount<Length>>();
		_xLEBreakPoints = new ArrayList<Amount<Length>>();
		_zLEBreakPoints = new ArrayList<Amount<Length>>();
		_chordsBreakPoints = new ArrayList<Amount<Length>>();
		_twistsBreakPoints = new ArrayList<Amount<Angle>>();
		_dihedralsBreakPoints = new ArrayList<Amount<Angle>>();
		
		_symmetricFlapsControlSurfaceAreaList = new ArrayList<>();
		_asymmetricFlapsControlSurfaceAreaList = new ArrayList<>();
		_slatsControlSurfaceAreaList = new ArrayList<>();
		_spoilersControlSurfaceAreaList = new ArrayList<>();

		_airfoilList = new ArrayList<>();
		_airfoilPathList = new ArrayList<>();
		_maxThicknessVsY = new ArrayList<>();
		_radiusLEVsY = new ArrayList<>();
		_camberRatioVsY = new ArrayList<>();
		_alpha0VsY = new ArrayList<>();
		_alphaStarVsY = new ArrayList<>();
		_alphaStallVsY = new ArrayList<>();
		_clAlphaVsY = new ArrayList<>(); 
		_cdMinVsY = new ArrayList<>();
		_clAtCdMinVsY = new ArrayList<>();
		_cl0VsY = new ArrayList<>();
		_clStarVsY = new ArrayList<>();
		_clMaxVsY = new ArrayList<>();
		_clMaxSweepVsY = new ArrayList<>();
		_kFactorDragPolarVsY = new ArrayList<>();
		_mExponentDragPolarVsY = new ArrayList<>();
		_cmAlphaQuarteChordVsY = new ArrayList<>();
		_xAcAirfoilVsY = new ArrayList<>();
		_cmACVsY = new ArrayList<>();
		_cmACStallVsY = new ArrayList<>();
		_criticalMachVsY = new ArrayList<>();

		_etaStations = new ArrayList<>();
		_yStations = new ArrayList<Amount<Length>>();
		_panelToSpanwiseDiscretizedVariables = new ArrayList<>();
		_spanwiseDiscretizedVariables = new ArrayList<>();

	}

	public static LiftingSurface importFromXML(ComponentEnum type, String pathToXML, String airfoilsDir) {

		JPADXmlReader reader = new JPADXmlReader(pathToXML);
		LiftingSurface liftingSurfaceCreator = null;

		System.out.println("Reading lifting surface data ...");

		//...................................................................................
		// Data initialization
		boolean mirrored = false;
		double mainSparDimensionlessPosition = 0.0;
		double secondarySparDimensionlessPosition = 0.0;
		Amount<Length> roughness = Amount.valueOf(0.0, SI.METER);
		Amount<Length> wingletHeight = Amount.valueOf(0.0, SI.METER);
		List<LiftingSurfacePanelCreator> panels = new ArrayList<>();
		List<SymmetricFlapCreator> symmetricFlaps = new ArrayList<>();
		List<SlatCreator> slats = new ArrayList<>();
		List<AsymmetricFlapCreator> asymmetricFlaps = new ArrayList<>();
		List<SpoilerCreator> spoilers = new ArrayList<>();
		//...................................................................................
		
		String id = MyXMLReaderUtils
				.getXMLPropertyByPath(
						reader.getXmlDoc(), reader.getXpath(),
						"//@id");
		
		String mirroredProperty = MyXMLReaderUtils
				.getXMLPropertyByPath(
						reader.getXmlDoc(), reader.getXpath(),
						"//@mirrored");
		if(mirroredProperty != null)
			mirrored = Boolean.valueOf(mirroredProperty);

		if(type.equals(ComponentEnum.WING)) {

			//---------------------------------------------------------------------------------
			// GLOBAL DATA
			String mainSparPositionProperty = reader.getXMLPropertyByPath("//global_data/main_spar_non_dimensional_position");
			if(mainSparPositionProperty != null)
				mainSparDimensionlessPosition = Double.valueOf(mainSparPositionProperty);
			
			String secondarySparPositionProperty = reader.getXMLPropertyByPath("//global_data/secondary_spar_non_dimensional_position");
			if(secondarySparPositionProperty != null)
				secondarySparDimensionlessPosition = Double.valueOf(secondarySparPositionProperty);
			
			String roughnessProperty = reader.getXMLPropertyByPath("//global_data/roughness");
			if(roughnessProperty != null)
				roughness = reader.getXMLAmountLengthByPath("//global_data/roughness");
			
			String wingletHeightProperty = reader.getXMLPropertyByPath("//global_data/winglet_height");
			if(wingletHeightProperty != null)
				wingletHeight = reader.getXMLAmountLengthByPath("//global_data/winglet_height");
			
			//---------------------------------------------------------------------------------
			// PANELS
			NodeList nodelistPanel = MyXMLReaderUtils
					.getXMLNodeListByPath(reader.getXmlDoc(), "//panels/panel");

			System.out.println("Panels found: " + nodelistPanel.getLength());

			for (int i = 0; i < nodelistPanel.getLength(); i++) {
				Node nodePanel  = nodelistPanel.item(i); // .getNodeValue();
				Element elementPanel = (Element) nodePanel;
				System.out.println("[" + i + "]\nPanel id: " + elementPanel.getAttribute("id"));
				if (elementPanel.getAttribute("linked_to").isEmpty()) {
					panels.add(LiftingSurfacePanelCreator.importFromPanelNode(nodePanel, airfoilsDir));
				} else {
					LiftingSurfacePanelCreator panel0 = panels.stream()
							.filter(p -> p.getId().equals(elementPanel.getAttribute("linked_to")))
							.findFirst()
							.get()
							;
					if (panel0 != null) {
						System.out.println("Panel linked_to: **" + elementPanel.getAttribute("linked_to") + "**");
						panels.add(LiftingSurfacePanelCreator.importFromPanelNodeLinked(nodePanel, panel0, airfoilsDir));
					} else {
						System.out.println("WARNING: panel not parsed. Unable to find the ID of linked_to attribute!");
					}
				}
			}

			//---------------------------------------------------------------------------------
			// SYMMETRIC FLAPS
			NodeList nodelistFlaps = MyXMLReaderUtils
					.getXMLNodeListByPath(reader.getXmlDoc(), "//symmetric_flaps/symmetric_flap");

			System.out.println("Symmetric flaps found: " + nodelistFlaps.getLength());

			for (int i = 0; i < nodelistFlaps.getLength(); i++) {
				Node nodeFlap  = nodelistFlaps.item(i); // .getNodeValue();
				Element elementFlap = (Element) nodeFlap;
				System.out.println("[" + i + "]\nFlap id: " + elementFlap.getAttribute("id"));

				symmetricFlaps.add(SymmetricFlapCreator.importFromSymmetricFlapNode(nodeFlap, type));
			}

			//---------------------------------------------------------------------------------
			// SYMMETRIC SLATS
			NodeList nodelistSlats = MyXMLReaderUtils
					.getXMLNodeListByPath(reader.getXmlDoc(), "//slats/slat");

			System.out.println("Slats found: " + nodelistSlats.getLength());

			for (int i = 0; i < nodelistSlats.getLength(); i++) {
				Node nodeSlat  = nodelistSlats.item(i); // .getNodeValue();
				Element elementSlat = (Element) nodeSlat;
				System.out.println("[" + i + "]\nSlat id: " + elementSlat.getAttribute("id"));

				slats.add(SlatCreator.importFromSymmetricSlatNode(nodeSlat));
			}

			//---------------------------------------------------------------------------------
			// ASYMMETRIC FLAPS
			NodeList nodelistAsymmetricFlaps = MyXMLReaderUtils
					.getXMLNodeListByPath(reader.getXmlDoc(), "//asymmetric_flaps/asymmetric_flap");

			System.out.println("Asymmetric flaps found: " + nodelistAsymmetricFlaps.getLength());

			for (int i = 0; i < nodelistAsymmetricFlaps.getLength(); i++) {
				Node nodeAsymmetricFlap  = nodelistAsymmetricFlaps.item(i); // .getNodeValue();
				Element elementAsymmetricFlap = (Element) nodeAsymmetricFlap;
				System.out.println("[" + i + "]\nSlat id: " + elementAsymmetricFlap.getAttribute("id"));

				asymmetricFlaps.add(AsymmetricFlapCreator.importFromAsymmetricFlapNode(nodeAsymmetricFlap));
			}

			//---------------------------------------------------------------------------------
			// SPOILERS
			NodeList nodelistSpoilers = MyXMLReaderUtils
					.getXMLNodeListByPath(reader.getXmlDoc(), "//spoilers/spoiler");

			System.out.println("Spoilers found: " + nodelistSpoilers.getLength());

			for (int i = 0; i < nodelistSpoilers.getLength(); i++) {
				Node nodeSpoiler  = nodelistSpoilers.item(i); // .getNodeValue();
				Element elementSpoiler = (Element) nodeSpoiler;
				System.out.println("[" + i + "]\nSlat id: " + elementSpoiler.getAttribute("id"));

				spoilers.add(SpoilerCreator.importFromSpoilerNode(nodeSpoiler));
			}
			
			liftingSurfaceCreator = new LiftingSurface(
					new ILiftingSurface.Builder()
					.setId(id)
					.setMirrored(mirrored)
					.setType(type)
					.setMainSparDimensionlessPosition(mainSparDimensionlessPosition)
					.setSecondarySparDimensionlessPosition(secondarySparDimensionlessPosition)
					.setRoughness(roughness)
					.setWingletHeight(wingletHeight)
					.addAllPanels(panels)
					.addAllSymmetricFlaps(symmetricFlaps)
					.addAllAsymmetricFlaps(asymmetricFlaps)
					.addAllSlats(slats)
					.addAllSpoilers(spoilers)
					.buildPartial()
					);
		
			int i=0;
			for(i=0 ; i<liftingSurfaceCreator.getTheLiftingSurfaceInterface().getPanels().size(); i++) {
			liftingSurfaceCreator.getTheLiftingSurfaceInterface().getPanels().get(i).getAirfoilRoot().setClMax(
					liftingSurfaceCreator.getTheLiftingSurfaceInterface().getPanels().get(i).getAirfoilRoot().getClMax() * Math.pow( Math.cos(
							liftingSurfaceCreator.getTheLiftingSurfaceInterface().getEquivalentWing().getPanels().get(0).getSweepLeadingEdge().doubleValue(SI.RADIAN)), 2)
							);
			}
			liftingSurfaceCreator.getTheLiftingSurfaceInterface().getPanels().get(i-1).getAirfoilTip().setClMax(
					liftingSurfaceCreator.getTheLiftingSurfaceInterface().getPanels().get(i-1).getAirfoilTip().getClMax() * Math.pow( Math.cos(
							liftingSurfaceCreator.getTheLiftingSurfaceInterface().getEquivalentWing().getPanels().get(0).getSweepLeadingEdge().doubleValue(SI.RADIAN)), 2)
							);
		}
		else if(type.equals(ComponentEnum.HORIZONTAL_TAIL)) { 

			//---------------------------------------------------------------------------------
			// GLOBAL DATA
			String mainSparPositionProperty = reader.getXMLPropertyByPath("//global_data/main_spar_non_dimensional_position");
			if(mainSparPositionProperty != null)
				mainSparDimensionlessPosition = Double.valueOf(mainSparPositionProperty);
			
			String secondarySparPositionProperty = reader.getXMLPropertyByPath("//global_data/secondary_spar_non_dimensional_position");
			if(secondarySparPositionProperty != null)
				secondarySparDimensionlessPosition = Double.valueOf(secondarySparPositionProperty);
			
			String roughnessProperty = reader.getXMLPropertyByPath("//global_data/roughness");
			if(roughnessProperty != null)
				roughness = reader.getXMLAmountLengthByPath("//global_data/roughness");
			
			//---------------------------------------------------------------------------------
			// PANELS
			NodeList nodelistPanel = MyXMLReaderUtils
					.getXMLNodeListByPath(reader.getXmlDoc(), "//panels/panel");

			System.out.println("Panels found: " + nodelistPanel.getLength());

			for (int i = 0; i < nodelistPanel.getLength(); i++) {
				Node nodePanel  = nodelistPanel.item(i); // .getNodeValue();
				Element elementPanel = (Element) nodePanel;
				System.out.println("[" + i + "]\nPanel id: " + elementPanel.getAttribute("id"));
				if (elementPanel.getAttribute("linked_to").isEmpty()) {
					panels.add(LiftingSurfacePanelCreator.importFromPanelNode(nodePanel, airfoilsDir));
				} else {
					LiftingSurfacePanelCreator panel0 = panels.stream()
							.filter(p -> p.getId().equals(elementPanel.getAttribute("linked_to")))
							.findFirst()
							.get()
							;
					if (panel0 != null) {
						System.out.println("Panel linked_to: **" + elementPanel.getAttribute("linked_to") + "**");
						panels.add(LiftingSurfacePanelCreator.importFromPanelNodeLinked(nodePanel, panel0, airfoilsDir));
					} else {
						System.out.println("WARNING: panel not parsed. Unable to find the ID of linked_to attribute!");
					}
				}
			}

			//---------------------------------------------------------------------------------
			// SYMMETRIC FLAPS
			NodeList nodelistFlaps = MyXMLReaderUtils
					.getXMLNodeListByPath(reader.getXmlDoc(), "//symmetric_flaps/symmetric_flap");

			System.out.println("Symmetric flaps found: " + nodelistFlaps.getLength());

			for (int i = 0; i < nodelistFlaps.getLength(); i++) {
				Node nodeFlap  = nodelistFlaps.item(i); // .getNodeValue();
				Element elementFlap = (Element) nodeFlap;
				System.out.println("[" + i + "]\nFlap id: " + elementFlap.getAttribute("id"));

				symmetricFlaps.add(SymmetricFlapCreator.importFromSymmetricFlapNode(nodeFlap, type));

			}
			
			liftingSurfaceCreator = new LiftingSurface(
					new ILiftingSurface.Builder()
					.setId(id)
					.setMirrored(mirrored)
					.setType(type)
					.setMainSparDimensionlessPosition(mainSparDimensionlessPosition)
					.setSecondarySparDimensionlessPosition(secondarySparDimensionlessPosition)
					.setRoughness(roughness)
					.setWingletHeight(wingletHeight)
					.addAllPanels(panels)
					.addAllSymmetricFlaps(symmetricFlaps)
					.addAllAsymmetricFlaps(asymmetricFlaps)
					.addAllSlats(slats)
					.addAllSpoilers(spoilers)
					.buildPartial()
					);
			
		}
		else if(type.equals(ComponentEnum.VERTICAL_TAIL)) {
			
			//---------------------------------------------------------------------------------
			// GLOBAL DATA
			String mainSparPositionProperty = reader.getXMLPropertyByPath("//global_data/main_spar_non_dimensional_position");
			if(mainSparPositionProperty != null)
				mainSparDimensionlessPosition = Double.valueOf(mainSparPositionProperty);
			
			String secondarySparPositionProperty = reader.getXMLPropertyByPath("//global_data/secondary_spar_non_dimensional_position");
			if(secondarySparPositionProperty != null)
				secondarySparDimensionlessPosition = Double.valueOf(secondarySparPositionProperty);
			
			String roughnessProperty = reader.getXMLPropertyByPath("//global_data/roughness");
			if(roughnessProperty != null)
				roughness = reader.getXMLAmountLengthByPath("//global_data/roughness");
			
			//---------------------------------------------------------------------------------
			// PANELS
			NodeList nodelistPanel = MyXMLReaderUtils
					.getXMLNodeListByPath(reader.getXmlDoc(), "//panels/panel");

			System.out.println("Panels found: " + nodelistPanel.getLength());

			for (int i = 0; i < nodelistPanel.getLength(); i++) {
				Node nodePanel  = nodelistPanel.item(i); // .getNodeValue();
				Element elementPanel = (Element) nodePanel;
				System.out.println("[" + i + "]\nPanel id: " + elementPanel.getAttribute("id"));
				if (elementPanel.getAttribute("linked_to").isEmpty()) {
					panels.add(LiftingSurfacePanelCreator.importFromPanelNode(nodePanel, airfoilsDir));
				} else {
					LiftingSurfacePanelCreator panel0 = panels.stream()
							.filter(p -> p.getId().equals(elementPanel.getAttribute("linked_to")))
							.findFirst()
							.get()
							;
					if (panel0 != null) {
						System.out.println("Panel linked_to: **" + elementPanel.getAttribute("linked_to") + "**");
						panels.add(LiftingSurfacePanelCreator.importFromPanelNodeLinked(nodePanel, panel0, airfoilsDir));
					} else {
						System.out.println("WARNING: panel not parsed. Unable to find the ID of linked_to attribute!");
					}
				}
			}

			//---------------------------------------------------------------------------------
			// SYMMETRIC FLAPS
			NodeList nodelistFlaps = MyXMLReaderUtils
					.getXMLNodeListByPath(reader.getXmlDoc(), "//symmetric_flaps/symmetric_flap");

			System.out.println("Symmetric flaps found: " + nodelistFlaps.getLength());

			for (int i = 0; i < nodelistFlaps.getLength(); i++) {
				Node nodeFlap  = nodelistFlaps.item(i); // .getNodeValue();
				Element elementFlap = (Element) nodeFlap;
				System.out.println("[" + i + "]\nFlap id: " + elementFlap.getAttribute("id"));

				symmetricFlaps.add(SymmetricFlapCreator.importFromSymmetricFlapNode(nodeFlap, type));

			}

			liftingSurfaceCreator = new LiftingSurface(
					new ILiftingSurface.Builder()
					.setId(id)
					.setMirrored(mirrored)
					.setType(type)
					.setMainSparDimensionlessPosition(mainSparDimensionlessPosition)
					.setSecondarySparDimensionlessPosition(secondarySparDimensionlessPosition)
					.setRoughness(roughness)
					.setWingletHeight(wingletHeight)
					.addAllPanels(panels)
					.addAllSymmetricFlaps(symmetricFlaps)
					.addAllAsymmetricFlaps(asymmetricFlaps)
					.addAllSlats(slats)
					.addAllSpoilers(spoilers)
					.buildPartial()
					);
			
		}
		else if(type.equals(ComponentEnum.CANARD)) {

			//---------------------------------------------------------------------------------
			// GLOBAL DATA
			String mainSparPositionProperty = reader.getXMLPropertyByPath("//global_data/main_spar_non_dimensional_position");
			if(mainSparPositionProperty != null)
				mainSparDimensionlessPosition = Double.valueOf(mainSparPositionProperty);
			
			String secondarySparPositionProperty = reader.getXMLPropertyByPath("//global_data/secondary_spar_non_dimensional_position");
			if(secondarySparPositionProperty != null)
				secondarySparDimensionlessPosition = Double.valueOf(secondarySparPositionProperty);
			
			String roughnessProperty = reader.getXMLPropertyByPath("//global_data/roughness");
			if(roughnessProperty != null)
				roughness = reader.getXMLAmountLengthByPath("//global_data/roughness");
			
			//---------------------------------------------------------------------------------
			// PANELS
			NodeList nodelistPanel = MyXMLReaderUtils
					.getXMLNodeListByPath(reader.getXmlDoc(), "//panels/panel");

			System.out.println("Panels found: " + nodelistPanel.getLength());

			for (int i = 0; i < nodelistPanel.getLength(); i++) {
				Node nodePanel  = nodelistPanel.item(i); // .getNodeValue();
				Element elementPanel = (Element) nodePanel;
				System.out.println("[" + i + "]\nPanel id: " + elementPanel.getAttribute("id"));
				if (elementPanel.getAttribute("linked_to").isEmpty()) {
					panels.add(LiftingSurfacePanelCreator.importFromPanelNode(nodePanel, airfoilsDir));
				} else {
					LiftingSurfacePanelCreator panel0 = panels.stream()
							.filter(p -> p.getId().equals(elementPanel.getAttribute("linked_to")))
							.findFirst()
							.get()
							;
					if (panel0 != null) {
						System.out.println("Panel linked_to: **" + elementPanel.getAttribute("linked_to") + "**");
						panels.add(LiftingSurfacePanelCreator.importFromPanelNodeLinked(nodePanel, panel0, airfoilsDir));
					} else {
						System.out.println("WARNING: panel not parsed. Unable to find the ID of linked_to attribute!");
					}
				}
			}

			// SYMMETRIC FLAPS
			NodeList nodelistFlaps = MyXMLReaderUtils
					.getXMLNodeListByPath(reader.getXmlDoc(), "//symmetric_flaps/symmetric_flap");

			System.out.println("Symmetric flaps found: " + nodelistFlaps.getLength());

			for (int i = 0; i < nodelistFlaps.getLength(); i++) {
				Node nodeFlap  = nodelistFlaps.item(i); // .getNodeValue();
				Element elementFlap = (Element) nodeFlap;
				System.out.println("[" + i + "]\nFlap id: " + elementFlap.getAttribute("id"));

				symmetricFlaps.add(SymmetricFlapCreator.importFromSymmetricFlapNode(nodeFlap, type));

			}

			liftingSurfaceCreator = new LiftingSurface(
					new ILiftingSurface.Builder()
					.setId(id)
					.setMirrored(mirrored)
					.setType(type)
					.setMainSparDimensionlessPosition(mainSparDimensionlessPosition)
					.setSecondarySparDimensionlessPosition(secondarySparDimensionlessPosition)
					.setRoughness(roughness)
					.setWingletHeight(wingletHeight)
					.addAllPanels(panels)
					.addAllSymmetricFlaps(symmetricFlaps)
					.addAllAsymmetricFlaps(asymmetricFlaps)
					.addAllSlats(slats)
					.addAllSpoilers(spoilers)
					.buildPartial()
					);
			
		}
		
		return liftingSurfaceCreator;
	}

	public void calculateGeometry(ComponentEnum type, Boolean mirrored) {
		calculateGeometry(_numberOfSpanwisePoints, type, mirrored);
	}

	public void calculateGeometry(int numberSpanwiseStations, ComponentEnum type, Boolean mirrored) {

		System.out.println("[LiftingSurfaceCreator] Calculating derived geometry parameters of lifting surface ...");

		initializeData();
		
		// Update inner geometric variables of each panel
		_theLiftingSurfaceInterface.getPanels().stream()
		.forEach(LiftingSurfacePanelCreator::calculateGeometry);

		// Total planform area
		if(mirrored) {
			double surfPlanform = _theLiftingSurfaceInterface.getPanels().stream()
					.mapToDouble(p -> p.getSurfacePlanform().doubleValue(SI.SQUARE_METRE))
					.sum();
			_surfacePlanform = Amount.valueOf(surfPlanform,SI.SQUARE_METRE).times(2);
		}
		else {
			double surfPlanform = _theLiftingSurfaceInterface.getPanels().stream()
					.mapToDouble(p -> p.getSurfacePlanform().doubleValue(SI.SQUARE_METRE))
					.sum();
			_surfacePlanform = Amount.valueOf(surfPlanform,SI.SQUARE_METRE);
		}
		
		// Total wetted area
		if(mirrored) {
			double surfWetted = _theLiftingSurfaceInterface.getPanels().stream()
					.mapToDouble(p -> p.getSurfaceWetted().doubleValue(SI.SQUARE_METRE))
					.sum();
			_surfaceWetted = Amount.valueOf(surfWetted,SI.SQUARE_METRE).times(2);
		}
		else {
			double surfWetted = _theLiftingSurfaceInterface.getPanels().stream()
					.mapToDouble(p -> p.getSurfaceWetted().doubleValue(SI.SQUARE_METRE))
					.sum();
			_surfaceWetted = Amount.valueOf(surfWetted,SI.SQUARE_METRE);
		}
		
		//======================================================
		// Update semiSpan and span
		calculateSpans(mirrored);

		//======================================================
		// Calculate break-points
		calculateVariablesAtBreakpoints();

		//======================================================
		// Discretize the wing spanwise
		discretizeGeometry(numberSpanwiseStations);

		//======================================================
		// Aspect-ratio
		_aspectRatio = Math.pow(_span.doubleValue(SI.METER), 2) / _surfacePlanform.doubleValue(SI.SQUARE_METRE);

		//======================================================
		// Mean aerodynamic chord
		calculateMAC(mirrored);

		//======================================================
		// Mean aerodynamic chord leading-edge coordinates
		calculateXYZleMAC(mirrored);

		//======================================================
		// Generation of the Equivalent Lifting Surface 
		if(getPanels().size() == 1) {
			IEquivalentLiftingSurface equivalentWing = new IEquivalentLiftingSurface.Builder()
					.addAllPanels(getPanels())
					.setRealWingDimensionlessXOffsetRootChordLE(0.0)
					.setRealWingDimensionlessXOffsetRootChordTE(0.0)
					.build();
			
			setTheLiftingSurfaceInterface(
					ILiftingSurface.Builder
					.from(_theLiftingSurfaceInterface)
					.setEquivalentWing(equivalentWing)
					.buildPartial()
					);
		}
		else
			calculateEquivalentLiftingSurface();
		
		//======================================================
		// Control surface area calculation

		// symmetric flaps
		if(!_theLiftingSurfaceInterface.getSymmetricFlaps().isEmpty()) {
			for(int i=0; i<_theLiftingSurfaceInterface.getSymmetricFlaps().size(); i++) {
				_symmetricFlapsControlSurfaceAreaList.add(
						Amount.valueOf(
								(_theLiftingSurfaceInterface.getSymmetricFlaps().get(i).getInnerChordRatio()
										*getChordAtYActual(
												_theLiftingSurfaceInterface.getSymmetricFlaps().get(i).getInnerStationSpanwisePosition()
												) + 
										_theLiftingSurfaceInterface.getSymmetricFlaps().get(i).getOuterChordRatio()
										*getChordAtYActual(
												_theLiftingSurfaceInterface.getSymmetricFlaps().get(i).getOuterStationSpanwisePosition()
												)
										)
								*(_theLiftingSurfaceInterface.getSymmetricFlaps().get(i).getOuterStationSpanwisePosition()
										- _theLiftingSurfaceInterface.getSymmetricFlaps().get(i).getInnerStationSpanwisePosition())
								*_semiSpan.doubleValue(SI.METER),
								SI.SQUARE_METRE
								)
						);
			}
		}
		// Asymmetric Flaps
		if(!_theLiftingSurfaceInterface.getAsymmetricFlaps().isEmpty()) {
			for(int i=0; i<_theLiftingSurfaceInterface.getAsymmetricFlaps().size(); i++) {
				_asymmetricFlapsControlSurfaceAreaList.add(
						Amount.valueOf(
								(_theLiftingSurfaceInterface.getAsymmetricFlaps().get(i).getInnerChordRatio()
										*getChordAtYActual(
												_theLiftingSurfaceInterface.getAsymmetricFlaps().get(i).getInnerStationSpanwisePosition()
												) + 
										_theLiftingSurfaceInterface.getAsymmetricFlaps().get(i).getOuterChordRatio()
										*getChordAtYActual(
												_theLiftingSurfaceInterface.getAsymmetricFlaps().get(i).getOuterStationSpanwisePosition()
												)
										)
								*(_theLiftingSurfaceInterface.getAsymmetricFlaps().get(i).getOuterStationSpanwisePosition()
										- _theLiftingSurfaceInterface.getAsymmetricFlaps().get(i).getInnerStationSpanwisePosition())
								*_semiSpan.doubleValue(SI.METER),
								SI.SQUARE_METRE
								)
						);
			}
		}		
		// Slats
		if(!_theLiftingSurfaceInterface.getSlats().isEmpty()) {
			for(int i=0; i<_theLiftingSurfaceInterface.getSlats().size(); i++) {
				_slatsControlSurfaceAreaList.add(
						Amount.valueOf(
								(_theLiftingSurfaceInterface.getSlats().get(i).getInnerChordRatio()
										*getChordAtYActual(
												_theLiftingSurfaceInterface.getSlats().get(i).getInnerStationSpanwisePosition()
												) + 
										_theLiftingSurfaceInterface.getSlats().get(i).getOuterChordRatio()
										*getChordAtYActual(
												_theLiftingSurfaceInterface.getSlats().get(i).getOuterStationSpanwisePosition()
												)
										)
								*(_theLiftingSurfaceInterface.getSlats().get(i).getOuterStationSpanwisePosition()
										- _theLiftingSurfaceInterface.getSlats().get(i).getInnerStationSpanwisePosition())
								*_semiSpan.doubleValue(SI.METER),
								SI.SQUARE_METRE
								)
						);
			}
		}
		// Spoilers
		if(!_theLiftingSurfaceInterface.getSpoilers().isEmpty()) {
			for(int i=0; i<_theLiftingSurfaceInterface.getSpoilers().size(); i++) {
				
				double yIn = _semiSpan.times(
						_theLiftingSurfaceInterface.getSpoilers().get(i).getInnerStationSpanwisePosition()
						).doubleValue(SI.METER);
				double yOut = _semiSpan.times(
						_theLiftingSurfaceInterface.getSpoilers().get(i).getOuterStationSpanwisePosition()
						).doubleValue(SI.METER);
				
				double localChordInner = GeometryCalc.getChordAtYActual(
						MyArrayUtils.convertListOfAmountTodoubleArray(getDiscretizedYs()),
						MyArrayUtils.convertListOfAmountTodoubleArray(getDiscretizedChords()),
						yIn);
				double localChordOuter = GeometryCalc.getChordAtYActual(
						MyArrayUtils.convertListOfAmountTodoubleArray(getDiscretizedYs()),
						MyArrayUtils.convertListOfAmountTodoubleArray(getDiscretizedChords()),
						yOut);
				
				double xLELocalInnerChord = GeometryCalc.getXLEAtYActual(
						MyArrayUtils.convertListOfAmountTodoubleArray(getDiscretizedYs()),
						MyArrayUtils.convertListOfAmountTodoubleArray(getDiscretizedXle()),
						yIn);
				double xLELocalOuterChord = GeometryCalc.getXLEAtYActual(
						MyArrayUtils.convertListOfAmountTodoubleArray(getDiscretizedYs()),
						MyArrayUtils.convertListOfAmountTodoubleArray(getDiscretizedXle()),
						yOut);
				
				double innerChordwisePosition = _theLiftingSurfaceInterface.getSpoilers().get(i).getInnerStationChordwisePosition();
				double outerChordwisePosition = _theLiftingSurfaceInterface.getSpoilers().get(i).getOuterStationChordwisePosition();
				
				double innerChordRatio = _theLiftingSurfaceInterface.getSpoilers().get(i).getInnerStationChordRatio();
				double outerChordRatio = _theLiftingSurfaceInterface.getSpoilers().get(i).getOuterStationChordRatio();
				
				double b1 = (xLELocalInnerChord + (localChordInner*innerChordwisePosition) + (localChordInner*innerChordRatio))
						- (xLELocalInnerChord + (localChordInner*innerChordwisePosition) );
				double b2 = (xLELocalOuterChord + (localChordOuter*outerChordwisePosition) + (localChordOuter*outerChordRatio))
						- (xLELocalOuterChord + (localChordOuter*outerChordwisePosition) );
				double h = yOut - yIn;
				
				_spoilersControlSurfaceAreaList.add(
						Amount.valueOf(
								((b1 + b2) * h),
								SI.SQUARE_METRE
								)
						);
			}
		}
		
		_symmetricFlapsControlSurfaceArea = Amount.valueOf( 
				_symmetricFlapsControlSurfaceAreaList.stream().mapToDouble(a -> a.doubleValue(SI.SQUARE_METRE)).sum(),
				SI.SQUARE_METRE
				);
		_asymmetricFlapsControlSurfaceArea = Amount.valueOf( 
				_asymmetricFlapsControlSurfaceAreaList.stream().mapToDouble(a -> a.doubleValue(SI.SQUARE_METRE)).sum(),
				SI.SQUARE_METRE
				);
		_slatsControlSurfaceArea = Amount.valueOf( 
				_slatsControlSurfaceAreaList.stream().mapToDouble(a -> a.doubleValue(SI.SQUARE_METRE)).sum(),
				SI.SQUARE_METRE
				);
		_spoilersControlSurfaceArea = Amount.valueOf( 
				_spoilersControlSurfaceAreaList.stream().mapToDouble(a -> a.doubleValue(SI.SQUARE_METRE)).sum(),
				SI.SQUARE_METRE
				);
		
		_totalControlSurfaceArea = _symmetricFlapsControlSurfaceArea.to(SI.SQUARE_METRE)
				.plus(_asymmetricFlapsControlSurfaceArea).to(SI.SQUARE_METRE)
				.plus(_slatsControlSurfaceArea.to(SI.SQUARE_METRE))
				.plus(_spoilersControlSurfaceArea).to(SI.SQUARE_METRE);
		
	}
	
	// New formulation using geometric analytic formulas
	private void calculateEquivalentLiftingSurface() {
		
		// ======================================================
		// ----------------- LEADING EDGE -----------------------
		// ======================================================
		double area1;
		Double xOffsetEquivalentWingRootLE;
		
		int nSec = this.getDiscretizedXle().size();
		
		List<Amount<Length>> discretizedYs = this.getDiscretizedYs();
		List<Amount<Length>> discretizedXle = this.getDiscretizedXle();
		
		double xLERoot = discretizedXle.get(0).doubleValue(SI.METER);
		double xLEKink = this.getXLEBreakPoints().get(1).doubleValue(SI.METER);
		double xLETip = discretizedXle.get(nSec-1).doubleValue(SI.METER);
		
		if(xLETip - xLEKink >= 0) { // case 1
			
			if(xLETip - xLERoot >= 0) { // case 1.1
				
				area1 = MyMathUtils.areaPolygon(
						new double[] {discretizedXle.get(0).doubleValue(SI.METER), discretizedYs.get(0).doubleValue(SI.METER)},
						new double[] {discretizedXle.get(nSec-1).doubleValue(SI.METER), discretizedYs.get(0).doubleValue(SI.METER)},
						new double[] {discretizedXle.get(nSec-1).doubleValue(SI.METER), discretizedYs.get(nSec-1).doubleValue(SI.METER)},
						new double[] {this.getXLEBreakPoints().get(1).doubleValue(SI.METER), this.getYBreakPoints().get(1).doubleValue(SI.METER)}			
						);
				
				xOffsetEquivalentWingRootLE = 
						(discretizedXle.get(nSec-1).doubleValue(SI.METER)*this._semiSpan.doubleValue(SI.METER) - 2*area1)
							/this._semiSpan.doubleValue(SI.METER);
				
			} else { // case 1.2
				
				area1 = MyMathUtils.areaPolygon(
						new double[] {discretizedXle.get(0).doubleValue(SI.METER), discretizedYs.get(0).doubleValue(SI.METER)},
						new double[] {discretizedXle.get(0).doubleValue(SI.METER), discretizedYs.get(nSec-1).doubleValue(SI.METER)},
						new double[] {discretizedXle.get(nSec-1).doubleValue(SI.METER), discretizedYs.get(nSec-1).doubleValue(SI.METER)},
						new double[] {this.getXLEBreakPoints().get(1).doubleValue(SI.METER), this.getYBreakPoints().get(1).doubleValue(SI.METER)}
						);
				
				xOffsetEquivalentWingRootLE = 
						(2*area1 - discretizedXle.get(nSec-1).doubleValue(SI.METER)*this._semiSpan.doubleValue(SI.METER))
							/this._semiSpan.doubleValue(SI.METER);
				
			}
					
		} else { // case 2
			
			if(xLEKink - xLERoot < 0) { // case 2.2
				
				area1 = MyMathUtils.areaPolygon(
						new double[] {discretizedXle.get(0).doubleValue(SI.METER), discretizedYs.get(0).doubleValue(SI.METER)},
						new double[] {this.getXLEBreakPoints().get(1).doubleValue(SI.METER), this.getYBreakPoints().get(1).doubleValue(SI.METER)},
						new double[] {discretizedXle.get(nSec-1).doubleValue(SI.METER), discretizedYs.get(nSec-1).doubleValue(SI.METER)},
						new double[] {discretizedXle.get(nSec-1).doubleValue(SI.METER), discretizedYs.get(0).doubleValue(SI.METER)}					
						);
				
				xOffsetEquivalentWingRootLE = 
						(2*area1 + discretizedXle.get(nSec-1).doubleValue(SI.METER)*this._semiSpan.doubleValue(SI.METER))
							/this._semiSpan.doubleValue(SI.METER);
						
			} else { // case 2.1
				
				if(xLETip - xLERoot >= 0) { // case 2.1.1
					
					area1 = MyMathUtils.areaPolygon(
							new double[] {discretizedXle.get(0).doubleValue(SI.METER), discretizedYs.get(0).doubleValue(SI.METER)},
							new double[] {this.getXLEBreakPoints().get(1).doubleValue(SI.METER), this.getYBreakPoints().get(1).doubleValue(SI.METER)},
							new double[] {discretizedXle.get(nSec-1).doubleValue(SI.METER), discretizedYs.get(nSec-1).doubleValue(SI.METER)},
							new double[] {discretizedXle.get(0).doubleValue(SI.METER), discretizedYs.get(nSec-1).doubleValue(SI.METER)}
							);
					
					xOffsetEquivalentWingRootLE = 
							(2*area1 - discretizedXle.get(nSec-1).doubleValue(SI.METER)*this._semiSpan.doubleValue(SI.METER))
								/this._semiSpan.doubleValue(SI.METER);
					
				} else { // case 2.1.2
					
					area1 = MyMathUtils.areaPolygon(
							new double[] {discretizedXle.get(0).doubleValue(SI.METER), discretizedYs.get(0).doubleValue(SI.METER)},
							new double[] {this.getXLEBreakPoints().get(1).doubleValue(SI.METER), this.getYBreakPoints().get(1).doubleValue(SI.METER)},
							new double[] {discretizedXle.get(nSec-1).doubleValue(SI.METER), discretizedYs.get(nSec-1).doubleValue(SI.METER)},
							new double[] {discretizedXle.get(nSec-1).doubleValue(SI.METER), discretizedYs.get(0).doubleValue(SI.METER)}
							);
					
					xOffsetEquivalentWingRootLE = 
							(2*area1 + discretizedXle.get(nSec-1).doubleValue(SI.METER)*this._semiSpan.doubleValue(SI.METER))
								/this._semiSpan.doubleValue(SI.METER);
							
				}
			}		
		}				
		
		// ======================================================
		// ----------------- TRAILING EDGE ----------------------
		// ======================================================	
		double area2;
		Double xEquivalentWingRootTE;
		
		Tuple2<
			List<Amount<Length>>, // Xle
			List<Amount<Length>>  // c
			> xlePlusCTuple = Tuple.of(this.getDiscretizedXle(), this.getDiscretizedChords());

		List<Double> xlePlusC = IntStream.range(0, this.getDiscretizedYs().size())
				.mapToObj(i -> 
					xlePlusCTuple._1.get(i).doubleValue(SI.METRE)
					+ xlePlusCTuple._2.get(i).doubleValue(SI.METRE))
				.collect(Collectors.toList());
		
		double xTERoot = xlePlusC.get(0);
		double xTEKink = this.getXLEBreakPoints().get(1).plus(this.getChordsBreakPoints().get(1)).doubleValue(SI.METER);
		double xTETip = xlePlusC.get(nSec-1);
		
		if(xTETip - xTEKink >= 0) { // case 1
			
			if(xTETip - xTERoot >= 0) { // case 1.1
				
				area2 = MyMathUtils.areaPolygon(
						new double[] {xlePlusC.get(0), discretizedYs.get(0).doubleValue(SI.METER)},
						new double[] {xlePlusC.get(nSec-1), discretizedYs.get(0).doubleValue(SI.METER)},
						new double[] {xlePlusC.get(nSec-1), discretizedYs.get(nSec-1).doubleValue(SI.METER)},
						new double[] {xTEKink, this.getYBreakPoints().get(1).doubleValue(SI.METER)}
						);
				
				xEquivalentWingRootTE = 
						(xlePlusC.get(nSec-1)*this._semiSpan.doubleValue(SI.METER) - 2*area2)
							/this._semiSpan.doubleValue(SI.METER);
				
			} else { // case 1.2
				
				area2 = MyMathUtils.areaPolygon(
						new double[] {xlePlusC.get(0), discretizedYs.get(0).doubleValue(SI.METER)},
						new double[] {xlePlusC.get(0), discretizedYs.get(nSec-1).doubleValue(SI.METER)},
						new double[] {xlePlusC.get(nSec-1), discretizedYs.get(nSec-1).doubleValue(SI.METER)},
						new double[] {xTEKink, this.getYBreakPoints().get(1).doubleValue(SI.METER)}
						);
				
				xEquivalentWingRootTE = 
						(2*xlePlusC.get(0)*this._semiSpan.doubleValue(SI.METER) 
								- xlePlusC.get(nSec-1)*this._semiSpan.doubleValue(SI.METER) - 2*area2)
							/this._semiSpan.doubleValue(SI.METER);
				
			}
			
		} else { // case 2
			
			if(xTEKink - xTERoot < 0) { // case 2.2
				
				area2 = MyMathUtils.areaPolygon(
						new double[] {xlePlusC.get(0), discretizedYs.get(0).doubleValue(SI.METER)},
						new double[] {xTEKink, this.getYBreakPoints().get(1).doubleValue(SI.METER)},
						new double[] {xlePlusC.get(nSec-1), discretizedYs.get(nSec-1).doubleValue(SI.METER)},
						new double[] {xlePlusC.get(nSec-1), discretizedYs.get(0).doubleValue(SI.METER)}
						);
				
				xEquivalentWingRootTE = 
						(2*area2 + xlePlusC.get(nSec-1)*this._semiSpan.doubleValue(SI.METER))
							/this._semiSpan.doubleValue(SI.METER);
				
			} else { // case 2.1
				
				if(xTETip - xTERoot >= 0) { // case 2.1.1
					
					area2 = MyMathUtils.areaPolygon(
							new double[] {xlePlusC.get(0), discretizedYs.get(0).doubleValue(SI.METER)},
							new double[] {xTEKink, this.getYBreakPoints().get(1).doubleValue(SI.METER)},
							new double[] {xlePlusC.get(nSec-1), discretizedYs.get(nSec-1).doubleValue(SI.METER)},
							new double[] {xlePlusC.get(0), discretizedYs.get(nSec-1).doubleValue(SI.METER)}
							);
					
					xEquivalentWingRootTE = 
							(2*area2 - xlePlusC.get(nSec-1)*this._semiSpan.doubleValue(SI.METER) 
									+ 2*xlePlusC.get(0)*this._semiSpan.doubleValue(SI.METER))
								/this._semiSpan.doubleValue(SI.METER);
					
				} else { // case 2.1.2
					
					area2 = MyMathUtils.areaPolygon(
							new double[] {xlePlusC.get(0), discretizedYs.get(0).doubleValue(SI.METER)},
							new double[] {xTEKink, this.getYBreakPoints().get(1).doubleValue(SI.METER)},
							new double[] {xlePlusC.get(nSec-1), discretizedYs.get(nSec-1).doubleValue(SI.METER)},
							new double[] {xlePlusC.get(nSec-1), discretizedYs.get(0).doubleValue(SI.METER)}
							);
					
					xEquivalentWingRootTE = 
							(2*area2 + xlePlusC.get(nSec-1)*this._semiSpan.doubleValue(SI.METER))
								/this._semiSpan.doubleValue(SI.METER);
				}
			}
		}
		
		Double xOffsetEquivalentWingRootTE = xEquivalentWingRootTE - xlePlusC.get(0);
		
		// Determine real LE and TE extension of the wing linear panel root
		double extensionLERootChordLinPanel;
		double extensionTERootChordLinPanel;
		
		Tuple2<
			List<Amount<Length>>,
			List<Amount<Length>> 
			> xTETuple = Tuple.of(this.getXLEBreakPoints(), this.getChordsBreakPoints());

		List<Amount<Length>> xTEBreakPoints = IntStream.range(0, this.getYBreakPoints().size())
				.mapToObj(i -> xTETuple._1.get(i).plus(xTETuple._2.get(i)))
				.collect(Collectors.toList());

		Double[] xLEPanel1 = MyArrayUtils.convertListOfAmountToDoubleArray(this.getXLEBreakPoints());

		Double[] xTEPanel1 = MyArrayUtils.convertListOfAmountToDoubleArray(xTEBreakPoints);

		Double[] yPanel1 = MyArrayUtils.convertListOfAmountToDoubleArray(this.getYBreakPoints());
		
		double xLEChordLinPanel = xLEPanel1[2] + yPanel1[2]/(yPanel1[2] - yPanel1[1])*(xLEPanel1[1] - xLEPanel1[2]);
		double xTEChordLinPanel = xTEPanel1[2] + yPanel1[2]/(yPanel1[2] - yPanel1[1])*(xTEPanel1[1] - xTEPanel1[2]);
		
		double chordLinPanel = xTEChordLinPanel - xLEChordLinPanel;
		
		extensionLERootChordLinPanel = (xLEChordLinPanel - xLEPanel1[0])/chordLinPanel;
		extensionTERootChordLinPanel = (xTEPanel1[0] - xTEChordLinPanel)/chordLinPanel;
		
		//======================================================
		// Equivalent wing parameters: 
		Amount<Length> chordRootEquivalentWing = Amount.valueOf(
				xEquivalentWingRootTE - xOffsetEquivalentWingRootLE, SI.METER);
		
		Amount<Length> chordTipEquivalentWing = _theLiftingSurfaceInterface.getPanels().get(
				_theLiftingSurfaceInterface.getPanels().size()-1
				).getChordTip();
		
		Airfoil airfoilRootEquivalent = _theLiftingSurfaceInterface.getPanels().get(0).getAirfoilRoot();
		Airfoil airfoilTipEquivalent = _theLiftingSurfaceInterface.getPanels().get(
				_theLiftingSurfaceInterface.getPanels().size()-1)
				.getAirfoilTip();
		
		Amount<Angle> twistGeometricTipEquivalentWing = _theLiftingSurfaceInterface.getPanels().get(
				_theLiftingSurfaceInterface.getPanels().size()-1)
				.getTwistGeometricAtTip();
		
		Amount<Angle> sweepLEEquivalentWing = Amount.valueOf(
				Math.atan(
						(getDiscretizedXle().get(getDiscretizedXle().size()-1).doubleValue(SI.METER) - xOffsetEquivalentWingRootLE)
						/getSemiSpan().doubleValue(SI.METER)
						),
				SI.RADIAN)
				.to(NonSI.DEGREE_ANGLE);
		
		Amount<Angle> dihedralEquivalentWing = Amount.valueOf(0.0, NonSI.DEGREE_ANGLE);
		for(int i=0; i<_theLiftingSurfaceInterface.getPanels().size(); i++)
			dihedralEquivalentWing = dihedralEquivalentWing.plus(_theLiftingSurfaceInterface.getPanels().get(i).getDihedral());
		dihedralEquivalentWing = dihedralEquivalentWing.divide(_theLiftingSurfaceInterface.getPanels().size());
		
		//======================================================
		// creation of the equivalent wing:
		LiftingSurfacePanelCreator equivalentWingPanel = new 
				LiftingSurfacePanelCreator(
						new ILiftingSurfacePanelCreator.Builder()
						.setId("Equivalent Wing Panel")
						.setLinkedTo(false)
						.setChordRoot(chordRootEquivalentWing)
						.setChordTip(chordTipEquivalentWing)
						.setAirfoilRoot(airfoilRootEquivalent)
						.setAirfoilTip(airfoilTipEquivalent)
						.setTwistGeometricAtRoot(Amount.valueOf(0.0, NonSI.DEGREE_ANGLE))
						.setTwistGeometricAtTip(twistGeometricTipEquivalentWing)
						.setSpan(getSemiSpan())
						.setSweepLeadingEdge(sweepLEEquivalentWing)
						.setDihedral(dihedralEquivalentWing)
						.buildPartial()
						);
		
		setXOffsetEquivalentWingRootLE(xOffsetEquivalentWingRootLE);
		setXOffsetEquivalentWingRootTE(xOffsetEquivalentWingRootTE);
		
		IEquivalentLiftingSurface equivalentWing = new IEquivalentLiftingSurface.Builder()
				.addPanels(equivalentWingPanel)
				.setRealWingDimensionlessXOffsetRootChordLE(extensionLERootChordLinPanel)
				.setRealWingDimensionlessXOffsetRootChordTE(extensionTERootChordLinPanel)
				.build();
		
		setTheLiftingSurfaceInterface(
				ILiftingSurface.Builder
				.from(_theLiftingSurfaceInterface)
				.setEquivalentWing(equivalentWing)
				.buildPartial()
				);
	}
	
	private void calculateMAC(Boolean mirrored) {

		// Mean Aerodynamic Chord

		//======================================================
		// Weighted sum on MACs of single _panels
		//======================================================
		// mac = (2/S) * int_0^(b/2) c^2 dy (if mirrored)
		// mac = (1/S) * int_0^(b/2) c^2 dy (if not mirrored)
		
		if (mirrored) {
			Double mac = MyMathUtils.integrate1DSimpsonSpline(
					MyArrayUtils.convertListOfAmountTodoubleArray(
							this.getDiscretizedYs()), // y
					MyArrayUtils.convertListOfAmountTodoubleArray(
							this.getDiscretizedChords().stream()
							.map(c -> c.pow(2))
							.collect(Collectors.toList())
							) // c^2
					);
			mac = 2.0 * mac / this.getSurfacePlanform().doubleValue(SI.SQUARE_METRE); // *= 2/S
			_meanAerodynamicChord = Amount.valueOf(mac,1e-9,SI.METRE);
		}
		else {
			Double mac = MyMathUtils.integrate1DSimpsonSpline(
					MyArrayUtils.convertListOfAmountTodoubleArray(
							this.getDiscretizedYs()), // y
					MyArrayUtils.convertListOfAmountTodoubleArray(
							this.getDiscretizedChords().stream()
							.map(c -> c.pow(2))
							.collect(Collectors.toList())
							) // c^2
					);
			mac = mac / this.getSurfacePlanform().doubleValue(SI.SQUARE_METRE); // *= 2/S
			_meanAerodynamicChord = Amount.valueOf(mac,1e-9,SI.METRE);
		}
	}

	private void calculateXYZleMAC(Boolean mirrored) {

		//======================================================
		// x_le_mac = (2/S) * int_0^(b/2) xle(y) c(y) dy (if mirrored)
		// x_le_mac = (2/S) * int_0^(b/2) xle(y) c(y) dy (if not mirrored)

		Tuple2<
			List<Amount<Length>>, // Xle
			List<Amount<Length>>  // c
		> xleTimeCTuple = Tuple.of(this.getDiscretizedXle(), this.getDiscretizedChords());
		
		List<Double> xleTimeC = IntStream.range(0, this.getDiscretizedYs().size())
				.mapToObj(i -> 
					xleTimeCTuple._1.get(i).doubleValue(SI.METRE)
					*xleTimeCTuple._2.get(i).doubleValue(SI.METRE)) // xle * c
				.collect(Collectors.toList());
		
		Double xle = MyMathUtils.integrate1DSimpsonSpline(
				MyArrayUtils.convertListOfAmountTodoubleArray(
						this.getDiscretizedYs()), // y
				MyArrayUtils.convertToDoublePrimitive(xleTimeC) // xle * c
			);
		
		if(mirrored) {
			xle = 2.0 * xle / this.getSurfacePlanform().doubleValue(SI.SQUARE_METRE);
			_meanAerodynamicChordLeadingEdgeX = Amount.valueOf(xle,1e-9,SI.METRE);
		}
		else {
			xle = xle / this.getSurfacePlanform().doubleValue(SI.SQUARE_METRE);
			_meanAerodynamicChordLeadingEdgeX = Amount.valueOf(xle,1e-9,SI.METRE);
		}
		//======================================================
		
		// y_le_mac = (2/S) * int_0^(b/2) yle(y) c(y) dy (if mirrored)
		// y_le_mac = (1/S) * int_0^(b/2) yle(y) c(y) dy (if not mirrored)

		Tuple2<
			List<Amount<Length>>, // Xle
			List<Amount<Length>>  // c
		> yTimeCTuple = Tuple.of(this.getDiscretizedYs(), this.getDiscretizedChords());

		List<Double> yTimeC = IntStream.range(0, this.getDiscretizedYs().size())
				.mapToObj(i -> 
					yTimeCTuple._1.get(i).doubleValue(SI.METRE)
					*yTimeCTuple._2.get(i).doubleValue(SI.METRE)) // y * c
				.collect(Collectors.toList());
		
		Double yle = MyMathUtils.integrate1DSimpsonSpline(
				MyArrayUtils.convertListOfAmountTodoubleArray(
						this.getDiscretizedYs()), // y
				MyArrayUtils.convertToDoublePrimitive(yTimeC) // y * c
			);
		if(mirrored) {
			yle = 2.0 * yle / this.getSurfacePlanform().doubleValue(SI.SQUARE_METRE); // *= 2/S
			_meanAerodynamicChordLeadingEdgeY = Amount.valueOf(yle,1e-9,SI.METRE);
		}
		else {
			yle = yle / this.getSurfacePlanform().doubleValue(SI.SQUARE_METRE); // *= 2/S
			_meanAerodynamicChordLeadingEdgeY = Amount.valueOf(yle,1e-9,SI.METRE);
		}
		//======================================================
		// z_le_mac = (2/S) * int_0^(b/2) zle(y) c(y) dy (if mirrored)
		// z_le_mac = (1/S) * int_0^(b/2) zle(y) c(y) dy (if not mirrored)
		Tuple2<
			List<Amount<Length>>, // Xle
			List<Amount<Length>>  // c
		> zTimeCTuple = Tuple.of(this.getDiscretizedZle(), this.getDiscretizedChords());

		List<Double> zTimeC = IntStream.range(0, this.getDiscretizedYs().size())
			.mapToObj(i -> 
				zTimeCTuple._1.get(i).doubleValue(SI.METRE)
				*zTimeCTuple._2.get(i).doubleValue(SI.METRE)) // z * c
			.collect(Collectors.toList());
		
		Double zle = MyMathUtils.integrate1DSimpsonSpline(
				MyArrayUtils.convertListOfAmountTodoubleArray(
						this.getDiscretizedYs()), // z
				MyArrayUtils.convertToDoublePrimitive(zTimeC) // z * c
			);
		if(mirrored) {
			zle = 2.0 * zle / this.getSurfacePlanform().doubleValue(SI.SQUARE_METRE); // *= 2/S
			_meanAerodynamicChordLeadingEdgeZ = Amount.valueOf(zle,1e-9,SI.METRE);
		}
		else {
			zle = zle / this.getSurfacePlanform().doubleValue(SI.SQUARE_METRE); // *= 2/S
			_meanAerodynamicChordLeadingEdgeZ = Amount.valueOf(zle,1e-9,SI.METRE);
		}
	}
	
	/**
	 * Calculate wing's span and semi-span according to current values
	 * i.e. _panels' semi-spans and dihedral angles
	 */
	private void calculateSpans(Boolean mirrored) {
		System.out.println("[LiftingSurfaceCreator] Lifting surface span ...");
		Double bhalf = _theLiftingSurfaceInterface.getPanels().stream()
				.mapToDouble(p ->
					p.getSpan().doubleValue(SI.METRE)
						*Math.cos(p.getDihedral().doubleValue(SI.RADIAN))
				)
				.sum();
		_semiSpan = Amount.valueOf(bhalf,SI.METRE);
		
		if(mirrored)
			_span = _semiSpan.times(2.0);
		else
			_span = _semiSpan;
	}

	private void calculateVariablesAtBreakpoints() {

		System.out.println("[LiftingSurfaceCreator] calculate variables at breakpoints ...");
		MyConfiguration.customizeAmountOutput();
		
		//======================================================
		// Break points Y's
		// root at symmetry plane
		_yBreakPoints.add(Amount.valueOf(0.0, 0.0, SI.METRE));
		// Accumulate values and add
		for(int i=1; i <= _theLiftingSurfaceInterface.getPanels().size(); i++) {
			_yBreakPoints.add(
					_yBreakPoints.get(i-1).plus( // semiSpan * cos( dihedral )
							_theLiftingSurfaceInterface.getPanels().get(i-1).getSpan()
								.times(Math.cos(_theLiftingSurfaceInterface.getPanels().get(i-1).getDihedral().doubleValue(SI.RADIAN))
										)
								)
					);
		}

		System.out.println("y Break-Points ->\n" + _yBreakPoints);

		// Leading-edge x at breakpoints
		_xLEBreakPoints.add(Amount.valueOf(0.0, 1e-8, SI.METRE));
		for (int i = 1; i <= _theLiftingSurfaceInterface.getPanels().size(); i++) {
			Amount<Length> x0 = _xLEBreakPoints.get(i-1);
			Amount<Length> y = _yBreakPoints.get(i).minus(_yBreakPoints.get(i-1));
			Amount<Angle> sweepLE = _theLiftingSurfaceInterface.getPanels().get(i-1).getSweepLeadingEdge();
			_xLEBreakPoints.add(
				x0.plus(
						y.times(Math.tan(sweepLE.doubleValue(SI.RADIAN)))
				));
		}

		System.out.println("xLE Break-Points ->\n" + _xLEBreakPoints);

		// Leading-edge z at breakpoints
		_zLEBreakPoints.add(Amount.valueOf(0.0, 1e-8, SI.METRE));
		for (int i = 1; i <= _theLiftingSurfaceInterface.getPanels().size(); i++) {
			Amount<Length> z0 = _zLEBreakPoints.get(i-1);
			Amount<Length> y = _yBreakPoints.get(i).minus(_yBreakPoints.get(i-1));
			Amount<Angle> dihedral = _theLiftingSurfaceInterface.getPanels().get(i-1).getDihedral();
			_zLEBreakPoints.add(
				z0.plus(
						y.times(Math.tan(dihedral.doubleValue(SI.RADIAN)))
				));
		}

		System.out.println("zLE Break-Points ->\n" + _zLEBreakPoints);

		// Chords at breakpoints
		_chordsBreakPoints.add(_theLiftingSurfaceInterface.getPanels().get(0).getChordRoot());
		for (int i = 0; i < _theLiftingSurfaceInterface.getPanels().size(); i++) {
			_chordsBreakPoints.add(
					_theLiftingSurfaceInterface.getPanels().get(i).getChordTip()
				);
		}

		System.out.println("Chords Break-Points ->\n" + _chordsBreakPoints);

		// Twists at breakpoints
		_twistsBreakPoints.add(Amount.valueOf(0.0,1e-9,NonSI.DEGREE_ANGLE));
		for (int i = 0; i < _theLiftingSurfaceInterface.getPanels().size(); i++) {
			_twistsBreakPoints.add(
					_theLiftingSurfaceInterface.getPanels().get(i).getTwistGeometricAtTip()
				);
		}

		// Dihedral at breakpoints
		_dihedralsBreakPoints.add(_theLiftingSurfaceInterface.getPanels().get(0).getDihedral());
		for(int i = 0; i < _theLiftingSurfaceInterface.getPanels().size(); i++) {
			_dihedralsBreakPoints.add(_theLiftingSurfaceInterface.getPanels().get(i).getDihedral());
		}
		
		System.out.println("Twists Break-Points ->\n" + _twistsBreakPoints);

		//======================================================
		// Break points eta's

		_etaBreakPoints = _yBreakPoints.stream()
			.mapToDouble(y ->
				y.to(SI.METRE).getEstimatedValue()/_semiSpan.doubleValue(SI.METRE))
			.boxed()
			.collect(Collectors.toList())
			;
		if(_etaBreakPoints.get(_etaBreakPoints.size()-1) != 1.0) {
			_etaBreakPoints.remove(_etaBreakPoints.size()-1);
			_etaBreakPoints.add(1.0);
		}
	}

	/**
	 * Calculate Chord Distribution of the Actual Wing along y axis
	 */
	private void mapPanelsToYDiscretized() {

		System.out.println("[LiftingSurfaceCreator] Map _panels to spanwise discretized Ys ...");
		MyConfiguration.customizeAmountOutput();
		
		//======================================================
		// Map _panels with lists of Y's, c, Xle, Yle, Zle, twist
		// for each panel Y's of inner and outer break-points
		// are included, i.e. Y's are repeated

		Tuple2<
			List<LiftingSurfacePanelCreator>,
			List<Amount<Length>>
			> tuple0 = Tuple.of(_theLiftingSurfaceInterface.getPanels(), _yStations);

		_panelToSpanwiseDiscretizedVariables.add(
			tuple0.map(
				p -> _theLiftingSurfaceInterface.getPanels().get(0),
				y -> Tuple.of(
						y.stream()
						// Innermost panel: Y's include 0 and panel's tip breakpoint Y
						.filter(y_ -> y_.isLessThan( _theLiftingSurfaceInterface.getPanels().get(0).getSpan() ) 
								|| y_.equals( _theLiftingSurfaceInterface.getPanels().get(0).getSpan()) )
						.collect(Collectors.toList())
						,
						y.stream()
						.filter(y_ -> y_.isLessThan( _theLiftingSurfaceInterface.getPanels().get(0).getSpan() ) 
								|| y_.equals( _theLiftingSurfaceInterface.getPanels().get(0).getSpan()) )
						.map(Y__ -> Amount.valueOf(0.0, SI.METRE))
						.collect(Collectors.toList()) // initialize Chords
						,
						y.stream()
						.filter(y_ -> y_.isLessThan( _theLiftingSurfaceInterface.getPanels().get(0).getSpan() )
								|| y_.equals( _theLiftingSurfaceInterface.getPanels().get(0).getSpan()) )
						.map(Y__ -> Amount.valueOf(0.0, SI.METRE))
						.collect(Collectors.toList()) // initialize Xle
						,
						y.stream()
						.filter(y_ -> y_.isLessThan( _theLiftingSurfaceInterface.getPanels().get(0).getSpan() )
								|| y_.equals( _theLiftingSurfaceInterface.getPanels().get(0).getSpan()) )
						.map(Y__ -> Amount.valueOf(0.0, SI.METRE))
						.collect(Collectors.toList()) // initialize Zle
						,
						y.stream()
						.filter(y_ -> y_.isLessThan( _theLiftingSurfaceInterface.getPanels().get(0).getSpan() ) 
								|| y_.equals( _theLiftingSurfaceInterface.getPanels().get(0).getSpan()) )
						.map(Y__ -> Amount.valueOf(0.0, SI.RADIAN))
						.collect(Collectors.toList()) // initialize twists
						)
				)
			);

		// All remaining _panels (innermost panel excluded)
		// Y's include only panel's tip breakpoint Y,
		// not including panel's root breakpoint Y
		for (int i=1; i < _theLiftingSurfaceInterface.getPanels().size(); i++) {
			final int i_ = i;
			_panelToSpanwiseDiscretizedVariables.add(
				tuple0.map(
					p -> _theLiftingSurfaceInterface.getPanels().get(i_),
					y -> Tuple.of(
						y.stream()
							.mapToDouble(a -> a.doubleValue(SI.METER))
							.filter(y_ -> (
									(Math.round(y_ * 100000) / 100000 > Math.round(_yBreakPoints.get(i_).doubleValue(SI.METER) * 100000) / 100000 )
									&& ( Math.round(y_ * 100000) / 100000 <= Math.round(_yBreakPoints.get(i_+1).doubleValue(SI.METER) * 100000) / 100000 )
									) )
							.mapToObj(y_ -> Amount.valueOf(y_, SI.METER))
							.collect(Collectors.toList())
						,
						y.stream()
							.mapToDouble(a -> a.doubleValue(SI.METER))
							.filter(y_ -> (
									(Math.round(y_ * 100000) / 100000 > Math.round(_yBreakPoints.get(i_).doubleValue(SI.METER) * 100000) / 100000 )
									&& ( Math.round(y_ * 100000) / 100000 <= Math.round(_yBreakPoints.get(i_+1).doubleValue(SI.METER) * 100000) / 100000 )
									) ) 
							.mapToObj(y_ -> Amount.valueOf(0.0, SI.METER))
							.collect(Collectors.toList()) // initialize Chords
						,
						y.stream()
							.mapToDouble(a -> a.doubleValue(SI.METER))
							.filter(y_ -> (
									(Math.round(y_ * 100000) / 100000 > Math.round(_yBreakPoints.get(i_).doubleValue(SI.METER) * 100000) / 100000 )
									&& ( Math.round(y_ * 100000) / 100000 <= Math.round(_yBreakPoints.get(i_+1).doubleValue(SI.METER) * 100000) / 100000 )
									) )
							.mapToObj(y_ -> Amount.valueOf(0.0, SI.METER))
							.collect(Collectors.toList()) // initialize Xle
						,
						y.stream()
							.mapToDouble(a -> a.doubleValue(SI.METER))
							.filter(y_ -> (
									(Math.round(y_ * 100000) / 100000 > Math.round(_yBreakPoints.get(i_).doubleValue(SI.METER) * 100000) / 100000 )
									&& ( Math.round(y_ * 100000) / 100000 <= Math.round(_yBreakPoints.get(i_+1).doubleValue(SI.METER) * 100000) / 100000 )
									) )
							.mapToObj(y_ -> Amount.valueOf(0.0, SI.METER))
							.collect(Collectors.toList()) // initialize Zle
						,
						y.stream()
							.mapToDouble(a -> a.doubleValue(SI.METER))
							.filter(y_ -> (
									(Math.round(y_ * 100000) / 100000 > Math.round(_yBreakPoints.get(i_).doubleValue(SI.METER) * 100000) / 100000 )
									&& ( Math.round(y_ * 100000) / 100000 <= Math.round(_yBreakPoints.get(i_+1).doubleValue(SI.METER) * 100000) / 100000 )
									) )
							.mapToObj(y_ -> Amount.valueOf(0.0, SI.RADIAN))
							.collect(Collectors.toList()) // initialize twists
						)
					)
				);
		}// end-of for

	}

	/**
	 * Calculate Chord Distribution of the Actual Wing along y axis
	 */
	private void calculateChordsAtYDiscretized() {

		System.out.println("[LiftingSurfaceCreator] Map _panels to spanwise discretized chords ...");

		//======================================================
		// Set chords versus Y's
		// according to location within _panels/yBP

		for (int k=0; k < _panelToSpanwiseDiscretizedVariables.size(); k++) {
			LiftingSurfacePanelCreator panel = _panelToSpanwiseDiscretizedVariables.get(k)._1();
			Amount<Length> y0 = _yBreakPoints.get(k);
			List<Amount<Length>> vY = _panelToSpanwiseDiscretizedVariables.get(k)._2()._1(); // Ys
			List<Amount<Length>> vC = _panelToSpanwiseDiscretizedVariables.get(k)._2()._2(); // Chords
			IntStream.range(0, vY.size())
				.forEach(i -> {
					Amount<Length> y = vY.get(i).minus(y0);
					// c(y) = cr + (2/b)*(ct - cr)*y
					Amount<Length> c = panel.getChordRoot().plus(
						y.times(
							panel.getChordTip().minus(panel.getChordRoot())
						).divide(panel.getSpan())
						);
					// assign the chord
					vC.set(i, c);
				});
		}

	}

	private void calculateXZleTwistAtYDiscretized() {

		System.out.println("[LiftingSurfaceCreator] Map _panels to spanwise discretized Xle, Yle, twist ...");

		for (int k=0; k < _panelToSpanwiseDiscretizedVariables.size(); k++) {
			LiftingSurfacePanelCreator panel = _panelToSpanwiseDiscretizedVariables.get(k)._1();
			Amount<Length> y0 = _yBreakPoints.get(k);
			Amount<Length> x0 = _xLEBreakPoints.get(k);
			Amount<Length> z0 = _zLEBreakPoints.get(k);
			Amount<Angle> twist0 = _twistsBreakPoints.get(k);

			List<Amount<Length>> vY = _panelToSpanwiseDiscretizedVariables.get(k)._2()._1(); // Ys
			@SuppressWarnings("unused")
			List<Amount<Length>> vC = _panelToSpanwiseDiscretizedVariables.get(k)._2()._2(); // Chords
			List<Amount<Length>> vXLE = _panelToSpanwiseDiscretizedVariables.get(k)._2()._3(); // XLEs
			List<Amount<Length>> vZLE = _panelToSpanwiseDiscretizedVariables.get(k)._2()._4(); // ZLEs
			List<Amount<Angle>> vTwistsLE = _panelToSpanwiseDiscretizedVariables.get(k)._2()._5(); // Twists

			Amount<Angle> sweepLE = panel.getSweepLeadingEdge();
			Amount<Angle> dihedral = panel.getDihedral();

			IntStream.range(0, vY.size())
				.forEach(i -> {
					// y := Y - y0
					Amount<Length> y = vY.get(i).minus(y0);
					// xle = x0 + y * tan(sweepLE)
					Amount<Length> xle =
						x0.plus(
							y.times(Math.tan(sweepLE.to(SI.RADIAN).getEstimatedValue()))
						);
					// assign the xle
					vXLE.set(i, xle);
					// zle = z0 + y * tan(dihedral)
					Amount<Length> zle =
							z0.plus(
								y.times(Math.tan(dihedral.to(SI.RADIAN).getEstimatedValue()))
							);
					vZLE.set(i, zle);
					//
					// twist(y) = twist_r + (2/b)*(twist_t - twist_r)*y
					Amount<Angle> twist = twist0.plus(
						y.times(
							panel.getTwistGeometricAtTip().minus(twist0)
						).divide(panel.getSpan())
						);
					// assign the chord
					vTwistsLE.set(i, twist);
				});
		}
	}

	private void calculateDiscretizedGeometry() {
		System.out.println("[LiftingSurfaceCreator] Map Ys to spanwise discretized variables ...");

		List<Amount<Length>> vy = new ArrayList<>();
		List<Amount<Length>> vc = new ArrayList<>();
		List<Amount<Length>> vxle = new ArrayList<>();
		List<Amount<Length>> vzle = new ArrayList<>();
		List<Amount<Angle>> vtwist = new ArrayList<>();

		for (int kp = 0; kp < _panelToSpanwiseDiscretizedVariables.size(); kp++) {

			// sublist indexing criteria
			int idxEnd = _panelToSpanwiseDiscretizedVariables.get(kp)
						._2()._1().size()
						// - 1 not necessary
						;
//			if (kp == (_panelToSpanwiseDiscretizedVariables.size() - 1))
//				idxEnd += 1;

			// System.out.println("kp=" + kp + ", end=" + idxEndExcluded);

			vy.addAll(
				_panelToSpanwiseDiscretizedVariables.get(kp)
					._2()._1() // Ys
					.subList(0, idxEnd)
					);
			vc.addAll(
					_panelToSpanwiseDiscretizedVariables.get(kp)
						._2()._2() // Chords
						.subList(0, idxEnd)
						);
			vxle.addAll(
					_panelToSpanwiseDiscretizedVariables.get(kp)
						._2()._3() // Xle's
						.subList(0, idxEnd)
						);
			vzle.addAll(
					_panelToSpanwiseDiscretizedVariables.get(kp)
						._2()._4() // Zle's
						.subList(0, idxEnd)
						);
			vtwist.addAll(
					_panelToSpanwiseDiscretizedVariables.get(kp)
						._2()._5() // Twists
						.subList(0, idxEnd)
						);
		}
//		System.out.println("== y ==> size: " + vy.size() + "\n" + vy);
//		System.out.println("== c ==> size: " + vc.size() + "\n" + vc);

		for(int i = 0; i < vy.size(); i++) {
			_spanwiseDiscretizedVariables.add(
				Tuple.of(
					vy.get(i),
					vc.get(i),
					vxle.get(i),
					vzle.get(i),
					vtwist.get(i)
					)
				);
		}

	}

	public void calculateThicknessMean() {
		
		Airfoil meanAirfoil = LSGeometryCalc.calculateMeanAirfoil(this);
		_thicknessMean = meanAirfoil.getThicknessToChordRatio();
		
	}
	
	public void calculateFormFactor(double compressibilityFactor) {

		if(_theLiftingSurfaceInterface.getType() == ComponentEnum.WING)
			// Wing Form Factor (ADAS pag 93 graphic or pag 9 meccanica volo appunti)
			_formFactor = ((1 + 1.2*_thicknessMean*
					Math.cos(_theLiftingSurfaceInterface.getEquivalentWing().getPanels().get(0).getSweepQuarterChord().doubleValue(SI.RADIAN))+
					100*Math.pow(compressibilityFactor,3)*
					(Math.pow(Math.cos(_theLiftingSurfaceInterface.getEquivalentWing().getPanels().get(0).getSweepQuarterChord().doubleValue(SI.RADIAN)),2))*
					Math.pow(_thicknessMean,4)));
		else if(_theLiftingSurfaceInterface.getType() == ComponentEnum.HORIZONTAL_TAIL)
			// HTail form factor from Giovanni Nardone thesis pag.86
			_formFactor = (1.03 
					+ (1.85*_thicknessMean) 
					+ (80*Math.pow(_thicknessMean,4))
					);
		else if(_theLiftingSurfaceInterface.getType() == ComponentEnum.VERTICAL_TAIL)
			// VTail form factor from Giovanni Nardone thesis pag.86
			_formFactor = (1.03 
					+ (2*_thicknessMean) 
					+ (60*Math.pow(_thicknessMean,4))
					);
		else if(_theLiftingSurfaceInterface.getType() == ComponentEnum.CANARD)
			// Canard assumed as HTail
			_formFactor = (1.03 
					+ (1.85*_thicknessMean) 
					+ (80*Math.pow(_thicknessMean,4))
					);
	}
	
	public List<Airfoil> populateAirfoilList(Boolean equivalentWingFlag) {	
		
		int nPanels = _theLiftingSurfaceInterface.getPanels().size();

		if(!equivalentWingFlag) {
			Airfoil airfoilRoot = new Airfoil(_theLiftingSurfaceInterface.getPanels().get(0).getAirfoilRoot().getTheAirfoilInterface());
			_airfoilList.add(airfoilRoot);
			_airfoilPathList.add(_theLiftingSurfaceInterface.getPanels().get(0).getAirfoilRootPath());

			for(int i=0; i<nPanels - 1; i++) {

				Airfoil innerAirfoil = new Airfoil(_theLiftingSurfaceInterface.getPanels().get(i).getAirfoilTip().getTheAirfoilInterface()); 
				_airfoilList.add(innerAirfoil);
				_airfoilPathList.add(_theLiftingSurfaceInterface.getPanels().get(i).getAirfoilTipPath());
				
			}

			Airfoil airfoilTip = new Airfoil(_theLiftingSurfaceInterface.getPanels().get(nPanels - 1).getAirfoilTip().getTheAirfoilInterface());
			_airfoilList.add(airfoilTip);
			_airfoilPathList.add(_theLiftingSurfaceInterface.getPanels().get(nPanels - 1).getAirfoilTipPath());
			
		}

		else{
			Airfoil airfoilRoot = new Airfoil(_theLiftingSurfaceInterface.getEquivalentWing().getPanels().get(0).getAirfoilRoot().getTheAirfoilInterface());
			_airfoilList.add(airfoilRoot);
			_airfoilPathList.add(_theLiftingSurfaceInterface.getPanels().get(0).getAirfoilRootPath());

			Airfoil airfoilTip = new Airfoil(_theLiftingSurfaceInterface.getEquivalentWing().getPanels().get(0).getAirfoilTip().getTheAirfoilInterface());
			_airfoilList.add(airfoilTip);
			_airfoilPathList.add(_theLiftingSurfaceInterface.getPanels().get(0).getAirfoilTipPath());
		}

		discretizeAirfoilCharacteristics();
		calculateTransitionPoints();
		
		return _airfoilList;
	}
	
	private void discretizeAirfoilCharacteristics () {
		
		for(int i=0; i<_airfoilList.size(); i++) {
		
			this._maxThicknessVsY.add(_airfoilList.get(i).getThicknessToChordRatio());
			this._radiusLEVsY.add(_airfoilList.get(i).getRadiusLeadingEdge());
			this._alpha0VsY.add(_airfoilList.get(i).getAlphaZeroLift());
			this._alphaStarVsY.add(_airfoilList.get(i).getAlphaEndLinearTrait());
			this._alphaStallVsY.add(_airfoilList.get(i).getAlphaStall());
			this._clAlphaVsY.add(_airfoilList.get(i).getClAlphaLinearTrait());
			this._cdMinVsY.add(_airfoilList.get(i).getCdMin());
			this._clAtCdMinVsY.add(_airfoilList.get(i).getClAtCdMin());
			this._cl0VsY.add(_airfoilList.get(i).getClAtAlphaZero());
			this._clStarVsY.add(_airfoilList.get(i).getClEndLinearTrait());
			this._clMaxVsY.add(_airfoilList.get(i).getClMax());
			this._kFactorDragPolarVsY.add(_airfoilList.get(i).getKFactorDragPolar());
			this._cmAlphaQuarteChordVsY.add(_airfoilList.get(i).getCmAlphaQuarterChord().getEstimatedValue());
			this._xAcAirfoilVsY.add(_airfoilList.get(i).getXACNormalized());
			this._cmACVsY.add(_airfoilList.get(i).getCmAC());
			this._cmACStallVsY.add(_airfoilList.get(i).getCmACAtStall());
			this._criticalMachVsY.add(_airfoilList.get(i).getMachCritical());

		}
	}

	private void calculateTransitionPoints() {

		Double xTransitionUpper = 0.0;
		Double xTransitionLower = 0.0;

		for(int i=0; i<_airfoilList.size(); i++) {
			xTransitionUpper += _airfoilList.get(i).getXTransitionUpper();
			xTransitionLower += _airfoilList.get(i).getXTransitionLower();
		}

		xTransitionUpper = xTransitionUpper/_airfoilList.size();
		xTransitionLower = xTransitionLower/_airfoilList.size();

		_xTransitionUpper = xTransitionUpper;
		_xTransitionLower = xTransitionLower;

	}
	
	public void reportPanelsToSpanwiseDiscretizedVariables(){

		System.out.println("=====================================================");
		System.out.println("List of Tuples, size " + _panelToSpanwiseDiscretizedVariables.size());

		_panelToSpanwiseDiscretizedVariables.stream()
			.forEach( tup2 -> {
				StringBuilder sb = new StringBuilder();
				sb
				.append("=====================================================\n")
				.append("Panel '" + tup2._1().getId() + "'")
				.append("\n")
				.append("Ys: size ")
				.append(
					tup2
						._2() // Tuple6
						._1() // Ys
						.size()
					)
				.append("\n")
				.append(
					tup2
						._2() // Tuple5
						._1() // Ys
					);
				sb
				.append("\n")
				.append("Chords: size ")
				.append(
					tup2
						._2() // Tuple5
						._2() // Chords
						.size()
					)
				.append("\n")
				.append(
					tup2
						._2() // Tuple5
						._2() // Chords
					);
				sb
				.append("\n")
				.append("Xle's: size ")
				.append(
					tup2
						._2() // Tuple5
						._3() // Xle's
						.size()
					)
				.append("\n")
				.append(
					tup2
						._2() // Tuple5
						._3() // Xle's
					)
				;
				sb
				.append("\n")
				.append("Zle's: size ")
				.append(
					tup2
						._2() // Tuple5
						._4() // Zle's
						.size()
					)
				.append("\n")
				.append(
					tup2
						._2() // Tuple5
						._4() // Zle's
					)
				;
				sb
				.append("\n")
				.append("Twists: size ")
				.append(
					tup2
						._2() // Tuple5
						._5() // Twists
						.size()
					)
				.append("\n")
				.append(
					tup2
						._2() // Tuple5
						._5() // Twists
					)
				.append("\n")
				;

				// spit out the string
				System.out.println(sb.toString());
			}
		);

	}

	private void reportDiscretizedVariables(StringBuilder sb){

		DecimalFormat numberFormat = new DecimalFormat("0.000");
		
		sb.append("\t=====================================================\n");
		sb.append("\tSpanwise discretized " + _theLiftingSurfaceInterface.getType() + ", size " + _spanwiseDiscretizedVariables.size() + "\n");
		sb.append("\t........................................................................................................................\n");
		sb.append("\tY(m),\tchord(m),\tXle(m),\tZle(m),\ttwist(deg),\tAlpha0l(deg)\n");
		sb.append("\t........................................................................................................................\n");
		_spanwiseDiscretizedVariables.stream()
			.forEach( t5 ->	{
				double y = t5._1().doubleValue(SI.METER);
				double c = t5._2().doubleValue(SI.METER);
				double xLE = t5._3().doubleValue(SI.METER);
				double zLE = t5._4().doubleValue(SI.METER);
				double t = t5._5().doubleValue(NonSI.DEGREE_ANGLE);
				
				sb.append(
						"\t" + numberFormat.format(y) 
						+ "\t" + numberFormat.format(c) 
						+ "\t" + numberFormat.format(xLE) 
						+ "\t" + numberFormat.format(zLE) 
						+ "\t" + numberFormat.format(t) 
						+"\n");	
			});
		
	}
	
	@Override
	public String toString() {

		MyConfiguration.customizeAmountOutput();

		StringBuilder sb = new StringBuilder();
		sb.append("\t-------------------------------------\n")
		.append("\tLifting surface\n")
		.append("\t-------------------------------------\n")
		.append("\tID: '" + _theLiftingSurfaceInterface.getId() + "'\n")
		.append("\tType: '" + _theLiftingSurfaceInterface.getType() + "'\n")
		.append("\t-------------------------------------\n")
		.append("\tNo. panels = " + _theLiftingSurfaceInterface.getPanels().size() + "\n")
		.append("\tMain spar position referred to chord = " + _theLiftingSurfaceInterface.getMainSparDimensionlessPosition() + "\n")
		.append("\tSecondary spar position referred to chord = " + _theLiftingSurfaceInterface.getSecondarySparDimensionlessPosition() + "\n")
		.append("\tSurface roughness = " + _theLiftingSurfaceInterface.getRoughness() + "\n")
		.append("\tWinglet height = " + _theLiftingSurfaceInterface.getWingletHeight() + "\n")
		;
		for (LiftingSurfacePanelCreator panel : _theLiftingSurfaceInterface.getPanels()) {
			sb.append(panel.toString());
		}

		sb.append("\t---------------------------------------\n")
		.append("\tEquivalent wing\n")
		.append("\t---------------------------------------\n")
		;
		sb.append(_theLiftingSurfaceInterface.getEquivalentWing().getPanels().get(0).toString());

		if(!_theLiftingSurfaceInterface.getSymmetricFlaps().isEmpty()) {
			for (SymmetricFlapCreator symmetricFlap : _theLiftingSurfaceInterface.getSymmetricFlaps()) {
				sb.append(symmetricFlap.toString());
			}
		}

		if(!_theLiftingSurfaceInterface.getSlats().isEmpty()) {
			for (SlatCreator slats : _theLiftingSurfaceInterface.getSlats()) {
				sb.append(slats.toString());
			}
		}

		if(!_theLiftingSurfaceInterface.getAsymmetricFlaps().isEmpty()) {
			for (AsymmetricFlapCreator asymmetricFlaps : _theLiftingSurfaceInterface.getAsymmetricFlaps()) {
				sb.append(asymmetricFlaps.toString());
			}
		}

		if(!_theLiftingSurfaceInterface.getSpoilers().isEmpty()) {
			for (SpoilerCreator spoilers : _theLiftingSurfaceInterface.getSpoilers()) {
				sb.append(spoilers.toString());
			}
		}

		reportDiscretizedVariables(sb);

		sb
		.append("\t=====================================\n")
		.append("\tOverall wing derived data\n")
		.append("\tSpan: " + _span.to(SI.METRE) +"\n")
		.append("\tSemi-span: " + _semiSpan.to(SI.METRE) +"\n")
		.append("\tSurface of planform: " + _surfacePlanform.to(SI.SQUARE_METRE) +"\n")
		.append("\tSurface wetted: " + _surfaceWetted.to(SI.SQUARE_METRE) + "\n")
		.append("\tAspect-ratio: " + _aspectRatio +"\n")
		.append("\tMean aerodynamic chord: " + _meanAerodynamicChord +"\n")
		.append("\t(X,Y,Z)_LE of mean aerodynamic chord: " + getMeanAerodynamicChordLeadingEdge() +"\n");

		if(this._symmetricFlapsControlSurfaceAreaList != null && !this._symmetricFlapsControlSurfaceAreaList.isEmpty())
			sb.append("\tSymmetric Flaps Control surface area list: " + this._symmetricFlapsControlSurfaceAreaList +"\n")
			.append("\tTotal Symmetric Flaps Control surface area: " + this._symmetricFlapsControlSurfaceArea +"\n");
		if(this._asymmetricFlapsControlSurfaceAreaList != null && !this._asymmetricFlapsControlSurfaceAreaList.isEmpty())
			sb.append("\tAsymmetric Flaps Control surface area list: " + this._asymmetricFlapsControlSurfaceAreaList +"\n")
			.append("\tTotal Asymmetric Flaps Control surface area: " + this._asymmetricFlapsControlSurfaceArea +"\n");
		if(this._slatsControlSurfaceAreaList != null && !this._slatsControlSurfaceAreaList.isEmpty())
			sb.append("\tSlats Control surface area list: " + this._slatsControlSurfaceAreaList +"\n")
			.append("\tTotal Slats Control surface area: " + this._slatsControlSurfaceArea +"\n");
		if(this._spoilersControlSurfaceAreaList != null && !this._spoilersControlSurfaceAreaList.isEmpty())
			sb.append("\tSpoilers Control surface area list: " + this._spoilersControlSurfaceAreaList +"\n")
			.append("\tTotal Spoilers Control surface area: " + this._spoilersControlSurfaceArea +"\n");

		sb.append("\tTotal Control surface area: " + this._totalControlSurfaceArea +"\n")
		.append("\tTransition point upper surface: " + this._xTransitionUpper +"\n")
		.append("\tTransition point lower surface: " + this._xTransitionLower +"\n")
		;
		
		return sb.toString();
	}
	
	public Amount<Length> getXLEAtYActual(Double yStation) {
		return Amount.valueOf(
				GeometryCalc.getXLEAtYActual(
						MyArrayUtils.convertListOfAmountTodoubleArray(getDiscretizedYs()),
						MyArrayUtils.convertListOfAmountTodoubleArray(getDiscretizedXle()),
						yStation
						),
				SI.METER
				);
	}
	
	public Amount<Length> getZLEAtYActual(Double yStation) {
		return Amount.valueOf(
				GeometryCalc.getZLEAtYActual(
						MyArrayUtils.convertListOfAmountTodoubleArray(getDiscretizedYs()), 
						MyArrayUtils.convertListOfAmountTodoubleArray(getDiscretizedZle()), 
						yStation
						), 
				SI.METER);
	}

	public Amount<Angle> getDihedralAtYActual(Double yStation) {
		if (yStation >= 0) return getDihedralSemispanAtYActual(yStation);
		else return getDihedralSemispanAtYActual(-yStation);
	}
	
	public Amount<Angle> getDihedralSemispanAtYActual(Double yStation) {
		
		Amount<Angle> dihedralAtY = Amount.valueOf(0.0, NonSI.DEGREE_ANGLE);
		
		if(yStation <= _yBreakPoints.get(0).doubleValue(SI.METER)) {
			System.err.println("INVALID Y STATION");
			dihedralAtY = null;
		}
		
		dihedralAtY = Amount.valueOf(
				MyMathUtils.getInterpolatedValue1DLinear(
						MyArrayUtils.convertListOfAmountTodoubleArray(_yBreakPoints),
						MyArrayUtils.convertListOfAmountTodoubleArray(_dihedralsBreakPoints),
						yStation
						),
				NonSI.DEGREE_ANGLE
				);
		
		return dihedralAtY;
	}
	
	public List<
		Tuple2<
			Amount<Length>, // Ys
			Amount<Length>  // Xs
			>
		> getDiscretizedTopViewAsList() {

		List<Tuple2<Amount<Length>,Amount<Length>>> listYX = new ArrayList<>();

		// leading edge (straight)
		IntStream.range(0, _spanwiseDiscretizedVariables.size())
			.forEach(i -> {
				listYX.add(Tuple.of(
						_spanwiseDiscretizedVariables.get(i)._1(), // y
						_spanwiseDiscretizedVariables.get(i)._3()  // xle
						)
					);
			});

		// trailing edge, reverse order
		int num = _spanwiseDiscretizedVariables.size() - 1;
		IntStream.rangeClosed(0, num)
			.forEach(i -> {
				listYX.add(Tuple.of(
						_spanwiseDiscretizedVariables.get(num - i)._1(),
						_spanwiseDiscretizedVariables.get(num - i)._3() // xle
							.plus(
								_spanwiseDiscretizedVariables.get(num - i)._2() // + chord
							)
						)
					);
			});

		return listYX;
	}

	public Double[][] getDiscretizedTopViewAsArray(ComponentEnum type) {
		
		// see: http://stackoverflow.com/questions/26050530/filling-a-multidimensional-array-using-a-stream/26053236#26053236

		List<Tuple2<Amount<Length>,Amount<Length>>> listYX = getDiscretizedTopViewAsList();

		Double[][] array = new Double[listYX.size()][2];
		if((type.equals(ComponentEnum.WING)) || (type.equals(ComponentEnum.HORIZONTAL_TAIL)) || (type.equals(ComponentEnum.CANARD))) {
			IntStream.range(0, listYX.size())
			.forEach(i -> {
				array[i][0] = listYX.get(i)._1().doubleValue(SI.METRE);
				array[i][1] = listYX.get(i)._2().doubleValue(SI.METRE);
			});
		}
		else if (type.equals(ComponentEnum.VERTICAL_TAIL)) {
			IntStream.range(0, listYX.size())
			.forEach(i -> {
				array[i][1] = listYX.get(i)._1().doubleValue(SI.METRE);
				array[i][0] = listYX.get(i)._2().doubleValue(SI.METRE);
			});
		}
		return array;
	}
	
	/** 
	 * Returns the chord of the 
	 * equivalent wing at y station
	 * 
	 * @author Lorenzo Attanasio
	 * @param y in meter or foot
	 * @return
	 */
	public double getChordEquivalentAtY(Double y) {

		double taperRatio = _theLiftingSurfaceInterface.getEquivalentWing().getPanels().get(0).getTaperRatio();
		
		double chord = ((2 * getSurfacePlanform().doubleValue(SI.SQUARE_METRE))/
				(_span.doubleValue(SI.METER) * (1+taperRatio))) * 
				(1-((2 * (1-taperRatio)/_span.doubleValue(SI.METER)) * 
						y));
		return chord;

	}
	
	public double getChordAtYActual(Double y) {
		return GeometryCalc.getChordAtYActual(
				MyArrayUtils.convertListOfAmountTodoubleArray(getDiscretizedYs()), 
				MyArrayUtils.convertListOfAmountTodoubleArray(getDiscretizedChords()),
				y
				);
	}
	
	/** 
	 * Get LE of the equivalent lifting surface 
	 * x coordinate at y location.
	 * 
	 * @param y
	 * @return
	 */
	public double getXLEAtYEquivalent(Double y){
		
		Amount<Length> span = Amount.valueOf(
				Math.sqrt(_surfacePlanform.doubleValue(SI.SQUARE_METRE)*_aspectRatio),
				SI.METER);
		
		return (getDiscretizedXle().get(getDiscretizedXle().size()-1).getEstimatedValue()
				/span.getEstimatedValue())
				* y;
	}
	
	public Amount<Angle> getDihedralMean() {
		
		this._dihedralMean = Amount.valueOf(0.0, NonSI.DEGREE_ANGLE);
		
		for(int i=0; i<_theLiftingSurfaceInterface.getPanels().size(); i++) {
			this._dihedralMean = this._dihedralMean.plus(this._theLiftingSurfaceInterface.getPanels().get(i).getDihedral());
		}
		
		_dihedralMean = _dihedralMean.divide(_theLiftingSurfaceInterface.getPanels().size());
		
		return _dihedralMean.to(NonSI.DEGREE_ANGLE);
	}
	
	public void discretizeGeometry(int numberSpanwiseStations) {
		//======================================================
		// Eta's discretizing the whole planform,
		// in the middle of each panel,
		// and including break-point eta's

		List<Double> eta0 =
			Arrays.asList(
				ArrayUtils.toObject(
						MyArrayUtils.linspace(0., 1., numberSpanwiseStations)
				)
			);

		List<Double> eta1 = ListUtils.union(eta0, _etaBreakPoints);
		Collections.sort(eta1);

		// Now that break-points are known generate eta's, including
		// break-point eta's
		_etaStations = eta1.stream()
				.distinct()
				.collect(Collectors.toList());

		_numberOfSpanwisePoints = _etaStations.size();
		
		//======================================================
		// Y's discretizing the whole planform,
		// in the middle of each panel,
		// and including break-point eta's

		_yStations = _etaStations.stream()
				.map(d -> _semiSpan.times(d))
				.collect(Collectors.toList());
		
		//======================================================
		// Assign lists of Y's to each panel
		mapPanelsToYDiscretized();

		//======================================================
		// Map Y's to chord
		calculateChordsAtYDiscretized();

		//======================================================
		// Map Y's to (Xle, Zle, twist)
		calculateXZleTwistAtYDiscretized();

		//======================================================
		// fill the list of all discretized variables
		calculateDiscretizedGeometry();

	}

	public void adjustDimensions(
			List<Double> v1, List<Double> v2, 
			List<Amount<Angle>> panelsSweepLE,
			List<Amount<Angle>> panelsDihedral,
			List<Amount<Angle>> panelsTwistAtTip,
			LiftingSurfaceAdjustCriteriaEnum criterion
			) {
		
		List<LiftingSurfacePanelCreator> currentPanels = getPanels();
		int panelNumber = currentPanels.size();
		
		if(panelsDihedral.size() != panelNumber 
				|| panelsSweepLE.size() != panelNumber
				|| panelsTwistAtTip.size() != panelNumber
				)
			System.err.println("WARNING (LIFTING SURFACE - ADJUST DIMENSIONS): PANELS DIHEDRAL, SWEEP_LE AND TWIST AT TIP MUST BE OF THE SAME SIZE OF PANELS!!");
		
		// independent variables initialization
		double surfaceRatio = 0.0;
		double ar = 0.0;
		double surface = 0.0;
		double span = 0.0;
		List<Double> panelSpans = new ArrayList<>();
		List<Double> panelChords = new ArrayList<>();
		double[] spanPercentages = new double[getPanels().size()];
		
		switch (criterion) {
		case ADJUST_AR_SPAN:
	
			if(v1.size() == 1) {
				ar = v1.get(0);
				if (0.0 == ar) 
					System.err.println("WARNING (LIFTING SURFACE - ADJUST DIMENSIONS): AR CAN'T BE 0.0!");
			}
			else
				System.err.println("WARNING (LIFTING SURFACE - ADJUST DIMENSIONS): AR is NULL");
			
			if(v2.size() == 1) {
				span = v2.get(0);
				if (0.0 == span) 
					System.err.println("WARNING (LIFTING SURFACE - ADJUST DIMENSIONS): AREA CAN'T BE 0.0!");
			}
			else
				System.err.println("WARNING (LIFTING SURFACE - ADJUST DIMENSIONS): SPAN is NULL");
			
			surface = Math.pow(span, 2) / ar;
			surfaceRatio = surface / this._surfacePlanform.doubleValue(SI.SQUARE_METRE);
			
			for (LiftingSurfacePanelCreator panel : currentPanels) {
				
				int index = currentPanels.indexOf(panel);
				
				panel.setChordRoot(panel.getChordRoot().times(surfaceRatio));
				panel.setChordTip(panel.getChordTip().times(surfaceRatio));
				panel.setSweepAtLeadingEdge(panelsSweepLE.get(index));
				panel.setDihedral(panelsDihedral.get(index));
				panel.setTwistGeometricAtTip(panelsTwistAtTip.get(index));
			}
			
			break;
		
		case ADJUST_AR_PANELS_SPANS:
			
			if(v1.size() == 1) {
				ar = v1.get(0);
				if (0.0 == ar) 
					System.err.println("WARNING (LIFTING SURFACE - ADJUST DIMENSIONS): AR CAN'T BE 0.0!");
			}
			else
				System.err.println("WARNING (LIFTING SURFACE - ADJUST DIMENSIONS): AR is NULL");
			
			if(v2.size() == currentPanels.size())
				for(int i=0; i<v2.size(); i++) {
					panelSpans.add(v2.get(i));
					if (0.0 == panelSpans.get(i)) 
						System.err.println("WARNING (LIFTING SURFACE - ADJUST DIMENSIONS): PANEL SPAN CAN'T BE 0.0!");		
				}
			else
				System.err.println("WARNING (LIFTING SURFACE - ADJUST DIMENSIONS): PANLES SPAN DIMENSION MUST BE EQUAL TO THE NUMBER OF PANELS!");
			
			double currentSpan = panelSpans.stream().mapToDouble(s -> s.doubleValue()).sum();
			surface = Math.pow(currentSpan, 2) / ar;
			surfaceRatio = surface / this._surfacePlanform.doubleValue(SI.SQUARE_METRE);
			
			for (LiftingSurfacePanelCreator panel : currentPanels) {
				
				int index = currentPanels.indexOf(panel);
				
				panel.setChordRoot(panel.getChordRoot().times(surfaceRatio));
				panel.setChordTip(panel.getChordTip().times(surfaceRatio));
				panel.setSweepAtLeadingEdge(panelsSweepLE.get(index));
				panel.setDihedral(panelsDihedral.get(index));
				panel.setTwistGeometricAtTip(panelsTwistAtTip.get(index));
			}
			
			break;
			
		case ADJUST_AR_AREA:
			
			if(v1.size() == 1) {
				ar = v1.get(0);
				if (0.0 == ar) 
					System.err.println("WARNING (LIFTING SURFACE - ADJUST DIMENSIONS): AR CAN'T BE 0.0!");
			}
			else
				System.err.println("WARNING (LIFTING SURFACE - ADJUST DIMENSIONS): AR is NULL");
			
			if(v2.size() == 1) {
				surface = v2.get(0);
				if (0.0 == surface) 
					System.err.println("WARNING (LIFTING SURFACE - ADJUST DIMENSIONS): AREA CAN'T BE 0.0!");
			}
			else
				System.err.println("WARNING (LIFTING SURFACE - ADJUST DIMENSIONS): AREA is NULL");
			
			span = Math.sqrt(surface*ar);
			
			for (LiftingSurfacePanelCreator panel : currentPanels) {
				
				int index = currentPanels.indexOf(panel);
				
				Amount<Length> panelSpan = panel.getSpan();
				double spanPercentage = panelSpan.doubleValue(SI.METER) / this._semiSpan.doubleValue(SI.METER);
				panel.setSpan(Amount.valueOf(span * spanPercentage, SI.METER));
				panel.setSweepAtLeadingEdge(panelsSweepLE.get(index));
				panel.setDihedral(panelsDihedral.get(index));
				panel.setTwistGeometricAtTip(panelsTwistAtTip.get(index));
			}
			
			break;
			
		case ADJUST_AR_PANEL_CHORDS:
			
			if(v1.size() == 1) {
				ar = v1.get(0);
				if (0.0 == ar) 
					System.err.println("WARNING (LIFTING SURFACE - ADJUST DIMENSIONS): AR CAN'T BE 0.0!");
			}
			else
				System.err.println("WARNING (LIFTING SURFACE - ADJUST DIMENSIONS): AR is NULL");
			
			if(v2.size() == currentPanels.size() + 1)
				for(int i=0; i<v2.size(); i++) {
					panelChords.add(v2.get(i));
					if (0.0 == panelChords.get(i)) 
						System.err.println("WARNING (LIFTING SURFACE - ADJUST DIMENSIONS): PANEL CHORDS CAN'T BE 0.0!");		
				}
			else
				System.err.println("WARNING (LIFTING SURFACE - ADJUST DIMENSIONS): PANLES SPAN DIMENSION MUST BE EQUAL TO THE NUMBER OF BREAKPOINTS!");

			double surfaceNormalized = 0.0;
			spanPercentages = new double[getPanels().size()];
			for (LiftingSurfacePanelCreator panel : currentPanels) {
				int index = currentPanels.indexOf(panel);
				Amount<Length> panelSpan = panel.getSpan();
				spanPercentages[index] = panelSpan.doubleValue(SI.METER) / this._semiSpan.doubleValue(SI.METER);
				surfaceNormalized += (panelChords.get(index) + panelChords.get(index+1)) * (spanPercentages[index]) / 2;
			}
			span = surfaceNormalized*ar;
			
			for (LiftingSurfacePanelCreator panel : currentPanels) {
				
				int index = currentPanels.indexOf(panel);
				
				panel.setSpan(Amount.valueOf(span * spanPercentages[index], SI.METER));
				panel.setChordRoot(Amount.valueOf(panelChords.get(index), SI.METER));
				panel.setChordTip(Amount.valueOf(panelChords.get(index+1), SI.METER));
				panel.setSweepAtLeadingEdge(panelsSweepLE.get(index));
				panel.setDihedral(panelsDihedral.get(index));
				panel.setTwistGeometricAtTip(panelsTwistAtTip.get(index));
			}
			
			break;
			
		case ADJUST_AREA_SPAN:
			
			if(v1.size() == 1) {
				surface = v1.get(0);
				if (0.0 == surface) 
					System.err.println("WARNING (LIFTING SURFACE - ADJUST DIMENSIONS): SURFACE CAN'T BE 0.0!");
			}
			else
				System.err.println("WARNING (LIFTING SURFACE - ADJUST DIMENSIONS): SURFACE is NULL");
			
			if(v2.size() == 1) {
				span = v2.get(0);
				if (0.0 == span) 
					System.err.println("WARNING (LIFTING SURFACE - ADJUST DIMENSIONS): SPAN CAN'T BE 0.0!");
			}
			else
				System.err.println("WARNING (LIFTING SURFACE - ADJUST DIMENSIONS): SPAN is NULL");

			
			for (LiftingSurfacePanelCreator panel : currentPanels) {
				int index = currentPanels.indexOf(panel);
				Amount<Length> panelSpan = panel.getSpan();
				spanPercentages[index] = panelSpan.doubleValue(SI.METER) / this._semiSpan.doubleValue(SI.METER);
			}
			surfaceRatio = surface / this._surfacePlanform.doubleValue(SI.SQUARE_METRE);
			
			for (LiftingSurfacePanelCreator panel : currentPanels) {
				
				int index = currentPanels.indexOf(panel);
				
				panel.setSpan(Amount.valueOf(span * spanPercentages[index], SI.METER));
				panel.setChordRoot(panel.getChordRoot().times(surfaceRatio));
				panel.setChordTip(panel.getChordTip().times(surfaceRatio));
				panel.setSweepAtLeadingEdge(panelsSweepLE.get(index));
				panel.setDihedral(panelsDihedral.get(index));
				panel.setTwistGeometricAtTip(panelsTwistAtTip.get(index));
			}
			
			break;
			
		case ADJUST_AREA_PANELS_SPANS:
			
			if(v1.size() == 1) {
				surface = v1.get(0);
				if (0.0 == surface) 
					System.err.println("WARNING (LIFTING SURFACE - ADJUST DIMENSIONS): SURFACE CAN'T BE 0.0!");
			}
			else
				System.err.println("WARNING (LIFTING SURFACE - ADJUST DIMENSIONS): SURFACE is NULL");
			
			if(v2.size() == currentPanels.size())
				for(int i=0; i<v2.size(); i++) {
					panelSpans.add(v2.get(i));
					if (0.0 == panelSpans.get(i)) 
						System.err.println("WARNING (LIFTING SURFACE - ADJUST DIMENSIONS): PANEL SPAN CAN'T BE 0.0!");		
				}
			else
				System.err.println("WARNING (LIFTING SURFACE - ADJUST DIMENSIONS): PANLES SPAN DIMENSION MUST BE EQUAL TO THE NUMBER OF PANELS!");

			
			surfaceRatio = surface / this._surfacePlanform.doubleValue(SI.SQUARE_METRE);
			
			for (LiftingSurfacePanelCreator panel : currentPanels) {
				
				int index = currentPanels.indexOf(panel);
				
				panel.setSpan(Amount.valueOf(panelSpans.get(index), SI.METER));
				panel.setChordRoot(panel.getChordRoot().times(surfaceRatio));
				panel.setChordTip(panel.getChordTip().times(surfaceRatio));
				panel.setSweepAtLeadingEdge(panelsSweepLE.get(index));
				panel.setDihedral(panelsDihedral.get(index));
				panel.setTwistGeometricAtTip(panelsTwistAtTip.get(index));
			}
			
			break;
			
		case ADJUST_PANELS_CHORDS_SPAN:
			
			if(v1.size() == currentPanels.size() + 1)
				for(int i=0; i<v1.size(); i++) {
					panelChords.add(v1.get(i));
					if (0.0 == panelChords.get(i)) 
						System.err.println("WARNING (LIFTING SURFACE - ADJUST DIMENSIONS): PANEL CHORDS CAN'T BE 0.0!");		
				}
			else
				System.err.println("WARNING (LIFTING SURFACE - ADJUST DIMENSIONS): PANLES SPAN DIMENSION MUST BE EQUAL TO THE NUMBER OF BREAKPOINTS!");
			
			if(v2.size() == 1) {
				span = v2.get(0);
				if (0.0 == span) 
					System.err.println("WARNING (LIFTING SURFACE - ADJUST DIMENSIONS): SPAN CAN'T BE 0.0!");
			}
			else
				System.err.println("WARNING (LIFTING SURFACE - ADJUST DIMENSIONS): SPAN is NULL");

			
			for (LiftingSurfacePanelCreator panel : currentPanels) {
				int index = currentPanels.indexOf(panel);
				Amount<Length> panelSpan = panel.getSpan();
				spanPercentages[index] = panelSpan.doubleValue(SI.METER) / this._semiSpan.doubleValue(SI.METER);
			}
			
			for (LiftingSurfacePanelCreator panel : currentPanels) {
				
				int index = currentPanels.indexOf(panel);
				
				panel.setSpan(Amount.valueOf(span*spanPercentages[index], SI.METER));
				panel.setChordRoot(Amount.valueOf(panelChords.get(index), SI.METER));
				panel.setChordTip(Amount.valueOf(panelChords.get(index+1), SI.METER));
				panel.setSweepAtLeadingEdge(panelsSweepLE.get(index));
				panel.setDihedral(panelsDihedral.get(index));
				panel.setTwistGeometricAtTip(panelsTwistAtTip.get(index));
			}
			
			break;
			
		case ADJUST_PANELS_CHORDS_PANELS_SPANS:
			
			if(v1.size() == currentPanels.size() + 1)
				for(int i=0; i<v1.size(); i++) {
					panelChords.add(v1.get(i));
					if (0.0 == panelChords.get(i)) 
						System.err.println("WARNING (LIFTING SURFACE - ADJUST DIMENSIONS): PANEL CHORDS CAN'T BE 0.0!");		
				}
			else
				System.err.println("WARNING (LIFTING SURFACE - ADJUST DIMENSIONS): PANLES SPAN DIMENSION MUST BE EQUAL TO THE NUMBER OF BREAKPOINTS!");
			
			if(v2.size() == currentPanels.size())
				for(int i=0; i<v2.size(); i++) {
					panelSpans.add(v2.get(i));
					if (0.0 == panelSpans.get(i)) 
						System.err.println("WARNING (LIFTING SURFACE - ADJUST DIMENSIONS): PANEL SPAN CAN'T BE 0.0!");		
				}
			else
				System.err.println("WARNING (LIFTING SURFACE - ADJUST DIMENSIONS): PANLES SPAN DIMENSION MUST BE EQUAL TO THE NUMBER OF PANELS!");

			
			for (LiftingSurfacePanelCreator panel : currentPanels) {
				
				int index = currentPanels.indexOf(panel);
				
				panel.setSpan(Amount.valueOf(panelSpans.get(index), SI.METER));
				panel.setChordRoot(Amount.valueOf(panelChords.get(index), SI.METER));
				panel.setChordTip(Amount.valueOf(panelChords.get(index+1), SI.METER));
				panel.setSweepAtLeadingEdge(panelsSweepLE.get(index));
				panel.setDihedral(panelsDihedral.get(index));
				panel.setTwistGeometricAtTip(panelsTwistAtTip.get(index));
			}
			
			break;
			
		}
		
//		setPanels(currentPanels);
		calculateGeometry(_theLiftingSurfaceInterface.getType(), _theLiftingSurfaceInterface.isMirrored());
		populateAirfoilList(false);
		
	}
	
	//---------------------------------------------------------------------------------------------
	// GETTERS & SETTERS
	//---------------------------------------------------------------------------------------------
	public ILiftingSurface getTheLiftingSurfaceInterface() {
		return _theLiftingSurfaceInterface;
	}
	
	public void setTheLiftingSurfaceInterface (ILiftingSurface theLiftingSurfaceInterface) {
		this._theLiftingSurfaceInterface = theLiftingSurfaceInterface;
	}
	
	public String getId() {
		return _theLiftingSurfaceInterface.getId();
	}
	
	public void setId (String id) {
		setTheLiftingSurfaceInterface(ILiftingSurface.Builder.from(_theLiftingSurfaceInterface).setId(id).build());
	}
	
	public boolean isMirrored() {
		return _theLiftingSurfaceInterface.isMirrored();
	}
	
	public void setMirrored (boolean mirrored) {
		setTheLiftingSurfaceInterface(ILiftingSurface.Builder.from(_theLiftingSurfaceInterface).setMirrored(mirrored).build());
	}
	
	public boolean getEquivalentWingFlag() {
		return equivalentWingFlag;
	}
	
	public void setEquivalentWingFlag (boolean equivalentWingFlag) {
		this.equivalentWingFlag = equivalentWingFlag;
	}
	
	public ComponentEnum getType() {
		return _theLiftingSurfaceInterface.getType();
	}
	
	public void setType (ComponentEnum type) {
		setTheLiftingSurfaceInterface(ILiftingSurface.Builder.from(_theLiftingSurfaceInterface).setType(type).build());
	}
	
	public double getMainSparDimensionlessPosition() {
		return _theLiftingSurfaceInterface.getMainSparDimensionlessPosition();
	}
	
	public void setMainSparDimensionlessPosition (double mainSparPosition) {
		setTheLiftingSurfaceInterface(
				ILiftingSurface.Builder.from(_theLiftingSurfaceInterface).setMainSparDimensionlessPosition(mainSparPosition).build()
				);
	}
	
	public double getSecondarySparDimensionlessPosition() {
		return _theLiftingSurfaceInterface.getSecondarySparDimensionlessPosition();
	}
	
	public void setSecondarySparDimensionlessPosition (double secondarySparPosition) {
		setTheLiftingSurfaceInterface(
				ILiftingSurface.Builder.from(_theLiftingSurfaceInterface).setSecondarySparDimensionlessPosition(secondarySparPosition).build()
				);
	}
	
	public Amount<Length> getRoughness() {
		return _theLiftingSurfaceInterface.getRoughness();
	}
	
	public void setRoughness (Amount<Length> roughness) {
		setTheLiftingSurfaceInterface(ILiftingSurface.Builder.from(_theLiftingSurfaceInterface).setRoughness(roughness).build());
	}
	
	public Amount<Length> getWingletHeight() {
		return _theLiftingSurfaceInterface.getWingletHeight();
	}
	
	public void setWingletHeight (Amount<Length> wingletHeight) {
		setTheLiftingSurfaceInterface(ILiftingSurface.Builder.from(_theLiftingSurfaceInterface).setWingletHeight(wingletHeight).build());
	}
	
	public IEquivalentLiftingSurface getEquivalentWing() {
		return _theLiftingSurfaceInterface.getEquivalentWing();
	}
	
	public void setEquivalentWing (IEquivalentLiftingSurface equivalentWing) {
		setTheLiftingSurfaceInterface(ILiftingSurface.Builder.from(_theLiftingSurfaceInterface).setEquivalentWing(equivalentWing).build());
	}
	
	public List<LiftingSurfacePanelCreator> getPanels() {
		return _theLiftingSurfaceInterface.getPanels();
	}
	
	public void setPanels (List<LiftingSurfacePanelCreator> panels) {
		setTheLiftingSurfaceInterface(ILiftingSurface.Builder.from(_theLiftingSurfaceInterface).clearPanels().addAllPanels(panels).build());
	}
	
	public List<SymmetricFlapCreator> getSymmetricFlaps() {
		return _theLiftingSurfaceInterface.getSymmetricFlaps();
	}
	
	public void setSymmetricFlaps (List<SymmetricFlapCreator> symmetricFlaps) {
		setTheLiftingSurfaceInterface(
				ILiftingSurface.Builder.from(_theLiftingSurfaceInterface).clearSymmetricFlaps().addAllSymmetricFlaps(symmetricFlaps).build()
				);
	}
	
	public List<SlatCreator> getSlats() {
		return _theLiftingSurfaceInterface.getSlats();
	}
	
	public void setSlats(List<SlatCreator> slats) {
		setTheLiftingSurfaceInterface(
				ILiftingSurface.Builder.from(_theLiftingSurfaceInterface).clearSlats().addAllSlats(slats).build()
				);
	}
	
	public List<AsymmetricFlapCreator> getAsymmetricFlaps() {
		return _theLiftingSurfaceInterface.getAsymmetricFlaps();
	}
	
	public void setAsymmetricFlaps(List<AsymmetricFlapCreator> ailerons) {
		setTheLiftingSurfaceInterface(
				ILiftingSurface.Builder.from(_theLiftingSurfaceInterface).clearAsymmetricFlaps().addAllAsymmetricFlaps(ailerons).build()
				);
	}
	
	public List<SpoilerCreator> getSpoilers() {
		return _theLiftingSurfaceInterface.getSpoilers();
	}
	
	public void setSpoilers(List<SpoilerCreator> spoilers) {
		setTheLiftingSurfaceInterface(
				ILiftingSurface.Builder.from(_theLiftingSurfaceInterface).clearSpoilers().addAllSpoilers(spoilers).build()
				);
	}

	public Amount<Length> getMeanAerodynamicChord() {
		return _meanAerodynamicChord;
	}

	public Amount<Length> getMeanAerodynamicChordLeadingEdgeX() {
		return _meanAerodynamicChordLeadingEdgeX;
	}
	
	public Amount<Length> getMeanAerodynamicChordLeadingEdgeY() {
		return _meanAerodynamicChordLeadingEdgeY;
	}
	
	public Amount<Length> getMeanAerodynamicChordLeadingEdgeZ() {
		return _meanAerodynamicChordLeadingEdgeZ;
	}
	
	public List<Amount<Length>> getMeanAerodynamicChordLeadingEdge() {
		return Arrays.asList(
				_meanAerodynamicChordLeadingEdgeX,
				_meanAerodynamicChordLeadingEdgeY,
				_meanAerodynamicChordLeadingEdgeZ
				);
	}
	
	public Amount<Length> getSpan() {
		return _span;
	}
	
	public Amount<Length> getSemiSpan() {
		return _semiSpan;
	}
	
	public Amount<Area> getSurfacePlanform() {
		return _surfacePlanform;
	}
	
	public Amount<Area> getSurfaceWetted() {
		return _surfaceWetted;
	}
	
	public Double getAspectRatio() {
		return _aspectRatio;
	}

	public Double getTaperRatio() {
		return _taperRatio;
	}
	
	public List<Amount<Length>> getDiscretizedYs() {
		return _spanwiseDiscretizedVariables.stream()
			.mapToDouble(t5 ->
				t5._1()
				.to(SI.METRE).getEstimatedValue())
			.mapToObj(y -> Amount.valueOf(y, 1e-8, SI.METRE))
			.collect(Collectors.toList());
	}
	

	public List<Amount<Length>> getDiscretizedChords() {
		return _spanwiseDiscretizedVariables.stream()
				.mapToDouble(t5 ->
					t5._2()
					.to(SI.METRE).getEstimatedValue())
				.mapToObj(y -> Amount.valueOf(y, 1e-8, SI.METRE)) 
				.collect(Collectors.toList());
	}

	public List<Amount<Length>> getDiscretizedXle() {
		return _spanwiseDiscretizedVariables.stream()
				.mapToDouble(t5 ->
					t5._3()
					.to(SI.METRE).getEstimatedValue())
				.mapToObj(y -> Amount.valueOf(y, 1e-8, SI.METRE))
				.collect(Collectors.toList());
	}

	public List<Amount<Length>> getDiscretizedZle() {
		return _spanwiseDiscretizedVariables.stream()
				.mapToDouble(t5 ->
					t5._4()
					.to(SI.METRE).getEstimatedValue())
				.mapToObj(y -> Amount.valueOf(y, 1e-8, SI.METRE))
				.collect(Collectors.toList());
	}
	
	public List<Amount<Angle>> getDiscretizedTwists() {
		return _spanwiseDiscretizedVariables.stream()
				.mapToDouble(t5 ->
					t5._5()
					.to(SI.RADIAN).getEstimatedValue())
				.mapToObj(y -> Amount.valueOf(y, 1e-9, SI.RADIAN))
				.collect(Collectors.toList());
	}
	public List<Amount<Length>> getYBreakPoints() {
		return _yBreakPoints;
	}

	public List<Double> getEtaBreakPoints() {
		return _etaBreakPoints;
	}

	public List<Amount<Length>> getXLEBreakPoints() {
		return _xLEBreakPoints;
	}

	public List<Amount<Length>> getZLEBreakPoints() {
		return _zLEBreakPoints;
	}

	public List<Amount<Length>> getChordsBreakPoints() {
		return _chordsBreakPoints;
	}
	
	public List<Amount<Angle>> getTwistsBreakPoints() {
		return _twistsBreakPoints;
	}
	
	public List<Amount<Angle>> getDihedralsBreakPoints() {
		return _dihedralsBreakPoints;
	}
	
	public Double getVolumetricRatio() {
		return _volumetricRatio;
	}

	public void setVolumetricRatio(Double volumetricRatio) {
		this._volumetricRatio = volumetricRatio;
	}

	public Amount<Length> getLiftingSurfaceACToWingACdistance() {
		return _liftingSurfaceACToWingACDistance;
	}

	public void setLiftingSurfaceACTOWingACDistance(Amount<Length> _liftingSurfaceACToWingACDistance) {
		this._liftingSurfaceACToWingACDistance = _liftingSurfaceACToWingACDistance;
	}

	public Amount<Length> getLiftingSurfaceArm() {
		return _liftingSurfaceArm;
	}

	public void setLiftingSurfaceArm(Amount<Length> _liftingSurfaceArm) {
		this._liftingSurfaceArm = _liftingSurfaceArm;
	}

	public Amount<Area> getSurfaceWettedExposed() {
		return _surfaceWettedExposed;
	}

	public void setSurfaceWettedExposed(Amount<Area> _surfaceWettedExposed) {
		this._surfaceWettedExposed = _surfaceWettedExposed;
	}

	public Amount<Area> getTotalControlSurfaceArea() {
		return _totalControlSurfaceArea;
	}

	public Amount<Area> getSymmetricFlapsControlSurfaceArea() {
		return _symmetricFlapsControlSurfaceArea;
	}
	
	public Amount<Area> getAsymmetricFlapsControlSurfaceArea() {
		return _asymmetricFlapsControlSurfaceArea;
	}

	public Amount<Area> getSlatsControlSurfaceArea() {
		return _slatsControlSurfaceArea;
	}
	
	public Amount<Area> getSpoilersControlSurfaceArea() {
		return _spoilersControlSurfaceArea;
	}
	
	public void setDihedralMean(Amount<Angle> _dihedralMean) {
		this._dihedralMean = _dihedralMean;
	}
	
	public Double getXTransitionUpper() {
		return _xTransitionUpper;
	}

	public void setXTransitionUpper(Double _xTransitionUpper) {
		this._xTransitionUpper = _xTransitionUpper;
	}

	public Double getXTransitionLower() {
		return _xTransitionLower;
	}

	public void setXTransitionLower(Double _xTransitionLower) {
		this._xTransitionLower = _xTransitionLower;
	}

	public double getFormFactor() {
		return _formFactor;
	}

	public void setFormFactor(double _formFactor) {
		this._formFactor = _formFactor;
	}

	public List<Double> getMaxThicknessVsY() {
		return _maxThicknessVsY;
	}

	public void setMaxThicknessVsY(List<Double> _maxThicknessVsY) {
		this._maxThicknessVsY = _maxThicknessVsY;
	}

	public List<Double> getRadiusLEVsY() {
		return _radiusLEVsY;
	}

	public void setRadiusLEVsY(List<Double> _radiusLEVsY) {
		this._radiusLEVsY = _radiusLEVsY;
	}

	public List<Double> getCamberRatioVsY() {
		return _camberRatioVsY;
	}

	public void setCamberRatioVsY(List<Double> _camberRatioVsY) {
		this._camberRatioVsY = _camberRatioVsY;
	}

	public List<Amount<Angle>> getAlpha0VsY() {
		return _alpha0VsY;
	}

	public void setAlpha0VsY(List<Amount<Angle>> _alpha0VsY) {
		this._alpha0VsY = _alpha0VsY;
	}

	public List<Amount<Angle>> getAlphaStarVsY() {
		return _alphaStarVsY;
	}

	public void setAlphaStarVsY(List<Amount<Angle>> _alphaStarVsY) {
		this._alphaStarVsY = _alphaStarVsY;
	}

	public List<Amount<Angle>> getAlphaStallVsY() {
		return _alphaStallVsY;
	}

	public void setAlphaStallVsY(List<Amount<Angle>> _alphaStallVsY) {
		this._alphaStallVsY = _alphaStallVsY;
	}

	public List<Amount<?>> getClAlphaVsY() {
		return _clAlphaVsY;
	}

	public void setClAlphaVsY(List<Amount<?>> _clAlphaVsY) {
		this._clAlphaVsY = _clAlphaVsY;
	}

	public List<Double> getCdMinVsY() {
		return _cdMinVsY;
	}

	public void setCdMinVsY(List<Double> _cdMinVsY) {
		this._cdMinVsY = _cdMinVsY;
	}

	public List<Double> getClAtCdMinVsY() {
		return _clAtCdMinVsY;
	}

	public void setClAtCdMinVsY(List<Double> _clAtCdMinVsY) {
		this._clAtCdMinVsY = _clAtCdMinVsY;
	}

	public List<Double> getCl0VsY() {
		return _cl0VsY;
	}

	public void setCl0VsY(List<Double> _cl0VsY) {
		this._cl0VsY = _cl0VsY;
	}

	public List<Double> getClStarVsY() {
		return _clStarVsY;
	}

	public void setClStarVsY(List<Double> _clStarVsY) {
		this._clStarVsY = _clStarVsY;
	}

	public List<Double> getClMaxVsY() {
		return _clMaxVsY;
	}

	public void setClMaxVsY(List<Double> _clMaxVsY) {
		this._clMaxVsY = _clMaxVsY;
	}

	public List<Double> getClMaxSweepVsY() {
		return _clMaxSweepVsY;
	}

	public void setClMaxSweepVsY(List<Double> _clMaxSweepVsY) {
		this._clMaxSweepVsY = _clMaxSweepVsY;
	}

	public List<Double> getKFactorDragPolarVsY() {
		return _kFactorDragPolarVsY;
	}

	public void setKFactorDragPolarVsY(List<Double> _kFactorDragPolarVsY) {
		this._kFactorDragPolarVsY = _kFactorDragPolarVsY;
	}

	public List<Double> getMExponentDragPolarVsY() {
		return _mExponentDragPolarVsY;
	}

	public void setMExponentDragPolarVsY(List<Double> _mExponentDragPolarVsY) {
		this._mExponentDragPolarVsY = _mExponentDragPolarVsY;
	}

	public List<Double> getCmAlphaQuarteChordVsY() {
		return _cmAlphaQuarteChordVsY;
	}

	public void setCmAlphaQuarteChordVsY(List<Double> _cmAlphaQuarteChordVsY) {
		this._cmAlphaQuarteChordVsY = _cmAlphaQuarteChordVsY;
	}

	public List<Double> getXACAirfoilVsY() {
		return _xAcAirfoilVsY;
	}

	public void setXACAirfoilVsY(List<Double> _xAcAirfoilVsY) {
		this._xAcAirfoilVsY = _xAcAirfoilVsY;
	}

	public List<Double> getCmACVsY() {
		return _cmACVsY;
	}

	public void setCmACVsY(List<Double> _cmACVsY) {
		this._cmACVsY = _cmACVsY;
	}

	public List<Double> getCmACStallVsY() {
		return _cmACStallVsY;
	}

	public void setCmACStallVsY(List<Double> _cmACStallVsY) {
		this._cmACStallVsY = _cmACStallVsY;
	}

	public List<Double> getCriticalMachVsY() {
		return _criticalMachVsY;
	}

	public void setCriticalMachVsY(List<Double> _criticalMachVsY) {
		this._criticalMachVsY = _criticalMachVsY;
	}

	public List<Airfoil> getAirfoilList() {
		return _airfoilList;
	}

	public void setAirfoilList(List<Airfoil> _airfoilList) {
		this._airfoilList = _airfoilList;
	}

	public List<String> getAirfoilPathList() {
		return _airfoilPathList;
	}

	public void setAirfoilPathList(List<String> _airfoilPathList) {
		this._airfoilPathList = _airfoilPathList;
	}

	public Airfoil getMeanAirfoil() {
		return _meanAirfoil;
	}

	public void setMeanAirfoil(Airfoil _meanAirfoil) {
		this._meanAirfoil = _meanAirfoil;
	}

	public Double getKExcr() {
		return _kExcr;
	}

	public void setKExcr(Double _kExcr) {
		this._kExcr = _kExcr;
	}

	public double getThicknessMean() {
		return _thicknessMean;
	}

	public void setThicknessMean(double _thicknessMean) {
		this._thicknessMean = _thicknessMean;
	}

	public double getPositionRelativeToAttachment() {
		return _positionRelativeToAttachment;
	}

	public void setPositionRelativeToAttachment(double _positionRelativeToAttachment) {
		this._positionRelativeToAttachment = _positionRelativeToAttachment;
	}

	public Amount<Length> getXApexConstructionAxes() {
		return _xApexConstructionAxes;
	}

	public void setXApexConstructionAxes(Amount<Length> _xApexConstructionAxes) {
		this._xApexConstructionAxes = _xApexConstructionAxes;
	}

	public Amount<Length> getYApexConstructionAxes() {
		return _yApexConstructionAxes;
	}

	public void setYApexConstructionAxes(Amount<Length> _yApexConstructionAxes) {
		this._yApexConstructionAxes = _yApexConstructionAxes;
	}

	public Amount<Length> getZApexConstructionAxes() {
		return _zApexConstructionAxes;
	}

	public void setZApexConstructionAxes(Amount<Length> _zApexConstructionAxes) {
		this._zApexConstructionAxes = _zApexConstructionAxes;
	}

	public Amount<Angle> getRiggingAngle() {
		return _riggingAngle;
	}

	public void setRiggingAngle(Amount<Angle> _riggingAngle) {
		this._riggingAngle = _riggingAngle;
	}

	public int getNumberOfEngineOverTheWing() {
		return _numberOfEngineOverTheWing;
	}

	public void setNumberOfEngineOverTheWing(int _numberOfEngineOverTheWing) {
		this._numberOfEngineOverTheWing = _numberOfEngineOverTheWing;
	}

	public LiftingSurface getExposedLiftingSurface() {
		return _exposedLiftingSurface;
	}

	public void setExposedLiftingSurface(LiftingSurface _exposedLiftingSurface) {
		this._exposedLiftingSurface = _exposedLiftingSurface;
	}

	public double getXOffsetEquivalentWingRootLE() {
		return _xOffsetEquivalentWingRootLE;
	}
	
	public void setXOffsetEquivalentWingRootLE(double _xOffsetEquivalentWingRootLE) {
		this._xOffsetEquivalentWingRootLE = _xOffsetEquivalentWingRootLE;
	}
	
	public double getXOffsetEquivalentWingRootTE() {
		return _xOffsetEquivalentWingRootTE;
	}
	
	public void setXOffsetEquivalentWingRootTE(double _xOffsetEquivalentWingRootTE) {
		this._xOffsetEquivalentWingRootTE = _xOffsetEquivalentWingRootTE;
	}

	public List<Amount<Area>> getSymmetricFlapsControlSurfaceAreaList() {
		return _symmetricFlapsControlSurfaceAreaList;
	}

	public void setSymmetricFlapsControlSurfaceAreaList(List<Amount<Area>> _symmetricFlapsControlSurfaceAreaList) {
		this._symmetricFlapsControlSurfaceAreaList = _symmetricFlapsControlSurfaceAreaList;
	}

	public List<Amount<Area>> getAsymmetricFlapsControlSurfaceAreaList() {
		return _asymmetricFlapsControlSurfaceAreaList;
	}

	public void setAsymmetricFlapsControlSurfaceAreaList(List<Amount<Area>> _asymmetricFlapsControlSurfaceAreaList) {
		this._asymmetricFlapsControlSurfaceAreaList = _asymmetricFlapsControlSurfaceAreaList;
	}

	public List<Amount<Area>> getSlatsControlSurfaceAreaList() {
		return _slatsControlSurfaceAreaList;
	}

	public void setSlatsControlSurfaceAreaList(List<Amount<Area>> _slatsControlSurfaceAreaList) {
		this._slatsControlSurfaceAreaList = _slatsControlSurfaceAreaList;
	}

	public List<Amount<Area>> getSpoilersControlSurfaceAreaList() {
		return _spoilersControlSurfaceAreaList;
	}

	public void setSpoilersControlSurfaceAreaList(List<Amount<Area>> _spoilersControlSurfaceAreaList) {
		this._spoilersControlSurfaceAreaList = _spoilersControlSurfaceAreaList;
	}
	
}