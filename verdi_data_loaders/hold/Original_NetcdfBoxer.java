package anl.verdi.loaders;

import java.awt.geom.Point2D;
import java.io.IOException;

import org.geotools.factory.Hints;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
//import org.geotools.referencing.FactoryFinder;
import org.geotools.referencing.ReferencingFactoryFinder;
//import org.geotools.referencing.factory.FactoryGroup;
import org.geotools.referencing.factory.ReferencingFactoryContainer;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransformFactory;

import simphony.util.messages.MessageCenter;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionPointImpl;
import ucar.unidata.geoloc.projection.LambertConformal;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.geoloc.projection.Mercator;
import ucar.unidata.geoloc.projection.Stereographic;
import ucar.unidata.geoloc.projection.UtmProjection;
import anl.verdi.core.VerdiConstants;
import anl.verdi.data.BoundingBoxer;

/**
 * Bounding boxer that uses netcdf to create the bounding box.
 * 
 * @author Nick Collier
 * @version $Revision$ $Date$
 */
public class CopyOfNetcdfBoxer implements BoundingBoxer {

	private static MessageCenter msg = MessageCenter.getMessageCenter(BoundingBoxer.class);

//	MathTransformFactory mtFactory = FactoryFinder.getMathTransformFactory(null);
	MathTransformFactory mtFactory = ReferencingFactoryFinder.getMathTransformFactory(null);

//	FactoryGroup factories = new FactoryGroup(null);
	ReferencingFactoryContainer factories = new ReferencingFactoryContainer(null);

	private GridDatatype grid;
	private CoordinateReferenceSystem crs;
	private boolean isLatLon;

	public CopyOfNetcdfBoxer(GridDatatype grid) {
		this.grid = grid;
		this.isLatLon = grid.getCoordinateSystem().isLatLon();
	}
	
	/**
	 * Gets the Projection associated with this BoundingBoxer.
	 * 
	 * @return the Projection associated with this BoundingBoxer.
	 */
	public Projection getProjection() {
		return grid.getCoordinateSystem().getProjection();
	}

	/**
	 * Converts the grid cell coordinate to a lat / lon coordinate.
	 * 
	 * @param x
	 *            the x value
	 * @param y
	 *            the y value
	 * @return the lat / lon coordinate.
	 */
	public Point2D axisPointToLatLonPoint(int x, int y) {
		CoordinateAxis1D xaxis = getXAxis();
		CoordinateAxis1D yaxis = getYAxis();
		// coordVal is the sw corner, so coordEdge + 1 should be the center
		// of that cell.
		double xVal = xaxis.getCoordEdge(x + 1);
		double yVal = yaxis.getCoordEdge(y + 1);
		if (isLatLon) {
			return new Point2D.Double(xVal, yVal);
		} else {
			Projection proj = grid.getCoordinateSystem().getProjection();
			LatLonPointImpl latLon = new LatLonPointImpl();
			proj.projToLatLon(new ProjectionPointImpl(xVal, yVal), latLon);
			return new Point2D.Double(latLon.getLongitude(), latLon
					.getLatitude());
		}
	}

	/**
	 * @param lat
	 *            latitude
	 * @param lon
	 *            longitude
	 * @return the location on the x and y axis if the latLon is with the grid,
	 *         otherwise (-1, -1).
	 */
	public Point2D latLonToAxisPoint(double lat, double lon) {
		Projection proj = grid.getCoordinateSystem().getProjection();
		ProjectionPointImpl point = new ProjectionPointImpl();
		proj.latLonToProj(new LatLonPointImpl(lat, lon), point);

		CoordinateAxis1D xaxis = getXAxis();
		CoordinateAxis1D yaxis = getYAxis();
		double x = xaxis.findCoordElement(point.x);
		double y = yaxis.findCoordElement(point.y);
		if (x != -1 && y != -1) {
			double leftEdge = xaxis.getCoordValue((int) x);
			double temp = (float) point.x; //NOTE: leftEdge is acutally a float value
			if (leftEdge == 0.0) temp = (float) Math.round(temp); //NOTE: rounding gets pretty silly here
			
			if (leftEdge > temp) {
				x -= 1;
			}

			double bottomEdge = yaxis.getCoordValue((int) y);
			temp = (float) point.y; //NOTE: bottomEdge is actually a float value
			if (bottomEdge == 0.0) temp = (float) Math.round(temp);
			
			if (bottomEdge > temp) {
				y -= 1;
			}
		}
		
		return new Point2D.Double(x, y);
	}

