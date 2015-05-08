package edu.ucsf.rbvi.layoutSaver.internal;

import static org.cytoscape.work.ServiceProperties.COMMAND;
import static org.cytoscape.work.ServiceProperties.COMMAND_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_NAMESPACE;
import static org.cytoscape.work.ServiceProperties.IN_MENU_BAR;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

import java.util.Properties;

import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.osgi.framework.BundleContext;

import edu.ucsf.rbvi.layoutSaver.internal.tasks.MapLayoutTaskFactory;

public class CyActivator extends AbstractCyActivator {

	public CyActivator() {
		super();
	}

	public void start(BundleContext bc) {

		CyNetworkViewManager viewManager = 
				getService(bc, CyNetworkViewManager.class);

		MapLayoutTaskFactory layoutMapper = 
					new MapLayoutTaskFactory(viewManager);
		Properties settingsProps = new Properties();
		settingsProps.setProperty(PREFERRED_MENU, "Apps.LayoutMapper");
		settingsProps.setProperty(TITLE, "Map layout");
		settingsProps.setProperty(IN_MENU_BAR, "true");
		settingsProps.setProperty(MENU_GRAVITY, "1.0");
		registerService(bc, layoutMapper, NetworkViewTaskFactory.class, settingsProps);

	}
}
