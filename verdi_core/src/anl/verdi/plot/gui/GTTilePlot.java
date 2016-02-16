/** GTTilePlot: GeoTools version of a tile plot
 * Developed 2016 Jo Ellen Brandmeyer
 * based on VERDI TilePlot and FastTilePlot
 * Uses new GTTilePlotPanel for GTTilePlot frame layout and to supply GeoTools geographic data support
 */

package anl.verdi.plot.gui;

import gov.epa.emvl.GridCellStatistics;
import gov.epa.emvl.Mapper;
import gov.epa.emvl.Numerics;
import gov.epa.emvl.Projector;

import java.awt.Color;

import javax.swing.JPopupMenu;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;

import anl.verdi.plot.anim.AnimationPanel;
import anl.verdi.plot.color.Palette;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.vecmath.Point4i;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.geotools.map.FeatureLayer;
import org.geotools.map.MapContent;
import org.geotools.swing.JMapPane;
import org.geotools.swing.data.JFileDataStoreChooser;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import saf.core.ui.event.DockableFrameEvent;
import ucar.ma2.InvalidRangeException;
import anl.verdi.core.VerdiApplication;
import anl.verdi.data.Axes;
import anl.verdi.data.DataFrame;
import anl.verdi.data.DataFrameAxis;
import anl.verdi.data.DataFrameIndex;
import anl.verdi.data.DataManager;
import anl.verdi.data.DataUtilities;
import anl.verdi.data.DataUtilities.MinMax;
import anl.verdi.data.Dataset;
import anl.verdi.data.ObsEvaluator;
import anl.verdi.data.Slice;
import anl.verdi.data.VectorEvaluator;
import anl.verdi.formula.Formula;
import anl.verdi.formula.Formula.Type;
import anl.verdi.gis.GTTileLayerEditor;
import anl.verdi.gis.OverlayObject;
import anl.verdi.plot.color.ColorMap;
import anl.verdi.plot.config.LoadConfiguration;
import anl.verdi.plot.config.PlotConfiguration;
import anl.verdi.plot.config.PlotConfigurationIO;
import anl.verdi.plot.config.SaveConfiguration;
import anl.verdi.plot.config.TilePlotConfiguration;
import anl.verdi.plot.probe.PlotEventProducer;
import anl.verdi.plot.types.TimeAnimatablePlot;
import anl.verdi.plot.util.GTTilePlotPrintAction;
import anl.verdi.plot.util.PlotExporter;
import anl.verdi.plot.util.PlotExporterAction;
import anl.verdi.util.Utilities;

/**
 * @author Jo Ellen Brandmeyer, Ph.D., Institute for the Environment, University of North Carolina at Chapel Hill
 *
 */
