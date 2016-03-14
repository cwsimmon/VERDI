package anl.verdi.loaders;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import anl.verdi.data.CoordAxis;
import anl.verdi.data.DataFrame;
import anl.verdi.data.MPASDataFrameIndex;
import anl.verdi.plot.data.IMPASDataset;
import anl.verdi.plot.data.MinMaxInfo;
import anl.verdi.plot.data.MinMaxLevelListener;

public class MPASMinMaxCalculator implements Runnable {
	
	static final long UPDATE_SPAN = 3000;
	
	IMPASDataset ds = null;
	
	int timeOrigin = 0;
	int numTimesteps = 1;
	int layerOrigin = 0;
	int numLayers = 1;
	int numCells = 1;
	
	int cellsInLayer = 1;
		
	MinMaxInfo plotInfo = null;
	DataFrame dataFrame = null;
	
	boolean calcDone = false;
	
	long layerUpdate = 0;
	long plotUpdate = 0;
	
	boolean isLog = false;
	
	Vector<MinMaxLevelListener> listeners = new Vector<MinMaxLevelListener>();
	
	public MPASMinMaxCalculator(IMPASDataset ds, DataFrame frame) {
		this.ds = ds;
		this.dataFrame = frame;
		this.isLog = frame.getArray().getClass().getName().endsWith("Log");

		CoordAxis axis = frame.getAxes().getTimeAxis();
		if (axis != null) {
			timeOrigin = (int)axis.getRange().getOrigin();
			numTimesteps = (int)axis.getRange().getExtent() - timeOrigin;
		}
		axis = frame.getAxes().getZAxis();
		if (axis != null) {
			layerOrigin = (int)axis.getRange().getOrigin();
			numLayers = (int)axis.getRange().getExtent() - layerOrigin;
		}
		axis = frame.getAxes().getCellAxis();
		numCells = (int)axis.getRange().getExtent();
		cellsInLayer = numCells * numTimesteps;
		plotInfo = new MinMaxInfo(numCells * numLayers * numTimesteps);
		new Thread(this).start();
	}
	
	public MinMaxInfo getMinMaxInfo(MinMaxLevelListener listener) {
		if (!calcDone)
			addLevelListener(listener);
		return plotInfo;
	}
	
	private double getValue(MPASDataFrameIndex dataFrameIndex, int layer, int step, int cell) {	
		dataFrameIndex.set(step, layer, cell);
		return dataFrame.getFloat(dataFrameIndex);
	}
	
	Map<Integer, MinMaxInfo> layerCache = new HashMap<Integer, MinMaxInfo>();
	Map<Integer, Map<Integer, MinMaxInfo>> timestepCache = new HashMap<Integer, Map<Integer, MinMaxInfo>>();
	
	//callers are aware they may be getting incomplete info, and will register for updates
	public MinMaxInfo getLayerInfo(int layer, MinMaxLevelListener listener) {
		final MPASDataFrameIndex index = new MPASDataFrameIndex(dataFrame);
		MinMaxInfo layerInfo = resolveLayerInfo(layer);
		//All callers require at least the first timestep calculated
		getTimestep(index, layer, 0);
		if (!calcDone)
			addLevelListener(listener);
		return layerInfo;
	}
	
	private MinMaxInfo resolveLayerInfo(int layer) {
		MinMaxInfo info = null;
		synchronized (layerCache) {
			info = layerCache.get(layer);
			if (info == null) {
				info = new MinMaxInfo(numCells * numTimesteps);
				layerCache.put(layer, info);
			}
		}
		return info;
	}
	
	private void calculateLayer(MPASDataFrameIndex index, int layer) {
		for (int i = 0; i < numTimesteps; ++i) {
			getTimestep(index, layer, i);
		}
		return;
	}
	
	private MinMaxInfo getTimestep(MPASDataFrameIndex index, int layer, int timestep) {
		Map<Integer, MinMaxInfo> layerMap = null;
		MinMaxInfo layerInfo = resolveLayerInfo(layer);
		synchronized (timestepCache) {
			layerMap = timestepCache.get(layer);
			if (layerMap == null) {
				layerMap = new HashMap<Integer, MinMaxInfo>();
				timestepCache.put(layer,  layerMap);
			}
		}
		MinMaxInfo stepInfo = null;
		synchronized (layerMap) {
			stepInfo = layerMap.get(timestep);
			if (stepInfo == null) {
				stepInfo = new MinMaxInfo(numCells);
				layerMap.put(timestep,  stepInfo);
			}
		}
		synchronized (stepInfo) {
			calculateStep(index, layer, timestep, layerInfo, stepInfo);
		}
		return stepInfo;
	}
	
	//Returns true if the timestep was just calculated and needs to be added to the layer
	private boolean calculateStep(MPASDataFrameIndex index, int layer, int step, MinMaxInfo layerInfo, MinMaxInfo stepInfo) {
		if (stepInfo.getCount() == numCells) //Already calculated
			return false;
		for (int i = 0; i < numCells; ++i) {
			stepInfo.visitValue(getValue(index, layer, step, i));
		}
		stepInfo.incrementCount(numCells);
		
		layerInfo.visitValue(stepInfo.getMin());
		layerInfo.visitValue(stepInfo.getMax());
		layerInfo.incrementCount(numCells);
		
		plotInfo.visitValue(layerInfo.getMin());
		plotInfo.visitValue(layerInfo.getMax());
		plotInfo.incrementCount(numCells);

		fireLayerUpdated(layer, layerInfo.getMin(), layerInfo.getMax(), layerInfo.getCompletion());
		fireDatasetUpdated(plotInfo.getMin(), plotInfo.getMax(), plotInfo.getCompletion());
		return true;
	}
	
	public void run() {
		final MPASDataFrameIndex index = new MPASDataFrameIndex(dataFrame);
		for (int i = 0; i < numLayers; ++i) {
			calculateLayer(index, i);
		}
		calcDone = true;
	}
	
	
	private void addLevelListener(MinMaxLevelListener listener) {
		if (listeners.indexOf(listener) == -1)
			listeners.add(listener);
	}
	
	private void fireLayerUpdated(int layer, double min, double max, double percentCompleted) {
		if (System.currentTimeMillis() - layerUpdate < UPDATE_SPAN && percentCompleted < 100)
			return;
		for (int i = 0; i < listeners.size(); ++i) {
			listeners.get(i).layerUpdated(layer, min, max, percentCompleted, isLog);
		}
		layerUpdate = System.currentTimeMillis();
	}
	
	private void fireDatasetUpdated(double min, double max, double percentCompleted) {
		if (System.currentTimeMillis() - plotUpdate < UPDATE_SPAN && percentCompleted < 100)
			return;
		for (int i = 0; i < listeners.size(); ++i) {
			listeners.get(i).datasetUpdated(min, max, percentCompleted, isLog);
		}
		plotUpdate = System.currentTimeMillis();
		if (percentCompleted == 100)
			listeners.clear();
		else if (percentCompleted > 100)
			System.out.println("How???");
	}

}
