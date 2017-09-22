package org.cytoscape.copycatLayout.internal.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.copycatLayout.internal.rest.CopycatLayoutResult;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 */
public class CopycatLayoutTask extends AbstractTask implements ObservableTask {

	private static final Logger logger = LoggerFactory.getLogger("org.cytoscape.application.userlog");
	private static final String displayName = "Copycat";
	protected final CyLayoutAlgorithmManager algoManager;
	protected CopycatLayoutResult result;

	private List<String> getValidColumnNames(CyNetworkView netView) {
		if (netView == null)
			return new ArrayList<String>();
		CyNetwork network = netView.getModel();
		List<String> columnNames = new ArrayList<String>();
		for (CyColumn col : network.getDefaultNodeTable().getColumns()) {
			if (col.getType() == String.class || col.getType() == Integer.class) {
				columnNames.add(col.getName());
			}
		}
		Collections.sort(columnNames);
		return columnNames;
	}

	private String getNetworkName(CyNetworkView view) {
		CyNetwork net = view.getModel();
		String name = net.getRow(net).get(CyNetwork.NAME, String.class);
		return name;
	}

	/* Tunables */
	Map<String, CyNetworkView> viewMap;

	public ListSingleSelection<String> sourceNetwork;

	@Tunable(description = "Source network view", required = true, gravity = 1.0)
	public ListSingleSelection<String> getsourceNetwork() {

		return sourceNetwork;
	}

	public void setsourceNetwork(ListSingleSelection<String> mfn) {
		if (sourceNetwork != null && mfn.getSelectedValue().equals(sourceNetwork.getSelectedValue()))
			return;
		sourceNetwork = mfn;
		CyNetworkView fromNetworkView = viewMap.get(sourceNetwork.getSelectedValue());
		sourceColumn = new ListSingleSelection<String>(getValidColumnNames(fromNetworkView));
	}

	public ListSingleSelection<String> sourceColumn = null;

	@Tunable(description = "Source network node column", required = true, gravity = 2.0, listenForChange = "sourceNetwork")
	public ListSingleSelection<String> getsourceColumn() {
		if (sourceColumn == null) {
			CyNetworkView fromNetworkView = viewMap.get(sourceNetwork.getSelectedValue());
			sourceColumn = new ListSingleSelection<String>(getValidColumnNames(fromNetworkView));
		}

		return sourceColumn;
	}

	public void setsourceColumn(ListSingleSelection<String> map) {
		sourceColumn = map;
	}

	public ListSingleSelection<String> targetNetwork;

	@Tunable(description = "Target network view", required = true, gravity = 3.0)
	public ListSingleSelection<String> gettargetNetwork() {
		return targetNetwork;
	}

	public void settargetNetwork(ListSingleSelection<String> mtn) {
		if (targetNetwork != null && mtn.getSelectedValue().equals(targetNetwork.getSelectedValue()))
			return;
		targetNetwork = mtn;
		CyNetworkView fromNetworkView = viewMap.get(targetNetwork.getSelectedValue());
		targetColumn = new ListSingleSelection<String>(getValidColumnNames(fromNetworkView));

	}

	public ListSingleSelection<String> targetColumn = null;

	@Tunable(description = "Target network node column", required = true, gravity = 4.0, listenForChange = "targetNetwork")
	public ListSingleSelection<String> gettargetColumn() {
		if (targetColumn == null) {
			CyNetworkView toNetworkView = viewMap.get(targetNetwork.getSelectedValue());
			targetColumn = new ListSingleSelection<String>(getValidColumnNames(toNetworkView));
		}

		return targetColumn;
	}

	public void settargetColumn(ListSingleSelection<String> map) {
		targetColumn = map;
	}

	public boolean selectUnmapped = false;

	@Tunable(description = "Select unmapped nodes", gravity = 5.0, groups = { "After Layout" })
	public boolean getselectUnmapped() {
		return selectUnmapped;
	}

	public void setselectUnmapped(boolean b) {
		selectUnmapped = b;
	}

