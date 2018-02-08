package org.cytoscape.copycatLayout.internal;

/*
 * #%L
 * Cytoscape Prefuse Layout Impl (layout-prefuse-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2013 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import java.util.Properties;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.ci.CIErrorFactory;
import org.cytoscape.ci.CIExceptionFactory;
import org.cytoscape.ci.CIResponseFactory;
import org.cytoscape.copycatLayout.internal.rest.CopycatLayoutResource;
import org.cytoscape.copycatLayout.internal.task.CopycatLayoutTaskFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.SynchronousTaskManager;
import org.osgi.framework.BundleContext;

import static org.cytoscape.work.ServiceProperties.*;

public class CyActivator extends AbstractCyActivator {
	public CyActivator() {
		super();
	}

	public void start(BundleContext bc) {
		CyNetworkViewManager viewManager = getService(bc, CyNetworkViewManager.class);
		CyLayoutAlgorithmManager cyLayoutAlgoManager = getService(bc, CyLayoutAlgorithmManager.class);
		CyApplicationManager cyApplicationManager = getService(bc, CyApplicationManager.class);

		Properties copycatLayoutOpsProps = new Properties();
		copycatLayoutOpsProps.setProperty(PREFERRED_MENU, "Layout");
		copycatLayoutOpsProps.setProperty("preferredTaskManager", "menu");
		copycatLayoutOpsProps.setProperty(TITLE, "Copycat Layout");
		copycatLayoutOpsProps.setProperty(MENU_GRAVITY, "5.55");

		copycatLayoutOpsProps.setProperty(COMMAND_NAMESPACE, "layout");
		copycatLayoutOpsProps.setProperty(COMMAND, "copycat");
		copycatLayoutOpsProps.setProperty(COMMAND_DESCRIPTION, "Copy network layout from one network view to another");
		copycatLayoutOpsProps.setProperty(COMMAND_LONG_DESCRIPTION,
				"Sets the coordinates for each node in the target network to the coordinates of a matching node in the source network.\n\nOptional parameters such as ```gridUnmapped``` and ```selectUnmapped``` determine the behavior of target network nodes that could not be matched.");
		copycatLayoutOpsProps.setProperty(COMMAND_SUPPORTS_JSON, "true");
		copycatLayoutOpsProps.setProperty(COMMAND_EXAMPLE_JSON, "{\n  \"mappedNodeCount\": 100,\n  \"unmappedNodeCount\": 0\n}");
		CopycatLayoutTaskFactory copycatLayout = new CopycatLayoutTaskFactory(cyApplicationManager, viewManager, cyLayoutAlgoManager);
		registerAllServices(bc, copycatLayout, copycatLayoutOpsProps);

		SynchronousTaskManager<Object> taskManager = getService(bc, SynchronousTaskManager.class);
		final CyNetworkManager cyNetworkManager = getService(bc, CyNetworkManager.class);
		final CyNetworkViewManager cyNetworkViewManager = getService(bc, CyNetworkViewManager.class);

		CIResponseFactory ciResponseFactory = this.getService(bc, CIResponseFactory.class);
		CIExceptionFactory ciExceptionFactory = this.getService(bc, CIExceptionFactory.class);
		CIErrorFactory ciErrorFactory = this.getService(bc, CIErrorFactory.class);

		CopycatLayoutResource resource = new CopycatLayoutResource(cyApplicationManager, taskManager, cyNetworkManager,
				cyNetworkViewManager, copycatLayout, ciResponseFactory, ciExceptionFactory, ciErrorFactory);
		registerService(bc, resource, CopycatLayoutResource.class, new Properties());

	}
}