public class GTTilePlot extends GTTilePlotPanel 
implements ActionListener, Printable, ChangeListener, ComponentListener, MouseListener, TimeAnimatablePlot, Plot
{
	private PlotEventProducer eventProducer = new PlotEventProducer();
	private boolean withHucs = false; // Draw watersheds on map?
	private boolean withRivers = false; // Draw rivers on map?
	private boolean withRoads = false; // Draw roads on map?
	protected DataFrame dataFrame;
	protected DataFrame dataFrameLog;
	static final Logger Logger = LogManager.getLogger(GTTilePlot.class.getName());
	private final MapContent myMapContent = new MapContent();
	public static final int NO_VAL = Integer.MIN_VALUE;
	private static final String STATES_LAYER = "STATES";
	private static final String COUNTIES_LAYER = "COUNTIES";
	private static final String WORLD_LAYER = "WORLD";
	private static final String NA_LAYER = "NA";
	private static final String HUCS = "HUCs";
	private static final String RIVERS = "Rivers";
	private static final String ROADS = "Roads";
	private static final String OTHER_MAPS = "Other_Maps";
	public static final String PROPERTIES_COMMAND = "PROPERTIES";
	public static final String SAVE_COMMAND = "SAVE";
	public static final String PRINT_COMMAND = "PRINT";
	public static final String ZOOM_IN_BOTH_COMMAND = "ZOOM_IN_BOTH";
	public static final String ZOOM_OUT_BOTH_COMMAND = "ZOOM_OUT_BOTH";
	public static final String ZOOM_OUT_MAX_COMMAND = "ZOOM_OUT_MAX";

	// Attributes:
	private static final int X = 0;
	private static final int Y = 1;
	private static final int MINIMUM = 0;
	private static final int MAXIMUM = 1;
	private static final int LONGITUDE = 0;
	private static final int LATITUDE = 1;
	private static final double MINIMUM_VALID_VALUE = -900.0;

	// Log related

	protected boolean log = false;
	private boolean preLog = false;
	private double logBase = 10.0; //Math.E;	

	// 2D grid parameters

	protected int startDate; 		// (YYYYDDD).
	protected int startTime; 		// 0 (HHMMSS).
	protected int timestepSize; 	// 10000 (HHMMSS)
	protected int timesteps; 
	protected int layers; 
	protected int rows;
	protected int columns;
	protected int rowOrigin;
	protected int columnOrigin;
	private double westEdge; 		// meters from projection center
	private double southEdge; 	// meters from projection center
	private double cellWidth; 	// meters.
	private double cellHeight; 	// meters.
	private NumberFormat format;
	private boolean invertRows; // HACK: Invert rows of AURAMS / GEM / CF Convention data?

	// For legend-colored grid cells and annotations:

	//	protected TilePlot tilePlot; // EMVL TilePlot.

	protected int timestep = 0; // 0..timesteps - 1.
	protected int firstTimestep = 0;
	protected int lastTimestep = 0;
	protected int layer = 0; // 0..layers - 1.
	protected int firstLayer = 0;
	protected int lastLayer = 0;
	private int firstRow = 0; // 0..lastRow.
	private int lastRow = 0; // firstRow..rows - 1.
	private int firstColumn = 0; // 0..lastColumn.
	private int lastColumn = 0; // firstColumn..columns - 1.

	protected double[] legendLevels;
	protected MinMax minMax;
	private boolean processTimeChange = true;
	protected PlotConfiguration config;

	protected Palette defaultPalette;
	private Color[] legendColors;
	protected ColorMap map;

	private Color axisColor = Color.darkGray;
	private Color labelColor = Color.black;

	// subsetLayerData[ 1 + lastRow - firstRow ][ 1 + lastColumn - firstColumn ]
	// at current timestep and layer.
	private float[][] subsetLayerData = null;

	// layerData[ rows ][ columns ][ timesteps ]
	private float[][][] layerData = null;
	//private float[][][] layerDataLog = null;
	private float[][][] statisticsData = null;
	//private float[][][] statisticsDataLog = null;
	private CoordinateReferenceSystem gridCRS = null;	// axes -> ReferencedEnvelope -> gridCRS

	// For clipped/projected/clipped map lines:

	private String mapFileDirectory = System.getenv("VERDI_HOME") + "/plugins/bootstrap/data";	// nov 2015

	private Mapper mapper = new Mapper(mapFileDirectory);
	protected Projector projector;

	protected double[][] gridBounds = { { 0.0, 0.0 }, { 0.0, 0.0 } };
	protected double[][] domain = { { 0.0, 0.0 }, { 0.0, 0.0 } };

	protected List<OverlayObject> obsData = new ArrayList<OverlayObject>();
	protected List<ObsAnnotation> obsAnnotations;
	protected VectorAnnotation vectAnnotation;

	// GUI attributes

	// GUI attributes:
	private JButton playStopButton;
	private JButton leftStepButton;
	private JButton rightStepButton;
	private final String LEFT = "<";
	private final String RIGHT = ">";
	private final String PLAY = "|>";
	private final String STOP = "||";
	private final String LEFT_TIP = "Move One Timestep to the Left";
	private final String RIGHT_TIP = "Move One Timestep to the Right";
	private final String PLAY_TIP = "Play/Stop";
	private TimeLayerPanel timeLayerPanel;
	private final JToolBar toolBar = new JToolBar();
	private JComboBox statisticsMenu;
	//	private int preStatIndex = -1;
	private JTextField threshold;
	private boolean recomputeStatistics = false;
	private boolean recomputeLegend = false;

	private boolean statError = false;
	protected JCheckBoxMenuItem showGridLines;
	protected boolean zoom = true;
	protected boolean probe = false;
	private boolean hasNoLayer = false;
	private final String DELAY_LABEL = "Slow:";
	private JTextField delayField;
	private JTextField firstRowField;
	private JTextField lastRowField;
	private JTextField firstColumnField;
	private JTextField lastColumnField;
	protected final Rubberband rubberband = new Rubberband(this);
	private int delay = 50; // animation delay in milliseconds.
	private final int MAXIMUM_DELAY = 3000; // maximum animation delay: 3 seconds per frame.

	protected List<JMenuItem> probeItems = new ArrayList<JMenuItem>();

	protected boolean showLatLon = false;
	protected boolean showObsLegend = false;

	private BufferedImage bImage;	// TODO Do we still need this?
	private JPopupMenu popup;
	//TODO change dataArea [was defined as a Rectangle()]; used in run()
	private Point popUpLocation = new Point(1,1);
	protected Slice probedSlice;
	private ConfigDialog dialog = null;
	private FeatureLayer controlLayer;
	final JMenu mapLayersMenu = new JMenu("Add Map Layers");

	@SuppressWarnings("unused")
	private Plot.ConfigSource configSource = Plot.ConfigSource.GUI;
	VerdiApplication app;


	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public GTTilePlot(VerdiApplication app, DataFrame dataFrame) {
		// TODO Auto-generated constructor stub
		super();										// call constructor of GTTilePlotPanel
		JToolBar theToolBar = createToolBar(dataFrame);	// create the tool bar here
		super.setToolBar(theToolBar);					// and send it to the GTTilePlotPanel
		
	}

	/**
	 * From TilePlot
	 * @param obsAnnot List<ObsAnnotation>
	 * @param showLegend boolean
	 */
	public void setObsLegend(List<ObsAnnotation> obsAnnot, boolean showLegend) {
		showObsLegend = showLegend;
		obsAnnotations = obsAnnot;
	}

	/**
	 * getPanel(): returns the largest JPanel container (contains all others)
	 */
	@Override
	public JPanel getPanel() {
		return getEntirePane();
	}

	/**
	 * getEntirePane(): returns the largest JPanel container
	 * @return	GTTilePlotPanel as a JPanel
	 */
	private JPanel getEntirePane() {
		return (JPanel) this;	// GTTilePlotPanel is-a JPanel
	}

	/**
	 * getMapPane(): returns the JMapPane container that contains just the geographic component.
	 */
	@Override
	public JMapPane getMapPane() {
		return super.getMap();
	}

	@Override
	public JMenuBar getMenuBar() {	// return the container of the menu bar at the top of the overall frame
		return super.getMenuBar();
	}

	/**
	 * Adds the specified PlotListener.
	 * 
	 * @param listener	the plot listener to add
	 */
	@Override
	public void addPlotListener(PlotListener listener) {
		eventProducer.addListener(listener);
	}

	/**
	 * Removes the specified PlotListener.
	 * 
	 * @param listener	the plot listener to remove
	 */
	@Override
	public void removePlotListener(PlotListener listener) {
		eventProducer.removeListener(listener);
	}

	/**
	 * Gets the type of the Plot.
	 * 
	 * @return the type of the Plot.
	 */
	@Override
	public Type getType() {
		return Formula.Type.TILE;
	}

	/**
	 * Gets the data that this Plot plots.
	 * 
	 * @return the data that this Plot plots.
	 */
	@Override
	public List<DataFrame> getData() {
		final List<DataFrame> result = new ArrayList<DataFrame>();
		result.add(getDataFrame());
		return result;
	}

	/**
	 * From FastTilePlot: getDataFrame(); uses the value of the data member "log" to determine if returning a log version or not.
	 * @return	DataFrame used in GTTilePlot
	 */
	protected DataFrame getDataFrame() {
		if (log) {
			return dataFrameLog;
		} else {
			return dataFrame;
		}
	}

	/**
	 * From FastTilePlot: getDataFrame(log); uses the passed value of the boolean "log" to determine if returning a log version or not.
	 * @param log	boolean value: if true returns log version of the DataFrame
	 * @return	DataFrame used in GTTilePlot
	 */
	protected DataFrame getDataFrame(boolean log) {
		if (log) {
			return dataFrameLog;
		} else {
			return dataFrame;
		}
	}


	/**
	 * Exports an image of this Plot to the specified file in the specified format.
	 * 
	 * @param format	the image format. One of PlotExporter.JPG, PlotExporter.TIF,
	 *            PlotExporter.PNG, or PlotExporter.BMP
	 * @param file	the file to save the image.
	 * @param width	width of image in pixels
	 * @param height	height of image in pixels
	 * @throws IOException	if there is an error creating the image
	 */
	@Override
	public void exportImage(String format, File file, int width, int height)
			throws IOException {
		// TODO rewrite exportImage function
		drawBatchImage(width, height);
		PlotExporter exporter = new PlotExporter(this);
		exporter.save(format, file, width, height);
	}

	private void drawBatchImage(int width, int height) {
		// TODO rewrite (or delete???) drawBatchImage function from FastTilePlot.java (uses BufferedImage, offScreenGraphics)
	}


	/**
	 * Configure this Plot according to the specified PlotConfiguration.
	 * 
	 * @param config    the new plot configuration
	 */
	@Override
	public void configure(PlotConfiguration config) {
		// TODO copied from FastTilePlot; may need some rewriting
		String configFile = config.getConfigFileName();
		double[] minmax = { 0.0, 0.0 };

		if (configFile != null) {
			try {
				configure(new PlotConfigurationIO().loadConfiguration(new File(configFile)));
			} catch (IOException ex) {
				Logger.error("IOException in GTTilePlot.configure: loading configuration: " + ex.getMessage());
				return;
			}
		}
		else
		{
			Logger.error("Error in GTTilePlot.configure: No configuration file specified.");
			return;
		}

		ColorMap colorMap = (ColorMap) config.getObject(TilePlotConfiguration.COLOR_MAP);

		if (colorMap != null) {

			// set log related info
			ColorMap.ScaleType sType = colorMap.getScaleType();
			preLog = log;
			if ( sType == ColorMap.ScaleType.LOGARITHM ) {
				log = true;
				logBase = (int)colorMap.getLogBase();

				// we need to also populate the non-log intervals to some default range...
				// a new map object is created and doesn't keep the interval ranges that was created in one of the constructors
				// so we need to make sure and prepopulate just in case user changes between linear and log scales
				computeDataRange(minmax, false);
				colorMap.setMinMax( minmax[0], minmax[1]);

			} else {
				log = false;
				// we need to also populate the log intervals to some default range...
				// a new map object is created and doesn't keep the interval ranges that was created in one of the constructors
				// so we need to make sure and prepopulate just in case user changes between linear and log scales
				computeDataRange(minmax, true);
				colorMap.setLogMinMax( minmax[0], minmax[1]);
			}
			updateColorMap(colorMap);
			recomputeStatistics = true;
		}

		draw();
		config = new TilePlotConfiguration(config);

		if (showGridLines != null) {
			Boolean gridlines = (Boolean)config.getObject(TilePlotConfiguration.SHOW_GRID_LINES);
			showGridLines.setSelected(gridlines == null ? false : gridlines);
		}
	}

	@Override
	public void configure(PlotConfiguration config, ConfigSource source) {
		// TODO copied from FastTilePlot; may need some rewriting
		String configFile = config.getConfigFileName();
		double[] minmax = { 0.0, 0.0 };

		if (configFile != null) {
			try {
				configure(new PlotConfigurationIO().loadConfiguration(new File(configFile)), source);
			} catch (IOException ex) {
				Logger.error("IOException in GTTilePlot.configure: loading configuration: " + ex.getMessage());
				return;
			}
		}
		else
		{
			Logger.error("Error in GTTilePlot.configure: No configuration file specified.");
			return;
		}

		configSource = source;		// not used, but was not commented out in v1.4.1

		ColorMap map = (ColorMap) config.getObject(TilePlotConfiguration.COLOR_MAP);

		if (map != null) {

			// set log related info
			ColorMap.ScaleType sType = map.getScaleType();
			preLog = log;
			if ( sType == ColorMap.ScaleType.LOGARITHM ) {
				log = true;
				logBase = (int)map.getLogBase();

				// we need to also populate the non-log intervals to some default range...
				// a new map object is created and doesn't keep the interval ranges that was created in one of the constructors
				// so we need to make sure and prepopulate just in case user changes between linear and log scales
				computeDataRange(minmax, false);
				map.setMinMax( minmax[0], minmax[1]);

			} else {
				log = false;
				// we need to also populate the log intervals to some default range...
				// a new map object is created and doesn't keep the interval ranges that was created in one of the constructors
				// so we need to make sure and prepopulate just in case user changes between linear and log scales
				computeDataRange(minmax, true);
				map.setLogMinMax( minmax[0], minmax[1]);
			}

			updateColorMap(map);
			recomputeStatistics = true;	
			if ( source == Plot.ConfigSource.FILE) {
				recomputeLegend = true;
			} 
		}

		config = new TilePlotConfiguration(config);
		draw();

		if (showGridLines != null) {
			Boolean gridlines = (Boolean)config.getObject(TilePlotConfiguration.SHOW_GRID_LINES);
			showGridLines.setSelected(gridlines == null ? false : gridlines);
		}
	}


	// Compute data range excluding BADVAL3 values:

	public void computeDataRange(double[] minmax, boolean log) {
		final int selection = statisticsMenu != null ? statisticsMenu.getSelectedIndex() : 0;
		boolean initialized = false;
		minmax[0] = minmax[1] = 0.0;
		if ( selection == 0 ) {
			DataFrame dataFrame = getDataFrame(log);
			final DataFrameIndex dataFrameIndex = 
					dataFrame.getIndex();

			for (int timestep = 0; timestep < timesteps; ++timestep) {
				for (int layer = 0; layer < layers; ++layer) {
					for (int row = 0; row < rows; ++row) {
						for (int column = 0; column < columns; ++column) {
							dataFrameIndex.set(timestep, layer, column, row);
							final float value = 
									dataFrame.getFloat(dataFrameIndex);

							if (value > MINIMUM_VALID_VALUE) {

								if (initialized) {
									if (value < minmax[0]) {
										minmax[0] = value;
									} else if (value > minmax[1]) {
										minmax[1] = value;
									}
								} else {
									minmax[0] = minmax[1] = value;
									initialized = true;
								}
							}
						}
					}
				}
			}
		} else {

			computeStatistics(log);					

			final int statistic = selection - 1;

			for ( int row = firstRow; row <= lastRow; ++row ) {

				for ( int column = firstColumn; column <= lastColumn; ++column ) {
					final float value = statisticsData[ statistic ][ row ][ column ];

					if (value > MINIMUM_VALID_VALUE) {

						if (initialized) {

							if (value < minmax[0]) {
								minmax[0] = value;
							} else if (value > minmax[1]) {
								minmax[1] = value;
							}
						} else {
							minmax[0] = minmax[1] = value;
							initialized = true;
						}
					}
				}
			}
		}
	}

	private void computeStatistics(boolean log) {
		if ( layerData == null ) {
			layerData = new float[ rows ][ columns ][ timesteps ];
			statisticsData = new float[ GridCellStatistics.STATISTICS ][ rows ][ columns ];
		}

		// Copy from dataFrame into layerData[ rows ][ columns ][ timesteps ]:

		final DataFrameIndex dataFrameIndex = getDataFrame(log).getIndex();

		for ( int row = 0; row < rows; ++row ) {
			final int dataRow = ! invertRows ? row : rows - 1 - row;

			for ( int column = 0; column < columns; ++column ) {

				for ( int timestep = 0; timestep < timesteps; ++timestep ) {
					dataFrameIndex.set( timestep, layer, column, dataRow );
					float value = getDataFrame(log).getFloat( dataFrameIndex ); 
					layerData[ row ][ column ][ timestep ] = value;
				}
			}
		}

		final double threshold = Double.parseDouble( this.threshold.getText() );
		final double hoursPerTimestep = 1.0;

		try {
			GridCellStatistics.computeStatistics( layerData,
					threshold, hoursPerTimestep,
					statisticsData, statisticsMenu.getSelectedIndex()-1 );
			statError = false;
		} catch ( Exception e) {
			Logger.error("Error occurred during computing statistics: " + e.getMessage());
			statError = true;
			if ( map != null && map.getScaleType() == ColorMap.ScaleType.LOGARITHM) {
				preLog = true;
				log = false;
				map.setScaleType( ColorMap.ScaleType.LINEAR);
				draw();	// draw() is here because just changed the ScaleType
			}
		}
	}

	/**
	 * This drawTitles member function passes the values of the GTTilePlot to the GTTilePlotPanel
	 * for the title and 2 subtitles. Adapted from part of the drawLabels function of TilePlot.java.
	 * Must have config already populated.
	 * @return	true = success, false = failure
	 */
	public boolean drawTitles()
	{
		boolean retValue = false;	// default to failure
		boolean showString;			// show string (title, etc.) or set it to null

		if(config == null)	// plot configuration structure must already be populated
		{
			Logger.error("Plot configuration was not set prior to calling drawTitles in GTTilePlot");
			return retValue;
		}
		// values for title
		String showTitle = config.getShowTitle();
		if (showTitle.compareTo("FALSE") == 0 )
			showString = false;
		else
			showString = true;
		String tString = config.getProperty(PlotConfiguration.TITLE);
		Font tFont = config.getFont(PlotConfiguration.TITLE_FONT);
		Color tColor = config.getColor(PlotConfiguration.TITLE_COLOR);
		if(tColor == null)
			tColor = labelColor;
		if(!showString)
			tString = null;
		// values for subtitle1
		String showSubtitle1 = config.getShowSubtitle1();
		if(showSubtitle1.compareTo("FALSE") == 0)
			showString = false;
		else
			showString = true;
		String s1String = config.getSubtitle1();
		Font s1Font = config.getFont(PlotConfiguration.SUBTITLE_1_FONT);
		Color s1Color = config.getColor(PlotConfiguration.SUBTITLE_1_COLOR);
		if(!showString)
			s1String = null;
		// values for subtitle2
		String showSubtitle2 = config.getShowSubtitle2();
		if(showSubtitle2.compareTo("FALSE") == 0)
			showString = false;
		else
			showString = true;
		String s2String = config.getShowSubtitle2();
		Font s2Font = config.getFont(PlotConfiguration.SUBTITLE_2_FONT);
		Color s2Color = config.getColor(PlotConfiguration.SUBTITLE_2_COLOR);
		if(!showString)
			s2String = null;
		// send the titles & their properties to the panel
		super.setTitlesPanel(tFont, tColor, tString, s1Font, s1Color, s1String, s2Font, s2Color, s2String);
		retValue = true;	// return success
		return retValue;
	}
	
	protected Action timeSeriesSelected = new AbstractAction(
			"Time Series of Probed Cell(s)") {
		private static final long serialVersionUID = -2940008125642497962L;

		public void actionPerformed(ActionEvent e) {
			requestTimeSeries(Formula.Type.TIME_SERIES_LINE);
		}
	};

	protected Action timeSeriesBarSelected = new AbstractAction(
			"Time Series Bar of Probed Cell(s)") {
		private static final long serialVersionUID = 2455217937515200807L;

		public void actionPerformed(ActionEvent e) {
			requestTimeSeries(Formula.Type.TIME_SERIES_BAR);
		}
	};

	protected Action timeSeriesMin = new AbstractAction(
			"Time Series of Min. Cell(s)") {
		private static final long serialVersionUID = 5282480503103839989L;

		public void actionPerformed(ActionEvent e) {
			DataUtilities.MinMaxPoint points = getMinMaxPoints();
			requestTimeSeries(points.getMinPoints(), "Min. cells ");
		}
	};

	protected Action timeSeriesMax = new AbstractAction(
			"Time Series of Max. Cell(s)") {
		private static final long serialVersionUID = -4465758432397962782L;

		public void actionPerformed(ActionEvent e) {
			DataUtilities.MinMaxPoint points = getMinMaxPoints();
			requestTimeSeries(points.getMaxPoints(), "Max. cells ");
		}
	};


	/**
	 *  From FastTilePlot. Creates a menu bar for this GTTilePlot. 
	 *  This may return null if there is no menu bar.
	 * 
	 * @return a menu bar for this Plot.
	 */
	public JMenuBar createMenuBar() {
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		JMenuBar bar = new JMenuBar();

		JMenu menu = new JMenu("File");
		menu.setMnemonic('F');
		menu.add(new GTTilePlotPrintAction(this));
		menu.add(new PlotExporterAction(this));
		bar.add(menu);

		menu = new JMenu("Configure");
		menu.add(new AbstractAction("Configure Plot") {
			private static final long serialVersionUID = 2455217937515200807L;

			public void actionPerformed(ActionEvent e) {
				editChartProperties();
			}
		});
		menu.add(new LoadConfiguration(this));
		menu.add(new SaveConfiguration(this));
//TODO		//configureMapMenu(menu);		// HERE IS WHERE THE CONFIGURE GIS LAYERS GOES??? JEB (Also commented out in v1.4.1
		bar.add(menu);

		menu = new JMenu("Controls");
		bar.add(menu);
		ButtonGroup grp = new ButtonGroup();
		JMenuItem item = menu.add(new JRadioButtonMenuItem(new AbstractAction("Zoom") {
			private static final long serialVersionUID = 5282480503103839989L;

			public void actionPerformed(ActionEvent e) {
				JRadioButtonMenuItem src = (JRadioButtonMenuItem) e.getSource();
				zoom = src.isSelected();
				probe = !zoom;
				if(probe)	// 2014 added logic and ability to turn probe off 
				{
					activateRubberBand();
					// change cursor
					setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
				}
				else
				{
					deactivateRubberBand();
					// change cursor
					setCursor(Cursor.getDefaultCursor());
				}
			}
		}));
		item.setSelected(true);
		grp.add(item);

		activateRubberBand();

		item = menu.add(new JRadioButtonMenuItem(new AbstractAction("Probe") {
			private static final long serialVersionUID = 8777942675687929471L;

			public void actionPerformed(ActionEvent e) {
				JRadioButtonMenuItem src = (JRadioButtonMenuItem) e.getSource();
				probe = src.isSelected();
				if(probe)
				{	// 2014 changed logic to allow user to turn probe mode on/off
					activateRubberBand();
					// change cursor
					setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
				}
				else
				{
					deactivateRubberBand();
					// change cursor
					setCursor(Cursor.getDefaultCursor());
				}
			}
		}));
		grp.add(item);
		menu.add(item);

		menu.addSeparator();

		JMenuItem menuItem = new JMenuItem(
				new AbstractAction("Set Row and Column Ranges") {
					private static final long serialVersionUID = -4465758432397962782L;

					@Override
					public void actionPerformed(ActionEvent arg0) {
						setDataRanges();
					}
				});
		menu.add(menuItem);

		menu.addSeparator();
		showGridLines = new JCheckBoxMenuItem(new AbstractAction("Show Grid Lines") {
			private static final long serialVersionUID = 2699330329257731588L;

			public void actionPerformed(ActionEvent e) {
				JCheckBoxMenuItem gridlines = (JCheckBoxMenuItem) e.getSource();
				Color configColor = (Color)config.getObject(TilePlotConfiguration.GRID_LINE_COLOR);
				Color glColor = (configColor == null ? Color.GRAY : configColor);
				((TilePlotConfiguration) config).setGridLines(gridlines.isSelected(), glColor);
				draw();
			}
		});
		menu.add(showGridLines);

		menu.addSeparator();
		item = new JCheckBoxMenuItem(new AbstractAction("Show Lat / Lon") {
			private static final long serialVersionUID = 2699330329257731588L;

			public void actionPerformed(ActionEvent e) {
				JCheckBoxMenuItem latlon = (JCheckBoxMenuItem) e.getSource();
				showLatLon = latlon.isSelected();
			}
		});
		menu.add(item);

		bar.add(menu);

		menu = new JMenu("Plot");
		bar.add(menu);
		item = menu.add(timeSeriesSelected);
		item.setEnabled(false);
		probeItems.add(item);

		item = menu.add(timeSeriesBarSelected);
		item.setEnabled(false);
		probeItems.add(item);

		item = menu.add(timeSeriesMin);
		item = menu.add(timeSeriesMax);

		menu.addSeparator();

		JMenuItem item2 = new JMenuItem(new AbstractAction("Animate Plot") {
			private static final long serialVersionUID = 6336130019191512947L;

			public void actionPerformed(ActionEvent e) {
				AnimationPanel panel = new AnimationPanel();
				panel.init(getDataFrame().getAxes(), GTTilePlot.this);
			}
		});
		menu.add(item2);

		if (this.getClass().equals(GTTilePlot.class)) {
			JMenu sub = new JMenu("Add Overlay");
			item2 = sub.add(new JMenuItem(new AbstractAction("Observations") {
				private static final long serialVersionUID = 2699330329257731588L;

				public void actionPerformed(ActionEvent e) {
					addObsOverlay();
				}
			}));

			item2 = sub.add(new JMenuItem(new AbstractAction("Vectors") {
				private static final long serialVersionUID = 1408918800912242196L;

				public void actionPerformed(ActionEvent e) {
					addVectorOverlay();
				}
			}));

			menu.add(sub);
		}

		menu = new JMenu("GIS Layers");	
		gisLayersMenu(menu);
		bar.add(menu);

		// change cursor for initial zoom state
		setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

		super.setMenuBar(bar);	// update the JMenuBar bar in GTTilePlotPanel
		return bar;
	}

	/**
	 * From FastTilePlot; handles which GIS layers to show and which ones to not show
	 * @param layerKey	Key into structure of layers
	 * @param show	boolean: true to show layer, false to not show that layer
	 * @param addJMenuLayers	JMenu
	 */
	private void showLayer(String layerKey, boolean show, JMenu addJMenuLayers) {
		try {
			if (show && layerKey.equals(STATES_LAYER)) {
				VerdiBoundaries map2Add = mapper.getUsaStatesMap();
				myMapContent.addLayers(map2Add.getMap().layers());
				mapper.getLayers().add(map2Add);
			}

			if (!show && layerKey.equals(STATES_LAYER)) {
				VerdiBoundaries map2Remove = mapper.getUsaStatesMap();
				myMapContent.removeLayer(map2Remove.getMap().layers().get(0));	// assume 1st layer
				mapper.removeUsaStates();
			}

			if (show && layerKey.equals(COUNTIES_LAYER)) {
				VerdiBoundaries map2Add = mapper.getUsaCountiesMap();
				myMapContent.addLayers(map2Add.getMap().layers());
				mapper.getLayers().add(map2Add);
			}

			if (!show && layerKey.equals(COUNTIES_LAYER)) {
				VerdiBoundaries map2Remove = mapper.getUsaCountiesMap();
				myMapContent.removeLayer(map2Remove.getMap().layers().get(0));
				mapper.removeUsaCounties();
			}

			if (show && layerKey.equals(WORLD_LAYER)) {
				VerdiBoundaries map2Add = mapper.getWorldMap();
				myMapContent.addLayers(map2Add.getMap().layers());
				mapper.getLayers().add(map2Add);
			}

			if (!show && layerKey.equals(WORLD_LAYER)) {
				VerdiBoundaries map2Remove = mapper.getWorldMap();
				myMapContent.removeLayer(map2Remove.getMap().layers().get(0));
				mapper.removeWorld();
			}

			if (show && layerKey.equals(NA_LAYER)) {
				VerdiBoundaries map2Add = mapper.getNorthAmericaMap();
				myMapContent.addLayers(map2Add.getMap().layers());
				mapper.getLayers().add(map2Add);
			}

			if (!show && layerKey.equals(NA_LAYER)) {
				VerdiBoundaries map2Remove = mapper.getNorthAmericaMap();
				myMapContent.removeLayer(map2Remove.getMap().layers().get(0));
				mapper.removeNorthAmerica();
			}

			if (show && layerKey.equals(HUCS)) {
				withHucs = show;
				myMapContent.addLayers(mapper.getUSHucMap().getMap().layers());
				mapper.getLayers().add(mapper.getUSHucMap());
			}

			if (!show && layerKey.equals(HUCS)) {
				withHucs = show;
				VerdiBoundaries map2Remove = mapper.getUSHucMap();
				myMapContent.removeLayer(map2Remove.getMap().layers().get(0));
				mapper.removeUSHucMap();
			}

			if (show && layerKey.equals(RIVERS)) {
				withRivers = show;
				myMapContent.addLayers(mapper.getUSRiversMap().getMap().layers());
				mapper.getLayers().add(mapper.getUSRiversMap());
			}

			if (!show && layerKey.equals(RIVERS)) {
				withRivers = show;
				VerdiBoundaries map2Remove = mapper.getUSRiversMap();
				myMapContent.removeLayer(map2Remove.getMap().layers().get(0));
				mapper.removeUSRiversMap();
			}

			if (show && layerKey.equals(ROADS)) {
				withRoads = show;
				myMapContent.addLayers(mapper.getUSRoadsMap().getMap().layers());
				mapper.getLayers().add(mapper.getUSRoadsMap());
			}

			if (!show && layerKey.equals(ROADS)) {
				withRoads = show;
				VerdiBoundaries map2Remove = mapper.getUSRoadsMap();
				myMapContent.removeLayer(map2Remove.getMap().layers().get(0));
				mapper.removeUSRoadsMap();
			}

			if (layerKey.equals(OTHER_MAPS)) {
				//				showGISLayersDialog();	// 2015 CHANGED THIS TO BRING UP FILE BROWSER FOR .SHP FILES
				File selectFile = JFileDataStoreChooser.showOpenFile("shp", null);
				VerdiBoundaries aVerdiBoundaries = new VerdiBoundaries();
				aVerdiBoundaries.setFileName(selectFile.getAbsolutePath());
				mapper.getLayers().add(aVerdiBoundaries);
				myMapContent.addLayers(aVerdiBoundaries.getMap().layers());
			}
			draw();
		} catch (Exception e) {
			Logger.error("Error adding layer " + e.getMessage());
		}
	}


	/**
	 * From FastTilePlot. Displays a dialog that allows the user to edit the properties for the current chart.
	 * 
	 * @since 1.0.5
	 */
	public void editChartProperties() {
		Window window = SwingUtilities.getWindowAncestor(GTTilePlot.this);
		dialog = null;
		if (window instanceof JFrame)
			dialog = new ConfigDialog((JFrame) window);	// ConfigDialog is anl.verdi.plot.gui.ConfigDialog
		else
			dialog = new ConfigDialog((JDialog) window);
		dialog.init(GTTilePlot.this, minMax);
		dialog.enableScale( !this.statError);
		dialog.setSize(500, 600);
		dialog.setVisible(true);
	}

	/**
	 * from FastTilePlot
	 */
	protected void addObsOverlay() {
		OverlayRequest<ObsEvaluator> request = new OverlayRequest<ObsEvaluator>(OverlayRequest.Type.OBS, this);
		eventProducer.fireOverlayRequest(request);
	}

	/**
	 * from FastTilePlot
	 */
	protected void addVectorOverlay() {
		OverlayRequest<VectorEvaluator> request = new OverlayRequest<VectorEvaluator>(OverlayRequest.Type.VECTOR, this);
		eventProducer.fireOverlayRequest(request);
	}

	/**
	 * From FastTilePlot; turn on rubber band widget.
	 */
	protected void activateRubberBand() {
		rubberband.setActive(true);
	}

	/**
	 * from FastTilePlot; turn off rubber band widget
	 */
	protected void deactivateRubberBand() {	// 2014 to allow user to turn OFF probe
		rubberband.setActive(false);
	}

	/**
	 * From FastTilePlot: setDataRanges instantiates a special dialog for data ranges and then calls that dialog.
	 */
	private void setDataRanges() {

		// NOTE: 2015 appears DataRangeDialog is an inner class; based on Oracle's JavaSE tutorials,
		// proper way to instantiate an inner class is to first instantiate the outer class, and
		// then create the inner object with this syntax:
		// OuterClass.InnerClass innerObject = outerObject.new InnerClass();
		GTTilePlot.DataRangeDialog dialog = this.new DataRangeDialog("Set Row and Column Ranges",
				GTTilePlot.this, firstRow + 1, lastRow + 1, firstColumn + 1,
				lastColumn + 1);
		dialog.showDialog();
	}

	/**
	 * Private class DataRangeDialog to handle functionality of DataRangeDialog objects
	 * @author Jo Ellen Brandmeyer
	 *
	 */
	private class DataRangeDialog extends JDialog {
		private static final long serialVersionUID = -1110292652911018568L;
		public static final int CANCEL_OPTION = -1;
		public static final int YES_OPTION = 1;
		public static final int ERROR = 0;
		private GTTilePlot plot;
		private JTextField fRowField;
		private JTextField lRowField;
		private JTextField fColumnField;
		private JTextField lColumnField;
		private boolean cancelled = false;
		private int firstRow, lastRow, firstColumn, lastColumn;

		/**
		 * From FastTilePlot: Constructor for DataRangeDialog class
		 * @param title	Title of the dialog frame.
		 * @param plot	The current GTTilePlot object.
		 * @param firstRow	Number of the first row of the plot.
		 * @param lastRow	Number of the last row of the plot.
		 * @param firstColumn	Number of the first column of the plot.
		 * @param lastColumn	Number of the last column of the plot.
		 */
		public DataRangeDialog(String title, GTTilePlot plot, int firstRow,
				int lastRow, int firstColumn, int lastColumn) {
			super.setTitle(title);
			super.setLocation(getCenterPoint(plot));
			super.setModal(true);
			super.setPreferredSize(new Dimension(400, 300));
			this.firstRow = firstRow;
			this.lastRow = lastRow;
			this.firstColumn = firstColumn;
			this.lastColumn = lastColumn;
			this.fRowField = new JTextField("1", 4);
			this.lRowField = new JTextField("1", 4);
			this.fColumnField = new JTextField("1", 4);
			this.lColumnField = new JTextField("1", 4);
			this.plot = plot;
			this.getContentPane().add(createLayout());
		}

		/**
		 * From FastTilePlot: showDialog() member function of private class DataRangeDialog.
		 * Displays the dialog and processes the ranges for the rows and columns as set by the user.
		 * @return	An integer representing the error condition.
		 */
		public int showDialog() {
			this.pack();
			this.setVisible(true);

			if (this.cancelled)
				return CANCEL_OPTION;

			try {
				firstRow = Integer.valueOf(fRowField.getText());
				lastRow = Integer.valueOf(lRowField.getText());
				firstColumn = Integer.valueOf(fColumnField.getText());
				lastColumn = Integer.valueOf(lColumnField.getText());
				plot.resetRowsNColumns(firstRow, lastRow, firstColumn,
						lastColumn);
				return YES_OPTION;
			} catch (NumberFormatException e) {
				Logger.error("Number Format Exception in GTTilePlot.showDialog: Set Rows and Columns: " + e.getMessage());
			}

			return ERROR;
		}


		/**
		 * From FastTilePlot; creates a JPanel to hold a "middle panel" JPanel and a "buttons panel" JPanel
		 * @return	JPanel containing 2 JPanel objects
		 */
		private JPanel createLayout() {
			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
			panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

			panel.add(createMiddlePanel());
			panel.add(createButtonsPanel());

			return panel;
		}

		/**
		 * From FastTilePlot
		 * @return	JPanel
		 */
		private JPanel createMiddlePanel() {
			GridBagLayout gridbag = new GridBagLayout();
			GridBagConstraints c = new GridBagConstraints();
			JPanel contentPanel = new JPanel(gridbag);

			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;
			c.insets = new Insets(1, 1, 7, 5);

			JLabel rowLabel = new JLabel("Rows:");
			JPanel rowPanel = new JPanel();
			rowPanel.add(fRowField, BorderLayout.LINE_START);
			fRowField.setText(this.firstRow + "");
			rowPanel.add(new JLabel("..."));
			rowPanel.add(lRowField, BorderLayout.LINE_END);
			lRowField.setText(this.lastRow + "");
			JLabel holder1 = new JLabel();

			gridbag.setConstraints(rowLabel, c);
			gridbag.setConstraints(rowPanel, c);
			c.gridwidth = GridBagConstraints.REMAINDER; // end row
			gridbag.setConstraints(holder1, c);
			contentPanel.add(rowLabel);
			contentPanel.add(rowPanel);
			contentPanel.add(holder1);

			c.gridwidth = 1; // next-to-last in row

			JLabel colLabel = new JLabel("Columns:");
			JPanel columnPanel = new JPanel();
			columnPanel.add(fColumnField, BorderLayout.LINE_START);
			fColumnField.setText(this.firstColumn + "");
			columnPanel.add(new JLabel("..."));
			columnPanel.add(lColumnField, BorderLayout.LINE_END);
			lColumnField.setText(this.lastColumn + "");
			JLabel holder2 = new JLabel();

			gridbag.setConstraints(colLabel, c);
			gridbag.setConstraints(columnPanel, c);
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(holder2, c);
			contentPanel.add(colLabel);
			contentPanel.add(columnPanel);
			contentPanel.add(holder2);

			return contentPanel;
		}

		/**
		 * From FastTilePlot; create a buttons panel (OK, Cancel buttons)
		 * @return	JPanel 
		 */
		private JPanel createButtonsPanel() {
			JPanel container = new JPanel();
			FlowLayout layout = new FlowLayout();
			layout.setHgap(20);
			layout.setVgap(2);
			container.setLayout(layout);

			JButton okButton = new JButton("OK");
			okButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					cancelled = false;
					dispose();
				}
			});

			container.add(okButton);
			getRootPane().setDefaultButton(okButton);

			JButton cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					cancelled = true;
					dispose();
				}
			});
			container.add(cancelButton);
			container.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

			return container;
		}



		/**
		 * From FastTilePlot; get the location of the center the screen (primary display only)
		 * @param comp	a Component; the screen
		 * @return	a Point; the center point of the screen
		 */
		private Point getCenterPoint(Component comp) {
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

			if (comp == null) {
				return new Point((int) screenSize.getWidth() / 2,
						(int) screenSize.getHeight() / 2);
			}

			Dimension frameSize = comp.getSize();

			if (frameSize.height > screenSize.height) {
				frameSize.height = screenSize.height;
			}

			if (frameSize.width > screenSize.width) {
				frameSize.width = screenSize.width;
			}

			return new Point((screenSize.width - frameSize.width) / 2,
					(screenSize.height - frameSize.height) / 2);
		}
	}

	/**
	 * From FastTilePlot
	 * @param addLayers	JMenu of GIS layers, each of which may be checked
	 */
	protected void resetMenuItems(JMenu addLayers) {
		if (addLayers == null) return;

		JCheckBoxMenuItem world = (JCheckBoxMenuItem)addLayers.getItem(0);
		JCheckBoxMenuItem na = (JCheckBoxMenuItem)addLayers.getItem(1);
		JCheckBoxMenuItem states = (JCheckBoxMenuItem)addLayers.getItem(2);
		JCheckBoxMenuItem counties = (JCheckBoxMenuItem)addLayers.getItem(3);
		JCheckBoxMenuItem hucs = (JCheckBoxMenuItem)addLayers.getItem(4);
		JCheckBoxMenuItem rivers = (JCheckBoxMenuItem)addLayers.getItem(5);
		JCheckBoxMenuItem roads = (JCheckBoxMenuItem)addLayers.getItem(6);

		world.setSelected(mapper.worldMapIncluded());
		states.setSelected(mapper.usStatesMapIncluded());
		counties.setSelected(mapper.usCountiesMapIncluded());
		hucs.setSelected(mapper.usHucsMapIncluded());
		rivers.setSelected(mapper.usRiversMapIncluded());
		roads.setSelected(mapper.usRoadsMapIncluded());
		na.setSelected(mapper.naMapIncluded());
	}

	
	
	/**
	 * From FastTilePlot: showGISLayersDialog()
	 * Possibly replace this by JFileDataStoreChooser
	 * and separate GeoTools dialog to handle layer order and features (colors, etc.)
	 * @return	GTTileLayerEditor
	 */
	private GTTileLayerEditor showGISLayersDialog() {
		// TODO evaluate GeoTools built-in box for specifying color, thickness of shapefile lines
		Logger.debug("in GTTilePlot.showGISLayersDialog()");
		Window frame = SwingUtilities.getWindowAncestor(this);
		GTTileLayerEditor editor = null;

		if (frame instanceof JFrame)
			editor = new GTTileLayerEditor((JFrame) frame);
		else
			editor = new GTTileLayerEditor((JDialog) frame);

		editor.init(mapper);
		editor.setLocationRelativeTo(frame);
		editor.setVisible(true);
		editor.pack();
		return editor;
	}

	/**
	 * From FastTilePlot. Set subdomain by rows and columns, with adjustment for 0-based data
	 * and 1-based user input.
	 * @param fRow	first row
	 * @param lRow	last for
	 * @param fColumn	first column
	 * @param lColumn	last column
	 */
	public void resetRowsNColumns(int fRow, int lRow, int fColumn, int lColumn) {
		if (fRow < 1)
			fRow = 1;

		if (fRow > rows)
			fRow = rows;

		firstRow = fRow - 1;

		if (lRow < 1)
			lRow = 1;

		if (lRow > rows)
			lRow = rows;

		lastRow = lRow - 1;

		if (firstRow > lastRow) {
			int temp = firstRow;
			firstRow = lastRow;
			lastRow = temp;
		}

		if (fColumn < 1)
			fColumn = 1;

		if (fColumn > columns)
			fColumn = columns;

		firstColumn = fColumn - 1;

		if (lColumn < 1)
			lColumn = 1;

		if (lColumn > columns)
			lColumn = columns;

		lastColumn = lColumn - 1;

		if (firstColumn > lastColumn) {
			int temp = firstColumn;
			firstColumn = lastColumn;
			lastColumn = temp;
		}

		computeDerivedAttributes();
	}

	/**
	 * From FastTilePlot; need to rewrite for a JMapPane
	 * Called by changing zoom and by changing subset through resetRowsNColumns()
	 */
	private void computeDerivedAttributes() {	// called by zooming and resetRowsNColumns
		// JEB 2016 figure this out & rewrite for JMapPane
		// & get rid of Projector (use CRS) ???

		// Compute grid bounds and domain:

		gridBounds[X][MINIMUM] = westEdge + firstColumn * cellWidth;
		gridBounds[X][MAXIMUM] = westEdge + (1 + lastColumn) * cellWidth;
		gridBounds[Y][MINIMUM] = southEdge + firstRow * cellHeight;
		gridBounds[Y][MAXIMUM] = southEdge + (1 + lastRow) * cellHeight;

		if (projector != null) {
			computeMapDomain(projector, gridBounds, domain);
		} else {
			domain[LONGITUDE][MINIMUM] = gridBounds[X][MINIMUM];
			domain[LONGITUDE][MAXIMUM] = gridBounds[X][MAXIMUM];
			domain[LATITUDE][MINIMUM] = gridBounds[Y][MINIMUM];
			domain[LATITUDE][MAXIMUM] = gridBounds[Y][MAXIMUM];
		}
	}

	// Compute map domain from grid bounds:

	private static void computeMapDomain(final Projector projector,		// 2016 replace Projector with CRS ???
			final double[][] gridBounds, double[][] mapDomain) {
		// JEB 2016 figure this out & rewrite for JMapPane
		// & get rid of Projector (use CRS) ???

		final double margin = 1.0; // Degrees lon/lat beyond grid corners.
		final double xMinimum = gridBounds[X][MINIMUM];
		final double xMaximum = gridBounds[X][MAXIMUM];
		final double yMinimum = gridBounds[Y][MINIMUM];
		final double yMaximum = gridBounds[Y][MAXIMUM];
		final double xMean = (xMinimum + xMaximum) * 0.5;
		double[] longitudeLatitude = { 0.0, 0.0 };

		// Unproject corners of bottom edge of grid for latitude minimum:
		projector.unproject(xMinimum, yMinimum, longitudeLatitude);
		mapDomain[LONGITUDE][MINIMUM] = longitudeLatitude[LONGITUDE];
		mapDomain[LATITUDE][MINIMUM] = longitudeLatitude[LATITUDE];
		projector.unproject(xMaximum, yMinimum, longitudeLatitude);
		mapDomain[LONGITUDE][MAXIMUM] = longitudeLatitude[LONGITUDE];
		mapDomain[LATITUDE][MINIMUM] = Math.min(mapDomain[LATITUDE][MINIMUM],
				longitudeLatitude[LATITUDE]);

		// Unproject corners and center of top edge of grid for latitude maximum:

		projector.unproject(xMinimum, yMaximum, longitudeLatitude);
		mapDomain[LONGITUDE][MINIMUM] = Math.min(mapDomain[LONGITUDE][MINIMUM],
				longitudeLatitude[LONGITUDE]);
		mapDomain[LATITUDE][MAXIMUM] = longitudeLatitude[LATITUDE];
		projector.unproject(xMaximum, yMaximum, longitudeLatitude);
		mapDomain[LONGITUDE][MAXIMUM] = Math.max(mapDomain[LONGITUDE][MAXIMUM],
				longitudeLatitude[LONGITUDE]);
		mapDomain[LATITUDE][MAXIMUM] = Math.max(mapDomain[LATITUDE][MAXIMUM],
				longitudeLatitude[LATITUDE]);

		if ( projector.getProjection() instanceof
				ucar.unidata.geoloc.projection.Stereographic ) {	// JEB 2016 probably need to change
			// testing for a polar projection

			// Must be a polar projection so
			// use full domain in case grid crosses the equator:

			mapDomain[LATITUDE ][MINIMUM] = -90.0;
			mapDomain[LATITUDE ][MAXIMUM] =  90.0;
			mapDomain[LONGITUDE][MINIMUM] = -180.0;
			mapDomain[LONGITUDE][MAXIMUM] = 180.0;
		} else { // Non-polar projection:
			projector.unproject(xMean, yMaximum, longitudeLatitude);
			mapDomain[LATITUDE][MAXIMUM] = Math.max(
					mapDomain[LATITUDE][MAXIMUM], longitudeLatitude[LATITUDE]);

			// Expand domain by margin all around, within valid range:

			mapDomain[LONGITUDE][MINIMUM] = Numerics.clamp(
					mapDomain[LONGITUDE][MINIMUM] - margin, -180.0, 180.0);
			mapDomain[LONGITUDE][MAXIMUM] = Numerics.clamp(
					mapDomain[LONGITUDE][MAXIMUM] + margin, -180.0, 180.0);
			mapDomain[LATITUDE][MINIMUM] = Numerics.clamp(
					mapDomain[LATITUDE][MINIMUM] - margin, -90.0, 90.0);
			mapDomain[LATITUDE][MAXIMUM] = Numerics.clamp(
					mapDomain[LATITUDE][MAXIMUM] + margin, -90.0, 90.0);
		}
	}
	/**
	 * From FastTilePlot constructor: new function to create the JToolBar that goes into the GTTilePlotPanel
	 * @param dataFrame	DataFrame passed from GTTilePlot constructor
	 * @return JToolBar object created and populated in this function, ready to be sent to GTTilePlotPanel
	 */
	public JToolBar createToolBar(DataFrame dataFrame)
	{
		timeLayerPanel = new TimeLayerPanel();
		final DataFrameAxis lAxis = dataFrame.getAxes().getZAxis();
		if (hasNoLayer) {
			timeLayerPanel.init(dataFrame.getAxes(), firstTimestep, 0, false);
		} else {
			timeLayerPanel.init(dataFrame.getAxes(), firstTimestep, firstLayer, lAxis.getExtent() > 1);
		}

		ChangeListener timeStepListener = new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (processTimeChange) {
					redrawTimeLayer();
				}
			}
		};

		timeLayerPanel.addSpinnerListeners(timeStepListener, timeStepListener);

		playStopButton = new JButton(PLAY);
		playStopButton.setToolTipText(PLAY_TIP);
		playStopButton.addActionListener(this);
		leftStepButton = new JButton(LEFT);
		leftStepButton.addActionListener(this);
		leftStepButton.setToolTipText(LEFT_TIP);
		rightStepButton = new JButton(RIGHT);
		rightStepButton.addActionListener(this);
		rightStepButton.setToolTipText(RIGHT_TIP);
		delayField = new JTextField("50", 4);
		delayField.addActionListener(this);						// 2014 needed to handle user changing delay in an animation
		delayField.setToolTipText("Set animation delay (ms)");	// 2014
		firstRowField = new JTextField("1", 4);
		firstRowField.addActionListener(this);
		lastRowField = new JTextField(rows + "", 4);
		lastRowField.addActionListener(this);
		firstColumnField = new JTextField("1", 4);
		firstColumnField.addActionListener(this);
		lastColumnField = new JTextField(columns + "", 4);
		lastColumnField.addActionListener(this);

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		JPanel panel = new JPanel(gridbag);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		c.insets = new Insets(0, 0, 0, 10);

		final JPanel statisticsPanel = new JPanel();
		final String[] statisticsNames = new String[ GridCellStatistics.STATISTICS ];
		statisticsNames[ 0 ] = "None";

		for ( int index = 1; index < GridCellStatistics.STATISTICS; ++index ) {
			statisticsNames[ index ] = GridCellStatistics.name( index - 1 );
		}

		// Force menu visible on WIN32?
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		statisticsMenu = new JComboBox(statisticsNames);
		statisticsMenu.addActionListener(this); // Just so draw() is called.
		statisticsPanel.add( new JLabel( "Stats:" ) );
		statisticsPanel.add( statisticsMenu );
		statisticsPanel.add( new JLabel( ">" ) );
		threshold = new JTextField( "0.12" );
		threshold.addActionListener( this );
		statisticsPanel.add( threshold );

		JPanel animate = new JPanel();
		animate.add(leftStepButton); 
		animate.add(playStopButton);
		animate.add(rightStepButton);
		animate.add(new JLabel(DELAY_LABEL));
		animate.add(delayField);

		gridbag.setConstraints(timeLayerPanel, c);
		c.weightx = 0.2;
		gridbag.setConstraints(animate, c);
		panel.add(timeLayerPanel);
		panel.add( statisticsPanel );
		panel.add(animate);
		toolBar.add(panel);		// END createToolBar function
		return toolBar;
	}

	/**
	 * From FastTilePlot: redrawTimeLayer called from createToolBar 
	 * Uses the selected time step and layer to update both the time step and the layer.
	 * The update functions also call the draw() function to trigger graphics repaint.
	 */
	private void redrawTimeLayer() {
		final int tValue = timeLayerPanel.getTime();
		final int lValue = timeLayerPanel.getLayer();

		if (tValue >= firstTimestep) {
			setTimestep(tValue); // Calls draw().
		} 

		if (lValue >= firstLayer) {
			setLayer(lValue); // Calls draw().
		}
	}

	/**
	 * From FastTilePlot: called as part of redrawTimeLayer, from createToolBar
	 * @param timestep	Time step for which map needs to be displayed
	 */
	public void setTimestep(int timestep) {
		if (timestep >= firstTimestep && timestep <= lastTimestep && timestep != this.timestep) {
			this.timestep = timestep;
			Logger.debug("ready to call copySubsetLayerData from setTimestep");
			copySubsetLayerData(this.log);
			draw();
			drawOverLays();
		}
	}

	/**
	 * From FastTilePlot: called as part of redrawTimeLayer, from createToolBar
	 * @param layer	vertical layer for which map needs to be displayed
	 */
	public void setLayer(int layer) {
		if (layer >= firstLayer && layer <= lastLayer && layer != this.layer) {
			this.layer = layer;
			final int selection = statisticsMenu.getSelectedIndex();

			if ( selection > 0 ) {
				recomputeStatistics = true;
			}

			Logger.debug("ready to call copySubsetLayerData from setLayer");
			copySubsetLayerData(this.log);
			draw();
		}
	}

	/**
	 * From FastTilePlot: called as part of setTimestep and setLayer; from redrawTimeLayer, from createToolBar
	 * Copy current timestep, layer and row/column subset data from dataFrame into subsetlayerdata[][]:
	 * @param log	boolean (true/false) to govern which parts of the code are executed
	 */
	private void copySubsetLayerData(boolean log) {

		// Reallocate the subsetLayerData[][] only if needed:

		final int subsetLayerRows = 1 + lastRow - firstRow;
		final int subsetLayerColumns = 1 + lastColumn - firstColumn;

		Logger.debug("into function copySubsetLayerData");	// NOT IN LOG FILE

		if (subsetLayerData == null
				|| subsetLayerData.length != subsetLayerRows * subsetLayerColumns
				|| subsetLayerData[0].length != subsetLayerColumns) {
			subsetLayerData = new float[subsetLayerRows][subsetLayerColumns];
		}

		final int selection = statisticsMenu.getSelectedIndex();

		if ( selection == 0 ) {

			// Copy from dataFrame into subsetLayerData[ rows ][ columns ]:

			final DataFrameIndex dataFrameIndex = getDataFrame(log).getIndex();

			for ( int row = firstRow; row <= lastRow; ++row ) {
				final int dataRow = ! invertRows ? row : rows - 1 - row;

				for ( int column = firstColumn; column <= lastColumn; ++column ) {
					dataFrameIndex.set( timestep-firstTimestep, layer-firstLayer, column, dataRow ) ;
					final float value = getDataFrame(log).getFloat( dataFrameIndex );
					subsetLayerData[row - firstRow][column - firstColumn] = value;
				}
			}
		} else {
			final int statistic = selection - 1;

			if ( statisticsData == null || recomputeStatistics ) {
				computeStatistics(log);
				recomputeStatistics = false;
			}

			// Copy from statisticsData into subsetLayerData[ rows ][ columns ]:

			for ( int row = firstRow; row <= lastRow; ++row ) {

				for ( int column = firstColumn; column <= lastColumn; ++column ) {
					final float value = statisticsData[ statistic ][ row ][ column ];
					subsetLayerData[row - firstRow][column - firstColumn] = value;
				}
			}
		}

		if ( recomputeLegend ) {
			// computeLegend();		// WHY IS THIS COMMENTED OUT?
			recomputeLegend = false;
		}
	}

	/**
	 * From FastTilePlot: update observation and/or vector annotations to current timestep
	 */
	private void drawOverLays() {
		try {
			if (obsAnnotations != null)  {
				for (ObsAnnotation ann : obsAnnotations) 
					ann.update(timestep);
			}

			if (vectAnnotation != null) {
				vectAnnotation.update(timestep);
			}
		} catch (Exception e) {
			setOverlayErrorMsg(e.getMessage());
		}
	}

	/**
	 * From FastTilePlot; menu of GIS coverages from which the user can pick layers to include on the chart 
	 * @param menu	JMenu on which the map layers are displayed
	 */
	private void gisLayersMenu(JMenu menu) {
		menu.add(mapLayersMenu);

		ActionListener listener = new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				Object srcObj = evt.getSource();

				if (srcObj instanceof JCheckBoxMenuItem) {
					JCheckBoxMenuItem item = (JCheckBoxMenuItem) srcObj;
					showLayer(item.getActionCommand(),  item.isSelected(), mapLayersMenu);
				} else 
					showLayer(OTHER_MAPS, false, mapLayersMenu);
			}
		};

		JCheckBoxMenuItem item = new JCheckBoxMenuItem("World", false);
		item.setActionCommand(WORLD_LAYER);
		item.addActionListener(listener);
		mapLayersMenu.add(item);

		item = new JCheckBoxMenuItem("North America", false);
		item.setActionCommand(NA_LAYER);
		item.addActionListener(listener);
		mapLayersMenu.add(item);

		item = new JCheckBoxMenuItem("USA States", false);
		item.setActionCommand(STATES_LAYER);
		item.addActionListener(listener);
		mapLayersMenu.add(item);

		item = new JCheckBoxMenuItem("USA Counties", false);
		item.setActionCommand(COUNTIES_LAYER);
		item.addActionListener(listener);
		mapLayersMenu.add(item);

		item = new JCheckBoxMenuItem("HUCs", false);
		item.setActionCommand(HUCS);
		item.addActionListener(listener);
		mapLayersMenu.add(item);

		item = new JCheckBoxMenuItem("Rivers", false);
		item.setActionCommand(RIVERS);
		item.addActionListener(listener);
		mapLayersMenu.add(item);

		item = new JCheckBoxMenuItem("Roads", false);
		item.setActionCommand(ROADS);
		item.addActionListener(listener);
		mapLayersMenu.add(item);

		JMenuItem otheritem = new JMenuItem("Other...");
		otheritem.setActionCommand(OTHER_MAPS);
		otheritem.addActionListener(listener);
		mapLayersMenu.add(otheritem);

		menu.add(new AbstractAction("Configure GIS Layers") {
			private static final long serialVersionUID = -3679673290623274686L;

			public void actionPerformed(ActionEvent e) {
				GTTileLayerEditor editor = showGISLayersDialog();

				if (!editor.wasCanceled()) {
					resetMenuItems(mapLayersMenu);
					draw();
				}
			}
		});

		menu.addSeparator();

		JMenuItem defaultItem = new JMenuItem(new AbstractAction(
				"Set Current Maps As Plot Default") {
			private static final long serialVersionUID = 2403382186582489960L;

			public void actionPerformed(ActionEvent e) {
				// TBI
			}
		});
		defaultItem.setEnabled(false);
		menu.add(defaultItem);
	}

	/**
	 * This drawFooters member function passes the values of the GTTilePlot to the GTTilePlotPanel
	 * for the 2 footers. Adapted from part of the drawLabels function of TilePlot.java.
	 * Must have config already populated.
	 * @return	true = success, false = failure
	 */
	public boolean drawFooters()
	{
		boolean retValue = false;		// default to failure
		boolean showString;				// show footer string or set it to null

		if(config == null)
		{
			Logger.error("Plot configuration was not set prior to calling drawFooters in GTTilePlot");
			return retValue;
		}
		// values for footer1
		String showFooter1 = config.getShowFooter1();
		if(showFooter1.compareTo("FALSE") == 0)
			showString = false;
		else
			showString = true;
		String f1String = config.getString(PlotConfiguration.FOOTER1);
		Font f1Font = config.getFont(PlotConfiguration.FOOTER1_FONT);
		Color f1Color = config.getColor(PlotConfiguration.FOOTER1_COLOR);
		if(f1Color == null)
			f1Color = labelColor;
		if(!showString)
			f1String = null;
		// values for footer2
		String showFooter2 = config.getShowFooter2();
		if(showFooter2.compareTo("FALSE") == 0)
			showString = false;
		else
			showString = true;
		String f2String = config.getString(PlotConfiguration.FOOTER2);
		Font f2Font = config.getFont(PlotConfiguration.FOOTER2_FONT);
		Color f2Color = config.getColor(PlotConfiguration.FOOTER2_COLOR);
		if(f2Color == null)
			f2Color = labelColor;
		if(!showString)
			f2String = null;
		// send the footers and their properties to the panel
		super.setFootersPanel(f1Font, f1Color, f1String, f2Font, f2Color, f2String);
		retValue = true;
		return retValue;
	}

	/**
	 * paintComponent method for overall graphics
	 */
	public void paintComponent(final Graphics g)
	{
		super.paintComponent(g);
		draw();
	}
	
	/**
	 * The purpose of this draw function is to trigger a redraw of the tile plot
	 */
	private void draw() {
		super.repaint();
		repaint();	// tell graphics system to redraw appropriate portion of the graphics
	}

	private void updateColorMap(ColorMap colorMap) {
		//		map = map;		// Feb 2016 Why have this line? It doesn't do anything

		try {
			minMax = new DataUtilities.MinMax(colorMap.getMin(), colorMap.getMax());
		} catch (Exception e) {
			Logger.error("Exception in GTTilePlot.updateColorMap: " + e.getMessage());
			e.printStackTrace();
			return;
		}

		defaultPalette = colorMap.getPalette();
		legendColors = defaultPalette.getColors();
		int count = legendColors.length;
		legendLevels = new double[count + 1];

		for (int i = 0; i < count; i++)
			try {
				legendLevels[i] = colorMap.getIntervalStart(i);
			} catch (Exception e) {
				Logger.error("Exception in GTTilePlot.updateColorMap: " + e.getMessage());
				e.printStackTrace();
			}

		try {
			legendLevels[count] = colorMap.getMax();
		} catch (Exception e) {
			Logger.error("Exception in GTTilePlot.updateColorMap: " + e.getMessage());
			e.printStackTrace();
			Logger.error("GTTilePlot's updateColorMap method "+ e.getMessage());
			return;
		}
	}


	/**
	 * Gets this Plot's configuration data.
	 * 
	 * @return this Plot's configuration data.
	 */
	@Override
	public PlotConfiguration getPlotConfiguration() {
		return config;
	}

