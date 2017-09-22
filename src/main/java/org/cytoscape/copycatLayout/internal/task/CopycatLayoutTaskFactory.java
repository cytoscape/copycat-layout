package org.cytoscape.copycatLayout.internal.task;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class CopycatLayoutTaskFactory extends AbstractTaskFactory {
	private final CyNetworkViewManager viewManager;
	private final CyLayoutAlgorithmManager algoManager;
	private final CyApplicationManager appManager;

	public CopycatLayoutTaskFactory(final CyApplicationManager appManager, final CyNetworkViewManager viewManager,
			final CyLayoutAlgorithmManager algoManager) {
		super();
		this.viewManager = viewManager;
		this.algoManager = algoManager;
		this.appManager = appManager;
	}
	
	@Override
	public TaskIterator createTaskIterator() {
		TaskIterator ti = new TaskIterator();
		CopycatLayoutTask task = new CopycatLayoutTask(appManager, viewManager, algoManager, ti);
		ti.append(task);
		return ti;
	}
	
	@Override
	public boolean isReady() {
		return viewManager.getNetworkViewSet().size() >= 2;
	}

}
