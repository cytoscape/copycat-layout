package org.cytoscape.copycatLayout.internal.task;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.NODE_X_LOCATION;
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.NODE_Y_LOCATION;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.copycatLayout.internal.rest.CopycatLayoutResult;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
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
	private final TaskIterator ti;

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
			final CyLayoutAlgorithmManager algoManager, final TaskIterator ti) {
		super();
		this.algoManager = algoManager;
		this.ti = ti;
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
		else if (names.hasNext()) {
			sourceList.setSelectedValue(names.next());
		}

		if (names.hasNext()) {
			String name = names.next();
			if (name == sourceList.getSelectedValue())
				name = names.next();
			targetList.setSelectedValue(name);
		}

		setsourceNetwork(sourceList);
		settargetNetwork(targetList);
		targetColumn.setSelectedValue("name");
		sourceColumn.setSelectedValue("name");
	}

	void showError(final String message, final String title) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
			}
		});

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
			throw new NetworkNotFoundError("Target network not found");
		}
		if (sourceNetworkView == null) {
			logger.error("Source network not found");
			throw new NetworkNotFoundError("Source network not found");
		}

		if (targetNetworkView.equals(sourceNetworkView)) {
			logger.warn("Source and target network must be different");
			showError("Source and target network must be different", "Invalid Network Views");
			return;
		}

		CyNetwork targetNetwork = targetNetworkView.getModel();
		CyNetwork sourceNetwork = sourceNetworkView.getModel();

		CyColumn sourceCol = sourceNetwork.getDefaultNodeTable().getColumn(sourceColumnName);
		if (sourceCol == null || !(sourceCol.getType() == String.class || sourceCol.getType() == Integer.class)) {
			logger.error("Source column not found or invalid. Must be existing String or Integer column");
			throw new InvalidColumnError(
					"Source column not found or invalid. Must be existing String or Integer column");
		}
		CyColumn targetCol = targetNetwork.getDefaultNodeTable().getColumn(targetColumnName);

		if (targetCol == null || !(targetCol.getType() == String.class || targetCol.getType() == Integer.class)) {
			logger.error("Target column not found or invalid. Must be existing String or Integer column");
			throw new InvalidColumnError(
					"Target column not found or invalid. Must be existing String or Integer column");
		}

		if (sourceCol.getType() != targetCol.getType()) {
			logger.error("Column types must match to map correctly");
			// TODO: don't throw error, inform user and return
			showError("Source and target column must be the same type", "Invalid Column Types");
			return;
		}
		Class<?> cls = sourceCol.getType();

		HashMap<Object, View<CyNode>> sourceMap = new HashMap<Object, View<CyNode>>();

		for (View<CyNode> nodeView : sourceNetworkView.getNodeViews()) {
			if (cancelled)
				return;
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

		double maxX = 0, minY = Double.MAX_VALUE;

		for (View<CyNode> nodeView : targetNetworkView.getNodeViews()) {
			if (cancelled)
				return;
			Object val = targetNetwork.getRow(nodeView.getModel()).get(targetColumnName, cls);
			if (sourceMap.containsKey(val)) {
				copyNodeLocation(sourceMap.get(val), nodeView);
				sourceUnmapped.remove(sourceMap.get(val));
				mappedNodeCount++;
				maxX = Math.max(maxX, nodeView.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION));
				minY = Math.min(minY, nodeView.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION));

			} else {
				targetUnmapped.add(nodeView);
			}
			if (selectUnmapped)
				targetNetwork.getRow(nodeView.getModel()).set("selected", false);
		}

		if (selectUnmapped) {
			for (View<CyNode> nodeView : sourceUnmapped) {
				if (cancelled)
					return;
				sourceNetwork.getRow(nodeView.getModel()).set("selected", true);
			}
			for (View<CyNode> nodeView : targetUnmapped) {
				if (cancelled)
					return;
				targetNetwork.getRow(nodeView.getModel()).set("selected", true);
			}
		}

		if (gridUnmapped) {
			// Move off to side
			for (View<CyNode> nodeView : targetUnmapped) {
				if (cancelled)
					return;
				nodeView.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, maxX + 200);
				nodeView.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, minY);
			}

			if (targetUnmapped.size() > 0) {
				grid(targetUnmapped, 80, 40);
			}
		}
		ti.append(new CopyNetworkViewLocationTask(sourceNetworkView, targetNetworkView));

		result = new CopycatLayoutResult();
		result.mappedNodeCount = mappedNodeCount;
		result.unmappedNodeCount = targetUnmapped.size();
	}

	private void grid(Set<View<CyNode>> nodesToLayOut, int nodeHorizontalSpacing, int nodeVerticalSpacing) {
		double currX = 0.0d;
		double currY = 0.0d;
		double initialX = 0.0d;
		double initialY = 0.0d;

		// Yes, our size and starting points need to be different
		final int nodeCount = nodesToLayOut.size();
		final int columns = (int) Math.sqrt(nodeCount);

		// Calculate our starting point as the geographical center of the
		// selected nodes.
		for (final View<CyNode> nView : nodesToLayOut) {
			if (cancelled)
				return;
			initialX += (nView.getVisualProperty(NODE_X_LOCATION) / nodeCount);
			initialY += (nView.getVisualProperty(NODE_Y_LOCATION) / nodeCount);
		}

		// initialX and initialY reflect the center of our grid, so we
		// need to offset by distance*columns/2 in each direction
		initialX = initialX - ((nodeHorizontalSpacing * (columns - 1)) / 2);
		initialY = initialY - ((nodeVerticalSpacing * (columns - 1)) / 2);
		currX = initialX;
		currY = initialY;

		int count = 0;

		// Set visual property.
		// TODO: We need batch apply method for Visual Property values for
		// performance.
		for (final View<CyNode> nView : nodesToLayOut) {
			if (cancelled)
				return;
			nView.setVisualProperty(NODE_X_LOCATION, currX);
			nView.setVisualProperty(NODE_Y_LOCATION, currY);

			count++;

			if (count == columns) {
				count = 0;
				currX = initialX;
				currY += nodeVerticalSpacing;
			} else {
				currX += nodeHorizontalSpacing;
			}
		}
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

	private class NetworkNotFoundError extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public NetworkNotFoundError(String message) {
			super(message);
		}
	}

	private class InvalidColumnError extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public InvalidColumnError(String message) {
			super(message);
		}
	}

}
