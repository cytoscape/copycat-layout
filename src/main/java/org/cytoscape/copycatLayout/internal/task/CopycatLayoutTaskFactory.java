package org.cytoscape.copycatLayout.internal.task;

import org.cytoscape.task.AbstractNetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.TaskIterator;

public class CopycatLayoutTaskFactory extends AbstractNetworkViewTaskFactory {
	CyNetworkViewManager viewManager;

	public CopycatLayoutTaskFactory(CyNetworkViewManager viewManager) {
		super();
		this.viewManager = viewManager;
	}

	public TaskIterator createTaskIterator(CyNetworkView arg0) {
		if (arg0 == null) {
			return null;
			//return new TaskIterator(new CopycatLayoutTask(viewManager));
		}
		return new TaskIterator(new CopycatLayoutTask(arg0, viewManager));
	}

	public boolean isReady(CyNetworkView arg0) {
		return viewManager.getNetworkViewSet().size() >= 2;
	}

}
