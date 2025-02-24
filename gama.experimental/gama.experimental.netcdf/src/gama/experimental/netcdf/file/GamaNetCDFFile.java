/*******************************************************************************************************
 *
 * msi.gama.util.file.GamaGridFile.java, in plugin msi.gama.core, is part of the source code of the GAMA modeling and
 * simulation platform (v. 1.8)
 *
 * (c) 2007-2018 UMI 209 UMMISCO IRD/SU & Partners
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.experimental.netcdf.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.Formatter;
import java.util.List;

import javax.swing.JOptionPane;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.DataSourceException;
import org.geotools.data.PrjFileReader; 
import org.geotools.gce.geotiff.GeoTiffReader;
import org.locationtech.jts.geom.Envelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import gama.core.common.geometry.Envelope3D;
import gama.core.metamodel.shape.GamaPoint;
import gama.core.metamodel.shape.GamaShape;
import gama.core.metamodel.shape.GamaShapeFactory;
import gama.core.metamodel.shape.IShape;
import gama.annotations.precompiler.GamlAnnotations.doc;
import gama.annotations.precompiler.GamlAnnotations.example;
import gama.annotations.precompiler.GamlAnnotations.file;
import gama.annotations.precompiler.GamlAnnotations.no_test;
import gama.annotations.precompiler.GamlAnnotations.operator;
import gama.annotations.precompiler.IConcept;
import gama.annotations.precompiler.IOperatorCategory;
import gama.annotations.precompiler.Reason;
import gama.core.runtime.GAMA;
import gama.core.runtime.IScope;
import gama.core.runtime.exceptions.GamaRuntimeException;
import gama.core.util.GamaListFactory;
import gama.core.util.IList;
import gama.core.util.file.GamaGridFile;
import gama.core.util.matrix.GamaFloatMatrix;
import gama.core.util.matrix.GamaIntMatrix;
import gama.core.util.matrix.IMatrix;
import gama.gaml.types.GamaGeometryType;
import gama.gaml.types.IType;
import gama.gaml.types.Types;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.ma2.MAMath;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.unidata.geoloc.ProjectionRect;

@file(name = "netcdf", extensions = {
		"nc" }, buffer_type = IType.LIST, buffer_content = IType.GEOMETRY, buffer_index = IType.INT, concept = {
				IConcept.GRID, IConcept.ASC, IConcept.TIF,
				IConcept.FILE }, doc = @doc("Represents multi-dimensional arrays encoded in NetCDF format"))
@SuppressWarnings({ "unchecked", "rawtypes" })
public class GamaNetCDFFile extends GamaGridFile {

	GamaNetCDFReader reader;
	IMatrix coverage;
//	GridCoverage2D coverage;
//	public int nbBands;

	@Override
	public IList<String> getAttributes(final IScope scope) {
		// No attributes
		return GamaListFactory.EMPTY_LIST;
	}

	private GamaNetCDFReader createReader(final IScope scope, final boolean fillBuffer) {
		if (reader == null) {
			final File gridFile = getFile(scope);
			gridFile.setReadable(true);
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(gridFile);
			} catch (final FileNotFoundException e) {
				e.printStackTrace();
			}
			try {
				reader = new GamaNetCDFReader(scope, fis, fillBuffer);
			} catch (final GamaRuntimeException e) {
				// A problem appeared, likely related to the wrong format of the
				// file (see Issue 412)
				GAMA.reportError(scope,
						GamaRuntimeException.warning(
								"The format of " + getName(scope) + " is incorrect. Attempting to read it anyway.",
								scope),
						false);

//				reader = fixFileHeader(scope,fillBuffer);
			}
		}
		return reader;
	}

//	public GamaNetCDFReader fixFileHeader(IScope scope,final boolean fillBuffer) {
//		final StringBuilder text = new StringBuilder();
//		final String NL = System.getProperty("line.separator");
//
//		try (Scanner scanner = new Scanner(getFile(scope))) {
//			// final int cpt = 0;
//			while (scanner.hasNextLine()) {
//				final String line = scanner.nextLine();
//
//				if (line.contains("dx")) {
//					text.append(line.replace("dx", "cellsize") + NL);
//				} else if (line.contains("dy")) {
//					// continue;
//				} else {
//					text.append(line + NL);
//				}
//
//				// if (cpt < 10) {}
//				// else {
//				// text.append(line + NL);
//				// }
//			}
//		} catch (final FileNotFoundException e2) {
//			final GamaRuntimeException ex = GamaRuntimeException.error(
//					"The format of " + getName(scope) + " is not correct. Error: " + e2.getMessage(), scope);
//			ex.addContext("for file " + getPath(scope));
//			throw ex;
//		}
//
//		text.append(NL);
//		// fis = new StringBufferInputStream(text.toString());
//		return new GamaNetCDFReader(scope, new StringBufferInputStream(text.toString()), fillBuffer);
//	}

	class GamaNetCDFReader {

		int numRows, numCols;
		IShape geom;
		Number noData = -9999;
		NetcdfDataset ds = null;
		private GridDataset gridDataset;
		private int ntimes = 1;
		boolean forward = true;

		GamaNetCDFReader(final IScope scope, final InputStream fis, final boolean fillBuffer)
				throws GamaRuntimeException {
			setBuffer(GamaListFactory.<IShape>create(Types.GEOMETRY));
//			AbstractGridCoverage2DReader store = null;
			
			
			try {
				if (fillBuffer) {
					scope.getGui().getStatus().beginSubStatus(scope, "Reading file " + getName(scope));
				}
				// Necessary to compute it here, because it needs to be passed
				// to the Hints
				final CoordinateReferenceSystem crs = getExistingCRS(scope);
//				NetCDFCRSUtilities nu=new NetCDFCRSUtilities();
//				nu.getCoordinateSystem(variableDS);
				ProjectionRect proj = null;
				if (ds == null) {
					String netCDF_File = getFile(scope).getAbsolutePath();

					ds = NetcdfDataset.openDataset(netCDF_File, true, null);
					if (ds != null) {
						ds.getCoordinateSystems().get(0);
						gridDataset = new ucar.nc2.dt.grid.GridDataset(ds, new Formatter());
						Array ma = null;
						List<?> grids = gridDataset.getGrids();
						GridDatatype grid = null;
						int nbGrid = 0;
						if (nbGrid >= grids.size()) {
							nbGrid = 0;
						}
						if (grids.size() > 0)
							grid = (GridDatatype) grids.get(nbGrid);// TODO number of the map
						if (grid != null) {
							GridCoordSystem gcsys = grid.getCoordinateSystem();
							if (gcsys.getTimeAxis() != null)
								ntimes = (int) gcsys.getTimeAxis().getSize();
							proj=gcsys.getBoundingBox();
							int t_index = 0;
							int z_index = 0;
							int y_index = -1;
							int x_index = -1;
							ma = grid.readDataSlice(t_index, z_index, y_index, x_index);
							if (ma.getRank() == 3)
								ma = ma.reduce();

							if (ma.getRank() == 3)
								ma = ma.slice(0, 0); // we need 2D

							numCols = ma.getShape()[1];
							numRows = ma.getShape()[0];

							coverage = matrixValue(scope, ma, numRows, numCols);


						}
					}

				} 
				final Envelope3D env = Envelope3D.of(proj.getMinX(), proj.getMaxX(), proj.getMinY(), proj.getMaxY(), 0, 0);
				computeProjection(scope, env);
				final Envelope envP = gis.getProjectedEnvelope();
				final double cellHeight = envP.getHeight() / numRows;
				final double cellWidth = envP.getWidth() / numCols;
				final IList<IShape> shapes = GamaListFactory.create(Types.GEOMETRY);
				final double originX = envP.getMinX();
				final double originY = envP.getMinY();
				final double maxY = envP.getMaxY();
				final double maxX = envP.getMaxX();
				shapes.add(new GamaPoint(originX, originY));
				shapes.add(new GamaPoint(maxX, originY));
				shapes.add(new GamaPoint(maxX, maxY));
				shapes.add(new GamaPoint(originX, maxY));
				shapes.add(shapes.get(0));
				geom = GamaGeometryType.buildPolygon(shapes);
				if (!fillBuffer) {
					return;
				}

				final GamaPoint p = new GamaPoint(0, 0);
//				coverage = store.read(null);
				final double cmx = cellWidth / 2;
				final double cmy = cellHeight / 2;
//				for (int n = numRows * numCols, i = n-1 ; i > -1; i--) {
				for (int i = 0, n = numRows * numCols; i < n; i++) {
					scope.getGui().getStatus().setSubStatusCompletion(scope, i / (double) n);
					final int yy = i / numCols;
					final int xx = i - yy * numCols;
//					final int xx = i / numRows;
//					final int yy = i - xx * numRows;
//					System.out.println(numCols - xx - 1 + " " + yy);
					p.x = originX + xx * cellWidth + cmx;
					p.y = maxY - (yy * cellHeight + cmy);
					GamaShape rect = (GamaShape) GamaGeometryType.buildRectangle(cellWidth, cellHeight, p);
//					final double vals = cov[numCols-1-xx][numRows-1-yy];
					final double vals = (double) coverage.get(scope,xx,yy);
					if (gis == null) {
						rect = GamaShapeFactory.createFrom(rect.getInnerGeometry());
					} else {
						rect = GamaShapeFactory.createFrom(gis.transform(rect.getInnerGeometry()));
					}

					rect.setAttribute("grid_value", vals);
					rect.setAttribute("bands", GamaListFactory.create(scope, Types.FLOAT, vals));
					nbBands = 1;
					((IList) getBuffer()).add(rect);
				}
			} catch (final Exception e) {
//				final GamaRuntimeException ex = GamaRuntimeException.error(
//						"The format of " + getFile(scope).getName() + " is not correct. Error: " + e.getMessage(),
//						scope);
//				ex.addContext("for file " + getFile(scope).getPath());
				e.printStackTrace();
//				throw ex;
			} finally {
//				if (store != null) {
//					store.dispose();
//				}
				scope.getGui().getStatus().endSubStatus(scope, "Opening file " + getName(scope));
			}
		}

	}

	private static IMatrix matrixValue(final IScope scope, final Array ma, int col, int row) {
		double[][] matrix = new double[col][row];
		double min = MAMath.getMinimum(ma); // LOOK we need missing values to be removed !!
		double max = MAMath.getMaximum(ma);
		double scale = (max - min);
		if (scale > 0.0)
			scale = 255.0 / scale;
		IndexIterator ii = ma.getIndexIterator();
		for (int i = 0; i < col; i++) {
			for (int j = 0; j < row; j++) {

				double val = ii.getDoubleNext();
				double sval = ((val - min) * scale);
				matrix[i][j] = sval;
			}
		}

		double[] temp = new double[matrix.length]; // This temporarily holds the row that needs to be flipped out
		for (int r = 0; r < matrix.length / 2; r++) { // Working one row at a time and only do half the image!!!
			temp = matrix[(matrix.length) - r - 1]; // Collect the temp row from the 'other side' of the array
			matrix[matrix.length - r - 1] = matrix[r]; // Put the current row in the row on the 'other side' of the
														// array
			matrix[r] = temp; // Now put the row from the other side in the current row
		}

		final IMatrix ret = new GamaFloatMatrix(row, col);
		for (int i = 0; i < col; i++) {
			for (int j = 0; j < row; j++) {

				ret.set(scope, j,i, matrix[i][j]);
			}
		}
		return ret;
	}

	@doc(value = "This file constructor allows to read a asc file or a tif (geotif) file", examples = {
			@example(value = "file f <- grid_file(\"file.asc\");", isExecutable = false) })

	public GamaNetCDFFile(final IScope scope, final String pathName) throws GamaRuntimeException {
		super(scope, pathName, (Integer) null);
	}

	@doc(value = "This file constructor allows to read a asc file or a tif (geotif) file specifying the coordinates system code, as an int (epsg code)", examples = {
			@example(value = "file f <- grid_file(\"file.asc\", 32648);", isExecutable = false) })
	public GamaNetCDFFile(final IScope scope, final String pathName, final Integer code) throws GamaRuntimeException {
		super(scope, pathName, code);
	}

	@doc(value = "This file constructor allows to read a asc file or a tif (geotif) file specifying the coordinates system code (epg,...,), as a string ", examples = {
			@example(value = "file f <- grid_file(\"file.asc\",\"EPSG:32648\");", isExecutable = false) })
	public GamaNetCDFFile(final IScope scope, final String pathName, final String code) {
		super(scope, pathName, code);
	}

	@Override
	public Envelope3D computeEnvelope(final IScope scope) {
		fillBuffer(scope);
		return gis.getProjectedEnvelope();
	}

	public Envelope computeEnvelopeWithoutBuffer(final IScope scope) {
		if (gis == null) {
			createReader(scope, false);
		}
		return gis.getProjectedEnvelope();
	}

	@Override
	protected void fillBuffer(final IScope scope) {
		if (getBuffer() != null) {
			return;
		}
		createReader(scope, true);
	}

	public int getNbRows(final IScope scope) {
		if (reader == null) {
			createReader(scope, true);
		}
		return reader.numRows;
	}

	public int getNbCols(final IScope scope) {
		if (reader == null) {
			createReader(scope, true);
		}
		return reader.numCols;
	}

	public boolean isTiff(final IScope scope) {
		return getExtension(scope).equals("tif");
	}

	@Override
	public IShape getGeometry(final IScope scope) {
		if (reader == null) {
			createReader(scope, true);
		}
		return reader.geom;
	}

	@Override
	protected CoordinateReferenceSystem getOwnCRS(final IScope scope) {
		final File source = getFile(scope);
		// check to see if there is a projection file
		// getting name for the prj file
		final String sourceAsString;
		sourceAsString = source.getAbsolutePath();
		final int index = sourceAsString.lastIndexOf('.');
		final StringBuffer prjFileName;
		if (index == -1) {
			prjFileName = new StringBuffer(sourceAsString);
		} else {
			prjFileName = new StringBuffer(sourceAsString.substring(0, index));
		}
		prjFileName.append(".prj");

		// does it exist?
		final File prjFile = new File(prjFileName.toString());
		if (prjFile.exists()) {
			// it exists then we have to read it
			PrjFileReader projReader = null;
			try (FileInputStream fip = new FileInputStream(prjFile); final FileChannel channel = fip.getChannel();) {
				projReader = new PrjFileReader(channel);
				return projReader.getCoordinateReferenceSystem();
			} catch (final FileNotFoundException e) {
				// warn about the error but proceed, it is not fatal
				// we have at least the default crs to use
				return null;
			} catch (final IOException e) {
				// warn about the error but proceed, it is not fatal
				// we have at least the default crs to use
				return null;
			} catch (final FactoryException e) {
				// warn about the error but proceed, it is not fatal
				// we have at least the default crs to use
				return null;
			} finally {
				if (projReader != null) {
					try {
						projReader.close();
					} catch (final IOException e) {
						// warn about the error but proceed, it is not fatal
						// we have at least the default crs to use
						return null;
					}
				}
			}
		} else if (isTiff(scope)) {
			try {
				final GeoTiffReader store = new GeoTiffReader(getFile(scope));
				return store.getCoordinateReferenceSystem();
			} catch (final DataSourceException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	// public static RenderedImage getImage(final String pathName) {
	// return GAMA.run(new InScope<RenderedImage>() {
	//
	// @Override
	// public RenderedImage run(final IScope scope) {
	// GamaGridFile file = new GamaGridFile(scope, pathName);
	// file.createReader(scope, true);
	// return file.coverage.getRenderedImage();
	// }
	// });
	// }

	@Override
	public void invalidateContents() {
		super.invalidateContents();
		reader = null;
//		if (coverage != null) {
//			coverage.dispose(true);
//		}
//		coverage = null;
	}

	public GridCoverage2D getCoverage() {
		return null;
	}

	public Double valueOf(final IScope scope, final GamaPoint loc) {
		if (getBuffer() == null) {
			fillBuffer(scope);
		}

		Object vals = null;
		try {
			vals = coverage.get(scope, (int) loc.getX(), (int) loc.getY());
		} catch (final Exception e) {
			vals = reader.noData.doubleValue();
		}
		final boolean doubleValues = vals instanceof double[];
		final boolean intValues = vals instanceof int[];
		final boolean byteValues = vals instanceof byte[];
		final boolean longValues = vals instanceof long[];
		final boolean floatValues = vals instanceof float[];
		Double val = null;
		if (doubleValues) {
			final double[] vd = (double[]) vals;
			val = vd[0];
		} else if (intValues) {
			final int[] vi = (int[]) vals;
			val = Double.valueOf(vi[0]);
		} else if (longValues) {
			final long[] vi = (long[]) vals;
			val = Double.valueOf(vi[0]);
		} else if (floatValues) {
			final float[] vi = (float[]) vals;
			val = Double.valueOf(vi[0]);
		} else if (byteValues) {
			final byte[] bv = (byte[]) vals;
			if (bv.length == 3) {
				final int red = bv[0] < 0 ? 256 + bv[0] : bv[0];
				final int green = bv[0] < 0 ? 256 + bv[1] : bv[1];
				final int blue = bv[0] < 0 ? 256 + bv[2] : bv[2];
				val = (red + green + blue) / 3.0;
			} else {
				val = Double.valueOf(((byte[]) vals)[0]);
			}
		}
		return val;
	}

	@operator(value = "openDataSet", can_be_const = false, category = IOperatorCategory.MATRIX)
	@doc(value = "general operator to manipylate multidimension netcdf data.")
	@no_test (Reason.IMPOSSIBLE_TO_TEST)
	public static Boolean openDataSet(final IScope scope, final GamaNetCDFFile netcdf) {
		if (netcdf == null || scope == null) {
			return false;
		} else {

			if (netcdf.reader == null) {
				netcdf.createReader(scope, true);
			}
			if (netcdf.reader.ds != null) {
				String netCDF_File = netcdf.getFile(scope).getAbsolutePath();
				try {
					netcdf.reader.ds = NetcdfDataset.openDataset(netCDF_File, true, null);
					if (netcdf.reader == null) {
						JOptionPane.showMessageDialog(null, "NetcdfDataset.open cant open " + netCDF_File);
						return null;
					}

					netcdf.reader.gridDataset = new ucar.nc2.dt.grid.GridDataset(netcdf.reader.ds, new Formatter());

				} catch (FileNotFoundException ioe) {
					JOptionPane.showMessageDialog(null,
							"NetcdfDataset.open cant open " + netCDF_File + "\n" + ioe.getMessage());
					ioe.printStackTrace();
				} catch (Throwable ioe) {
					ioe.printStackTrace();
				}
				return true;
			}
		}

		return false;
	}

	@operator(value = "getTimeAxisSize", can_be_const = false, category = IOperatorCategory.MATRIX)
	@doc(value = "general operator to manipylate multidimension netcdf data.")
	@no_test (Reason.IMPOSSIBLE_TO_TEST)
	public static Integer getTimeAxisSize(final IScope scope, final GamaNetCDFFile netcdf, int nbGrid) {
		if (netcdf == null || scope == null) {
			return -1;
		} else {

			if (netcdf.reader != null) {

				List<?> grids = netcdf.reader.gridDataset.getGrids();
				GridDatatype grid = null;
				if (nbGrid >= grids.size()) {
					nbGrid = 0;
				}
				if (grids.size() > 0)
					grid = (GridDatatype) grids.get(nbGrid);// TODO number of the map
				if (grid != null) {
					GridCoordSystem gcsys = grid.getCoordinateSystem();
					if (gcsys.getTimeAxis() != null)
						netcdf.reader.ntimes = (int) gcsys.getTimeAxis().getSize();

					return netcdf.reader.ntimes;

				}
			}
		}

		return -1;
	}

	@operator(value = "getGridsSize", can_be_const = false, category = IOperatorCategory.FILE)
	@doc(value = "general operator to manipylate multidimension netcdf data.")
	@no_test (Reason.IMPOSSIBLE_TO_TEST)
	public static Integer getGridsSize(final IScope scope, final GamaNetCDFFile netcdf) {
		if (netcdf == null || scope == null) {
			return -1;
		} else {
			if (netcdf.reader != null) {
				List<?> grids = netcdf.reader.gridDataset.getGrids();
				return grids.size();
			}
		}

		return -1;
	}

	@operator(value = "readDataSlice", can_be_const = false, category = IOperatorCategory.MATRIX)
	@doc(value = "general operator to manipylate multidimension netcdf data.")
	@no_test (Reason.IMPOSSIBLE_TO_TEST)
	public static IMatrix readDataSlice(final IScope scope, final GamaNetCDFFile netcdf, int nbGrid, int t_index,
			int z_index, int y_index, int x_index) {
		if (netcdf == null || scope == null) {
			return new GamaIntMatrix(0, 0);
		} else {
			if (netcdf.reader != null) {

				List<?> grids = netcdf.reader.gridDataset.getGrids();
				GridDatatype grid = null;
				if (nbGrid >= grids.size()) {
					nbGrid = 0;
				}
				if (grids.size() > 0)
					grid = (GridDatatype) grids.get(nbGrid);// TODO number of the map
				if (grid != null) {
					GridCoordSystem gcsys = grid.getCoordinateSystem();
					if (gcsys.getTimeAxis() != null)
						netcdf.reader.ntimes = (int) gcsys.getTimeAxis().getSize();

					Array ma;
					try {
						ma = grid.readDataSlice(t_index, z_index, y_index, x_index);
						if (ma.getRank() == 3)
							ma = ma.reduce();

						if (ma.getRank() == 3)
							ma = ma.slice(0, 0); // we need 2D

						int h = ma.getShape()[0];
						int w = ma.getShape()[1];

						return matrixValue(scope, ma, h, w);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		return new GamaIntMatrix(0, 0);
	}
}