	public boolean gridUnmapped = false;

	@Tunable(description = "Layout unmapped nodes in a grid", gravity = 6.0, groups = { "After Layout" })
	public boolean getgridUnmapped() {
		return gridUnmapped;
	}

	public void setgridUnmapped(boolean b) {
		gridUnmapped = b;
	}

	public CopycatLayoutTask(final CyApplicationManager cyApplicationManager, final CyNetworkViewManager viewManager,
			final CyLayoutAlgorithmManager algoManager) {
		super();
		this.algoManager = algoManager;
		viewMap = new HashMap<String, CyNetworkView>();

		for (CyNetworkView v : viewManager.getNetworkViewSet()) {
			viewMap.put(getNetworkName(v), v);
		}
		ListSingleSelection<String> sourceList = new ListSingleSelection<String>(
				new ArrayList<String>(viewMap.keySet()));
		ListSingleSelection<String> targetList = new ListSingleSelection<String>(
				new ArrayList<String>(viewMap.keySet()));
		Iterator<String> names = viewMap.keySet().iterator();
		CyNetworkView networkView = cyApplicationManager.getCurrentNetworkView();
		if (networkView != null)
			sourceList.setSelectedValue(getNetworkName(networkView));
		else {
			sourceList.setSelectedValue(names.next());
		}

		String name = names.next();
		if (name == sourceList.getSelectedValue())
			name = names.next();
		targetList.setSelectedValue(name);

		setsourceNetwork(sourceList);
		settargetNetwork(targetList);
		targetColumn.setSelectedValue("name");
		sourceColumn.setSelectedValue("name");
	}

	private class SameNetworkError extends Exception {
	}

	private class NetworkNotFoundError extends Exception {
	}

	private class InvalidColumnError extends Exception {
	}

	private class ColumnTypeMismatchError extends Exception {
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @throws Exception
	 */
	@Override
	public void run(final TaskMonitor taskMonitor) throws Exception {

		taskMonitor.setTitle(displayName);

		String targetNetworkName = targetNetwork.getSelectedValue();
		String targetColumnName = targetColumn.getSelectedValue();
		String sourceNetworkName = sourceNetwork.getSelectedValue();
		String sourceColumnName = sourceColumn.getSelectedValue();

		CyNetworkView targetNetworkView = viewMap.get(targetNetworkName);
		CyNetworkView sourceNetworkView = viewMap.get(sourceNetworkName);

		if (targetNetworkView == null) {
			logger.error("Target network not found");
			throw new NetworkNotFoundError();
		}
		if (sourceNetworkView == null) {
			logger.error("Source network not found");
			throw new NetworkNotFoundError();
		}

		if (targetNetworkView.equals(sourceNetworkView)) {
			logger.error("Source and target network must be different");
			throw new SameNetworkError();
		}

		CyNetwork targetNetwork = targetNetworkView.getModel();
		CyNetwork sourceNetwork = sourceNetworkView.getModel();
		
		CyColumn sourceCol = sourceNetwork.getDefaultNodeTable().getColumn(sourceColumnName);
		if (sourceCol == null || !(sourceCol.getType() == String.class || sourceCol.getType() == Integer.class)) {
			logger.error("Source column not found or invalid. Must be existing String or Integer column");
			throw new InvalidColumnError();
		}
		CyColumn targetCol = targetNetwork.getDefaultNodeTable().getColumn(targetColumnName);

		if (targetCol == null || !(targetCol.getType() == String.class || targetCol.getType() == Integer.class)) {
			logger.error("Target column not found or invalid. Must be existing String or Integer column");
			throw new InvalidColumnError();
		}

		if (sourceCol.getType() != targetCol.getType()) {
			logger.error("Column types must match to map correctly");
			throw new ColumnTypeMismatchError();
		}
		Class<?> cls = sourceCol.getType();

		HashMap<Object, View<CyNode>> sourceMap = new HashMap<Object, View<CyNode>>();

		for (View<CyNode> nodeView : sourceNetworkView.getNodeViews()) {
			Object val = sourceMap.put(sourceNetwork.getRow(nodeView.getModel()).get(sourceColumnName, cls), nodeView);
			if (val != null) {
				logger.warn("Duplicate key in source");
			}
			if (selectUnmapped)
				sourceNetwork.getRow(nodeView.getModel()).set("selected", false);
		}
		HashSet<View<CyNode>> sourceUnmapped = new HashSet<View<CyNode>>(sourceMap.values()),
				targetUnmapped = new HashSet<View<CyNode>>();
		int mappedNodeCount = 0;
		
		double maxX = 0, maxY = 0;
		
		for (View<CyNode> nodeView : targetNetworkView.getNodeViews()) {
			Object val = targetNetwork.getRow(nodeView.getModel()).get(targetColumnName, cls);
			if (sourceMap.containsKey(val)) {
				copyNodeLocation(sourceMap.get(val), nodeView);
				sourceUnmapped.remove(sourceMap.get(val));
				mappedNodeCount++;
				maxX = Math.max(maxX, nodeView.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION));
				maxY = Math.max(maxY, nodeView.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION));
				
			} else {
				targetUnmapped.add(nodeView);
			}
			if (selectUnmapped)
				targetNetwork.getRow(nodeView.getModel()).set("selected", false);
		}

