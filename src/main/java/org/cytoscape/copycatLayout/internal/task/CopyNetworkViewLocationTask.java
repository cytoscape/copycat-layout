package org.cytoscape.copycatLayout.internal.task;

import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

public class CopyNetworkViewLocationTask extends AbstractTask {

	private final CyNetworkView sourceNetworkView, targetNetworkView;
	public CopyNetworkViewLocationTask(CyNetworkView source, CyNetworkView target){
		this.sourceNetworkView = source;
		this.targetNetworkView = target;
	}
	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		taskMonitor.setTitle("Copying network location");
		
		Double height = sourceNetworkView.getVisualProperty(BasicVisualLexicon.NETWORK_HEIGHT);
		Double width = sourceNetworkView.getVisualProperty(BasicVisualLexicon.NETWORK_WIDTH);
		Double scale = sourceNetworkView.getVisualProperty(BasicVisualLexicon.NETWORK_SCALE_FACTOR);

		targetNetworkView.setVisualProperty(BasicVisualLexicon.NETWORK_HEIGHT, height);
		targetNetworkView.setVisualProperty(BasicVisualLexicon.NETWORK_WIDTH, width);
		targetNetworkView.setVisualProperty(BasicVisualLexicon.NETWORK_SCALE_FACTOR, scale);
		
		Double x_center = sourceNetworkView.getVisualProperty(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION);
		Double y_center = sourceNetworkView.getVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION);
		Double z_center = sourceNetworkView.getVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Z_LOCATION);

		targetNetworkView.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION, x_center);
		targetNetworkView.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION, y_center);
		targetNetworkView.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Z_LOCATION, z_center);

		
		
	}

}
