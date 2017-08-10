package org.cytoscape.layoutMapper.internal.task;

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

	public TaskIterator createTaskIterator(CyNetworkView arg0) {
		if (arg0 == null) {
			return new TaskIterator(new MapLayoutTask(viewManager));
		}
		return new TaskIterator(new MapLayoutTask(arg0, viewManager));
	}

	public boolean isReady(CyNetworkView arg0) {
		return true;
	}

}