	public Point2D CRSPointToAxis(double x, double y) {
		CoordinateAxis1D xaxis = getXAxis();
		CoordinateAxis1D yaxis = getYAxis();
		x = x / 1000;
		y = y / 1000;
		x -= (xaxis.getIncrement() / 2);
		y -= (yaxis.getIncrement() / 2);
		int xCell = xaxis.findCoordElement(x);
		int yCell = yaxis.findCoordElement(y);
		if (xCell == -1) {
			double bottomX = xaxis.getCoordValue(0);
			if (x < bottomX) {
				xCell = 0;
			} else {
				xCell = xaxis.getCoordValues().length - 1;
			}
		}
		if (yCell != -1) {
			yCell = (yaxis.getCoordValues().length - 1) - yCell;
		} else {
			double bottomY = yaxis.getCoordValue(0);
			if (y > bottomY) {
				yCell = 0;
			} else {
				yCell = yaxis.getCoordValues().length - 1;
			}
		}
		return new Point2D.Double(xCell, yCell);
	}

	/**
	 * Creates a bounding box from the specified ranges. The xRange and yRange
	 * are specified in terms of x / y cell coordinates.
	 * 
	 * @param xMin
	 * @param xMax
	 * @param yMin
	 * @param yMax
	 * @return the created bounding box.
	 */
	public ReferencedEnvelope createBoundingBox(double xMin, double xMax,
			double yMin, double yMax, int netcdfConv) {
System.out.println("in ReferencedEnvelope; printing parameter values");
System.out.println("xMin = " + xMin);
System.out.println("xMax = " + xMax);
System.out.println("yMin = " + yMin);
System.out.println("yMax = " + yMax);
System.out.println("netcdfConv = " + netcdfConv);
		CoordinateAxis1D xaxis = getXAxis();
System.out.println("xaxis = " + xaxis.toString());
		CoordinateAxis1D yaxis = getYAxis();
System.out.println("yaxis = " + yaxis.toString());
System.out.println("Axis limits:");
System.out.println("xaxis.getStart = " + xaxis.getStart());
System.out.println("xaxis.getIncrement = " + xaxis.getIncrement());
System.out.println("xaxis.getMinValue = " + xaxis.getMinValue());
System.out.println("xaxis.getMaxValue = " + xaxis.getMaxValue());
System.out.println("yaxis.getStart = " + yaxis.getStart());
System.out.println("yaxis.getIncrement = " + yaxis.getIncrement());
System.out.println("yaxis.getMinValue = " + yaxis.getMinValue());
System.out.println("yaxis.getMaxValue = " + yaxis.getMaxValue());
		
		// 
		double xStart, xEnd, yStart, yEnd; //

		// latlon coord does not need to be scaled
		double scaler = isLatLon ? 1.0 : 1000.0;
System.out.println("scaler = " + scaler);
		double xInc = xaxis.getIncrement() * scaler;
System.out.println("xInc = " + xInc);
		double yInc = yaxis.getIncrement() * scaler;
System.out.println("yInc = " + yInc);
		
		int limit = xaxis.getCoordValues().length - 1;
System.out.println("limit = " + limit);
		double start = xaxis.getStart() * scaler;
System.out.println("start = " + start);
		double end = (xaxis.getCoordValue(limit) + xaxis.getIncrement()) * scaler;
System.out.println("end = " + end);
		
		xStart = start + xMin * xInc;
System.out.println("xStart = start + xMin * xInc = " + xStart );
		xEnd = end - ((limit - xMax) * xInc);
System.out.println("xEnd = end - ((limit - xMax) * xInc) = " + xEnd);

		if ( netcdfConv == VerdiConstants.NETCDF_CONV_ARW_WRF) { // JIZHEN-SHIFT
System.out.println("in recompute code section");
			xStart = xStart - xaxis.getIncrement() * scaler * 0.5;
System.out.println("xStart now set to:xStart - xaxis.getIncrement() * scaler * 0.5 = " + xStart );
			//xEnd = xEnd - xaxis.getIncrement() * scaler * ( 0.5 + 1);
			xEnd = xEnd - xaxis.getIncrement() * scaler * 0.5;
System.out.println("xEnd now set to:  xEnd - xaxis.getIncrement() * scaler * 0.5 = " + xEnd);
		}

		limit = yaxis.getCoordValues().length - 1;
System.out.println("limit = yaxis.getCoordValues().length - 1 = " + limit);
		start = yaxis.getStart() * scaler;
System.out.println("start = yaxis.getStart() * scaler = " + start );
		end = (yaxis.getCoordValue(limit) + yaxis.getIncrement()) * scaler;
System.out.println("end = (yaxis.getCoordValue(limit) + yaxis.getIncrement()) * scaler = " + end);

		yStart = start + yMin * yInc;
System.out.println("yStart =start + yMin * yInc = " + yStart );
		yEnd = end - ((limit - yMax) * yInc);
System.out.println("yEnd = end - ((limit - yMax) * yInc) = " + yEnd);
		if ( netcdfConv == VerdiConstants.NETCDF_CONV_ARW_WRF) { // JIZHEN-SHIFT
System.out.println("within if block:");
			yStart = yStart - yaxis.getIncrement() * scaler * 0.5; // bottom_left
System.out.println("yStart = yStart - yaxis.getIncrement() * scaler * 0.5 = " + yStart);
			//yEnd = yEnd - yaxis.getIncrement() * scaler * ( 0.5 + 1);
			yEnd = yEnd - yaxis.getIncrement() * scaler * 0.5; // top_right
System.out.println("yEnd = yEnd - yaxis.getIncrement() * scaler * 0.5 = " + yEnd);
		}
//		Hints hints = new Hints(Hints.COMPARISON_TOLERANCE, 1E-9);	// TODO: 2014  probably need to do in beginning of VERDI
		Hints.putSystemDefault(Hints.COMPARISON_TOLERANCE, 10e-9);

		Projection proj = grid.getCoordinateSystem().getProjection();

		if (proj instanceof LambertConformal) {
System.out.println("proj = " + proj.toString() + '\n' + "  projection is of type LambertConformal");
			if (crs == null) {
System.out.println("NOTE: crs is null");
				try {
System.out.println("within try/catch block");
					String strCRS = new LambertWKTCreator().createWKT((LambertConformal) proj);
System.out.println("created strCRS = " + strCRS);
System.out.println("Ready to call CRS.parseWKT");
					crs = CRS.parseWKT(strCRS);	// NOTE: preferred method (docs.geotools.org/stable/userguide/library/referencing/crs.html)
System.out.println("parsed CRS: " + crs.toString());
				} catch (IOException ex) {
System.out.println("into exception handling");
					System.out.println("Error while creating CRS for LambertConformal\n");
					ex.printStackTrace();
				} catch (FactoryException e) {
					System.out.println("Caught FactoryException while creating CRS for LambertConformal");
					e.printStackTrace();
				}
			}
		} else if (proj instanceof UtmProjection) {
System.out.println("projection is of type UtmProjection");
			if (crs == null) {
System.out.println("NOTE: crs is null");
				try {
System.out.println("within try/catch block");
					String strCRS = new UtmWKTCreator().createWKT((UtmProjection) proj);
System.out.println("created strCRS = " + strCRS.toString());
System.out.println("Ready to call CRS.parseWKT");
					crs = CRS.parseWKT(strCRS);
				} catch (Exception ex) {
					msg.error("Error while creating CRS for UTM", ex);
				}
			}
		} else if (proj instanceof Stereographic) {
System.out.println("projection is of type Stereographic");
			if (crs == null) {
System.out.println("NOTE: crs is null");
				try {
System.out.println("within try/catch block");
					String strCRS = new PolarStereographicWKTCreator().createWKT((Stereographic) proj);
System.out.println("created strCRS = " + strCRS.toString());
System.out.println("Ready to call CRS.parseWKT");
					crs = CRS.parseWKT(strCRS);
System.out.println("parsed CRS: " + crs.toString());
				} catch (Exception ex) {
					msg.error("Error while creating CRS for Stereographic", ex);
				}
			}
		} else if (isLatLon) {
			if (crs == null) {
				try {
					String strCRS = new LatlonWKTCreator().createWKT((LatLonProjection)proj);
					crs = CRS.parseWKT(strCRS);
				} catch (Exception ex) {
					msg.error("Error while creating CRS for Lat-Lon", ex);
				}
			}
		} else if (proj instanceof Mercator) {
			if (crs == null) {
				try {
					String strCRS = new MercatorWKTCreator().createWKT((Mercator)proj);
					crs = CRS.parseWKT(strCRS);
				} catch (Exception e) {
					msg.error("Error while creating CRS for Mercator", e);
				}
			}
		}

		// TODO: add more projections here
		else {
			msg.warn("Projection is not recognized!!");
		} 
		
		return new ReferencedEnvelope(xStart, xEnd, yStart, yEnd, crs);
	}

