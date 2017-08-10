package org.cytoscape.layoutMapper.internal.rest;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.ci.CIErrorFactory;
import org.cytoscape.ci.CIExceptionFactory;
import org.cytoscape.ci.model.CIError;
import org.cytoscape.ci.model.CIResponse;
import org.cytoscape.ci_bridge_impl.CIProvider;
import org.cytoscape.layoutMapper.internal.task.MapLayoutTaskFactory;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.util.ListSingleSelection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Api(tags = { "Apps: Map Layout" })
@Path("/map_layout/")
public class MapLayoutResource {

	private final CyApplicationManager cyApplicationManager;

	private final CyNetworkManager cyNetworkManager;
	private final CyNetworkViewManager cyNetworkViewManager;

	private final MapLayoutTaskFactory mapLayoutTaskFactory;
	private final SynchronousTaskManager<?> taskManager;

	private final CIExceptionFactory ciExceptionFactory;
	private final CIErrorFactory ciErrorFactory;

	public static final String CY_NETWORK_NOT_FOUND_CODE = "1";
	public static final String CY_NETWORK_VIEW_NOT_FOUND_CODE = "2";
	public static final String TASK_EXECUTION_ERROR_CODE = "3";

	private static final String GENERIC_SWAGGER_NOTES = "Map Layouts will transfer one network view layout onto another,"
			+ "setting the node location and view scale to match. This makes visually comparing networks simple." + '\n'
			+ '\n';

	public MapLayoutResource(final CyApplicationManager cyApplicationManager,
			final SynchronousTaskManager<?> taskManager, final CyNetworkManager cyNetworkManager,
			final CyNetworkViewManager cyNetworkViewManager, final MapLayoutTaskFactory mapLayoutTaskFactory,
			final CIExceptionFactory ciExceptionFactory, final CIErrorFactory ciErrorFactory) {
		this.cyApplicationManager = cyApplicationManager;
		this.taskManager = taskManager;
		this.cyNetworkManager = cyNetworkManager;
		this.cyNetworkViewManager = cyNetworkViewManager;
		this.mapLayoutTaskFactory = mapLayoutTaskFactory;
		this.ciExceptionFactory = ciExceptionFactory;
		this.ciErrorFactory = ciErrorFactory;

	}

	private static final Logger logger = LoggerFactory.getLogger(MapLayoutResource.class);

	private final static String resourceErrorRoot = "urn:cytoscape:ci:mapLayout-app:v1";

	private CIError buildCIError(int status, String resourcePath, String code, String message, Exception e) {
		return ciErrorFactory.getCIError(status, resourceErrorRoot + ":" + resourcePath + ":" + code, message);
	}

	CIResponse<Object> buildCIErrorResponse(int status, String resourcePath, String code, String message, Exception e) {
		CIResponse<Object> response = CIProvider.getCIResponseFactory().getCIResponse(new Object());

		CIError error = buildCIError(status, resourcePath, code, message, e);
		if (e != null) {
			logger.error(message, e);

		} else {
			logger.error(message);
		}

		response.errors.add(error);
		return response;
	}

	public CyNetwork getCyNetwork(String resourcePath, String errorType) {
		CyNetwork cyNetwork = cyApplicationManager.getCurrentNetwork();

		if (cyNetwork == null) {
			String messageString = "Could not find current Network";
			throw ciExceptionFactory.getCIException(404,
					new CIError[] { this.buildCIError(404, resourcePath, errorType, messageString, null) });
		}
		return cyNetwork;
	}

	public CyNetworkView getCyNetworkView(String resourcePath, String errorType) {
		CyNetworkView cyNetworkView = cyApplicationManager.getCurrentNetworkView();
		if (cyNetworkView == null) {
			String messageString = "Could not find current Network View";
			throw ciExceptionFactory.getCIException(404,
					new CIError[] { this.buildCIError(404, resourcePath, errorType, messageString, null) });
		}
		return cyNetworkView;
	}

