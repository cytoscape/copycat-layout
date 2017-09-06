package org.cytoscape.copycatLayout.internal.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.cytoscape.copycatLayout.internal.rest.CopycatLayoutParameters;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

public class CopycatLayoutTask extends AbstractTask implements ObservableTask {
	Map<String, CyNetworkView> viewMap;
	private CopycatLayoutParameters copyResult = null;

	public ListSingleSelection<String> fromNetwork;

	@Tunable(description = "Choose network to copy the layout from", gravity = 1.0)
	public ListSingleSelection<String> getfromNetwork() {
		ArrayList<String> available = new ArrayList<String>(viewMap.keySet());
		available.remove(fromNetwork.getSelectedValue());
		toNetwork.setPossibleValues(available);

		return fromNetwork;
	}

	public void setfromNetwork(ListSingleSelection<String> mfn) {
		if (fromNetwork != null && mfn.getSelectedValue().equals(fromNetwork.getSelectedValue()))
			return;
		fromNetwork = mfn;
		CyNetworkView fromNetworkView = viewMap.get(fromNetwork.getSelectedValue());
		fromColumn = new ListSingleSelection<String>(getColumnNames(fromNetworkView));
	}

	public ListSingleSelection<String> toNetwork;

	@Tunable(description = "Choose network to paste this layout to", required = true, gravity = 3.0)
	public ListSingleSelection<String> gettoNetwork() {
		return toNetwork;
	}

	public void settoNetwork(ListSingleSelection<String> mtn) {
		if (toNetwork != null && mtn.getSelectedValue().equals(toNetwork.getSelectedValue()))
			return;
		toNetwork = mtn;
		CyNetworkView toNetworkView = viewMap.get(toNetwork.getSelectedValue());
		toColumn = new ListSingleSelection<String>(getColumnNames(toNetworkView));
	}

	public ListSingleSelection<String> fromColumn = null;

	@Tunable(description = "Choose column to map from", required = true, gravity = 2.0, listenForChange = "mapFromNetwork")
	public ListSingleSelection<String> getfromColumn() {
		if (fromColumn == null) {
			CyNetworkView fromNetworkView = viewMap.get(fromNetwork.getSelectedValue());
			fromColumn = new ListSingleSelection<String>(getColumnNames(fromNetworkView));
		}
		return fromColumn;
	}

	public void setfromColumn(ListSingleSelection<String> map) {
		fromColumn = map;
	}

	public ListSingleSelection<String> toColumn = null;

	@Tunable(description = "Choose column to map to", required = true, gravity = 4.0, listenForChange = "mapToNetwork")
	public ListSingleSelection<String> gettoColumn() {
		if (toColumn == null) {
			CyNetworkView toNetworkView = viewMap.get(toNetwork.getSelectedValue());
			toColumn = new ListSingleSelection<String>(getColumnNames(toNetworkView));
		}
		return toColumn;
	}

	public void settoColumn(ListSingleSelection<String> map) {
		toColumn = map;
	}

	public CopycatLayoutTask(CyNetworkView view, CyNetworkViewManager viewManager) {
		super();

		viewMap = new HashMap<String, CyNetworkView>();

		for (CyNetworkView v : viewManager.getNetworkViewSet()) {
			// if (v.equals(view)) continue;
			viewMap.put(getName(v), v);
		}

		if (fromNetwork != null) {
			for (CyNetworkView v : viewManager.getNetworkViewSet()) {
				if (v.getModel().equals(fromNetwork)) {
					view = v;
					break;
				}
			}
		}

		toNetwork = new ListSingleSelection<String>(new ArrayList<String>(viewMap.keySet()));

		fromNetwork = new ListSingleSelection<String>(new ArrayList<String>(viewMap.keySet()));
		fromNetwork.setSelectedValue(getName(view));
		fromColumn = new ListSingleSelection<String>(getColumnNames(view));

	}

	@ProvidesTitle
	public String getTitle() {
		return "Copycat Layout";
	}

