package org.cytoscape.layoutMapper.internal;

import static org.cytoscape.work.ServiceProperties.COMMAND;
import static org.cytoscape.work.ServiceProperties.COMMAND_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_NAMESPACE;
import static org.cytoscape.work.ServiceProperties.IN_MENU_BAR;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;
import static org.cytoscape.work.ServiceProperties.TOOL_BAR_GRAVITY;

import java.util.Properties;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.ci.CIErrorFactory;
import org.cytoscape.ci.CIExceptionFactory;
import org.cytoscape.ci.CIResponseFactory;
import org.cytoscape.layoutMapper.internal.rest.MapLayoutResource;
import org.cytoscape.layoutMapper.internal.task.MapLayoutTaskFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.SynchronousTaskManager;
import org.osgi.framework.BundleContext;

public class CyActivator extends AbstractCyActivator {

	public CyActivator() {
		super();
	}

	public void start(BundleContext bc) {

		CyNetworkViewManager viewManager = getService(bc, CyNetworkViewManager.class);

		MapLayoutTaskFactory layoutMapper = new MapLayoutTaskFactory(viewManager);
		Properties settingsProps = new Properties();
		settingsProps.setProperty(PREFERRED_MENU, "Layout");
		settingsProps.setProperty(TITLE, "Map layout");
		settingsProps.setProperty(TOOL_BAR_GRAVITY, "7.0");
		settingsProps.setProperty(IN_MENU_BAR, "true");
		settingsProps.setProperty(MENU_GRAVITY, "5.0");

		settingsProps.setProperty(COMMAND_NAMESPACE, "layout");
		settingsProps.setProperty(COMMAND, "map");
		settingsProps.setProperty(COMMAND_DESCRIPTION, "Map network layout from one network view to another");

		registerService(bc, layoutMapper, NetworkViewTaskFactory.class, settingsProps);

		CyApplicationManager cyApplicationManager = getService(bc, CyApplicationManager.class);

		SynchronousTaskManager<?> taskManager = getService(bc, SynchronousTaskManager.class);
		final CyNetworkManager cyNetworkManager = getService(bc, CyNetworkManager.class);
		final CyNetworkViewManager cyNetworkViewManager = getService(bc, CyNetworkViewManager.class);
		
		CIResponseFactory ciResponseFactory = this.getService(bc, CIResponseFactory.class);
		CIExceptionFactory ciExceptionFactory = this.getService(bc, CIExceptionFactory.class);
		CIErrorFactory ciErrorFactory = this.getService(bc, CIErrorFactory.class);

		MapLayoutResource resource = new MapLayoutResource(cyApplicationManager, taskManager, cyNetworkManager,
				cyNetworkViewManager, layoutMapper, ciResponseFactory, ciExceptionFactory, ciErrorFactory);
		registerService(bc, resource, MapLayoutResource.class, new Properties());

	}
}