	public CyNetworkView getCyNetworkView(String resourcePath, String errorType, long networkSUID,
			long networkViewSUID) {
		final CyNetwork network = cyNetworkManager.getNetwork(networkSUID);
		if (network == null) {
			String messageString = "Could not find network with SUID: " + networkSUID;
			throw ciExceptionFactory.getCIException(404,
					new CIError[] { this.buildCIError(404, resourcePath, errorType, messageString, null) });
		}
		final Collection<CyNetworkView> views = cyNetworkViewManager.getNetworkViews(network);
		if (views.isEmpty()) {
			String messageString = "No views are available for network with SUID: " + networkSUID;
			throw ciExceptionFactory.getCIException(404,
					new CIError[] { this.buildCIError(404, resourcePath, errorType, messageString, null) });
		}
		for (final CyNetworkView view : views) {
			final Long vid = view.getSUID();
			if (vid.equals(networkViewSUID)) {
				return view;
			}
		}
		String messageString = "Could not find network view with SUID: " + networkViewSUID + " for network with SUID: "
				+ networkSUID;
		throw ciExceptionFactory.getCIException(404,
				new CIError[] { this.buildCIError(404, resourcePath, errorType, messageString, null) });
	}

	@ApiModel(value = "Map Layout Response", description = "Map Layout Results in CI Format", parent = CIResponse.class)
	public static class MapLayoutResponse extends CIResponse<String> {
	}

	@POST
	@Produces("application/json")
	@Consumes("text/plain")
	@Path("currentView/map_layout")
	@ApiOperation(value = "Map the current network view layout to another view", notes = GENERIC_SWAGGER_NOTES, response = MapLayoutResponse.class)
	@ApiResponses(value = {
			@ApiResponse(code = 404, message = "Network or Network View does not exist", response = CIResponse.class) })
	public Response mapLayout(
			@ApiParam(value = "Map Layout Parameters", required = true) MapLayoutParameters mapLayoutParameters) {
		CyNetwork cyNetwork = getCyNetwork("map_current_layout", CY_NETWORK_NOT_FOUND_CODE);
		CyNetworkView cyNetworkView = getCyNetworkView("map_current_layout", CY_NETWORK_VIEW_NOT_FOUND_CODE);

		return mapLayout(cyNetwork.getSUID(), cyNetworkView.getSUID(), mapLayoutParameters);
	}

	@POST
	@Produces("application/json")
	@Consumes("text/plain")
	@Path("{networkSUID}/views/{networkViewSUID}/map_layout")
	@ApiOperation(value = "Execute map layout on a Specific Network View", notes = GENERIC_SWAGGER_NOTES, response = MapLayoutResponse.class)
	@ApiResponses(value = { @ApiResponse(code = 404, message = "Network does not exist", response = CIResponse.class) })

	public Response mapLayout(
			@ApiParam(value = "Network SUID (see GET /v1/networks)") @PathParam("networkSUID") long networkSUID,
			@ApiParam(value = "Network View SUID (see GET /v1/networks/{networkId}/views)") @PathParam("networkViewSUID") long networkViewSUID,
			@ApiParam(value = "Map Layout Parameters", required = true) MapLayoutParameters mapLayoutParameters) {

		System.out.println("Mapping Layout via REST");
		CyNetworkView cyNetworkView = getCyNetworkView("map_layout", CY_NETWORK_VIEW_NOT_FOUND_CODE, networkSUID,
				networkViewSUID);
		MapLayoutTaskObserver taskObserver = new MapLayoutTaskObserver(this, "map_layout", TASK_EXECUTION_ERROR_CODE);
		

		Map<String, Object> tunableMap = new HashMap<String, Object>();

		ListSingleSelection<String> toNetwork = new ListSingleSelection<String>();
		ListSingleSelection<String> toColumn = new ListSingleSelection<String>();
		ListSingleSelection<String> fromNetwork = new ListSingleSelection<String>();
		ListSingleSelection<String> fromColumn = new ListSingleSelection<String>();

		toNetwork.setSelectedValue(mapLayoutParameters.toNetworkName);
		toColumn.setSelectedValue(mapLayoutParameters.toColumnName);
		fromNetwork.setSelectedValue(mapLayoutParameters.fromNetworkName);
		fromColumn.setSelectedValue(mapLayoutParameters.fromColumnName);

		tunableMap.put("toNetworkName", toNetwork);
		tunableMap.put("toColumnName", toColumn);

		TaskIterator taskIterator = mapLayoutTaskFactory.createTaskIterator(cyNetworkView);
		
		taskManager.setExecutionContext(tunableMap);
		taskManager.execute(taskIterator, taskObserver);

		return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity("SUCCESS").build();
	}
}