		if (selectUnmapped) {
			for (View<CyNode> nodeView : sourceUnmapped) {
				sourceNetwork.getRow(nodeView.getModel()).set("selected", true);
			}
			for (View<CyNode> nodeView : targetUnmapped) {
				targetNetwork.getRow(nodeView.getModel()).set("selected", true);
			}
		}

		if (gridUnmapped) {
			// Move off to side
			for (View<CyNode> nodeView : targetUnmapped){
				nodeView.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, maxX + 200);
				nodeView.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, maxY + 200);	
			}
			
			CyLayoutAlgorithm algo = algoManager.getLayout("grid");
			if (targetUnmapped.size() > 0) {
				TaskIterator targetIterator = algo.createTaskIterator(targetNetworkView, algo.getDefaultLayoutContext(),
						targetUnmapped, targetColumnName);
				targetIterator.next().run(taskMonitor);
			}
			
			
			// TODO : shift nodes off to the side
		}

		Double x_center = sourceNetworkView.getVisualProperty(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION);
		Double y_center = sourceNetworkView.getVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION);
		Double z_center = sourceNetworkView.getVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Z_LOCATION);

		targetNetworkView.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION, x_center);
		targetNetworkView.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION, y_center);
		targetNetworkView.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Z_LOCATION, z_center);

		Double height = sourceNetworkView.getVisualProperty(BasicVisualLexicon.NETWORK_HEIGHT);
		Double width = sourceNetworkView.getVisualProperty(BasicVisualLexicon.NETWORK_WIDTH);
		Double scale = sourceNetworkView.getVisualProperty(BasicVisualLexicon.NETWORK_SCALE_FACTOR);

		targetNetworkView.setVisualProperty(BasicVisualLexicon.NETWORK_HEIGHT, height);
		targetNetworkView.setVisualProperty(BasicVisualLexicon.NETWORK_WIDTH, width);
		targetNetworkView.setVisualProperty(BasicVisualLexicon.NETWORK_SCALE_FACTOR, scale);

		result = new CopycatLayoutResult();
		result.mappedNodeCount = mappedNodeCount;
		result.unmappedNodeCount = targetUnmapped.size();
	}

	private void copyNodeLocation(View<CyNode> source, View<CyNode> target) {
		Double x = source.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION);
		Double y = source.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);
		Double z = source.getVisualProperty(BasicVisualLexicon.NODE_Z_LOCATION);

		target.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, x);
		target.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, y);
		target.setVisualProperty(BasicVisualLexicon.NODE_Z_LOCATION, z);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <R> R getResults(Class<? extends R> type) {
		return (R) result;
	}

}
