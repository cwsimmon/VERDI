/**
 * FastTilePlot - Fast alternative to the original VERDI TilePlot.
 * @author plessel.todd@epa.gov cathey.tommy@epa.gov 2008-09-16
 * @version $Revision$ $Date$
 **/

package anl.verdi.plot.gui;

import gov.epa.emvl.ASCIIGridWriter;
import gov.epa.emvl.AxisLabelCreator;
import gov.epa.emvl.GridCellStatistics;
//import gov.epa.emvl.GridShapefileWriter;		// 2014 disable write shapefile VERDI 1.5.0
import gov.epa.emvl.MapLines;
import gov.epa.emvl.Mapper;
import gov.epa.emvl.GridCellStatistics;
import gov.epa.emvl.MeshCellStatistics;
import gov.epa.emvl.Numerics;
import gov.epa.emvl.Projector;
import gov.epa.emvl.TilePlot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
//import java.util.Date;		// functions deprecated, replaced by GregorianCalendar
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
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
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;
import javax.vecmath.Point4i;

import net.sf.epsgraphics.ColorMode;
import net.sf.epsgraphics.Drawable;
import net.sf.epsgraphics.EpsTools;

import org.apache.logging.log4j.LogManager;		// 2014
import org.apache.logging.log4j.Logger;			// 2014 replacing System.out.println with logger messages
import org.geotools.data.Query;
//import org.geotools.data.DefaultQuery;	// deprecated, replacing with Query
import org.geotools.data.shapefile.ShapefileDataStore;
//import org.geotools.data.shapefile.indexed.IndexedShapefileDataStoreFactory;	// deprecated
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
//import org.geotools.map.DefaultMapLayer;	// deprecated, replacing with FeatureLayer
import org.geotools.map.FeatureLayer;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;

import saf.core.ui.event.DockableFrameEvent;
import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.projection.LatLonProjection;
import anl.map.coordinates.Decidegrees;
import anl.verdi.core.VerdiApplication;
import anl.verdi.core.VerdiGUI;
import anl.verdi.data.Axes;
import anl.verdi.data.CoordAxis;
import anl.verdi.data.DataFrame;
import anl.verdi.data.DataFrameAxis;
import anl.verdi.data.DataFrameIndex;
import anl.verdi.data.DataManager;
import anl.verdi.data.DataReader;
import anl.verdi.data.DataUtilities;
import anl.verdi.data.MPASAxisLabelCreator;
import anl.verdi.data.DataUtilities.MinMax;
import anl.verdi.data.Dataset;
import anl.verdi.data.MPASDataFrameIndex;
import anl.verdi.data.ObsEvaluator;
import anl.verdi.data.Slice;
import anl.verdi.data.Variable;
import anl.verdi.data.VectorEvaluator;
import anl.verdi.formula.Formula;
import anl.verdi.gis.FastTileLayerEditor;
import anl.verdi.gis.OverlayObject;
import anl.verdi.plot.anim.AnimationPanel;
import anl.verdi.plot.color.ColorMap;
import anl.verdi.plot.color.Palette;
import anl.verdi.plot.color.PavePaletteCreator;
import anl.verdi.plot.config.LoadConfiguration;
import anl.verdi.plot.config.PlotConfiguration;
import anl.verdi.plot.config.PlotConfigurationIO;
import anl.verdi.plot.config.SaveConfiguration;
import anl.verdi.plot.config.TilePlotConfiguration;
import anl.verdi.plot.probe.PlotEventProducer;
import anl.verdi.plot.probe.ProbeEvent;
import anl.verdi.plot.types.TimeAnimatablePlot;
import anl.verdi.plot.util.PlotExporter;
import anl.verdi.plot.util.PlotExporterAction;
import anl.verdi.util.Tools;
import anl.verdi.util.Utilities;

import com.vividsolutions.jts.geom.Envelope;