	@Override
	public void run(TaskMonitor monitor) throws Exception {
		CyNetworkView toNetworkView = viewMap.get(toNetwork.getSelectedValue());
		if (toNetworkView == null){
			return;
		}
		CyNetwork toNetwork = toNetworkView.getModel();
		CyNetworkView fromNetworkView = viewMap.get(fromNetwork.getSelectedValue());

		if (toNetworkView == null || fromNetworkView == null) {
			System.out.println("NULL");
		}
		if (toNetworkView.equals(fromNetworkView)) {
			// logger.error("Could not parse the following Diffusion service
			// response: " + responseJSONString);
			throw new Exception("Can not copy a network layout to itself");
		}

		Map<Object, View<CyNode>> targetMap = new HashMap<Object, View<CyNode>>();

		// Build a map of our mapping column
		for (View<CyNode> nodeView : toNetworkView.getNodeViews()) {
			CyNode node = nodeView.getModel();
			targetMap.put(toNetwork.getRow(node).getRaw(toColumn.getSelectedValue()), nodeView);
		}

		// Now, map the layout
		for (View<CyNode> sourceNodeView : fromNetworkView.getNodeViews()) {
			CyNode sourceNode = sourceNodeView.getModel();
			Object mapper = fromNetworkView.getModel().getRow(sourceNode).getRaw(fromColumn.getSelectedValue());
			if (!targetMap.containsKey(mapper)) {
				continue;
			}

			View<CyNode> targetNodeView = targetMap.get(mapper);

			Double x = sourceNodeView.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION);
			Double y = sourceNodeView.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);
			Double z = sourceNodeView.getVisualProperty(BasicVisualLexicon.NODE_Z_LOCATION);

			targetNodeView.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, x);
			targetNodeView.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, y);
			targetNodeView.setVisualProperty(BasicVisualLexicon.NODE_Z_LOCATION, z);
			
		}
		Double x_center = fromNetworkView.getVisualProperty(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION);
		Double y_center = fromNetworkView.getVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION);
		Double z_center = fromNetworkView.getVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Z_LOCATION);

		toNetworkView.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION, x_center);
		toNetworkView.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION, y_center);
		toNetworkView.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Z_LOCATION, z_center);

		toNetworkView.setVisualProperty(BasicVisualLexicon.NETWORK_HEIGHT,
				fromNetworkView.getVisualProperty(BasicVisualLexicon.NETWORK_HEIGHT));
		toNetworkView.setVisualProperty(BasicVisualLexicon.NETWORK_WIDTH,
				fromNetworkView.getVisualProperty(BasicVisualLexicon.NETWORK_WIDTH));
		toNetworkView.setVisualProperty(BasicVisualLexicon.NETWORK_SCALE_FACTOR,
				fromNetworkView.getVisualProperty(BasicVisualLexicon.NETWORK_SCALE_FACTOR));
		copyResult =  new CopycatLayoutParameters();
		copyResult.toColumn = toColumn.getSelectedValue();
		copyResult.fromColumn = fromColumn.getSelectedValue();
		copyResult.toNetwork = toNetwork.getRow(toNetwork).get(CyNetwork.NAME, String.class);
		
	
	}

	private List<String> getColumnNames(CyNetworkView netView) {
		if (netView == null)
			return new ArrayList<String>();
		CyNetwork network = netView.getModel();
		List<String> columnNames = new ArrayList<String>(CyTableUtil.getColumnNames(network.getDefaultNodeTable()));
		Collections.sort(columnNames);
		return columnNames;
	}

	private String getName(CyNetworkView view) {
		CyNetwork net = view.getModel();
		String name = net.getRow(net).get(CyNetwork.NAME, String.class);
		return name;
	}

	@SuppressWarnings("unchecked")
	public <R> R getResults(Class<? extends R> type) {
		String name = fromNetwork.getSelectedValue();
		if (type.equals(String.class)) {
			return (R) (copyResult != null ? "Copied " + copyResult.fromColumn + " in " + name + " onto "
					+ copyResult.toColumn + " in " + copyResult.toNetwork + "." : "No result columns available");
		} else if (type.isAssignableFrom(CopycatLayoutParameters.class)) {
			return (R) copyResult;
		}
		return null;
	}
}