	private CoordinateAxis1D getXAxis() {
		GridCoordSystem gcs = grid.getCoordinateSystem();
		return (CoordinateAxis1D) gcs.getXHorizAxis();
	}

	private CoordinateAxis1D getYAxis() {
		GridCoordSystem gcs = grid.getCoordinateSystem();
		return (CoordinateAxis1D) gcs.getYHorizAxis();
	}

}

/*
 * private CoordinateReferenceSystem getLambert(LambertConformal gcs) { try {
 * String wkt = "PROJCS[\"\", GEOGCS[\"Normal Sphere (r=6371007\"," + "
 * DATUM[\"unknown\",\n" + " SPHEROID[\"SPHERE\", 6370997, 0]],\n" + "
 * PRIMEM[\"Greenwich\", 0],\n" + " UNIT[\"degree\", 0.0174532925199433],\n" + "
 * AXIS[\"Geodetic longitude\", EAST],\n" + " AXIS[\"Geodetic latitude\",
 * NORTH]]," + "PROJECTION[\"Lambert_Conformal_Conic_2SP\"],\n" + "
 * PARAMETER[\"central_meridian\", -90.0],\n" + "
 * PARAMETER[\"latitude_of_origin\", 40.0],\n" + "
 * PARAMETER[\"longitude_of_origin\", -90.0],\n" + "
 * PARAMETER[\"standard_parallel_1\", 30],\n" + "
 * PARAMETER[\"standard_parallel_2\", 60],\n" + " PARAMETER[\"false_easting\",
 * 0.0],\n" + " PARAMETER[\"false_northing\", 0.0],\n" + " UNIT[\"m\", 1.0],\n" + "
 * AXIS[\"x\", EAST],\n" + " AXIS[\"y\", NORTH]]";
 * 
 * return CRS.parseWKT(wkt); } catch (Exception e) { throw new
 * IllegalArgumentException("Unable to create coordinate reference system", e); } }
 */