public class MeshPlot extends JPanel implements ActionListener, Printable,
		ChangeListener, ComponentListener, MouseListener,
		TimeAnimatablePlot, Plot {
	
	static final Logger Logger = LogManager.getLogger(MeshPlot.class.getName());
	private static final long serialVersionUID = 5835232088528761729L;

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
	
	public static final double RAD_TO_DEG = 180 / Math.PI;
	
	// Log related
	
	protected boolean log = false;
	@SuppressWarnings("unused")
	private boolean preLog = false;
	private double logBase = 10.0; //Math.E;	

	// 2D grid parameters:

	protected final int startDate; // 2005241 (YYYYDDD).
	protected final int startTime; // 0 (HHMMSS).
	protected final int timestepSize; // 10000 (HHMMSS)
	protected final int timesteps; // 24.
	protected final int layers; // 14.
	protected final int rows; // 259.
	protected final int cells;
	protected final int columns; // 268.
	protected final CoordAxis columnAxis;
	protected final CoordAxis rowAxis;
	protected final DataFrameAxis timeAxis;
	protected final DataFrameAxis layerAxis;
	protected final AxisLabelCreator rowLabels;
	protected final AxisLabelCreator columnLabels;
	protected final int rowOrigin;
	protected final int columnOrigin;
	private final double westEdge; // -420000.0 meters from projection center
	private final double southEdge; // -1716000.0 meters from projection center
	private final double cellWidth; // 12000.0 meters.
	private final double cellHeight; // 12000.0 meters.
	private NumberFormat format;
	private NumberFormat coordFormat;
	private final boolean invertRows; // HACK: Invert rows of AURAMS / GEM / CF Convention data?

	/** Cell id are stored as colors in an image to facilitate a quick mapping of
	 *  pixels to cell id.  Retrieved values must be adjusted by COLOR_BASE to get cell id
	 **/
	private static final int COLOR_BASE = 16777216;

	//Screen width / height in pixels
	private int screenWidth;
	private int screenHeight;
	private int xOffset = 0;
	private int yOffset = 0;
	// For legend-colored grid cells and annotations:

	protected TilePlot tilePlot; // EMVL TilePlot.

	protected int timestep = 0; // 0..timesteps - 1.
	private int previousTimestep = 0;
	protected int firstTimestep = 0;
	protected int lastTimestep = 0;
	protected int layer = 0; // 0..layers - 1.
	private int previousLayer = 0;
	protected int firstLayer = 0;
	protected int lastLayer = 0;
	private int firstRow = 0; // 0..lastRow.
	private int lastRow = 0; // firstRow..rows - 1.
	private int firstColumn = 0; // 0..lastColumn.
	private int lastColumn = 0; // firstColumn..columns - 1.

	protected double[] legendLevels;

	protected Palette defaultPalette;
	private Color[] legendColors;
	//private ColorMap map;
	protected ColorMap map;

	private Color axisColor = Color.darkGray;
	private Color labelColor = Color.black;

	// subsetLayerData[ 1 + lastRow - firstRow ][ 1 + lastColumn - firstColumn ]
	// at current timestep and layer.
	private float[] subsetLayerData = null;

	// layerData[ rows ][ columns ][ timesteps ]
	private float[][] layerData = null;
	//private float[][][] layerDataLog = null;
	private float[][][] statisticsData = null;
	//private float[][][] statisticsDataLog = null;
	
	private BufferedImage cellIdMap = null;

	// For clipped/projected/clipped map lines:

	private static final String mapFileDirectory = System.getProperty("user.dir")
			+ "/data"; // Contains map_*.bin files

	private //final 
	Mapper mapper = new Mapper(mapFileDirectory);

	protected final Projector projector = null;

	protected double[][] gridBounds = { { 0.0, 0.0 }, { 0.0, 0.0 } };
	protected double[][] domain = { { 0.0, 0.0 }, { 0.0, 0.0 } };

	private final String variable; // "PM25".
	protected String units; // "ug/m3".
	private String unitString; // "ug/m3".

	private boolean withHucs = false; // Draw watersheds on map?
	private boolean withRivers = false; // Draw rivers on map?
	private boolean withRoads = false; // Draw roads on map?

	protected //final 
	DataFrame dataFrame;
	protected DataFrame dataFrameLog;
	protected List<OverlayObject> obsData = new ArrayList<OverlayObject>();
	protected List<ObsAnnotation> obsAnnotations;
	protected VectorAnnotation vectAnnotation;

	// It remains a mystery why eventProducer is needed.
	private PlotEventProducer eventProducer = new PlotEventProducer();

	// GUI attributes:
	private final JButton playStopButton;
	private final JButton leftStepButton;
	private final JButton rightStepButton;
	private final String LEFT = "<";
	private final String RIGHT = ">";
	private final String PLAY = "|>";
	private final String STOP = "||";
	private final String LEFT_TIP = "Move One Timestep to the Left";
	private final String RIGHT_TIP = "Move One Timestep to the Right";
	private final String PLAY_TIP = "Play/Stop";
	private TimeLayerPanel timeLayerPanel;
	private final JToolBar toolBar = new JToolBar();
	private final JComboBox statisticsMenu;
	private int preStatIndex = -1;
	private final JTextField threshold;
	private boolean recomputeStatistics = false;
	private boolean recomputeLegend = false;

	private final int DRAW_NONE = 0;
	private final int DRAW_ONCE = 1;
	private final int DRAW_CONTINUOUS = 2;
	private final int DRAW_END = 3;
	private int drawMode = DRAW_ONCE;
	private int draw_once_requests = -1;
	private final String DELAY_LABEL = "Slow:";
	private final JTextField delayField;
	private final JTextField firstRowField;
	private final JTextField lastRowField;
	private final JTextField firstColumnField;
	private final JTextField lastColumnField;
	protected final Rubberband rubberband = new Rubberband(this);
	protected boolean zoom = true;
	protected boolean probe = false;
	private boolean hasNoLayer = false;
	private int delay = 50; // In milliseconds.
	private final int MAXIMUM_DELAY = 3000; // 3 seconds per frame.

	protected boolean showObsLegend = false;

	private final JPanel threadParent = this;
	private BufferedImage bImage;
	private static final Object lock = new Object();
	protected java.util.List<JMenuItem> probeItems = new ArrayList<JMenuItem>();
	private JPopupMenu popup;
	protected Rectangle dataArea = new Rectangle();
	private boolean inDataArea = false;
	private Point popUpLocation = new Point(1,1);
	protected Slice probedSlice;
	protected JCheckBoxMenuItem showGridLines;
	final JMenu mapLayersMenu = new JMenu("Add Map Layers");
	
	private double xClickLocation = 0;
	private double yClickLocation = 0;
	private int currentClickedCell = 0;
	private int previousClickedCell = 0;
	private static final boolean SHOW_ZOOM_LOCATION = true;
	
	private ConfigDialog dialog = null;
	@SuppressWarnings("unused")
	private Plot.ConfigSoure configSource = Plot.ConfigSoure.GUI;

	VerdiApplication app;
	
	// Create a Thread that contains the above Runnable:

	private Thread doubleBufferedRendererThread = null;
	protected MinMax minMax;
	protected PlotConfiguration config;
	private boolean processTimeChange = true;
//	private MapLayer controlLayer;
	private FeatureLayer controlLayer;
	

	
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

	// Declare a Runnable attribute which will create and run a thread
	// whose run method draws double-buffered to a graphics iff graphics
	// is not null:

	public final Runnable doubleBufferedRenderer = new Runnable() {

		private final RepaintManager repaintManager =
			RepaintManager.currentManager(threadParent);

		public void run() {

			do {

				
				if ( drawMode != DRAW_NONE &&
					 ! VerdiGUI.isHidden( (Plot) threadParent ) ) {
					
					if (drawMode == DRAW_ONCE) {
//						synchronized (lock) {
							if (get_draw_once_requests() > 0) {
								draw_once_requests = 0;
							}
							if ( get_draw_once_requests() >=0 ) {
								showBusyCursor();
							}
					}
					
					// When animating, pause based on user-set delay rate:

					if (drawMode == DRAW_CONTINUOUS && delay != 0) {
						try {
							Thread.sleep(delay);
						} catch (Exception unused) {}
					}

					final int canvasWidth = getWidth();
					final int canvasHeight = getHeight();
					float marginScale = 0.95f; // Controls whitespace margin around plot window.
					String sTitle1 = config.getSubtitle1();
					String sTitle2 = config.getSubtitle2();
					Font tFont = config.getFont(PlotConfiguration.TITLE_FONT);
					Font sFont1 = config.getFont(PlotConfiguration.SUBTITLE_1_FONT);
					Font sFont2 = config.getFont(PlotConfiguration.SUBTITLE_2_FONT);
					int fontSize = (tFont == null) ? 20 : tFont.getSize();
					yOffset = 20 + fontSize;
					marginScale *= 20f / (fontSize - 20 < 0 ? 20 : 20 + fontSize / 10f);

					if (sTitle1 != null && !sTitle1.trim().isEmpty()) {
						fontSize = (sFont1 == null) ? 20 : sFont1.getSize();
						yOffset += fontSize + 6;
						marginScale *= 20f / (fontSize - 20 < 0 ? 20
								: 20 + fontSize / 10f);
					}

					if (sTitle2 != null && !sTitle2.trim().isEmpty()) {
						fontSize = (sFont2 == null) ? 20 : sFont2.getSize();

						if (sTitle1 == null || sTitle1.trim().isEmpty()) {
							yOffset += 26;
							marginScale *= 20f / 22f;
						}

						yOffset += fontSize + 6;
						marginScale *= 20f / (fontSize - 20 < 0 ? 20
								: 20 + fontSize / 10f);
					}

					xOffset = 100;
					final int legendWidth = 80 + xOffset;
					final int canvasSize =
						Math.round( Math.min( Math.max( 0, canvasWidth - legendWidth ), canvasHeight ) * marginScale );
					final int subsetRows = 1 + lastRow - firstRow;
					final int subsetColumns = 1 + lastColumn - firstColumn;
					final float subsetMax = Math.max(subsetRows, subsetColumns);
					final float rowScale = subsetRows / subsetMax;
					final float columnScale = subsetColumns / subsetMax;
					final int width = Math.round(canvasSize * columnScale);
					final int height = Math.round(canvasSize * rowScale);
					
					if (previousCanvasSize != canvasSize || zoomFactor != previousZoomFactor || previousClippedDataRatio != clippedDataRatio
							|| timestep != previousTimestep || layer != previousLayer || panX != previousPanX || panY != previousPanY) {
						transformCells(/*gr,*/ canvasSize, xOffset, yOffset);						
						previousCanvasSize = canvasSize;
						previousZoomFactor = zoomFactor;
						previousClippedDataRatio = clippedDataRatio;
						previousTimestep = timestep;
						previousLayer = layer;
						previousPanX = panX;
						previousPanY = panY;
					}
					

					if (canvasSize == 0) {
						if ( get_draw_once_requests() < 0) 
							restoreCursor();
						continue;
					}

					// Use off-screen graphics for double-buffering:
					// don't start processing until graphics system is ready!

					final Image offScreenImage =
						repaintManager.getOffscreenBuffer(threadParent, canvasWidth, canvasHeight);

					// offScreenImage = (Image) (offScreenImage.clone());

					if (offScreenImage == null) {
						if ( get_draw_once_requests() < 0) 
							restoreCursor();
						continue;// graphics system is not ready
					}

					final Graphics offScreenGraphics = offScreenImage.getGraphics();

					if (offScreenGraphics == null) {
						if ( get_draw_once_requests() < 0) 
							restoreCursor();
						continue;// graphics system is not ready
					}

					final Graphics graphics = threadParent.getGraphics();

					if (graphics == null) {
						if ( get_draw_once_requests() < 0) 
							restoreCursor();
						continue;// graphics system is not ready
					}

					// graphics system should now be ready
					assert offScreenImage != null;
					assert offScreenGraphics != null;
					assert graphics != null;

					if (drawMode == DRAW_CONTINUOUS) {
						previousTimestep = timestep;
						timestep = nextValue(1, timestep, firstTimestep, lastTimestep);
						timeLayerPanel.setTime(timestep);
						drawOverLays();
					}
					
//					synchronized (lock) {
						if (get_draw_once_requests() > 0) {
							draw_once_requests = 0;
							if ( get_draw_once_requests() < 0) 
								restoreCursor();
							continue;
						}
//					}

					copySubsetLayerData(log); // Based on current timestep and layer.

					
					synchronized (lock) {
						
						// Erase canvas:

						offScreenGraphics.setColor(Color.white);
						offScreenGraphics.fillRect(0, 0, canvasWidth,
								canvasHeight);
						boolean finishedRework = false;
						finishedRework = true;
												
						if (finishedRework) {

						// Draw legend-colored grid cells, axis, text labels and
						// legend:

						final Boolean showGridLines = (Boolean)
							config.getObject( TilePlotConfiguration.SHOW_GRID_LINES );
						final Color gridLineColor = (Color)
							( ( showGridLines == null || showGridLines == false ) ? null
								: config.getObject( TilePlotConfiguration.GRID_LINE_COLOR ) );

						final int stepsLapsed = timestep - firstTimestep;
						final int statisticsSelection = statisticsMenu.getSelectedIndex();
						Logger.debug("statisticsSelection = " + statisticsSelection);
						final String statisticsUnits =
							statisticsSelection == 0 ? null : GridCellStatistics.units( statisticsSelection - 1 );
						Logger.debug("statisticsUnits = " + statisticsUnits);
						final String plotVariable =
							statisticsSelection == 0 ? variable
							: variable + GridCellStatistics.shortName( statisticsSelection - 1 );
						final String plotUnits =
							statisticsUnits != null ? statisticsUnits : units;
						Logger.debug("units = " + units);
						
						if (get_draw_once_requests() > 0) {
							draw_once_requests = 0;
							//System.err.println("Resetting 3 draw once requests");
							if ( get_draw_once_requests() < 0) 
								restoreCursor();
							continue;
						}
						
						try {
							Logger.debug("ready to call tilePlot.draw(); first resolve any function calls for argument values, thread = " + Thread.currentThread().toString());
							int aRow = firstRow + rowOrigin;
							Logger.debug("aRow = " + aRow);
							int bRow = lastRow + rowOrigin;
							Logger.debug("bRow = " + bRow);
							int aCol = firstColumn + columnOrigin;
							Logger.debug("aCol = " + aCol);
							int bCol = lastColumn + columnOrigin;
							Logger.debug("bCol = " + bCol);
							int aLegendLevelsLength = legendLevels.length;
							Logger.debug("LegendLevels.length = " + aLegendLevelsLength);
							int aLegendColorsLength = legendColors.length;
							Logger.debug("LegendColors.length = " + aLegendColorsLength);
							Logger.debug("plotUnits = " + plotUnits);
							String aPlotUnits = (plotUnits==null || plotUnits.trim().equals(""))?"none":plotUnits;
							Logger.debug("aPlotUnits = " + aPlotUnits);
							NumberFormat aNumberFormat = map.getNumberFormat();
							Logger.debug("aNumberFormat = " + aNumberFormat);
							int aSubsetLayerDataLength = 0;
							if (subsetLayerData != null)
								aSubsetLayerDataLength = subsetLayerData.length;
							Logger.debug("subsetLayerData.length = " + aSubsetLayerDataLength);
							Logger.debug("ready to make revised function call to tilePlot.draw, thread = " + Thread.currentThread().toString());

							//debug
							
							tilePlot.draw(offScreenGraphics, xOffset, yOffset,
									screenWidth, screenHeight, stepsLapsed, MeshPlot.this.layer, aRow,
									bRow, aCol, bCol, legendLevels,
									legendColors, axisColor, labelColor, plotVariable,
									aPlotUnits, 
									config, aNumberFormat, gridLineColor,
									null, rowLabels, columnLabels);
//							tilePlot.draw(offScreenGraphics, xOffset, yOffset,
//									width, height, stepsLapsed, layer, firstRow + rowOrigin,
//									lastRow + rowOrigin, firstColumn + columnOrigin, lastColumn + columnOrigin, legendLevels,
//									legendColors, axisColor, labelColor, plotVariable,
//									((plotUnits==null || plotUnits.trim().equals(""))?"none":plotUnits), config, map.getNumberFormat(), gridLineColor,
//									subsetLayerData);
						} catch (Exception e) {
							Logger.error("FastTilePlot's run method " + e.getMessage());
							//TODO - remove this
							e.printStackTrace();
						}
						} //TAH finish rework if

						renderCells(offScreenGraphics, xOffset, yOffset);

						dataArea.setRect(xOffset, yOffset, screenWidth, screenHeight);

						// Draw projected/clipped map border lines over grid
						// cells:

						if (get_draw_once_requests() > 0) {
							draw_once_requests = 0;
							//System.err.println("Resetting 4 draw once requests " + getWidth() + "x" + getHeight());
							if ( get_draw_once_requests() < 0)
								restoreCursor();
							continue;
						}
						
						mapper.draw(domain, gridBounds, projector,
								offScreenGraphics, xOffset, yOffset, width,
								height, withHucs, withRivers, withRoads);

						if (obsAnnotations != null) {
							for (ObsAnnotation ann : obsAnnotations)
								ann.draw(offScreenGraphics, xOffset, yOffset, width, height, 
										legendLevels, legendColors, projector, domain, gridBounds);
						}
						
						if (vectAnnotation != null) {
							vectAnnotation.draw(offScreenGraphics, xOffset, yOffset, width, height, 
									firstRow, lastRow, firstColumn, lastColumn);
						}

						resetMenuItems(mapLayersMenu);
						Toolkit.getDefaultToolkit().sync();

						try {
							bImage = toBufferedImage(offScreenImage, BufferedImage.TYPE_INT_RGB, canvasWidth, canvasHeight);
							graphics.drawImage(offScreenImage, 0, 0,threadParent);
						} finally {
							graphics.dispose();
							offScreenGraphics.dispose();
						}

						Toolkit.getDefaultToolkit().sync();
					
					} // End of synchronized block.
					if (drawMode == DRAW_ONCE ) {
						decrease_draw_once_requests();
						draw_once_requests = -1;
						if (get_draw_once_requests() < 0) {
							drawMode = DRAW_NONE;
							restoreCursor();
						}
					} else {
						//drawMode = DRAW_NONE;
					}
					
				} else {
					try {
						Thread.sleep(100); /* ms. */
					} catch (Exception unused) {}
				}
			} while (drawMode != DRAW_END);
		}
	};
	
	private BufferedImage toBufferedImage(Image image, int type, int width, int height) {
        BufferedImage result = new BufferedImage(width, height, type);
        Graphics2D g = result.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return result;
    }

	public void drawBatchImage(int wdth, int hght) {
			final int canvasWidth = wdth;
			final int canvasHeight = hght;
			// around plot window.

			String sTitle1 = config.getSubtitle1();
			String sTitle2 = config.getSubtitle2();
			Font tFont = config.getFont(PlotConfiguration.TITLE_FONT);
			Font sFont1 = config.getFont(PlotConfiguration.SUBTITLE_1_FONT);
			Font sFont2 = config.getFont(PlotConfiguration.SUBTITLE_2_FONT);
			int fontSize = (tFont == null) ? 20 : tFont.getSize();
			int yOffset = 20 + fontSize;

			if (sTitle1 != null && !sTitle1.trim().isEmpty()) {
				fontSize = (sFont1 == null) ? 20 : sFont1.getSize();
				yOffset += fontSize + 6;
			}

			if (sTitle2 != null && !sTitle2.trim().isEmpty()) {
				fontSize = (sFont2 == null) ? 20 : sFont2.getSize();

				if (sTitle1 == null || sTitle1.trim().isEmpty()) {
					yOffset += 26;
				}

				yOffset += fontSize + 6;
			}

			final int xOffset = 100;

			final Image offScreenImage = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_RGB);
			final Graphics offScreenGraphics = offScreenImage.getGraphics();

			// graphics system should now be ready
			assert offScreenImage != null;
			assert offScreenGraphics != null;

			copySubsetLayerData(this.log); // Based on current timestep and layer.

				offScreenGraphics.setColor(Color.white);
				offScreenGraphics.fillRect(0, 0, canvasWidth, canvasHeight);

				// Draw legend-colored grid cells, axis, text labels and legend:

				final Boolean showGridLines = (Boolean)
					config.getObject( TilePlotConfiguration.SHOW_GRID_LINES );
				final Color gridLineColor = (Color)
					( ( showGridLines == null || showGridLines == false ) ? null
						: config.getObject( TilePlotConfiguration.GRID_LINE_COLOR ) );
				
				final int statisticsSelection = statisticsMenu.getSelectedIndex();
				final String statisticsUnits =
					statisticsSelection == 0 ? null : GridCellStatistics.units( statisticsSelection - 1 );
				Logger.debug("statisticsUnit = " + statisticsUnits);
				final String plotVariable =
					statisticsSelection == 0 ? variable
					: variable + GridCellStatistics.shortName( statisticsSelection - 1 );
				Logger.debug("plotVariable = " + plotVariable);
				final String plotUnits = statisticsUnits != null ? statisticsUnits : units;
				Logger.debug("plotUnits = " + plotUnits);
				final int stepsLapsed = timestep - firstTimestep;
				try {
					tilePlot.drawBatchImage(offScreenGraphics, xOffset, yOffset,
							canvasWidth, canvasHeight, stepsLapsed, layer, firstRow,
							lastRow, firstColumn, lastColumn, legendLevels,
							legendColors, axisColor, labelColor, plotVariable,
							((plotUnits==null || plotUnits.trim().equals(""))?"none":plotUnits), config, map.getNumberFormat(), gridLineColor,
							null);
				} catch (Exception e) {
					Logger.error("FastTilePlot's drawBatch method" + e.getMessage());
					e.printStackTrace();
				}
				
				dataArea.setRect(xOffset, yOffset, tilePlot.getPlotWidth(), tilePlot.getPlotHeight());

				// Draw projected/clipped map border lines over grid cells:

				mapper.draw(domain, gridBounds, projector,
						offScreenGraphics, xOffset, yOffset, tilePlot.getPlotWidth(),
						tilePlot.getPlotHeight(), withHucs, withRivers, withRoads);

				try {
					bImage = (BufferedImage) offScreenImage;
				} finally {
					offScreenGraphics.dispose();
				}
	}
	


	// Methods --------------------------------------------------------

	// GUI Callbacks:

	// Plot frame closed:

	public void stopThread() {
		drawMode = DRAW_END;
		draw();
	}

	// Window hidden callback:

	public void componentHidden(ComponentEvent unused) { }

	// Window shown callback:

	public void componentShown(ComponentEvent unused) {
		draw();
	}

	// Window resized callback:

	public void componentResized(ComponentEvent unused) {
		draw();
	}

	// Window moved callback:

	public void componentMoved(ComponentEvent unused) { }

	// Mouse callbacks:

	protected void showPopup( MouseEvent me ) {
		popup = createPopupMenu(true, true, true, zoom);

		int mod = me.getModifiers();
		int mask = MouseEvent.BUTTON3_MASK;

		if ((mod & mask) != 0) {
			popup.show(this, me.getPoint().x, me.getPoint().y);
		}
	}

	public void mousePressed( MouseEvent unused_ ) { }
	public void mouseEntered( MouseEvent unused_ ) { }
	public void mouseExited( MouseEvent unused_ ) { }
	public void mouseReleased( MouseEvent unused_ ) { }
	public void mouseClicked( MouseEvent unused_ ) { }

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
		
		doubleBufferedRendererThread = null;
		
		subsetLayerData = null;
		layerData = null;
		statisticsData = null;	
		
		format = null;
		tilePlot = null;
		legendLevels = null;
		defaultPalette = null;
		legendColors = null;
		map = null;
		gridBounds = null;
		domain = null;

		timeLayerPanel = null;
		probeItems = null;
		popup = null;
		dataArea = null;
		popUpLocation = null;
		probedSlice = null;
		showGridLines = null;
		dialog = null;
		app = null;
		doubleBufferedRendererThread = null;
		minMax = null;
		config = null;
		controlLayer = null;
		
	}
	public void viewFloated(DockableFrameEvent unused_ ) { }
	public void viewRestored(DockableFrameEvent unused_ ) { }		

	// Paint/draw:

	public void paintComponent(final Graphics graphics) {
		super.paintComponent(graphics);
		draw();
	}

	public void draw() {

		if (drawMode == DRAW_NONE) {
			drawMode = DRAW_ONCE;
		}
		
		if (drawMode == DRAW_ONCE) {
			increase_draw_once_requests();
		}
	}

	// Buttons and fields:

	public void actionPerformed(ActionEvent event) {
		final Object source = event.getSource();

		if ( source == statisticsMenu || source == threshold ) {
			
			if ( statisticsMenu.getSelectedIndex() != this.preStatIndex) {
				if( statisticsMenu.getSelectedIndex() != 0) {
					recomputeStatistics = true;
				} else {
					//reset to no statistics...
					copySubsetLayerData(this.log);
				}
				this.preStatIndex = statisticsMenu.getSelectedIndex();
			}
			
			recomputeLegend = true;
			
			//populate legend colors and ranges on initiation
			//default to not a log scale
			double[] minmax = { 0.0, 0.0 };
			//calculate the non log min/max values, keep the code here
			//first part of IF ELSE will use the min/max values
			computeDataRange(minmax, false);
//			ColorMap.ScaleType sType = map.getScaleType();		// local variable sType not used
			//computeDataRange function need this.log set correctly...
			if (map.getPalette() == null)
			{
				Logger.debug("no palette so calling new PavePaletteCreator().createPalettes(8).get(0)");
			}
			defaultPalette = (map.getPalette() != null) ? map.getPalette() : new PavePaletteCreator().createPalettes(8).get(0);
			map.setPalette(defaultPalette);
			
			//set min/max for both log and non log values...
			map.setMinMax( minmax[0], minmax[1]);
			computeDataRange(minmax, true);
			map.setLogMinMax( minmax[0], minmax[1]);
			//this final one is for the below legend value calculations
			computeDataRange(minmax, this.log);

			legendColors = defaultPalette.getColors();
			final double minimum = minmax[0];
			final double maximum = minmax[1];
			minMax = new DataUtilities.MinMax(minimum, maximum);
			int count = legendColors.length + 1;
			final double delta = (minmax[1] - minmax[0]) / (count - 1);
			legendLevels = new double[count];
			for (int level = 0; level < count; ++level) {
				legendLevels[level] = minmax[0] + level * delta;
			}
			config.setUnits("");
	    } else if (source == playStopButton) {

			if (playStopButton.getText().equals(PLAY)) {
				processTimeChange = false;
				playStopButton.setText(STOP);
				leftStepButton.setEnabled(false);
				rightStepButton.setEnabled(false);
				drawMode = DRAW_CONTINUOUS;
				int holdDelay = delay;
						try {
							delay = Integer.parseInt(delayField.getText().toString());
						} catch (Exception unused) { }

							if (delay < 0) {
								delay = holdDelay;
								delayField.setText(Integer.toString(delay));
							} else if (delay > MAXIMUM_DELAY) {
								delay = MAXIMUM_DELAY;
								delayField.setText(Integer.toString(delay));
							}
			} else {
				processTimeChange = true;
				playStopButton.setText(PLAY);
				drawMode = DRAW_NONE;
				leftStepButton.setEnabled(true);
				rightStepButton.setEnabled(true);
				try {
					Thread.sleep(100);
				} catch (Exception unused) {}
				
				timeLayerPanel.setTime(timestep);
			}
		} else if (source == leftStepButton) {
			doStep(-1);
		} else if (source == rightStepButton) {
			doStep(1);
		} else if (source instanceof JTextField) {
			final JTextField field = (JTextField) source;
			int value = 0;

			try {
				value = Integer.parseInt(field.getText().toString());
			} catch (Exception unused) {
				value = -1;
			}

			if (source == delayField) {

				if (value < 0) {
					value = 0;
					delayField.setText(Integer.toString(value));
				} else if (value > MAXIMUM_DELAY) {
					value = MAXIMUM_DELAY;
					delayField.setText(Integer.toString(value));
				}

				delay = value;
			} 
		}

		String command = event.getActionCommand();
		//Point start = new Point((popUpLocation), getRow(popUpLocation));
		Rectangle rect = new Rectangle(popUpLocation, new Dimension(0, 0));

		if (command.equals(PROPERTIES_COMMAND)) {
			editChartProperties();
		} else if (command.equals(SAVE_COMMAND)) {
			try {
				PlotExporterAction save = new PlotExporterAction(this);
				save.actionPerformed(event);
			} catch (Exception e) {
				Logger.error("Error exporting image " + e.getMessage());
			}
		/*} else if (command.equals(PRINT_COMMAND)) {
			FastTilePlotPrintAction print = new FastTilePlotPrintAction(this);
			print.actionPerformed(event);*/
		} else if (command.equals(ZOOM_IN_BOTH_COMMAND) && inDataArea) {
			zoom(false, false, true, false, false, rect);
		} else if (command.equals(ZOOM_OUT_BOTH_COMMAND) && inDataArea) {
			zoom(false, false, false, false, true, rect);
		} else if (command.equals(ZOOM_OUT_MAX_COMMAND)) {
			resetZooming();
		}

		draw();
	}

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
	
	public void processMouseMotionEvent(MouseEvent me) {
		if(!isInDataArea(me)){
			app.getGui().setStatusTwoText("");
		}
		
		super.processMouseMotionEvent(me);
	}

	// Mouse-based zoom callback:

	public void processMouseEvent(MouseEvent me) {
		inDataArea = isInDataArea(me);
		int mod = me.getModifiers();
		int mask = MouseEvent.BUTTON3_MASK;
		
		if ((mod & mask) != 0) {
			showPopup(me);
			popUpLocation = me.getPoint();
			return;
		}
		
		super.processMouseEvent(me);
	}
	
	protected boolean isInDataArea(MouseEvent me) {
		int x = me.getPoint().x;
		int y = me.getPoint().y;
		
		if (x < dataArea.x || x > dataArea.x + dataArea.width)
			return false;
		
		if (y < dataArea.y || y > dataArea.y + dataArea.height)
			return false;
		
		return true;
	}
	
	protected int getRow(Point p) {
		int dist =  (p.y - dataArea.y);
		return dist;
		/*
		int div = dist * (lastRow - firstRow + 1);
		int den = dataArea.height;
		
		return firstRow +  div/den;*/
	}
	
	protected int getCol(Point p) {
		int dist = p.x - dataArea.x;
		return dist;
		/*
		int div = dist * (lastColumn - firstColumn + 1);
		int den = dataArea.width;
		
		return firstColumn +  div/den;*/
	}
	
	protected double getXDistance(Point p) {
		int dist = p.x - dataArea.x;
		double xCoord = dist / compositeFactor + panX;
		xCoord *= RAD_TO_DEG;
		int div = dist * (lastColumn - firstColumn + 1);
		int den = dataArea.width;
		
		return xCoord;
	}
	
	protected double getYDistance(Point p) {
		int dist = p.y - dataArea.y;
		double yCoord = latMax - dist / compositeFactor + panY;
		yCoord *= RAD_TO_DEG;
		//int div = dist * (lastColumn - firstColumn + 1);
		//int den = dataArea.width;
		
		return yCoord;
	}
	
	private void zoom(boolean rightClick, boolean leftClick, boolean popZoomIn, boolean reset, boolean zoomOut, Rectangle bounds) {
		if (reset) {
			resetZooming();
			return;
		}
		
		if (rightClick)
			return;
		

		boolean popZoom = true;
		
		previousPanX = panX;
		previousPanY = panY;
		
		if (popZoomIn) { // click to zoom in or popup menu zoom in
			if (zoomFactor < MAX_ZOOM) {
				previousZoomFactor = zoomFactor;
				//zoomFactor += 0.5;
				zoomFactor *= 4/3.0;
			}
		} else if (zoomOut) {  //zoom out
			if (zoomFactor > MIN_ZOOM) {
				previousZoomFactor = zoomFactor;
				//zoomFactor -= 0.5;
				zoomFactor /= 4/3.0;
				if (zoomFactor < MIN_ZOOM)
					zoomFactor = MIN_ZOOM;
			} else {
				previousZoomFactor = zoomFactor;
				zoomFactor = MIN_ZOOM;
				
				if (clippedDataRatio < dataRatio) {
					previousClippedDataRatio = clippedDataRatio;
					clippedDataRatio *= 1.25;
					if (clippedDataRatio > dataRatio)
						clippedDataRatio = dataRatio;
					screenWidth = (int)Math.round(screenHeight * clippedDataRatio);
				}
				else if (clippedDataRatio > dataRatio) {
					previousClippedDataRatio = clippedDataRatio;
					clippedDataRatio /= 1.25;
					if (clippedDataRatio < dataRatio)
						clippedDataRatio = dataRatio;
					screenHeight = (int)Math.round(screenWidth / clippedDataRatio);
				}
				
			}
		} else if (leftClick && bounds.height == 0 && bounds.width == 0) {
			return;
		} else { // regular zoom in
			previousClippedDataRatio = clippedDataRatio;
			clippedDataRatio = bounds.width / (double)bounds.height;
			previousPanX = panX;
			previousPanY = panY;
			panX = (bounds.x - xOffset) / compositeFactor + panX;
			panY = (bounds.y - yOffset) / compositeFactor + panY;
			previousZoomFactor = zoomFactor;
			zoomFactor *= screenWidth / (double)bounds.width;
			popZoom = false;
		}
		
		if (clippedDataRatio > previousClippedDataRatio) {		
			screenHeight = (int)Math.round(screenWidth / clippedDataRatio);
		}
		else {
			screenWidth = (int)Math.round(screenHeight * clippedDataRatio);
		}
		
		double zoomedFactor = screenWidth / dataWidth * zoomFactor;
		visibleDataWidth = screenWidth / zoomedFactor;
		
		xClickLocation = bounds.x;
		yClickLocation = bounds.y;
		
		Point p = new Point(bounds.x, bounds.y);
		setCellClicked(getCellIdByCoord(getCol(p), getRow(p)));
		
		double xClickDistance = (bounds.x - xOffset) / compositeFactor + panX;
		if (popZoom)
			panX = xClickDistance - visibleDataWidth / 2;
		if (panX < 0)
			panX = 0;
		if (panX > dataWidth - visibleDataWidth)
			panX = dataWidth - visibleDataWidth;
		
		double yClickDistance = (bounds.y - yOffset) / compositeFactor + panY;
		visibleDataHeight = screenHeight / zoomedFactor;  //half visible height
		if (popZoom)
			panY = yClickDistance - visibleDataHeight / 2;
		if (panY < 0)
			panY = 0;
		if (panY > dataHeight - visibleDataHeight)
			panY = dataHeight - visibleDataHeight;
				
		firstRow = (int)Math.round((dataHeight - panY - visibleDataHeight) * RAD_TO_DEG);
		lastRow = (int)Math.round((dataHeight - panY) * RAD_TO_DEG);
		firstColumn = (int)Math.round(panX * RAD_TO_DEG);
		lastColumn = (int)Math.round((panX + visibleDataWidth) * RAD_TO_DEG);

		computeDerivedAttributes();
		draw();
	}
	
	private void setCellClicked(int cellId) {
		previousClickedCell = currentClickedCell;
		currentClickedCell = cellId;
		CellInfo cell = cellIdInfoMap.get(previousClickedCell);
		if (cell != null)
			cell.cellClicked = false;
		cell = cellIdInfoMap.get(currentClickedCell);
		if (cell != null)
			cell.cellClicked = true;
	}

	private void doStep(int steps) {
		final int newValue = nextValue(steps, timestep, firstTimestep, lastTimestep);
		setTimestep(newValue); // Calls draw().
		timeLayerPanel.setTime(newValue);
	}

	// Sliders:

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

	// Helper.

	private int nextValue(int delta, int current, int min, int max) {
		int result = current + delta;

		if (result < min) {
			result = max - (min - result) + 1;
		} else if (result > max) {
			result = min + (result - max) - 1;
		}

		return result;
	}

	public void setTimestep(int timestep) {
		if (timestep >= firstTimestep && timestep <= lastTimestep && timestep != this.timestep) {
			previousTimestep = timestep;
			this.timestep = timestep;
			copySubsetLayerData(this.log);
			draw();
			drawOverLays();
		}
	}

	public void setLayer(int layer) {
		if (layer >= firstLayer && layer <= lastLayer && layer != this.layer) {
			previousLayer = this.layer;
			this.layer = layer;
			final int selection = statisticsMenu.getSelectedIndex();

			if ( selection > 0 ) {
				recomputeStatistics = true;
			}

			copySubsetLayerData(this.log);
			draw();
		}
	}
	
	private static int indexOfObsValue(float value, final double[] values) {
		if (new Float(value).toString().equals("NaN"))
			return -1;
		
		if (value <= DataUtilities.BADVAL3 || value <= DataUtilities.AMISS3) 	// 2014 changed AMISS3 comparison from == to <=
			return -1;

		final int count = values.length;

		if (values[0] == values[values.length - 1])
			return 0;

		for (int index = 1; index < count; index++) {
			if (values[index] > value)
				return index - 1;
		}

		return count - 2;
	}
	
	private class CellInfo { 
		double[] latCoords;
		double[] lonCoords;
		int[] latTransformed;
		int[] lonTransformed;
		int cellId;
		int colorIndex;
		double lon = 0;
		double lat = 0;
		boolean cellClicked = false;
		
		private CellInfo(int id, int numVertices) {
			latCoords = new double[numVertices];
			lonCoords = new double[numVertices];
			latTransformed = new int[numVertices];
			lonTransformed = new int[numVertices];
			cellId = id;
			cellIdInfoMap.put(id,  this);
		}
		
		public String getElevation() {
			if (layerAxis != null) {
				if (layerAxis.getName().equals(VAR_ELEVATION))
					return " " + coordFormat.format(((ArrayDouble.D2)elevation).get(cellId, MeshPlot.this.layer + 1)) + " m";
				if (layerAxis.getName().equals(VAR_DEPTH) && depth != null)
					return " " + coordFormat.format(((ArrayDouble.D3)depth).get(MeshPlot.this.timestep, cellId, MeshPlot.this.layer)) + " m";
			}
			return "";
		}
		
		public double getValue() {
			if (timeAxis == null)
				return ((ArrayDouble.D2)renderVariable).get(cellId, MeshPlot.this.layer);
			else if (renderVariable instanceof ArrayDouble.D2)
				return ((ArrayDouble.D2)renderVariable).get(MeshPlot.this.timestep, cellId);
			else
				return ((ArrayDouble.D3)renderVariable).get(MeshPlot.this.timestep, cellId, MeshPlot.this.layer);		}
		
		public double getValue(int time, int level) {
			if (time < 0)
				time = MeshPlot.this.timestep;
			if (level < 0)
				level = MeshPlot.this.layer;
			if (timeAxis == null)
				return ((ArrayDouble.D2)renderVariable).get(cellId, level);
			else if (renderVariable instanceof ArrayDouble.D2)
				return ((ArrayDouble.D2)renderVariable).get(time, cellId);
			else
				return ((ArrayDouble.D3)renderVariable).get(time, cellId, level);
		}
		
		private void transformCell(double factor, int xOffset, int yOffset) {
			for (int i = 0; i < lonCoords.length; ++i) {
				lonTransformed[i] = (int)Math.round((lonCoords[i] - lonMin - panX) * factor) + xOffset;
				latTransformed[i] = (int)Math.round((latCoords[i] - latMin - panY) * factor) + yOffset;
			}
			colorIndex = indexOfObsValue((float)getValue(), legendLevels);
		}
		
		public CellInfo clone() {
			CellInfo clone = new CellInfo(cellId, lonCoords.length);
			clone.latCoords = Arrays.copyOf(latCoords, latCoords.length);
			clone.lonCoords = Arrays.copyOf(lonCoords, lonCoords.length);
			return clone;
		}
	}

	
	Array cellVertices, latVert, lonVert, latCell, lonCell, indexToVertexId, indexToCellId, elevation, depth;
	private Map<Integer, Integer> vertexPositionMap;
	private Map<Integer, CellInfo> cellIdInfoMap;
	private CellInfo[] cellsToRender = null;
	private Map<Integer, CellInfo> splitCells = null;
	
	ArrayDouble eleveation = null;
	ArrayDouble renderVariable = null;
	ucar.ma2.ArrayInt.D2 vertexList;
	double latMin = Double.MAX_VALUE;
	double lonMin = Double.MAX_VALUE;
	double latMax = Double.MAX_VALUE * -1;
	double lonMax = Double.MAX_VALUE * -1;
	int previousCanvasSize = 0;
	double panX = 0;
	double panY = 0;
	double previousPanX = 0;
	double previousPanY = 0;
	final double MAX_ZOOM = Double.MAX_VALUE;
	final double MIN_ZOOM = 1.0;
	double zoomFactor = 1;
	double compositeFactor;
	double previousZoomFactor = 1;
	double dataWidth = 0;
	double dataHeight = 0;
	double visibleDataWidth = 0;
	double visibleDataHeight = 0;
	double dataRatio = 0;
	double clippedDataRatio = 0;
	double previousClippedDataRatio = 0;
	double clippedScreenWidth = 0;
	double clippedScreenHeight = 0;
	double avgCellDiam = 0;
	boolean renderBorder = true; //preseverd at the moment but replaced with Show Cell Border menu item
	double transformedCellDiam = 0;
	double borderDisplayCutoff = 0.02; //Map colors become hidden by cell borders if borders are drawn
										//when cells don't take up enough pixels.  Only allow borders
										//if cell width / screen width is above this percentage;
	Axes<CoordAxis> mpasAxes;
	
	private static final String VAR_ELEVATION = "nVertLevels";
	private static final String VAR_DEPTH = "nSoilLevels";
	
	private void loadCellStructure() throws IOException {
		Dataset ds = dataFrame.getDataset().get(0);
		DataReader reader = app.getDataManager().getDataReader(ds);
		Variable var = ds.getVariable("nEdgesOnCell");
		cellVertices = reader.getValues(ds, null, var).getArray();
		var = ds.getVariable("latVertex");
		latVert = reader.getValues(ds, null, var).getArray();
		var = ds.getVariable("lonVertex");
		lonVert = reader.getValues(ds, null, var).getArray();
		var = ds.getVariable("latCell");
		latCell = reader.getValues(ds, null, var).getArray();
		var = ds.getVariable("lonCell");
		lonCell = reader.getValues(ds, null, var).getArray();
		var = ds.getVariable("indexToVertexID");
		indexToVertexId = reader.getValues(ds, null, var).getArray();
		var = ds.getVariable("zgrid");
		elevation = reader.getValues(ds, null, var).getArray();
		var = ds.getVariable("zs");
		if (var != null)
			depth = reader.getValues(ds, null, var).getArray();
		//var = ds.getVariable("indexToCellID");
		//indexToCellId = reader.getValues(ds, null, var).getArray();
		var = ds.getVariable("verticesOnCell");
		vertexList = (ucar.ma2.ArrayInt.D2) reader.getValues(ds, null, var).getArray();
		
		vertexPositionMap = new HashMap<Integer, Integer>();
			
		int numVertices = indexToVertexId.getShape()[0];
		for (int i = 0; i < numVertices; ++i)
			vertexPositionMap.put(indexToVertexId.getInt(i), i);
		
		//TODO - fixme, maybe delete this
		/*int numCells = foo;
		for (int i = 0; i < numCells; ++i)
			cellPositionMap.put(indexToCellId.getInt(i), i);*/
			
			int[] vertexShape = vertexList.getShape();

			cellsToRender = new CellInfo[vertexShape[0]];
			splitCells = new HashMap<Integer, CellInfo>();
			boolean splitHeight = false;
			cellIdInfoMap = new HashMap<Integer, CellInfo>();
			for (int i = 0; i < vertexShape[0]; ++i) { //for each cell
				int vertices = cellVertices.getInt(i);
				cellsToRender[i] = new CellInfo(i, vertices);
				for (int j = 0; j < vertices; ++j) { //for each vertex
					int vId = vertexList.get(i,j);
					vId = vertexPositionMap.get(vId);
					cellsToRender[i].latCoords[j] =latVert.getDouble(vId) * -1;
					cellsToRender[i].lonCoords[j] =lonVert.getDouble(vId) - Math.PI;

					if (cellsToRender[i].lonCoords[j] < 0)
						cellsToRender[i].lonCoords[j] = 2 * Math.PI + cellsToRender[i].lonCoords[j];
					
					if (latVert.getDouble(vId) < latMin)
						latMin = latVert.getDouble(vId);
					if (lonVert.getDouble(vId) < lonMin)
						lonMin = lonVert.getDouble(vId);
					if (latVert.getDouble(vId) > latMax)
						latMax = latVert.getDouble(vId);
					if (lonVert.getDouble(vId) > lonMax)
						lonMax = lonVert.getDouble(vId);

					//System.out.println("Cell " + i + " vertex " + j + " id " + vId + " x " + xCord.getDouble(vId) + " y " + yCord.getDouble(vId) + " z " + zCord.getDouble(vId));
				}
				cellsToRender[i].lat = latCell.getDouble(i);
				cellsToRender[i].lon = lonCell.getDouble(i) - Math.PI;
				if (cellsToRender[i].lon < 0)
					cellsToRender[i].lon  = 2 * Math.PI + cellsToRender[i].lon;
				cellsToRender[i].lon -= Math.PI;
				cellsToRender[i].lat *= RAD_TO_DEG;
				cellsToRender[i].lon *= RAD_TO_DEG;
				
				boolean splitCell = false;
				CellInfo cell = cellsToRender[i];
				CellInfo cellHalf = null;
				double minCellLon = Math.PI * 2;
				double maxCellLon = 0; 
				
				double minCellLat = Double.MAX_VALUE;
				double maxCellLat = Double.MIN_VALUE;
				for (int j = 0; !splitHeight && j < cell.latCoords.length; ++j) {
					if (cell.latCoords[j] < maxCellLat)
						minCellLat = cell.latCoords[j];
					if (cell.latCoords[j] > maxCellLat)
						maxCellLat = cell.latCoords[j];
					if (maxCellLat - minCellLat > Math.PI * .4) {
						latMin = Math.PI * -0.5;
						latMax = Math.PI * 0.5;
						splitHeight = true;
					}
				}

				for (int j = 0; j < cell.lonCoords.length; ++j) {
					if (cell.lonCoords[j] < minCellLon)
						minCellLon = cell.lonCoords[j];
					if (cell.lonCoords[j] > maxCellLon)
						maxCellLon = cell.lonCoords[j];
					if (maxCellLon - minCellLon > Math.PI * 1.5) {
						splitCell = true;
						lonMin = 0;
						lonMax = Math.PI * 2;
						cellHalf = cell.clone();
					}
				}
				if (splitCell) {
					for (int j = 0; j < cell.lonCoords.length; ++j) {
						if (cell.lonCoords[j] < Math.PI) {
							cell.lonCoords[j] = Math.PI * 2;
						}
						else if (cellHalf.lonCoords[j] > Math.PI)
							cellHalf.lonCoords[j] = 0;
					}
					splitCells.put(i, cellHalf);
					
					//System.out.println("Split cell " + cell.cellId + " negative: " + negativeCell);
					//System.out.println("longitudes " + Arrays.toString(cell.lonCoords));
					//System.out.println("latitudes " + Arrays.toString(cell.latCoords));
					//System.out.println("longitudes " + Arrays.toString(cellHalf.lonCoords));
					//System.out.println("latitudes " + Arrays.toString(cellHalf.latCoords));
				}
			}
			
			dataWidth = lonMax - lonMin;
			dataHeight = latMax - latMin;
			visibleDataWidth = dataWidth;
			visibleDataHeight = dataHeight;
			dataRatio = dataWidth / dataHeight;
			previousClippedDataRatio = clippedDataRatio;
			clippedDataRatio = dataRatio;
			renderVariable = (ArrayDouble)dataFrame.getArray();
			CoordAxis xAxis = mpasAxes.getXAxis();
			var = ds.getVariable("verdi.avgCellDiam");
			avgCellDiam = reader.getValues(ds, null, var).getDouble(null);
			System.out.println("Lat min " + latMin + " max " + latMax + " lon min " + lonMin + " max " + lonMax);
	}
	
	
	
	private void transformCells(/*Graphics gr, */ int canvasSize, int xOrigin, int yOrigin) {
		long start = System.currentTimeMillis();
		
		if (clippedDataRatio > 1) {		
			screenWidth = canvasSize;
			screenHeight = (int)Math.round(screenWidth / clippedDataRatio);
		}
		else {
			screenHeight = canvasSize;
			screenWidth = (int)Math.round(screenHeight * clippedDataRatio);
		}
		compositeFactor = screenWidth / dataWidth * zoomFactor;
		transformedCellDiam = (int)Math.round(avgCellDiam * compositeFactor);
		renderBorder = transformedCellDiam / screenWidth > borderDisplayCutoff;

		//int imageWidth = screenWidth;
		//int imageHeight = (int)Math.round(imageWidth * dataHeight / dataWidth);
		for (int i = 0; i < cellsToRender.length; ++i) {
			//cellsToRender[i].transformCell(factor, imageWidth, imageHeight, xOffset, yOffset);		
			cellsToRender[i].transformCell(compositeFactor, xOrigin, yOrigin);		
		}
		for (CellInfo cell : splitCells.values())
			cell.transformCell(compositeFactor, xOrigin, yOrigin);
		
		gridBounds[X][MINIMUM] = westEdge + panX * RAD_TO_DEG;
		gridBounds[X][MAXIMUM] = westEdge + (panX + visibleDataWidth) * RAD_TO_DEG;
		gridBounds[Y][MINIMUM] = southEdge + (dataHeight - panY - visibleDataHeight) * RAD_TO_DEG;
		gridBounds[Y][MAXIMUM] = southEdge + (dataHeight - panY) * RAD_TO_DEG;

		domain[LONGITUDE][MINIMUM] = gridBounds[X][MINIMUM];
		domain[LONGITUDE][MAXIMUM] = gridBounds[X][MAXIMUM];
		domain[LATITUDE][MINIMUM] = gridBounds[Y][MINIMUM];
		domain[LATITUDE][MAXIMUM] = gridBounds[Y][MAXIMUM];
		
		if (screenWidth > 0) {
			cellIdMap = new BufferedImage(screenWidth + 1, screenHeight + 1, BufferedImage.TYPE_INT_RGB);
			java.awt.Graphics2D g = cellIdMap.createGraphics();
	        g.setColor(new Color(COLOR_BASE * -1));
	        g.fillRect(0,  0,  screenWidth+1, screenHeight + 1);
	        g.translate(xOffset * -1,  yOffset * -1);
	        renderCells(g, 0, 0);
	       /* File outputfile = new File("/tmp/data.bmp");
	        try {
				ImageIO.write(cellIdMap,  "bmp", outputfile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
		}
        
		System.out.println("Scaled cells in " + (System.currentTimeMillis() - start) + "ms");
	}

	public void renderCells(Graphics gr, int xOffset, int yOffset) {
		
		long renderStart = System.currentTimeMillis();
		
		if (xOffset != 0)
			gr.setClip(xOffset, yOffset, screenWidth, screenHeight);
		final int statisticsSelection = statisticsMenu.getSelectedIndex();
		
		final Boolean showGridLines = (Boolean)
				config.getObject( TilePlotConfiguration.SHOW_GRID_LINES );
		final boolean showCellBorder = showGridLines != null && showGridLines.booleanValue();
		
		/*
		BufferedImage img = new java.awt.image.BufferedImage(imageWidth, imageHeight, java.awt.image.BufferedImage.TYPE_3BYTE_BGR);
        java.awt.Graphics2D g = img.createGraphics();
        */

		for (int i = 0; i < cells; ++i) { //for each cell
			renderCell(gr, xOffset, yOffset, cellsToRender[i], statisticsSelection, showGridLines, showCellBorder, i);
		}
		for (Map.Entry<Integer, CellInfo> cell : splitCells.entrySet()) {
			renderCell(gr, xOffset, yOffset, cell.getValue(), statisticsSelection, showGridLines, showCellBorder, cell.getKey());
		}
		
		gr.setClip(null);
		//gr.translate(0,  0);
		long renderTime = System.currentTimeMillis() - renderStart;
		//TODO - consider only doing this once, scaling mouseover coordinates on the fly
		System.out.println("Finished drawing image " + new Date() + " image in " + renderTime + "ms");
		//System.out.println("Var min " + varMin + " max " + varMax);
		/*
		java.io.File outputFile = new java.io.File("/tmp/mpasout.png");
		try {
			boolean res = javax.imageio.ImageIO.write(img, "png", outputFile);
			System.out.println("Image written: " + res + " " + new Date());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		gr.drawImage(img.getScaledInstance(windowWidth, -1, Image.SCALE_FAST), 0, 0, null);
		*/
		
	}
	
	private void renderCell(Graphics gr, int xOffset, int yOffset, CellInfo cell, int statisticsSelection, boolean showGridLines, boolean showCellBorder, int index) {
		if (cell.colorIndex == -1)
			return;
		/**
		 * 0 xOffset means this is the image used to map coordinates to cell IDs, not the actual screen.
		 * Store the cell ID as the image's color.
		 */
		if (xOffset == 0)
			gr.setColor(new Color(cell.cellId));
		else if (SHOW_ZOOM_LOCATION && cell.cellClicked) {
			gr.setColor(Color.BLACK);
			System.err.println("Rendering clicked cell location " + cell.lon + ", " + cell.lat + " id " + cell.cellId);
		}
		else if (statisticsSelection == 0)
			gr.setColor(legendColors[cell.colorIndex]);
		else
			gr.setColor(legendColors[indexOfObsValue(subsetLayerData[index], legendLevels)]);

		gr.fillPolygon(cell.lonTransformed, cell.latTransformed, cell.lonTransformed.length);
		if (showCellBorder) {
			gr.setColor(Color.BLACK);
			int length = cell.lonTransformed.length - 1;
			for (int j = 0; j < length; ++j) {
				gr.drawLine(cell.lonTransformed[j], cell.latTransformed[j],
						cell.lonTransformed[j + 1], cell.latTransformed[j + 1]);
			}
			gr.drawLine(cell.lonTransformed[length],  cell.latTransformed[length], 
					cell.lonTransformed[0], cell.latTransformed[0]);
		}
	}
	
	// Construct but do not draw yet.

	public MeshPlot(VerdiApplication app, DataFrame dataFrame) {
		super(true);
		this.app=app;
		setDoubleBuffered(true);
		assert dataFrame != null;
		this.dataFrame = dataFrame;
		this.calculateDataFrameLog();
		hasNoLayer = (dataFrame.getAxes().getZAxis() == null);
		format = NumberFormat.getInstance();
		format.setMaximumFractionDigits(4);
		
		coordFormat = NumberFormat.getInstance();
		coordFormat.setMaximumFractionDigits(2);

		AreaFinder finder = new AreaFinder();
		this.addMouseListener(finder);
		this.addMouseMotionListener(finder);
		// Initialize attributes from dataFrame argument:
		
		final Variable dataFrameVariable = dataFrame.getVariable();
		variable = dataFrameVariable.getName();
		Logger.debug("dataFrameVariable = " + dataFrameVariable);
		Logger.debug("dataFrameVariable name = " + variable);
		units = dataFrameVariable.getUnit().toString();
		Logger.debug("units of dataFrameVariable = " + units);
		if ( units==null || units.trim().equals("")) {
			units = "none";
			unitString = "";
		}
		else if (units.equals("-"))
			unitString = "";
		else
			unitString = " " + units;
		Logger.debug("now units = " + units);
		
		assert dataFrame.getAxes() != null;
		final Axes<DataFrameAxis> axes = dataFrame.getAxes();

		// Create cartographic projector (used by mapper.draw):

		final Dataset dataset = dataFrame.getDataset().get(0);
		final Axes<CoordAxis> coordinateAxes = dataset.getCoordAxes();
		mpasAxes = coordinateAxes;

		
		/*final Projection projection = coordinateAxes.getProjection();

		if (projection instanceof LatLonProjection) {
			projector = null;
		} else {
			projector = new Projector(projection);
		}*/

		// Initialize grid dimensions: timesteps, layers, rows, columns:

		timeAxis = axes.getTimeAxis();

		if (timeAxis == null) {
			timesteps = 1;
			firstTimestep = timestep = lastTimestep  = 0;
		} else {
			timesteps = timeAxis.getExtent();
			firstTimestep = timestep = timeAxis.getOrigin();
			lastTimestep = firstTimestep + timesteps - 1;
		}

		layerAxis = axes.getZAxis();

		if (layerAxis == null) {
			layers = 1;
			firstLayer = layer = lastLayer = 0;
		} else {
			layers = layerAxis.getExtent();
			firstLayer = layer = layerAxis.getOrigin();
			lastLayer = firstLayer + layers - 1;
		}
		
		//TAH
		//TODO - remove unneccessary bits here
		final CoordAxis cellAxis = axes.getCellAxis();
		cells = (int)cellAxis.getRange().getExtent();
		
		rowAxis = mpasAxes.getYAxis();
		rowLabels = rowAxis != null ? new MPASAxisLabelCreator(null) : null;
		rows = rowAxis != null ? (int)rowAxis.getRange().getExtent() : 1;
		rowOrigin = rowAxis != null ? (int)rowAxis.getRange().getOrigin() : 0;
		firstRow = 0;
		lastRow = firstRow + rows - 1;

		columnAxis = mpasAxes.getXAxis();
		columnLabels = columnAxis != null ? new MPASAxisLabelCreator(null) : null;
		columns = columnAxis != null ? (int)columnAxis.getRange().getExtent() : 1;
		columnOrigin = columnAxis != null ? (int)columnAxis.getRange().getOrigin() : 0;
		firstColumn = 0;
		lastColumn = firstColumn + columns - 1;
		final Envelope envelope = mpasAxes.getBoundingBox(dataFrame.getDataset().get(0).getNetcdfCovn());

		if (envelope == null)
			axes.e.printStackTrace();
		westEdge = envelope.getMinX(); // E.g., -420000.0.
		southEdge = envelope.getMinY(); // E.g., -1716000.0.
//		cellWidth = Numerics.round1(envelope.getWidth() / columns); // 12000.0.
//		cellHeight = Numerics.round1(envelope.getHeight() / rows); // 12000.0.
		
		cellWidth = envelope.getWidth() / columns; // 12000.0.
		cellHeight = envelope.getHeight() / rows; // 12000.0.

		// HACK to invert row indexing for AURAMS / GEM / CF Convention files:

		final CoordAxis yAxis = coordinateAxes.getYAxis();
		final double firstYValue = yAxis.getValue( 0 );
		final double lastYValue  = yAxis.getValue( rows - 1 );
		invertRows = firstYValue > lastYValue;

		// 2014 footer date/time for footer1
		
		GregorianCalendar firstDate = mpasAxes.getDate(firstTimestep);
		final GregorianCalendar date0 = (firstDate == null) ? new GregorianCalendar() : firstDate;
		Utilities.formatDate(date0);		// 2014 appears to fix starting date/time issue 
		final int yyyy = date0.get(GregorianCalendar.YEAR);
		final int ddd = date0.get(GregorianCalendar.DAY_OF_YEAR);
		final int hh = date0.get(GregorianCalendar.HOUR_OF_DAY);
		final int mm = date0.get(GregorianCalendar.MINUTE);
		final int ss = date0.get(GregorianCalendar.SECOND);
		startDate = yyyy * 1000 + ddd; // E.g., 2005241.
		startTime = hh * 10000 + mm * 100 + ss; // HHMMSS, e.g., 10000.

		if (timesteps > 1) {
			final GregorianCalendar date1 = axes.getDate(firstTimestep + 1);
			long step = (date1.getTimeInMillis() - date0.getTimeInMillis()) / 1000l;	// 2014 must  use getTimeInMillis()
			final int dhh = (int) (step / 3600);
			final int dmm = (int) (step % 3600 / 60);
			final int dss = (int) (step % 3600 % 60);
			timestepSize = dhh * 10000 + dmm * 100 + dss; // HHMMSS, e.g.,	// 2014 correct timestepSize computed here 
			// 10000.
		} else {
			timestepSize = 10000;
		}
		
		//populate legend colors and ranges on initiation
		double[] minmax = { 0.0, 0.0 };
		//default to not a log scale
		this.log = false;
		//calculate the non log min/max values, keep the code here
		//first part of IF ELSE will use the min/max values
		computeDataRange(minmax, false);
		if ( this.map == null) {
			
			Logger.debug("in FastTilePlot, this.map == null so calling new PavePaletteCreator");
			defaultPalette = new PavePaletteCreator().createPalettes(8).get(0);
			map = new ColorMap(defaultPalette, minmax[0], minmax[1]);
		} else {
			ColorMap.ScaleType sType = map.getScaleType();
			if ( sType != null && sType == ColorMap.ScaleType.LOGARITHM ) 
				this.log = true;
		if(map.getPalette() == null)
		{
			Logger.debug("map.getPalette is null so calling new PavePaletteCreator");
		}
			defaultPalette = (map.getPalette() != null) ? map.getPalette() : new PavePaletteCreator().createPalettes(8).get(0);
			map.setPalette(defaultPalette);
		}

		//set min/max for both log and non log values...
		map.setMinMax( minmax[0], minmax[1]);
		computeDataRange(minmax, true);
		map.setLogMinMax( minmax[0], minmax[1]);
		//this final one is for the below legend value calculations
		computeDataRange(minmax, this.log);

		//default to this type...
		map.setPaletteType(ColorMap.PaletteType.SEQUENTIAL);
		legendColors = defaultPalette.getColors();
		final double minimum = minmax[0];
		final double maximum = minmax[1];
		minMax = new DataUtilities.MinMax(minimum, maximum);
		int count = legendColors.length + 1;
		final double delta = (minmax[1] - minmax[0]) / (count - 1);
		legendLevels = new double[count];
		for (int level = 0; level < count; ++level) {
			legendLevels[level] = minmax[0] + level * delta;
		}
//		computeLegend();
		config = new TilePlotConfiguration();
		((TilePlotConfiguration) config).setColorMap(map);
		((TilePlotConfiguration) config).setGridLines(false, Color.gray);
		((TilePlotConfiguration) config).setSubtitle1(Tools.getDatasetNames(getDataFrame()));

		// Compute attributes derived from the above attributes and dataFrame:

		computeDerivedAttributes();

		// Create EMVL TilePlot (but does not draw yet - see draw()):

		tilePlot = new TilePlot(startDate, startTime, timestepSize);

		// Create GUI.

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
		javax.swing.JPopupMenu.setDefaultLightWeightPopupEnabled( false );
		statisticsMenu = new JComboBox( statisticsNames );
		statisticsMenu.addActionListener( this ); // Just so draw() is called.
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
		toolBar.add(panel);

		try {
			loadCellStructure();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}

		// add(toolBar);
		doubleBufferedRendererThread = new Thread(doubleBufferedRenderer);
		doubleBufferedRendererThread.start(); // Calls
		
		draw();
	}
	
	// Compute legend levels from data range:

	private void computeLegend() {

		//populate legend colors and ranges on initiation
		//default to not a log scale
		double[] minmax = { 0.0, 0.0 };
		//calculate the non log min/max values, keep the code here
		//first part of IF ELSE will use the min/max values
		computeDataRange(minmax, false);
//		ColorMap.ScaleType sType = map.getScaleType();
		//computeDataRange function need this.log set correctly...
		if(map.getPalette() == null)
		{
			Logger.debug("getPalette is null here also so getting ready to call PavePaletteCreator");
		}
		defaultPalette = (map.getPalette() != null) ? map.getPalette() : new PavePaletteCreator().createPalettes(8).get(0);
		map.setPalette(defaultPalette);
		
		//set min/max for both log and non log values...
		map.setMinMax( minmax[0], minmax[1]);
		computeDataRange(minmax, true);
		map.setLogMinMax( minmax[0], minmax[1]);
		//this final one is for the below legend value calculations
		computeDataRange(minmax, this.log);

		legendColors = defaultPalette.getColors();
		final double minimum = minmax[0];
		final double maximum = minmax[1];
		minMax = new DataUtilities.MinMax(minimum, maximum);
		int count = legendColors.length + 1;
		final double delta = (minmax[1] - minmax[0]) / (count - 1);
		legendLevels = new double[count];
		for (int level = 0; level < count; ++level) {
			legendLevels[level] = minmax[0] + level * delta;
		}
		config.setUnits("");
	}
	
	private boolean statError = false;
	private String statErrMsg = "";

	private void computeStatistics(boolean log) {

		if ( layerData == null ) {
			layerData = new float[ cellsToRender.length ][ timesteps ];
			//layerDataLog = new float[ rows ][ columns ][ timesteps ];
			statisticsData = new float[ GridCellStatistics.STATISTICS ][1][ cellsToRender.length ];
			//statisticsDataLog = new float[ GridCellStatistics.STATISTICS ][ rows ][ columns ];
		}
			
		// Copy from dataFrame into layerData[ rows ][ columns ][ timesteps ]:
		
		DataFrame dataFrame = getDataFrame(log);
		final MPASDataFrameIndex dataFrameIndex = new MPASDataFrameIndex(dataFrame);
			
		for (int timestep = 0; timestep < timesteps; ++timestep) {
			for (int cell = 0; cell < cellsToRender.length; ++cell) {
					dataFrameIndex.set(timestep, layer, cell);
					//float value = this.dataFrame.getFloat( dataFrameIndex ); // do not replace this one with getDataFrame()
					float value = this.getDataFrame(log).getFloat( dataFrameIndex ); 
					layerData[ cell ][ timestep ] = value;
					//value = this.dataFrameLog.getFloat( dataFrameIndex );
					//layerDataLog[ row ][ column ][ timestep ] = value;
				
			}
		}

		final double threshold = Double.parseDouble( this.threshold.getText() );
		final double hoursPerTimestep = 1.0;
		
		try {
			MeshCellStatistics.computeStatistics( layerData,
					threshold, hoursPerTimestep,
					statisticsData, this.statisticsMenu.getSelectedIndex()-1 );
			//GridCellStatistics.computeStatistics( layerDataLog,
			//		threshold, hoursPerTimestep,
			//		statisticsDataLog, this.statisticsMenu.getSelectedIndex()-1 );
			this.statError = false;
			this.statErrMsg = "";
		} catch ( Exception e) {
			e.printStackTrace();
			Logger.error("Error occurred during computing statistics: " + e.getMessage());
			this.statError = true;
			this.statErrMsg = "Error occurred during computing statistics: " + e.getMessage();
			if ( map != null && map.getScaleType() == ColorMap.ScaleType.LOGARITHM) {
				this.preLog = true;
				this.log = false;
				if ( this.tilePlot != null) {
					this.tilePlot.setLog( false);
				}
				map.setScaleType( ColorMap.ScaleType.LINEAR);
				
				// 2014 this section previously commented out
				//this.computeLegend();
//				double[] minmax = { 0.0, 0.0 };
//				computeDataRange(minmax);
//				final double minimum = minmax[0];
//				final double maximum = minmax[1];
//				minMax = new DataUtilities.MinMax(minimum, maximum);
//				this.recomputeLegend = true;
//				this.recomputeStatistics = true;				
//
//				if ( this.dialog != null) {
//					this.dialog.initColorMap(map, minMax);
//				}
				draw();
			}
		}
	}

	// Compute derived attributes:

	private void computeDerivedAttributes() {
		if (true)
			return;

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

	private static void computeMapDomain(final Projector projector,
			final double[][] gridBounds, double[][] mapDomain) {

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
				ucar.unidata.geoloc.projection.Stereographic ) {

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

	// Copy current timestep, layer and row/column subset data from dataFrame
	// into subsetlayerdata[][]:

	private void copySubsetLayerData(boolean log) {

		if (subsetLayerData == null) {
			subsetLayerData = new float[cellsToRender.length];
		}

		final int selection = statisticsMenu.getSelectedIndex();
		
		if ( selection == 0 ) {			
			for ( int cell = 0; cell < cellsToRender.length; ++cell ) {
				subsetLayerData[cell] = (float)cellsToRender[cell].getValue();
			}
		} else {
			final int statistic = selection - 1;

			if ( statisticsData == null || recomputeStatistics ) {
				computeStatistics(log);
				recomputeStatistics = false;
			}

			// Copy from statisticsData into subsetLayerData[ rows ][ columns ]:
			
			for ( int cell = 0; cell < cellsToRender.length; ++cell ) {
				subsetLayerData[cell] = (float)cellsToRender[cell].getValue();
			}
		}

		if ( recomputeLegend ) {
//			computeLegend();
			recomputeLegend = false;
		}
	}

	// Compute data range excluding BADVAL3 values:

	public void computeDataRange(double[] minmax, boolean log) {
		final int selection = statisticsMenu != null ? statisticsMenu.getSelectedIndex() : 0;
		boolean initialized = false;
		minmax[0] = minmax[1] = 0.0;
		if ( selection == 0 ) {
			DataFrame dataFrame = getDataFrame(log);
			final MPASDataFrameIndex dataFrameIndex = new MPASDataFrameIndex(dataFrame);
				
			for (int timestep = 0; timestep < timesteps; ++timestep) {
				for (int layer = 0; layer < layers; ++layer) {
					for (int cell = 0; cell < cells; ++cell) {
						dataFrameIndex.set(timestep, layer, cell);
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
		} else {
						
			//if ( this.statisticsData == null || selection != this.preStatIndex //|| this.preLog != this.log 
				//	) {
				this.computeStatistics(log);					
			//}
			
			final int statistic = selection - 1;

			for ( int cell = firstRow; cell < cellsToRender.length; ++cell ) {

				final float value = statisticsData[ statistic ][0][ cell ];
				
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

	/**
	 * Gets the panel that contains the plot component.
	 * 
	 * @return the panel that contains the plot component.
	 */

	public JPanel getPanel() {
		return this;
	}

	/**
	 * Gets a menu bar for this Plot. This may return null if there is no menu
	 * bar.
	 * 
	 * @return a menu bar for this Plot.
	 */

	public JMenuBar getMenuBar() {
		javax.swing.JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		JMenuBar bar = new JMenuBar();

		JMenu menu = new JMenu("File");
		menu.setMnemonic('F');
		//menu.add(new FastTilePlotPrintAction(this));
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
		//configureMapMenu(menu);
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
		
		// item.setSelected(true);
		JMenuItem menuItem = new JMenuItem(
				new AbstractAction("Set Longitude and Latitude Ranges") {
					private static final long serialVersionUID = -4465758432397962782L;

					@Override
					public void actionPerformed(ActionEvent arg0) {
						setDataRanges();
					}
				});
		menu.add(menuItem);

		menu.addSeparator();
		showGridLines = new JCheckBoxMenuItem(new AbstractAction("Show Cell Borders") {
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
				panel.init(getDataFrame().getAxes(), MeshPlot.this);
			}
		});
		menu.add(item2);

		/*if (this.getClass().equals(MeshPlot.class)) {
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
		}*/
		
		menu = new JMenu("GIS Layers");	// in VERDI versions through 1.5.0 does not pertain to shapefiles
		gisLayersMenu(menu);
		bar.add(menu);
		
		// change cursor for initial zoom state
		setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		
		return bar;
	}
	
	/*protected void addObsOverlay() {
		OverlayRequest<ObsEvaluator> request = new OverlayRequest<ObsEvaluator>(OverlayRequest.Type.OBS, this);
		eventProducer.fireOverlayRequest(request);
	}
	
	protected void addVectorOverlay() {
		OverlayRequest<VectorEvaluator> request = new OverlayRequest<VectorEvaluator>(OverlayRequest.Type.VECTOR, this);
		eventProducer.fireOverlayRequest(request);
	}*/

	protected void activateRubberBand() {
		rubberband.setActive(true);
	}
	
	protected void deactivateRubberBand() {	// 2014 to allow user to turn OFF probe
		rubberband.setActive(false);
	}
	
	private void resetZooming() {
		previousZoomFactor = zoomFactor;
		clippedDataRatio = dataRatio;
		zoomFactor = 1;
		compositeFactor = screenWidth / dataWidth;
		previousPanX = panX;
		previousPanY = panY;
		panX = 0;
		panY = 0;
		firstRow = 0;
		lastRow = rows - 1;
		firstColumn = 0;
		lastColumn = columns - 1;
		visibleDataWidth = dataWidth;
		visibleDataHeight = dataHeight;
		setCellClicked(0);
		draw();
		computeDerivedAttributes();
		copySubsetLayerData(this.log);
	}

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
		
		// String defaultMaps = System.getProperty("default.maps", "");

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
				FastTileLayerEditor editor = showGISLayersDialog();

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

	private FastTileLayerEditor showGISLayersDialog() {
		Logger.debug("in FastTilePlot.showGISLayersDialog()");
		Window frame = SwingUtilities.getWindowAncestor(this);
		FastTileLayerEditor editor = null;
		
		if (frame instanceof JFrame)
			editor = new FastTileLayerEditor((JFrame) frame);
		else
			editor = new FastTileLayerEditor((JDialog) frame);
		
		editor.init(mapper);
		editor.setLayerControl(createControlLayer());
		editor.setLocationRelativeTo(frame);
		editor.setVisible(true);
		editor.pack();
		return editor;
	}

//	protected MapLayer createControlLayer() {
	protected FeatureLayer createControlLayer() {
		if (controlLayer != null)
			return controlLayer;
		
		try {
			URL url = new File("data/map_na.shp").toURI().toURL();
			Map<String, Serializable> params = new HashMap<String, Serializable>();
			// replaced IndexedShapefileDataStoreFactory with ShapefileDataStoreFactory
			params.put(ShapefileDataStoreFactory.URLP.key, url);
			params.put(ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key, true);
			ShapefileDataStoreFactory fac = new ShapefileDataStoreFactory();
			ShapefileDataStore ds = (ShapefileDataStore) fac.createDataStore(params);
			StyleBuilder builder = new StyleBuilder();
			Style style = builder.createStyle(builder.createLineSymbolizer());
//			DefaultQuery query = new DefaultQuery();
			Query query = new Query();
			query.setCoordinateSystemReproject(getDataFrame().getAxes().getBoundingBox(getDataFrame().getDataset().get(0).getNetcdfCovn()).getCoordinateReferenceSystem());
//			controlLayer = new DefaultMapLayer(ds.getFeatureSource().getFeatures(query), style);
			controlLayer = new FeatureLayer(ds.getFeatureSource().getFeatures(query), style);
			controlLayer.setTitle("Control Layer");
		} catch (Exception e) {
			Logger.error("Exception in FastTilePlot.createControlLayer: " + e.getMessage());
		}
		
		return controlLayer;
	}
	
	public void setLayerMapLine(MapLines mapLines) {
		mapper.getLayers().add(mapLines);
	}

	private void showLayer(String layerKey, boolean show, JMenu addLayers) {
		try {
			if (show && layerKey.equals(STATES_LAYER)) {
//				MapLines map2Add = getEditedMapLayer(mapper.getUsaStatesMap());
				MapLines map2Add = mapper.getUsaStatesMap();
				mapper.getLayers().add(map2Add);
			}

			if (!show && layerKey.equals(STATES_LAYER)) {
				mapper.removeUsaStates();
			}
			
			if (show && layerKey.equals(COUNTIES_LAYER)) {
//				MapLines map2Add = getEditedMapLayer(mapper.getUsaCountiesMap());
				MapLines map2Add = mapper.getUsaCountiesMap();
				mapper.getLayers().add(map2Add);
			}

			if (!show && layerKey.equals(COUNTIES_LAYER)) {
				mapper.removeUsaCounties();
			}

			if (show && layerKey.equals(WORLD_LAYER)) {
//				MapLines map2Add = getEditedMapLayer(mapper.getWorldMap());
				MapLines map2Add = mapper.getWorldMap();
				mapper.getLayers().add(map2Add);
			}

			if (!show && layerKey.equals(WORLD_LAYER)) {
				mapper.removeWorld();
			}

			if (show && layerKey.equals(NA_LAYER)) {
//				MapLines map2Add = getEditedMapLayer(mapper.getNorthAmericaMap());
				MapLines map2Add = mapper.getNorthAmericaMap();
				mapper.getLayers().add(map2Add);
			}

			if (!show && layerKey.equals(NA_LAYER)) {
				mapper.removeNorthAmerica();
			}

			if (show && layerKey.equals(HUCS)) {
				withHucs = show;
//				mapper.getLayers().add(getEditedMapLayer(mapper.getUSHucMap()));
				mapper.getLayers().add(mapper.getUSHucMap());
			}
			
			if (!show && layerKey.equals(HUCS)) {
				withHucs = show;
				mapper.removeUSHucMap();
			}

			if (show && layerKey.equals(RIVERS)) {
				withRivers = show;
//				mapper.getLayers().add(getEditedMapLayer(mapper.getUSRiversMap()));
				mapper.getLayers().add(mapper.getUSRiversMap());
			}
			
			if (!show && layerKey.equals(RIVERS)) {
				withRivers = show;
				mapper.removeUSRiversMap();
			}

			if (show && layerKey.equals(ROADS)) {
				withRoads = show;
//				mapper.getLayers().add(getEditedMapLayer(mapper.getUSRoadsMap()));	// 2014 getting rid of popup edit box
				mapper.getLayers().add(mapper.getUSRoadsMap());
			}
			
			if (!show && layerKey.equals(ROADS)) {
				withRoads = show;
				mapper.removeUSRoadsMap();
			}
			
			if (layerKey.equals(OTHER_MAPS)) {
				showGISLayersDialog();
			}
			draw();
		} catch (Exception e) {
			Logger.error("Error adding layer " + e.getMessage());
		}
	}

//	private MapLines getEditedMapLayer(MapLines map) {
//Logger.debug("in FastTilePlot.getEditedMapLayer for map: " + map);
//		Window frame = SwingUtilities.getWindowAncestor(this);
//Logger.debug("got window ancestor = " + frame.toString() + "\n\tReady to launch FastTileAddLayerWizard");
//		FastTileAddLayerWizard wizard = new FastTileAddLayerWizard(new File(map.getMapFile()), createControlLayer(), map, false);
//Logger.debug("back from FastTileAddLayerWizard, now ready to return the display");
//		return wizard.display((JFrame)frame, true);
//	}

	/**
	 * Displays a dialog that allows the user to edit the properties for the
	 * current chart.
	 * 
	 * @since 1.0.5
	 */
	public void editChartProperties() {
		Window window = SwingUtilities.getWindowAncestor(MeshPlot.this);
		dialog = null;
		if (window instanceof JFrame)
			dialog = new ConfigDialog((JFrame) window);
		else
			dialog = new ConfigDialog((JDialog) window);
		dialog.init(MeshPlot.this, minMax);
		dialog.enableScale( !this.statError);
		dialog.setSize(500, 600);
		dialog.setVisible(true);
	}

	/**
	 * Gets a tool bar for this plot. This may return null if there is no tool
	 * bar.
	 * 
	 * @return a tool bar for this plot.
	 */

	public JToolBar getToolBar() {
		return toolBar;
	}

	/**
	 * Gets the type of the Plot.
	 * 
	 * @return the type of the Plot.
	 */

	public Formula.Type getType() {
		return Formula.Type.TILE;
	}

	/**
	 * Gets the data that this Plot plots.
	 * 
	 * @return the data that this Plot plots.
	 */

	public List<DataFrame> getData() {
		final List<DataFrame> result = new ArrayList<DataFrame>();
		result.add(getDataFrame());
		return result;
	}

	/**
	 * Exports an image of this Plot to the specified file in the specified
	 * format.
	 * 
	 * @param format
	 *            the image format. One of PlotExporter.JPG, PlotExporter.TIF,
	 *            PlotExporter.PNG, or PlotExporter.BMP
	 * @param file
	 *            the file to save the image to.
	 * @param width
	 *            width of image in pixels
	 * @param height
	 *            height of image in pixels
	 * @throws IOException
	 *             if there is an error creating the image
	 */

	public void exportImage(String format, File file, int width, int height)
			throws IOException {
		drawBatchImage(width, height);
		PlotExporter exporter = new PlotExporter(this);
		exporter.save(format, file, width, height);
	}

	/**
	 * Configure this Plot according to the specified PlotConfiguration.
	 * 
	 * @param config
	 *            the new plot configuration
	 */

	public void configure(PlotConfiguration config) {
		String configFile = config.getConfigFileName();
		double[] minmax = { 0.0, 0.0 };

		if (configFile != null) {
			try {
				configure(new PlotConfigurationIO().loadConfiguration(new File(configFile)));
			} catch (IOException ex) {
				Logger.error("IOException in FastTilePlot.configure: loading configuration: " + ex.getMessage());
			}
		}

		ColorMap map = (ColorMap) config.getObject(TilePlotConfiguration.COLOR_MAP);

		if (map != null) {
			
			// set log related info
			ColorMap.ScaleType sType = map.getScaleType();
			this.preLog = this.log;
			if ( sType == ColorMap.ScaleType.LOGARITHM ) {
				this.log = true;
				if ( this.tilePlot != null) {
					this.tilePlot.setLog( true);
					this.tilePlot.setLogBase( (int)map.getLogBase());
				}					
				
				//we need to also populate the non log intervals to some default range...
				//a new map object is created and doesn't keep the interval ranges that was created in one of the constructors
				//so we need to make sure and pre populate just in case user changes between linear and log scales
				computeDataRange(minmax, false);
				map.setMinMax( minmax[0], minmax[1]);

			} else {
				this.log = false;
				if ( this.tilePlot != null) {
					this.tilePlot.setLog( false);
				}				
				//we need to also populate the log intervals to some default range...
				//a new map object is created and doesn't keep the interval ranges that was created in one of the constructors
				//so we need to make sure and pre populate just in case user changes between linear and log scales
				computeDataRange(minmax, true);
				map.setLogMinMax( minmax[0], minmax[1]);
			}
			updateColorMap(map);
			recomputeStatistics = true;
		}

		this.draw();
		this.config = new TilePlotConfiguration(config);
		
		if (this.showGridLines != null) {
			Boolean gridlines = (Boolean)config.getObject(TilePlotConfiguration.SHOW_GRID_LINES);
			this.showGridLines.setSelected(gridlines == null ? false : gridlines);
		}
	}
	
	public void configure(PlotConfiguration config, Plot.ConfigSoure source) {
		String configFile = config.getConfigFileName();
		double[] minmax = { 0.0, 0.0 };

		if (configFile != null) {
			try {
				configure(new PlotConfigurationIO().loadConfiguration(new File(configFile)), source);
			} catch (IOException ex) {
				Logger.error("IOException in FastTilePlot.configure: loading configuration: " + ex.getMessage());
			}
		}
		
		this.configSource = source;

		ColorMap map = (ColorMap) config.getObject(TilePlotConfiguration.COLOR_MAP);

		if (map != null) {
			
			// set log related info
			ColorMap.ScaleType sType = map.getScaleType();
			this.preLog = this.log;
			if ( sType == ColorMap.ScaleType.LOGARITHM ) {
				this.log = true;
				if ( this.tilePlot != null) {
					this.tilePlot.setLog( true);
					this.tilePlot.setLogBase( (int)map.getLogBase());
				}					
				
				//we need to also populate the non log intervals to some default range...
				//a new map object is created and doesn't keep the interval ranges that was created in one of the constructors
				//so we need to make sure and pre populate just in case user changes between linear and log scales
				computeDataRange(minmax, false);
				map.setMinMax( minmax[0], minmax[1]);

			} else {
				this.log = false;
				if ( this.tilePlot != null) {
					this.tilePlot.setLog( false);
				}				
				//we need to also populate the log intervals to some default range...
				//a new map object is created and doesn't keep the interval ranges that was created in one of the constructors
				//so we need to make sure and pre populate just in case user changes between linear and log scales
				computeDataRange(minmax, true);
				map.setLogMinMax( minmax[0], minmax[1]);
			}
			
			updateColorMap(map);
			recomputeStatistics = true;	
			if ( source == Plot.ConfigSoure.FILE) {
				this.recomputeLegend = true;
			} 
		}

		this.config = new TilePlotConfiguration(config);
		this.draw();
		
		if (this.showGridLines != null) {
			Boolean gridlines = (Boolean)config.getObject(TilePlotConfiguration.SHOW_GRID_LINES);
			this.showGridLines.setSelected(gridlines == null ? false : gridlines);
		}
	}	

	private void updateColorMap(ColorMap map) {
		this.map = map;
		
		try {
			minMax = new DataUtilities.MinMax(map.getMin(), map.getMax());
		} catch (Exception e) {
			Logger.error("Exception in FastTilePlot.updateColorMap: " + e.getMessage());
			e.printStackTrace();
			return;
		}
		
		defaultPalette = map.getPalette();
		legendColors = defaultPalette.getColors();
		int count = legendColors.length;
		legendLevels = new double[count + 1];

		for (int i = 0; i < count; i++)
			try {
				legendLevels[i] = map.getIntervalStart(i);
			} catch (Exception e) {
				Logger.error("Exception in FastTilePlot.updateColorMap: " + e.getMessage());
				e.printStackTrace();
			}

		try {
			legendLevels[count] = map.getMax();
		} catch (Exception e) {
			Logger.error("Exception in FastTilePlot.updateColorMap: " + e.getMessage());
			e.printStackTrace();
			Logger.error("FastTilePlot's updateColorMap method "+ e.getMessage());
			return;
		}
	}

	/**
	 * Gets this Plot's configuration data.
	 * 
	 * @return this Plot's configuration data.
	 */

	public PlotConfiguration getPlotConfiguration() {
		return tilePlot.getPlotConfiguration();
	}

	/**
	 * Gets a BufferedImage of the plot.
	 * 
	 * @return a BufferedImage of the plot.
	 */

	public BufferedImage getBufferedImage() {
		return getBufferedImage(getWidth(), getHeight());
	}

	/**
	 * Gets a BufferedImage of the plot.
	 * 
	 * @param width
	 *            the width of the image in pixels
	 * @param height
	 *            the height of the image in pixels
	 * @return a BufferedImage of the plot.
	 */

	public BufferedImage getBufferedImage(int width, int height) {
		return bImage;
	}

	/**
	 * Adds the specified PlotListener.
	 * 
	 * @param listener
	 *            the plot listener to add
	 */

	public void addPlotListener(PlotListener listener) {
		eventProducer.addListener(listener);
	}

	/**
	 * Removes the specified PlotListener.
	 * 
	 * @param listener
	 *            the plot listener to remove
	 */

	public void removePlotListener(PlotListener listener) {
		eventProducer.removeListener(listener);
	}

	@Override
	public int print(Graphics g, PageFormat pf, int pageIndex)
			throws PrinterException {
		if (pageIndex != 0) {
			return NO_SUCH_PAGE;
		}
		Graphics2D g2 = (Graphics2D) g;
		double x = pf.getImageableX();
		double y = pf.getImageableY();
		double w = pf.getImageableWidth();
		double h = pf.getImageableHeight();
		BufferedImage img = getBufferedImage();
		
		// scale image to fit paper while maintaining original aspect ratio
		double origRatio = (double) getWidth() / (double) getHeight();
		double paperRatio = w / h;
		
		if (origRatio > paperRatio) {
			// constrain width
			h = w / origRatio;
		} else {
			// constrain height
			w = h * origRatio;
		}

		g2.drawImage(img, (int) x, (int) y, (int) w, (int) h, null);
		return PAGE_EXISTS;
	}

	private void setDataRanges() {
		DataRangeDialog dialog = new DataRangeDialog("Set Longitude and Latitude Ranges",
				MeshPlot.this, firstColumn + columnOrigin, lastColumn + columnOrigin, firstRow + rowOrigin, lastRow + rowOrigin);
		dialog.showDialog();
	}

	private class DataRangeDialog extends JDialog {
		private static final long serialVersionUID = -1110292652911018568L;
		public static final int CANCEL_OPTION = -1;
		public static final int YES_OPTION = 1;
		public static final int ERROR = 0;
		private MeshPlot plot;
		private JTextField fLonField;
		private JTextField lLonField;
		private JTextField fLatField;
		private JTextField lLatField;
		private boolean cancelled = false;
		private int lonLow, lonHigh, latLow, latHigh;

		public DataRangeDialog(String title, MeshPlot plot, int lonLow,
				int lonHigh, int latLow, int latHigh) {
			super.setTitle(title);
			super.setLocation(getCenterPoint(plot));
			super.setModal(true);
			super.setPreferredSize(new Dimension(400, 300));
			this.lonLow = lonLow;
			this.lonHigh = lonHigh;
			this.latLow = latLow;
			this.latHigh = latHigh;
			this.fLonField = new JTextField("1", 4);
			this.lLonField = new JTextField("1", 4);
			this.fLatField = new JTextField("1", 4);
			this.lLatField = new JTextField("1", 4);
			this.plot = plot;
			this.getContentPane().add(createLayout());
		}

		public int showDialog() {
			this.pack();
			this.setVisible(true);

			if (this.cancelled)
				return CANCEL_OPTION;

			try {
				lonLow = Integer.valueOf(fLonField.getText());
				lonHigh = Integer.valueOf(lLonField.getText());
				latLow = Integer.valueOf(fLatField.getText());
				latHigh = Integer.valueOf(lLatField.getText());
				
				//TODO - move this into resetRowsNColumns
				//plot.resetRowsNColumns(firstRow, lastRow, firstColumn,
						//lastColumn);
				
				System.out.println("Setting latlon, screenWidth " + screenWidth + " height " + screenHeight);
				
				double clippedWidth = (lonHigh - lonLow) / RAD_TO_DEG;
				double clippedHeight = (latHigh - latLow) / RAD_TO_DEG;
				previousClippedDataRatio = clippedDataRatio;
				clippedDataRatio = clippedWidth / clippedHeight;
				previousZoomFactor = zoomFactor;
				double originalZoom = screenWidth / dataWidth;
				double compositeZoom;
				if (clippedDataRatio > 1) {
					compositeZoom = screenWidth / clippedWidth;
					visibleDataWidth = clippedWidth;
					visibleDataHeight = visibleDataWidth / clippedDataRatio;
				}
				else {
					compositeZoom = screenHeight / clippedHeight;
					visibleDataHeight = clippedHeight;
					visibleDataWidth = visibleDataHeight * clippedDataRatio;
				}
				zoomFactor = compositeZoom / originalZoom;
				
				firstRow = latLow - rowOrigin;
				lastRow = latHigh - rowOrigin;
				firstColumn = lonLow - columnOrigin;
				lastColumn = lonHigh - columnOrigin;
				
				previousPanX = panX;
				previousPanY = panY;
				
				panX = firstColumn / RAD_TO_DEG;
				panY = dataHeight - lastRow / RAD_TO_DEG;
												
				computeDerivedAttributes();
				draw();

				return YES_OPTION;
			} catch (NumberFormatException e) {
				Logger.error("Number Format Exception in FastTilePlot.showDialog: Set Rows and Columns: " + e.getMessage());
			}

			return ERROR;
		}

		private JPanel createLayout() {
			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
			panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

			panel.add(createMiddlePanel());
			panel.add(createButtonsPanel());

			return panel;
		}

		private JPanel createMiddlePanel() {
			GridBagLayout gridbag = new GridBagLayout();
			GridBagConstraints c = new GridBagConstraints();
			JPanel contentPanel = new JPanel(gridbag);

			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;
			c.insets = new Insets(1, 1, 7, 5);

			JLabel lonLabel = new JLabel("Longitude:");
			JPanel lonPanel = new JPanel();
			lonPanel.add(fLonField, BorderLayout.LINE_START);
			fLonField.setText(this.lonLow + "");
			lonPanel.add(new JLabel("..."));
			lonPanel.add(lLonField, BorderLayout.LINE_END);
			lLonField.setText(this.lonHigh + "");
			JLabel holder1 = new JLabel();

			gridbag.setConstraints(lonLabel, c);
			gridbag.setConstraints(lonPanel, c);
			c.gridwidth = GridBagConstraints.REMAINDER; // end row
			gridbag.setConstraints(holder1, c);
			contentPanel.add(lonLabel);
			contentPanel.add(lonPanel);
			contentPanel.add(holder1);

			c.gridwidth = 1; // next-to-last in row

			JLabel latLabel = new JLabel("Latitude:");
			JPanel latPanel = new JPanel();
			latPanel.add(fLatField, BorderLayout.LINE_START);
			fLatField.setText(this.latLow + "");
			latPanel.add(new JLabel("..."));
			latPanel.add(lLatField, BorderLayout.LINE_END);
			lLatField.setText(this.latHigh + "");
			JLabel holder2 = new JLabel();

			gridbag.setConstraints(latLabel, c);
			gridbag.setConstraints(lonPanel, c);
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(holder2, c);
			contentPanel.add(latLabel);
			contentPanel.add(latPanel);
			contentPanel.add(holder2);

			return contentPanel;
		}

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
	 * Creates a popup menu for the panel.
	 * 
	 * @param properties
	 *            include a menu item for the chart property editor.
	 * @param save
	 *            include a menu item for saving the chart.
	 * @param print
	 *            include a menu item for printing the chart.
	 * @param zoom
	 *            include menu items for zooming.
	 * @return The popup menu.
	 */
	protected JPopupMenu createPopupMenu(boolean properties, boolean save,
			boolean print, boolean zoomable) {

		JPopupMenu result = new JPopupMenu("FastTile:");
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

	@Override
	public void updateTimeStep(int step) {
		processTimeChange = false;
		drawMode = DRAW_ONCE;
		previousTimestep = timestep;
		timestep = firstTimestep + step;
		
		try {
			timeLayerPanel.setTime(timestep);
		} catch (Exception e) {
			Logger.error("Exception setting time step. Time step = " + timestep + ". Is this 1-based? " + e.getMessage());
		}
		
		drawOverLays();
		processTimeChange = true;

		try {
			Thread.sleep(500); //wait for the drawing thread to finish drawing
		} catch (InterruptedException e) {
			Logger.error("Interrupted Exception in FastTilePlot.updateTimeStep: " + e.getMessage());
		}
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
			Logger.error("Invalid Range Exception in FastTilePlot getMinMaxPoints: " + e.getMessage());
		}
		return null;
	}

	private void probe(Rectangle axisRect) {
		synchronized (lock) {
			Slice slice = new Slice();
			slice.setTimeRange(timestep - firstTimestep, 1);
			if (!hasNoLayer) slice.setLayerRange(layer - firstLayer, 1);
			Axes<DataFrameAxis> axes = getDataFrame().getAxes();
			final int probeFirstColumn = axisRect.x - axes.getXAxis().getOrigin(); 
			final int probeColumns = axisRect.width + 1;
			final int probeFirstRow = axisRect.y - axisRect.height - axes.getYAxis().getOrigin();
			final int probeRows = axisRect.height + 1;

			slice.setXRange( probeFirstColumn, probeColumns );
			slice.setYRange( probeFirstRow, probeRows );

			try {
				DataFrame subsection = null;

				//			boolean isLog = false;		// isLog is not used
				double logBase = 10.0;
				ColorMap map = (ColorMap) config.getObject(TilePlotConfiguration.COLOR_MAP);
				if (map != null) {
					// set log related info
					ColorMap.ScaleType iType = map.getScaleType();
					if ( iType == ColorMap.ScaleType.LOGARITHM ) {
						//					isLog = true;
						logBase = map.getLogBase();
					}
				}			

				
				if ( statisticsMenu.getSelectedIndex() != 0 ) {
					// HACK: copy and overwrite subsection.array with subsetLayerData:
					//TODO - implement this for mesh plots
					/*
					subsection = getDataFrame().sliceCopy( slice );
					final int probeLastColumn = probeFirstColumn + probeColumns -1 ;
					final int probeLastRow = probeFirstRow + probeRows - 1 ;
					final ucar.ma2.Array array = subsection.getArray();
					final ucar.ma2.Index index = array.getIndex();

					for ( int row = probeFirstRow; row <= probeLastRow; ++row ) {
						final int sliceRow = (probeFirstRow - firstRow) + (row - probeFirstRow) - 1;
						final int sliceRowIndex = (row - probeFirstRow);
						index.set2( sliceRowIndex );

						for ( int column = probeFirstColumn; column <= probeLastColumn; ++column ) {
							final int sliceColumn = (probeFirstColumn - firstColumn) +  (column - probeFirstColumn) -1;
							final int sliceColumnIndex = (column - probeFirstColumn);
							index.set3( sliceColumnIndex );
							final float value = subsetLayerData[ sliceRow ][ sliceColumn ];
							array.setFloat( index, value );
						}
					}
					*/

				} else {
					subsection = getDataFrame().slice(slice);
				}

				probedSlice = slice;
				enableProbeItems(true);
				ProbeEvent ent = new ProbeEvent(this, subsection, slice, Formula.Type.TILE);	// 2014 fixed code not knowing what TILE meant
				ent.setIsLog( false); //isLog); // JIZHEN: always set to false, take log inside this class
				ent.setLogBase( logBase);
				eventProducer.fireProbeEvent(ent);//new ProbeEvent(this, subsection, slice, TILE));
			} catch (InvalidRangeException e) {
				Logger.error("Invalid Range Exception in FastTilePlot.Probe: " + e.getMessage());
			}
		}
	}

	private void requestTimeSeries(Formula.Type type) {
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
			Logger.error("InvalidRangeException in FastTilePlot.requestTimeSeries: " + e1.getMessage());
		}
	}

	private void requestTimeSeries(Set<Point> points, String title) {
		MultiTimeSeriesPlotRequest request = new MultiTimeSeriesPlotRequest(title);
		for (Point point : points) {
			Slice slice = new Slice();
			// slice needs to be in terms of the actual array indices
			// of the frame, but the axes ranges refer to the range
			// of the original dataset. So, the origin will always
			// be 0 and the exent is the frame's exent.
			slice.setTimeRange(0, getDataFrame().getAxes().getTimeAxis().getExtent());
			DataFrameAxis frameAxis = getDataFrame().getAxes().getZAxis();
			if (frameAxis != null) slice.setLayerRange(0, frameAxis.getExtent());
			slice.setXRange(point.x, 1);
			slice.setYRange(point.y, 1);
			try {
				DataFrame subsection = getDataFrame().slice(slice);
				request.addItem(subsection);
			} catch (InvalidRangeException e1) {
				Logger.error("InvalidRangeException in FastTilePlot.requestTimeSeries: " + e1.getMessage());
			}
		}
		eventProducer.firePlotRequest(request);
	}

	/**
	 * Enables / disables the menu items that work with
	 * the currently probed point.
	 *
	 * @param val true to enable
	 */
	protected void enableProbeItems(boolean val) {
		for (JMenuItem item : probeItems) {
			item.setEnabled(val);
		}
	}

	public Decidegrees getLatLonFor(int screenx,int screeny){
		int yHeightOffset = (int)(dataArea.getHeight() + dataArea.getY());
		int xOffset=(int)dataArea.getX();
		int width=(int)dataArea.getWidth();
		int height=(int)dataArea.getHeight();
		    
		double xMinimum = gridBounds[ 0 ][ 0 ];
		double xMaximum = gridBounds[ 0 ][ 1 ];
		double yMinimum = gridBounds[ 1 ][ 0 ];
		double yMaximum = gridBounds[ 1 ][ 1 ];
		
		double xRange   = xMaximum - xMinimum;
		double yRange   = yMaximum - yMinimum;
		double xScale   = width  / xRange;
		double yScale   = height / yRange;
		  
		double tx = (screenx-xOffset+0.5)/xScale+xMinimum;
		double ty = -(screeny-yHeightOffset+0.5)/yScale+yMinimum;					
		double[] longitudeLatitude = { 0.0, 0.0 };
		
		projector.unproject(tx, ty, longitudeLatitude);
		
		return new Decidegrees(longitudeLatitude[1], longitudeLatitude[0]);
	}
	
	public String createAreaString(int[] point) {
		Point4i p = new Point4i();
		p.w = point[0];
		p.z = point[1];
		p.x = point[2];
		p.y = point[3];
		return formatPointLatLon(p);
	}
	
	public String formatPoint(Point4i point) {
		StringBuilder builder = new StringBuilder("(");
		int[] vals = new int[4];
//		vals[0] = point.getW();
//		vals[1] = point.getZ();
//		vals[2] = point.getX();
//		vals[3] = point.getY();
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
	
	
	public static void main(String[] args) {
		System.out.println("Hello there");
		//ARGB String: Color 1 -16777194 color 2 -1677717 from 22 / 44
		//BufferedImage imgMap = new java.awt.image.BufferedImage(10, 10, java.awt.image.BufferedImage.TYPE_INT_ARGB);
		//BufferedImage imgMap = new java.awt.image.BufferedImage(10, 10, java.awt.image.BufferedImage.TYPE_3BYTE_BGR);
		BufferedImage imgMap = new java.awt.image.BufferedImage(10, 10, java.awt.image.BufferedImage.TYPE_INT_RGB);
		Graphics g = imgMap.getGraphics();
		g.setColor(new Color(22));
		g.fillRect(0,  0,  2,  2);
		g.setColor(new Color(44));
		g.fillRect(2, 0, 2, 2);
		g.setColor(new Color(199990));
		g.fillRect(3, 0, 2, 2);
		System.out.println("Color 1 " + (imgMap.getRGB(0,  0) + COLOR_BASE) + " color 3 " + (imgMap.getRGB(3, 0) + COLOR_BASE));
		System.out.println("Color 1 " + imgMap.getRGB(0,  0) + " color 3 " + imgMap.getRGB(3, 0));

	}
	
	private int getCellIdByCoord(int x, int y) {
		return cellIdMap.getRGB(x, y) + COLOR_BASE;
	}
	
	private String formatPointLatLon(Point4i point) {
		double lonCoord = ((point.x / compositeFactor) + panX) * RAD_TO_DEG + columnOrigin;
		double latCoord = (dataHeight - ((point.y / compositeFactor)  + panY )) * RAD_TO_DEG + rowOrigin;
		
		String lonSuffix = "E";
		String latSuffix = "N";
		if (lonCoord < 0) {
			lonCoord *= -1;
			lonSuffix = "W";
		}
		if (latCoord < 0) {
			latCoord *= -1;
			latSuffix = "S";
		}
		String ret = "(" + coordFormat.format(lonCoord) + lonSuffix + ", " + coordFormat.format(latCoord) + latSuffix + ")"; 
		//ret += " xy " + point.x + "," + point.y;

		try {
			int hoveredId = getCellIdByCoord(point.x, point.y);
			CellInfo cell = cellIdInfoMap.get(hoveredId);
			if (cell != null) {
				ret += cell.getElevation() + " " + coordFormat.format(cell.getValue()) + unitString;

			}
		} catch (ArrayIndexOutOfBoundsException e) {
			ret += "ArrayIndexOutOfBoundsException";
		}
		return ret;
	}
	
	protected Point2D getLatLonForAxisPoint(Point axisPoint) {
		//Since the netcdf boxer used middle of the grid as origin of the grid
		//FastTilePlot use SW corner as the origin of the grid, hence the minus 1
//		return getDataFrame().getAxes().getBoundingBoxer().axisPointToLatLonPoint(axisPoint.x-1, axisPoint.y-1); 
		return getDataFrame().getAxes().getBoundingBoxer().axisPointToLatLonPoint(axisPoint.x, axisPoint.y); //NOTE: the shift has been considered for in the netcdf boxer!!!
	}
	
	class AreaFinder extends MouseInputAdapter {
		
		private Point mpStart, mpEnd;

		// this rect measured axis coordinates
		private Rectangle rect;

		public void mousePressed(MouseEvent e) {
			if (isInDataArea(e)) {
				mpStart = e.getPoint();
				rect = new Rectangle(mpStart, new Dimension(0, 0));
				eventProducer.fireAreaSelectionEvent(new AreaSelectionEvent(rect, createAreaString(rect),
								false));
			} else {
				mpStart = null;
			}
		}

		public void mouseDragged(MouseEvent e) {
			if (mpStart != null) {
				if (isInDataArea(e)) {
					mpEnd = e.getPoint();
					rect.width = mpEnd.x - rect.x;
					rect.height = rect.y - mpEnd.y;
					
					boolean finished = rect.width < 0 || rect.height < 0;
					eventProducer.fireAreaSelectionEvent(new AreaSelectionEvent(rect, createAreaString(rect),
									finished));
				} else {
					eventProducer.fireAreaSelectionEvent(new AreaSelectionEvent(rect, true));
				}
			}
		}

		public void mouseMoved(MouseEvent e) {
			if (isInDataArea(e)) {
				Point p = new Point(getCol(e.getPoint()), getRow(e.getPoint()));
				Rectangle rect = new Rectangle(p.x, p.y, 0, 0);
				eventProducer.fireAreaSelectionEvent(new AreaSelectionEvent(rect, createAreaString(rect), false));
			} else {
				eventProducer.fireAreaSelectionEvent(new AreaSelectionEvent(new Rectangle(0, 0, 0, 0), true));
			}
		}

		public void mouseExited(MouseEvent e) {
			eventProducer.fireAreaSelectionEvent(new AreaSelectionEvent(new Rectangle(0, 0, 0, 0), true));
		}

		public void mouseReleased(MouseEvent e) {
			if (mpStart != null) {
				if (isInDataArea(e)) {
					mpEnd = e.getPoint();
					rect.width = mpEnd.x - rect.x;
					rect.height = mpEnd.y - rect.y;

					if (probe)
						probe(rect);
					else {
						int mod = e.getModifiers();
						int mask = MouseEvent.BUTTON3_MASK;
						boolean rightclick = (mod & mask) != 0;
						
						zoom(rightclick, !rightclick, false, mpEnd.x < mpStart.x || mpEnd.y < mpStart.y, false, rect);
					}
				}
				
				eventProducer.fireAreaSelectionEvent(new AreaSelectionEvent(rect, true));
			}
		}

		private String createAreaString(Rectangle rect) {
			Point4i[] points = rectToPoints(rect);
			return createLonLatAreaString(points);
		}
		
		private Point4i[] rectToPoints(Rectangle rect) {
			Point4i[] points = new Point4i[2];
			// rect is x,y
			Point4i point = new Point4i(rect.x, rect.y, NO_VAL, NO_VAL);
			points[0] = point;

			if (rect.getWidth() > 0 || rect.getHeight() > 0) {
				point = new Point4i(rect.x + rect.width, rect.y - rect.height, NO_VAL, NO_VAL);
				points[1] = point;
			}
			return points;
		}
		
		// rect is in axis coords
		private String createAxisCoordAreaString(Point4i[] points) {
			return createLonLatAreaString(points);
		}
		
		// rect is in axis coordinates
		private String createLonLatAreaString(Point4i[] points) {
			StringBuilder builder = new StringBuilder();
			builder.append(formatPointLatLon(points[0]));
			if (points[1] != null) {
				builder.append(" - ");
				builder.append(formatPointLatLon(points[1]));
			}
			return builder.toString();
		}
	}

//	2014 called from anl.verdi.plot.util.PlotExporter; disabling exporting of Shapefiles for VERDI v1.5.0
//		public void exportShapefile( String baseFileName ) throws IOException {
//		final int subsetLayerRows = 1 + lastRow - firstRow;
//		final int subsetLayerColumns = 1 + lastColumn - firstColumn;
//		// Filter variable name/expression so operators aren't a problem in Excel:
//		
//		// changed to this in v. 529
//		final int end = variable.indexOf( '[' );
//		final String filteredVariableName = variable.substring( 0, end );
//		
//		// change back now 2012-06-14
////		final String filteredVariableName =
////			variable.replaceAll( "[\\[\\d\\]]", "" ).replaceAll( "\\W", "" );
//		
//		final double subsetWestEdge = westEdge + firstColumn * cellWidth;
//		final double subsetSouthEdge = southEdge + firstRow * cellWidth;
//		GridShapefileWriter.write( baseFileName,
//				subsetLayerRows, subsetLayerColumns,
//				subsetWestEdge, subsetSouthEdge,
//				cellWidth, cellHeight,
//				filteredVariableName, subsetLayerData, projector );
//	}
	
	public void exportASCIIGrid( String baseFileName ) {
		final int subsetLayerRows = 1 + lastRow - firstRow;
		final int subsetLayerColumns = 1 + lastColumn - firstColumn;
		final double subsetWestEdge = westEdge + firstColumn * cellWidth;
		final double subsetSouthEdge = southEdge + firstRow * cellWidth;
		float[][] exportCellData = new float[1][];
		exportCellData[0] = subsetLayerData;
		ASCIIGridWriter.write( baseFileName + ".asc",
		1, cellsToRender.length,
		subsetWestEdge, subsetSouthEdge,
		cellWidth, exportCellData );
	}

//	public void exportASCIIGrid( String baseFileName ) {
//		final int subsetLayerRows = 1 + lastRow - firstRow;
//		final int subsetLayerColumns = 1 + lastColumn - firstColumn;
//		ASCIIGridWriter.write( baseFileName + ".asc",
//								subsetLayerRows, subsetLayerColumns,
//								westEdge, southEdge,
//								cellWidth, subsetLayerData );
//	}

	public void exportEPSImage(String filename) {
		int width = (getWidth() <= 0) ? 800 : getWidth();
		int height = (getHeight() <= 0) ? 600 : getHeight();
		
		exportEPSImage(filename, width, height);
	}
	
	public void exportEPSImage(String filename, int width, int height) {
		EpsRenderer renderer = new EpsRenderer(width, height);
		EpsTools.createFromDrawable(renderer, filename, width, height, ColorMode.COLOR_RGB);
	}
	
	class EpsRenderer implements Drawable {
		final int canvasWidth, canvasHeight;
		
		public EpsRenderer(int width, int height) {
			canvasWidth = width;
			canvasHeight = height;
		}
		
		@Override
		public void draw(Graphics2D g, Rectangle2D rect) {
			// around plot window.

			String sTitle1 = config.getSubtitle1();
			String sTitle2 = config.getSubtitle2();
			Font tFont = config.getFont(PlotConfiguration.TITLE_FONT);
			Font sFont1 = config.getFont(PlotConfiguration.SUBTITLE_1_FONT);
			Font sFont2 = config.getFont(PlotConfiguration.SUBTITLE_2_FONT);
			int fontSize = (tFont == null) ? 20 : tFont.getSize();
			int yOffset = 20 + fontSize;

			if (sTitle1 != null && !sTitle1.trim().isEmpty()) {
				fontSize = (sFont1 == null) ? 20 : sFont1.getSize();
				yOffset += fontSize + 6;
			}

			if (sTitle2 != null && !sTitle2.trim().isEmpty()) {
				fontSize = (sFont2 == null) ? 20 : sFont2.getSize();

				if (sTitle1 == null || sTitle1.trim().isEmpty()) {
					yOffset += 26;
				}

				yOffset += fontSize + 6;
			}

			final int xOffset = 100;

			g.setColor(Color.white);
			g.fillRect(0, 0, canvasWidth, canvasHeight);

			// Draw legend-colored grid cells, axis, text labels and
			// legend:

			final Boolean showGridLines = (Boolean)
				config.getObject( TilePlotConfiguration.SHOW_GRID_LINES );
			final Color gridLineColor = (Color)
				( ( showGridLines == null || showGridLines == false ) ? null
					: config.getObject( TilePlotConfiguration.GRID_LINE_COLOR ) );
				
			final int stepsLapsed = timestep - firstTimestep;
			try {
				tilePlot.drawBatchImage(g, xOffset, yOffset,
							canvasWidth, canvasHeight, stepsLapsed, layer, firstRow,
							lastRow, firstColumn, lastColumn, legendLevels,
							legendColors, axisColor, labelColor, variable,
							((units==null || units.trim().equals("")) ? "none" : units), config, map.getNumberFormat(), gridLineColor,
							null);
			} catch (Exception e) {
				Logger.error("Exception in FastTilePlot.Draw (EpsRenderer's draw method): " + e.getMessage());
				e.printStackTrace();
				return;
			}
				
			// Draw projected/clipped map border lines over grid cells:

			g.setClip(xOffset, yOffset, screenWidth, screenHeight);
			mapper.draw(domain, gridBounds, projector,
						g, xOffset, yOffset, tilePlot.getPlotWidth(),
						tilePlot.getPlotHeight(), withHucs, withRivers, withRoads);
			g.setClip(null);
			
			if (obsAnnotations != null) {
				for (ObsAnnotation ann : obsAnnotations)
					ann.draw(g, xOffset, yOffset, tilePlot.getPlotWidth(), tilePlot.getPlotHeight(), 
							legendLevels, legendColors, projector, domain, gridBounds);
			}
			
			if (vectAnnotation != null) {
				vectAnnotation.draw(g, xOffset, yOffset, tilePlot.getPlotWidth(), tilePlot.getPlotHeight(), 
						firstRow, lastRow, firstColumn, lastColumn);
			}
		}
	}
	
	public void addVectorAnnotation(VectorEvaluator eval) {
		vectAnnotation = new VectorAnnotation(eval, timestep, getDataFrame().getAxes().getBoundingBoxer());
	}

	public void addObservationData(DataManager manager, boolean showLegend) {
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
					if (subtitle1.indexOf(temp) < 0 && !subtitles.contains(temp)) subtitles.add(temp);
				}
			}
			
			tilePlot.setObsLegend(obsAnnotations, showLegend);
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
			drawMode = DRAW_NONE;
		}
	}
	
	public List<OverlayObject> getObservationData() {
		return this.obsData;
	}
	
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
			drawMode = DRAW_NONE;
		}
	}
	
	private void setOverlayErrorMsg(String msg) {
		if (msg == null) msg = "";
		JOptionPane.showMessageDialog(app.getGui().getFrame(), "Please check if the overlay time steps match the underlying data.\n" + msg, "Overlay Error", JOptionPane.ERROR_MESSAGE, null);
	}

	public String getTitle() {
		return tilePlot.getTitle();
	}
	
	private void calculateDataFrameLog() {
		if ( this.dataFrame == null) {
			return;
		}
		
		boolean doDebug = false;
		
		this.dataFrameLog = DataUtilities.createDataFrame( this.dataFrame);
 
		if ( doDebug) {
			Logger.debug( "debug print 1:");
		}
		
		int count = 0;	    	
		IndexIterator iter2 = this.dataFrameLog.getArray().getIndexIterator();
		IndexIterator iter1 = this.dataFrame.getArray().getIndexIterator();
		if ( doDebug) 
			Logger.debug( "debug print 2:");
		float val1, val2;
		while (iter2.hasNext()) {
			val1 = iter1.getFloatNext(); 
			val2 = iter2.getFloatNext(); 
			if ( doDebug && count<100) 
				Logger.debug( "" + val1 + " " + val2);
			val2 = (float)(Math.log(val1) / Math.log( this.logBase));
			iter2.setFloatCurrent( (float)( val2));

			val2 = iter2.getFloatCurrent();

			if ( doDebug && count++<100) 
				Logger.debug( " : " + val1 + " " + val2);
		}
	}
	
	protected DataFrame getDataFrame() {
		if ( this.log) {
			return this.dataFrameLog;
		} else {
			return this.dataFrame;
		}
	}
	
	protected DataFrame getDataFrame(boolean log) {
		if (log) {
			return this.dataFrameLog;
		} else {
			return this.dataFrame;
		}
	}
	
	public void showBusyCursor() {
		//  show busy cursor
		if (app!=null && app.getGui()!=null) {
			app.getGui().busyCursor(); 
		}
		synchronized(this) {
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		}
	}
	
	public void restoreCursor() {
		//   cursor restored
		//synchronized(this) {
			app.getGui().defaultCursor(); 
//		}
		if ( zoom) 
			setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		else 
			setCursor(Cursor.getDefaultCursor());
	}
	
	private void increase_draw_once_requests() {
		draw_once_requests ++;
		//System.err.println("Increased requests to " + draw_once_requests + " from " + Thread.currentThread().getStackTrace()[3]);
	}
	
	private void decrease_draw_once_requests() {
		draw_once_requests --;
		//System.err.println("Decreased requests to " + draw_once_requests);
	}
	
	private int get_draw_once_requests() {
		//System.err.println("Got " + draw_once_requests + " requests from " + Thread.currentThread().getStackTrace()[3]);
		return draw_once_requests;
	}

}