//	/**
//	 * Gets a BufferedImage of the plot.
//	 * 
//	 * @return a BufferedImage of the plot.
//	 */
//	@Override
//	public BufferedImage getBufferedImage() {
//		// copied from FastTilePlot; needs rewrite
//		return getBufferedImage(getWidth(), getHeight());
//	}
//
//	/**
//	 * Gets a BufferedImage of the plot.
//	 * 
//	 * @param width	the width of the image in pixels
//	 * @param height	the height of the image in pixels
//	 * @return a BufferedImage of the plot.
//	 */
//	@Override
//	public BufferedImage getBufferedImage(int width, int height) {
//		// copied from FastTilePlot; needs rewrite
//		return bImage;
//	}

	/**
	 * getTitle(): Return the figure's title to the calling program
	 */
	@Override
	public String getTitle() {
		return super.getTString();
	}

	@Override
	public void updateTimeStep(int timestep) {
		// TODO Auto-generated method stub

	}

	/**
	 * From FastTilePlot: stateChanged for a ChangeEvent
	 * Executed only if the ChangeEvent is for the TimeLayerPanel
	 */
	@Override
	public void stateChanged(ChangeEvent event) {
		final Object source = event.getSource();
		if (source instanceof TimeLayerPanel) {
			final TimeLayerPanel timelayer = (TimeLayerPanel) source;
			final int tValue = timelayer.getTime();
			final int lValue = timelayer.getLayer();

			if (tValue > 0) {
				final int newValue = nextValue(tValue - timestep, timestep, firstTimestep, lastTimestep);
				timeLayerPanel.setTime(newValue);
				setTimestep(newValue); // Calls draw().
			} else if (lValue > 0) {
				final int newValue = nextValue(lValue - layer, layer, firstLayer, lastLayer);
				timeLayerPanel.setLayer(newValue);
				setLayer(newValue); // Calls draw().
			}
		}

	}

	/**
	 * From FastTilePlot: Get the next value in a series; used for both time and layer
	 * @param delta	Difference from current to selected value
	 * @param current	Current value
	 * @param min	Minimum value
	 * @param max	Maximum value
	 * @return	The next value
	 */
	private int nextValue(int delta, int current, int min, int max) {
		int result = current + delta;

		if (result < min) {
			result = max - (min - result) + 1;
		} else if (result > max) {
			result = min + (result - max) - 1;
		}

		return result;
	}


	@Override
	public int print(Graphics graphics, PageFormat pageFormat, int pageIndex)
			throws PrinterException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub

	}


	/**
	 * from FastTilePlot.java; converts 4 ints to a Point4i to a string for display
	 */
	public String createAreaString(int[] point) {
		// TODO rewrite; currently converts int[] to Point4i and then calls formatPointLatLon or formatPoint; will this work?
		Point4i p = new Point4i();
		p.w = point[0];
		p.z = point[1];
		p.x = point[2];
		p.y = point[3];
		if (showLatLon) 
			return formatPointLatLon(p);
		return formatPoint(p);
	}

	/**
	 * from FastTilePlot.java; converts a Point4i to a string for display
	 * @param point	coordinate as Point4i
	 * @return	string to display point formatted as a string
	 */
	public String formatPoint(Point4i point) {
		StringBuilder builder = new StringBuilder("(");
		int[] vals = new int[4];
		vals[0] = point.w;
		vals[1] = point.z;
		vals[2] = point.x;
		vals[3] = point.y;
		boolean addComma = false;
		for (int val : vals) {
			if (val != NO_VAL) {
				if (addComma) builder.append(", ");
				builder.append(val + 1);
				addComma = true;
			}
		}
		builder.append(")");
		return builder.toString();
	}

	/**
	 * from FastTilePlot.java; converts a Point4i to lat/lon for display
	 * @param point	coordinate as Point4i
	 * @return	string to display point as latitude/longitude coordinates
	 */
	private String formatPointLatLon(Point4i point) {
		Point2D llul = getLatLonForAxisPoint(new Point(point.x, point.y));
		StringBuilder builder = new StringBuilder("(");
		double[] vals = new double[4];
		vals[2] = llul.getY();
		vals[3] = llul.getX();
		vals[0] = point.w;
		vals[1] = point.z;
		boolean addComma = false;
		for (int i = 0; i < 4; i++) {
			double val = vals[i];
			if (val != NO_VAL) {
				if (addComma) builder.append(", ");

				if (i == 2) builder.append(Utilities.formatLat(val, 4));
				else if (i == 3) builder.append(Utilities.formatLon(val, 4));
				else builder.append(format.format(val + 1)); //NOTE: to make the notation of timesteps and layers 1-based
				addComma = true;
			}
		}
		builder.append(")");
		return builder.toString();
	}

	/**
	 * from FastTilePlot.java
	 * @param axisPoint
	 * @return
	 */
	protected Point2D getLatLonForAxisPoint(Point axisPoint) {
		//Since the NetCDF boxer used middle of the grid as origin of the grid
		//FastTilePlot use SW corner as the origin of the grid, hence the minus 1
		// TODO may need to change this: GeoTools uses top-left corner of each JPanel as (0,0)
		//		return getDataFrame().getAxes().getBoundingBoxer().axisPointToLatLonPoint(axisPoint.x-1, axisPoint.y-1); 
		return getDataFrame().getAxes().getBoundingBoxer().axisPointToLatLonPoint(axisPoint.x, axisPoint.y); //NOTE: the shift has been considered for in the netcdf boxer!!!
	}

	/**
	 * copied from FastTilePlot
	 * @param manager
	 * @param showLegend
	 */
	public void addObservationData(DataManager manager, boolean showLegend) {
		// TODO edit as needed
		showObsLegend = showLegend;
		obsAnnotations = new ArrayList<ObsAnnotation>();
		Axes<DataFrameAxis> axs = getDataFrame().getAxes();
		GregorianCalendar initDate = getDataFrame().getAxes().getDate(timestep);
		List<String> subtitles = new ArrayList<String>();
		String subtitle1 = "";
		boolean showST1 = false;

		if (config != null) {
			subtitle1 += (config.getSubtitle1() == null ? "" : config.getSubtitle1()).trim();
			showST1 = !subtitle1.isEmpty();
			if (showST1) subtitles.add(subtitle1);
		}

		try {
			for (OverlayObject obs : obsData) {
				ObsEvaluator eval = new ObsEvaluator(manager, obs.getVariable());
				ObsAnnotation ann = new ObsAnnotation(eval, axs, initDate, layer);
				ann.setDrawingParams(obs.getSymbol(), obs.getStrokeSize(), obs.getShapeSize(), map);
				obsAnnotations.add(ann);
				Dataset ds = eval.getVariable().getDataset();

				if (showST1 && ds != null) {
					StringBuffer sb = new StringBuffer(ds.getName());
					String alias = ds.getAlias();
					int index = sb.indexOf(alias) + alias.length();
					String temp = sb.replace(index, ++index, "=").toString();
					if (subtitle1.indexOf(temp) < 0 && !subtitles.contains(temp)) 
						subtitles.add(temp);
				}
			}

			setObsLegend(obsAnnotations, showLegend);
			config.putObject(PlotConfiguration.OBS_SHOW_LEGEND, showLegend);

			if (showST1) {
				Collections.sort(subtitles);
				subtitle1 = "";

				for (String str : subtitles)
					subtitle1 += str + "  ";

				config.setSubtitle1(subtitle1.trim());
			}

			draw();
		} catch (Exception e) {
			setOverlayErrorMsg(e.getMessage());
			Logger.error("Check if overlay time steps match the underlying data");
			Logger.error(e.getMessage());
			// TODO evaluate what drawing activity should take place
			//			drawMode = DRAW_NONE;	// no longer using the drawMode etc. for drawing the tile plot
		}
	}


	/**
	 * copied from FastTilePlot
	 * @return	observation data
	 */
	public List<OverlayObject> getObservationData() {
		return obsData;
	}

	/**
	 * Function to throw up a dialog box to tell the user that the overlay time steps may not match the time steps of the underlying data.
	 * Copied from FastTilePlot
	 * @param msg	The getMessage() exception message caught in the try/catch block
	 */
	private void setOverlayErrorMsg(String msg) {
		if (msg == null) 
			msg = "";
		JOptionPane.showMessageDialog(app.getGui().getFrame(), "Please check if the overlay time steps match the underlying data.\n" + msg, "Overlay Error", JOptionPane.ERROR_MESSAGE, null);
	}

	/**
	 * copied from FastTilePlot
	 * @param eval a VectorEvaluator object
	 */
	public void addVectorAnnotation(VectorEvaluator eval) {
		// TODO rewrite VectorAnnotation: different method required to draw vectors on a JMapPane instead of
		// on a Rectangle2D
		vectAnnotation = new VectorAnnotation(eval, timestep, getDataFrame().getAxes().getBoundingBoxer());
	}

	// GUI Callbacks:

	// Plot frame closed:

	public void stopThread() {	// called by anl.verdi.plot.gui.PlotPanel
//		// TODO figure out what stopThread needs to do because not using drawMode
	}

	/**
	 * Window hidden callback (does nothing)
	 */
	@Override
	public void componentHidden(ComponentEvent unused) { }

	/**
	 * Window shown callback (redraws graphics)
	 */
	@Override
	public void componentShown(ComponentEvent unused) {
		draw();
	}

	/**
	 * Window resized callback (redraws graphics)
	 */
	@Override
	public void componentResized(ComponentEvent unused) {
		draw();
	}

	/**
	 * Window moved callback (does nothing)
	 */
	@Override
	public void componentMoved(ComponentEvent unused) { }

	/**
	 * Mouse callbacks: showPopup
	 * Creates popup menu in response to a mouse event
	 * @param me	MouseEvent
	 */
	protected void showPopup( MouseEvent me ) {
		popup = createPopupMenu(true, true, true, zoom);

		int mod = me.getModifiers();
		int mask = MouseEvent.BUTTON3_MASK;

		if ((mod & mask) != 0) {
			popup.show(this, me.getPoint().x, me.getPoint().y);
		}
	}

	// mousePressed, mouseEntered, mouseExited, mouseReleased, and mouseClicked events are all ignored
	public void mousePressed( MouseEvent unused_ ) { }
	public void mouseEntered( MouseEvent unused_ ) { }
	public void mouseExited( MouseEvent unused_ ) { }
	public void mouseReleased( MouseEvent unused_ ) { }
	public void mouseClicked( MouseEvent unused_ ) { }

	/**
	 * viewClosed function to reset components to null
	 */
	public void viewClosed() { 
		// 
		mapper = null;
		dataFrameLog = null;
		dataFrame = null;

		obsData = null;
		obsAnnotations = null;
		vectAnnotation = null;
		eventProducer = null;

		bImage = null;

		dialog = null;
		controlLayer = null;
		config = null;

		//		doubleBufferedRendererThread = null;

		subsetLayerData = null;
		layerData = null;
		statisticsData = null;	

		format = null;
		//		tilePlot = null;
		legendLevels = null;
		defaultPalette = null;
		legendColors = null;
		map = null;
		gridBounds = null;
		domain = null;

		timeLayerPanel = null;
		probeItems = null;
		popup = null;
		//		dataArea = null;		// TODO change dataArea; was defined as a Rectangle
		popUpLocation = null;
		probedSlice = null;
		showGridLines = null;
		app = null;
		minMax = null;
	}

	// viewFloated and viewRestored events are ignored
	public void viewFloated(DockableFrameEvent unused_ ) { }
	public void viewRestored(DockableFrameEvent unused_ ) { }		

	/**
	 * Creates a popup menu for the panel. Copied from FastTilePlot
	 * 
	 * @param properties	include a menu item for the chart property editor.
	 * @param save	include a menu item for saving the chart.
	 * @param print	include a menu item for printing the chart.
	 * @param zoom	include menu items for zooming.
	 * @return The popup menu.
	 */
	protected JPopupMenu createPopupMenu(boolean properties, boolean save,
			boolean print, boolean zoomable) {

		JPopupMenu result = new JPopupMenu("GTTile:");
		boolean separator = false;

		if (properties) {
			JMenuItem propertiesItem = new JMenuItem("Properties...");
			propertiesItem.setActionCommand(PROPERTIES_COMMAND);
			propertiesItem.addActionListener(this);
			result.add(propertiesItem);
			separator = true;
		}

		if (save) {
			if (separator) {
				result.addSeparator();
				separator = false;
			}
			JMenuItem saveItem = new JMenuItem("Save Image As...");
			saveItem.setActionCommand(SAVE_COMMAND);
			saveItem.addActionListener(this);
			result.add(saveItem);
			separator = true;
		}

		if (print) {
			if (separator) {
				result.addSeparator();
				separator = false;
			}
			JMenuItem printItem = new JMenuItem("Print...");
			printItem.setActionCommand(PRINT_COMMAND);
			printItem.addActionListener(this);
			result.add(printItem);
			separator = true;
		}

		if (zoomable) {
			if (separator) {
				result.addSeparator();
				separator = false;
			}

			JMenuItem zoomInItem = new JMenuItem("Zoom_In");
			zoomInItem.setActionCommand(ZOOM_IN_BOTH_COMMAND);
			zoomInItem.addActionListener(this);
			result.add(zoomInItem);

			JMenuItem zoomOutItem = new JMenuItem("Zoom_Out");
			zoomOutItem.setActionCommand(ZOOM_OUT_BOTH_COMMAND);
			zoomOutItem.addActionListener(this);
			result.add(zoomOutItem);

			JMenuItem zoomOut2Pic = new JMenuItem("Max_Zoom_Out");
			zoomOut2Pic.setActionCommand(ZOOM_OUT_MAX_COMMAND);
			zoomOut2Pic.addActionListener(this);
			result.add(zoomOut2Pic);
		}
		return result;
	}
	
	/**
	 * From FastTilePlot: requestTimeSeries for a formula type
	 * @param type	value of Formula.Type
	 */
	private void requestTimeSeries(Formula.Type type) {
		// TODO probably need to rewrite this
		Slice slice = new Slice();
		// slice needs to be in terms of the actual array indices
		// of the frame, but the axes ranges refer to the range
		// of the original dataset. So, the origin will always
		// be 0 and the extent is the frame's extent.
		slice.setTimeRange(0, getDataFrame().getAxes().getTimeAxis().getExtent());
		DataFrameAxis frameAxis = getDataFrame().getAxes().getZAxis();
		if (frameAxis != null) slice.setLayerRange(0, frameAxis.getExtent());
		slice.setXRange(probedSlice.getXRange());
		slice.setYRange(probedSlice.getYRange());

		try {
			DataFrame subsection = getDataFrame().slice(slice);
			eventProducer.firePlotRequest(new TimeSeriesPlotRequest(subsection, slice, type));
		} catch (InvalidRangeException e1) {
			Logger.error("InvalidRangeException in GTTilePlot.requestTimeSeries: " + e1.getMessage());
		}
	}

	/**
	 * From FastTilePlot: requestTimeSeries for a set of points
	 * @param points	Set<Point>
	 * @param title	Title to display on the plot
	 */
	private void requestTimeSeries(Set<Point> points, String title) {
		// TODO probably need to rewrite this
		MultiTimeSeriesPlotRequest request = new MultiTimeSeriesPlotRequest(title);
		for (Point point : points) {
			Slice slice = new Slice();
			// slice needs to be in terms of the actual array indices
			// of the frame, but the axes ranges refer to the range
			// of the original dataset. So, the origin will always
			// be 0 and the extent is the frame's extent.
			slice.setTimeRange(0, getDataFrame().getAxes().getTimeAxis().getExtent());
			DataFrameAxis frameAxis = getDataFrame().getAxes().getZAxis();
			if (frameAxis != null) slice.setLayerRange(0, frameAxis.getExtent());
			slice.setXRange(point.x, 1);
			slice.setYRange(point.y, 1);
			try {
				DataFrame subsection = getDataFrame().slice(slice);
				request.addItem(subsection);
			} catch (InvalidRangeException e1) {
				Logger.error("InvalidRangeException in GTTilePlot.requestTimeSeries: " + e1.getMessage());
			}
		}
		eventProducer.firePlotRequest(request);
	}


	/**
	 * Gets the MinMax points for this plot.
	 *
	 * @return the MinMax points for this plot.
	 */
	protected DataUtilities.MinMaxPoint getMinMaxPoints() {
		try {
			if (hasNoLayer) return DataUtilities.minMaxPoint(getDataFrame(), timestep - firstTimestep);
			return DataUtilities.minMaxTLPoint(getDataFrame(), timestep - firstTimestep, layer - firstLayer);
		} catch (InvalidRangeException e) {
			Logger.error("Invalid Range Exception in GTTilePlot getMinMaxPoints: " + e.getMessage());
		}
		return null;
	}

	@Override
	public BufferedImage getBufferedImage() {
		// not used by GTTilePlot
		return null;
	}

	@Override
	public BufferedImage getBufferedImage(int width, int height) {
		// not used by GTTilePlot
		return null;
	}

	/**
	 * drawLegend member function originally from TilePlot, called by TilePlot.draw
	 * Read and construct some original values and pass them to GTTilePlotPanel.setLegendPanel.
	 * Remainder of TilePlot.drawLegend functionality in GTTilePlotPanel, legendPanel, paintComponent
	 * @param xMaximum
	 * @param yMinimum
	 * @param yMaximum
	 * @param legendLevels	array of double containing the break points for each change in color of legend and tiles
	 * @param legendColors	array of Color containing the color for each tile and associated position in legend
	 * @param units	string representing the unit of measure for displayed values in this tile plot
	 */
	protected void drawLegend(int xMaximum, int yMinimum, int yMaximum, final double[] legendLevels,
			final Color[] legendColors, final String units)
	{
		final int colors = legendColors.length;
		super.setLog(log); 	// set log value for GTTilePlotPanel
		String unitStr = config.getProperty(PlotConfiguration.UNITS);	// get unit of measure from config
		unitStr = (unitStr == null || unitStr.isEmpty() ? units : unitStr);	// must use units if grid cell statistics
		config.putObject(PlotConfiguration.UNITS, unitStr);		// save updated unit of measure to config
		String logStr = " (Log";
		String baseStr = null;		// base of log string
		Boolean uShowTick = (Boolean) config.getObject(PlotConfiguration.UNITS_SHOW_TICK);
		uShowTick = (uShowTick == null ? true : uShowTick); 
		config.putObject(PlotConfiguration.UNITS_SHOW_TICK, uShowTick); 	// save updated show tick for units to config
		Color uTickColor = (Color) config.getObject(PlotConfiguration.UNITS_TICK_COLOR);	// get color for tick marks
		uTickColor = (uTickColor == null ? Color.black : uTickColor);	// if no color designated, default to black
		config.putObject(PlotConfiguration.UNITS_TICK_COLOR, uTickColor); 	// save updated color for tick marks
		Integer labelCnt = (Integer) config.getObject(TilePlotConfiguration.UNITS_TICK_NUMBER);	// number of tick marks
			// NOTE: using Integer instead of int because (1) object stored in a container, (2) null value must be allowed
		config.putObject(PlotConfiguration.UNITS_TICK_NUMBER, labelCnt == null ? colors + 1 : labelCnt);
		Font labelFont = config.getFont(TilePlotConfiguration.UNITS_TICK_FONT); 	// get Font
		Font gFont = super.getLegendPanel().getGraphics().getFont();	// get the font for the legendPanel
		config.putObject(PlotConfiguration.UNITS_TICK_FONT, labelFont == null ? gFont : labelFont);	// save the labelFont or, if null, the legendPanel's default font
		Font unitsFont = config.getFont(TilePlotConfiguration.UNITS_FONT);	// get the font for the units
		config.putObject(PlotConfiguration.UNITS_FONT, unitsFont == null ? gFont : unitsFont);
		Color unitsClr = (Color)config.getObject(TilePlotConfiguration.UNITS_COLOR);	// get the color for the units
		final Color currentColor = super.getLegendPanel().getGraphics().getColor();		// get the color from the legendPanel
		config.putObject(PlotConfiguration.UNITS_COLOR, unitsClr == null ? currentColor : unitsClr);	// save updated color for units

		if(log)
		{
			if(logBase == Math.E)
			{
				baseStr = "E";
			}
			else
			{
				baseStr = logBase + "";
			}
		}
		super.setLegendPanel(uShowTick, labelCnt, xMaximum, yMinimum, yMaximum, uTickColor, unitsClr,
				labelFont, unitsFont, baseStr, logStr, unitStr, legendLevels, legendColors);
	}
	
}