/*
 * Map<String, Object> params = new HashMap<String, Object>();
 * params.put("name", "unknown"); DefaultEllipsoid sphere =
 * DefaultEllipsoid.createEllipsoid("SPHERE", 6371007.0, 6371007.0,
 * DefaultEllipsoid.SPHERE.getAxisUnit()); GeodeticDatum datum =
 * FactoryFinder.getDatumFactory(null).createGeodeticDatum(params,
 * DefaultEllipsoid.SPHERE, DefaultPrimeMeridian.GREENWICH); params = new
 * HashMap<String, Object>(); params.put("name", "Normal Sphere (r=6371007)");
 * 
 * 
 * GeographicCRS crs =
 * FactoryFinder.getCRSFactory(null).createGeographicCRS(params, datum,
 * DefaultEllipsoidalCS.GEODETIC_2D); // Lambert_Conformal_Conic_1SP (EPSG code
 * 9801) // Lambert_Conformal_Conic_2SP (EPSG code 9802) //
 * Lambert_Conic_Conformal_2SP_Belgium (EPSG code 9803) //
 * Lambert_Conformal_Conic - An alias for the ESRI 2SP case that includes a
 * scale_factor parameter ParameterValueGroup parameters =
 * mtFactory.getDefaultParameters("Lambert_Conformal_Conic_2SP");
 * parameters.parameter("standard_parallel_1").setValue(gcs.getParallelOne());
 * parameters.parameter("standard_parallel_2").setValue(gcs.getParallelTwo());
 * parameters.parameter("latitude_of_origin").setValue(gcs.getOriginLat());
 * parameters.parameter("longitude_of_origin").setValue(gcs.getOriginLon());
 * parameters.parameter("central_meridian").setValue(gcs.getOriginLon());
 * parameters.parameter("false_easting").setValue(gcs.getFalseEasting());
 * parameters.parameter("false_northing").setValue(gcs.getFalseNorthing());
 * //parameters.parameter("scale_factor").setValue(1.0); Map properties =
 * Collections.singletonMap("name", "unknown"); return
 * factories.createProjectedCRS(properties, crs, null, parameters,
 * DefaultCartesianCS.GENERIC_2D);
 */
