package edu.ucsf.rbvi.layoutSaver.internal.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

public class MapLayoutTask extends AbstractTask {
	Map<String, CyNetworkView> viewMap;
	CyNetworkView view;

	public ListSingleSelection<String> mapToNetwork;
	@Tunable(description="Choose network to map this layout to", required=true, gravity=1.0)
	public ListSingleSelection<String> getMapToNetwork() {
		return mapToNetwork;
	}

	public void setMapToNetwork(ListSingleSelection<String> mtn) {
		CyNetworkView toNetworkView = viewMap.get(mapToNetwork.getSelectedValue());
		mapToColumn = new ListSingleSelection<String>(getColumnNames(toNetworkView));
	}

	@Tunable(description="Choose column to map from", required=true, gravity=2.0)
	public ListSingleSelection<String> mapFromColumn;

	public ListSingleSelection<String> mapToColumn = null;
	@Tunable(description="Choose column to map to", required=true, gravity=3.0, listenForChange="mapToNetwork")
	public ListSingleSelection<String> getMapToColumn() {
		if (mapToColumn == null) {
			CyNetworkView toNetworkView = viewMap.get(mapToNetwork.getSelectedValue());
			mapToColumn = new ListSingleSelection<String>(getColumnNames(toNetworkView));
		}
		return mapToColumn;
	}

	public void setMapToColumn(ListSingleSelection<String> map) {
	}
	
	/**
	 */
	public MapLayoutTask(CyNetworkView view, CyNetworkViewManager viewManager) {
		super();

		viewMap = new HashMap<String, CyNetworkView>();
		this.view = view;

		for (CyNetworkView v: viewManager.getNetworkViewSet()) {
			if (v.equals(view)) continue;
			viewMap.put(getName(v), v);
		}

		mapToNetwork = new ListSingleSelection<String>(new ArrayList<String>(viewMap.keySet()));

		mapFromColumn = new ListSingleSelection<String>(getColumnNames(view));
	}

	@ProvidesTitle
	public String getTitle() { return "Map Layout"; }
	
	@Override
	public void run(TaskMonitor monitor) throws Exception {
		CyNetworkView toNetworkView = viewMap.get(mapToNetwork.getSelectedValue());
		CyNetwork toNetwork = toNetworkView.getModel();

		Map<Object, View<CyNode>> targetMap = new HashMap<Object,View<CyNode>>();

		// Build a map of our mapping column
		for (View<CyNode> nodeView: toNetworkView.getNodeViews()) {
			CyNode node = nodeView.getModel();
			targetMap.put(toNetwork.getRow(node).getRaw(mapToColumn.getSelectedValue()), nodeView);
		}

		// Now, map the layout
		for (View<CyNode> sourceNodeView: view.getNodeViews()) {
			CyNode sourceNode = sourceNodeView.getModel();
			Object mapper = view.getModel().getRow(sourceNode).getRaw(mapFromColumn.getSelectedValue());
			if (!targetMap.containsKey(mapper))
				continue;

			View<CyNode> targetNodeView = targetMap.get(mapper);

			Double x = sourceNodeView.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION);
			Double y = sourceNodeView.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);
			Double z = sourceNodeView.getVisualProperty(BasicVisualLexicon.NODE_Z_LOCATION);

			targetNodeView.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, x);
			targetNodeView.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, y);
			targetNodeView.setVisualProperty(BasicVisualLexicon.NODE_Z_LOCATION, z);

		}
	}

	private List<String> getColumnNames(CyNetworkView netView) {
		CyNetwork network = netView.getModel();
		List<String> columnNames = 
						new ArrayList<String>(CyTableUtil.getColumnNames(network.getDefaultNodeTable()));
		Collections.sort(columnNames);
		return columnNames;
	}

	private String getName(CyNetworkView view) {
		CyNetwork net = view.getModel();
		String name = net.getRow(net).get(CyNetwork.NAME, String.class);
		return name;
	}
}
