package edu.ucsf.rbvi.layoutSaver.internal.tasks;

import org.cytoscape.task.AbstractNetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.TaskIterator;

public class MapLayoutTaskFactory extends AbstractNetworkViewTaskFactory {
	CyNetworkViewManager viewManager;

	public MapLayoutTaskFactory(CyNetworkViewManager viewManager) {
		super();
		this.viewManager = viewManager;
	}
	
	public boolean isReady(CyNetworkView networkView) {
		if (super.isReady(networkView))
			return true;
		else return false;
	}
	
	public TaskIterator createTaskIterator(CyNetworkView arg0) {
		MapLayoutTask task = new MapLayoutTask(arg0, viewManager);
		return new TaskIterator(task);
	}

}
