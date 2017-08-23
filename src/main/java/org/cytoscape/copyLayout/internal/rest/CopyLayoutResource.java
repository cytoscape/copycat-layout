package org.cytoscape.copyLayout.internal.rest;

import java.util.ArrayList;
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
import org.cytoscape.ci.CIResponseFactory;
import org.cytoscape.ci.model.CIError;
import org.cytoscape.ci.model.CIResponse;
import org.cytoscape.copyLayout.internal.task.CopyLayoutTaskFactory;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyTable;
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

@Api(tags = { "Apps: Copy Layout" })
@Path("/v1/layout/")
public class CopyLayoutResource {

	private final CyApplicationManager cyApplicationManager;

	private final CyNetworkManager cyNetworkManager;
	private final CyNetworkViewManager cyNetworkViewManager;

	private final CopyLayoutTaskFactory copyLayoutTaskFactory;
	private final SynchronousTaskManager<?> taskManager;

	private final CIExceptionFactory ciExceptionFactory;
	private final CIResponseFactory ciResponseFactory;
	private final CIErrorFactory ciErrorFactory;

	public static final String CY_NETWORK_NOT_FOUND_CODE = "1";
	public static final String CY_NETWORK_VIEW_NOT_FOUND_CODE = "2";
	public static final String TASK_EXECUTION_ERROR_CODE = "3";

	private static final String GENERIC_SWAGGER_NOTES = "Copy one network view layout onto another, "
			+ "setting the node location and view scale to match. This makes visually comparing networks simple." + '\n'
			+ '\n';

	public CopyLayoutResource(final CyApplicationManager cyApplicationManager,
			final SynchronousTaskManager<?> taskManager, final CyNetworkManager cyNetworkManager,
			final CyNetworkViewManager cyNetworkViewManager, final CopyLayoutTaskFactory copyLayoutTaskFactory,
			final CIResponseFactory ciResponseFactory, final CIExceptionFactory ciExceptionFactory,
			final CIErrorFactory ciErrorFactory) {
		this.cyApplicationManager = cyApplicationManager;
		this.taskManager = taskManager;
		this.cyNetworkManager = cyNetworkManager;
		this.cyNetworkViewManager = cyNetworkViewManager;
		this.copyLayoutTaskFactory = copyLayoutTaskFactory;
		this.ciExceptionFactory = ciExceptionFactory;
		this.ciResponseFactory = ciResponseFactory;
		this.ciErrorFactory = ciErrorFactory;

	}

	private static final Logger logger = LoggerFactory.getLogger(CopyLayoutResource.class);

	private final static String resourceErrorRoot = "urn:cytoscape:ci:copyLayout-app:v1";

	private CIError buildCIError(int status, String resourcePath, String code, String message, Exception e) {
		return ciErrorFactory.getCIError(status, resourceErrorRoot + ":" + resourcePath + ":" + code, message);
	}

	CIResponse<Object> buildCIErrorResponse(int status, String resourcePath, String code, String message, Exception e) {
		CIResponse<Object> response = ciResponseFactory.getCIResponse(new Object());

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

	public CyNetwork getCyNetwork(String resourcePath, String errorType, long networkSUID) {
		final CyNetwork cyNetwork = cyNetworkManager.getNetwork(networkSUID);

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

	@ApiModel(value = "Copy Layout Response", description = "Copy Layout Results in CI Format", parent = CIResponse.class)
	public static class CopyLayoutResponse extends CIResponse<CopyLayoutParameters> {
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("currentView/copy")
	@ApiOperation(value = "Copy the current network view layout to another view", notes = GENERIC_SWAGGER_NOTES, response = CopyLayoutResponse.class)
	@ApiResponses(value = {
			@ApiResponse(code = 404, message = "Network or Network View does not exist", response = CIResponse.class) })
	public Response copyLayout(
			@ApiParam(value = "Copy Layout Parameters", required = true) CopyLayoutParameters copyLayoutParameters) {
		CyNetwork cyNetwork = getCyNetwork("copy_current_layout", CY_NETWORK_NOT_FOUND_CODE);
		CyNetworkView cyNetworkView = getCyNetworkView("copy_current_layout", CY_NETWORK_VIEW_NOT_FOUND_CODE);

		return copyLayout(cyNetwork.getSUID(), cyNetworkView.getSUID(), copyLayoutParameters);
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("{networkSUID}/views/{networkViewSUID}/copy")
	@ApiOperation(value = "Execute copy layout on a specific network view", notes = GENERIC_SWAGGER_NOTES, response = CopyLayoutResponse.class)
	@ApiResponses(value = { @ApiResponse(code = 404, message = "Network does not exist", response = CIResponse.class) })

	public Response copyLayout(
			@ApiParam(value = "Network SUID (see GET /v1/networks)") @PathParam("networkSUID") long networkSUID,
			@ApiParam(value = "Network View SUID (see GET /v1/networks/{networkId}/views)") @PathParam("networkViewSUID") long networkViewSUID,
			@ApiParam(value = "Copy Layout Parameters", required = true) CopyLayoutParameters copyLayoutParameters) {

		CyNetworkView cyNetworkView = getCyNetworkView("copy_layout", CY_NETWORK_VIEW_NOT_FOUND_CODE, networkSUID,
				networkViewSUID);
		CopyLayoutTaskObserver taskObserver = new CopyLayoutTaskObserver(this, "copy_layout", TASK_EXECUTION_ERROR_CODE);

		Map<String, Object> tunableMap = new HashMap<String, Object>();

		ListSingleSelection<String> toNetwork = new ListSingleSelection<String>();
		ListSingleSelection<String> toColumn = new ListSingleSelection<String>();
		ListSingleSelection<String> fromColumn = new ListSingleSelection<String>();
		ListSingleSelection<String> fromNetwork = new ListSingleSelection<String>();

		CyNetwork net = getCyNetwork("fromNetwork", CY_NETWORK_VIEW_NOT_FOUND_CODE, networkSUID);
		CyTable table = net.getDefaultNodeTable();
		ArrayList<String> cols = new ArrayList<String>();
		for (CyColumn col : table.getColumns()) {
			cols.add(col.getName());
		}
		if (!cols.contains(copyLayoutParameters.fromColumn)){
			String errorString = "Source column does not describe a column in the source network";
			throw ciExceptionFactory.getCIException(404, new CIError[]{this.buildCIError(404, "copy_layout", TASK_EXECUTION_ERROR_CODE, errorString, null)});		
		}
		
		fromColumn.setPossibleValues(cols);
		fromColumn.setSelectedValue(copyLayoutParameters.fromColumn);

		ArrayList<String> networkNames = new ArrayList<String>();
		for (CyNetwork toNet : cyNetworkManager.getNetworkSet()) {
			String toName = toNet.getRow(toNet).get(CyNetwork.NAME, String.class);
			networkNames.add(toName);
		}
		if (!networkNames.contains(copyLayoutParameters.toNetwork)){
			String errorString = "Target network not found";
			throw ciExceptionFactory.getCIException(404, new CIError[]{this.buildCIError(404, "copy_layout", TASK_EXECUTION_ERROR_CODE, errorString, null)});		
		}
		toNetwork.setPossibleValues(networkNames);
		toNetwork.setSelectedValue(copyLayoutParameters.toNetwork);

		ArrayList<String> toColumnNames = new ArrayList<String>();
		ArrayList<String> toCols = new ArrayList<String>();
		
		for (CyNetwork network : cyNetworkManager.getNetworkSet()){
			if (network.getRow(network).get(CyNetwork.NAME, String.class).equals(toNetwork.getSelectedValue())){
				CyTable toTable = network.getDefaultNodeTable();
				for (CyColumn col : toTable.getColumns()) {
					toCols.add(col.getName());
				}
				break;
			}
		}
		
		if (!toCols.contains(copyLayoutParameters.toColumn)){
			String errorString = "Target column does not describe a column in the target network";
			throw ciExceptionFactory.getCIException(404, new CIError[]{this.buildCIError(404, "copy_layout", TASK_EXECUTION_ERROR_CODE, errorString, null)});		
		}
		
		toColumnNames.add(copyLayoutParameters.toColumn);
		toColumn.setSelectedValue(copyLayoutParameters.toColumn);

		fromNetwork.setSelectedValue(net.getRow(net).get(CyNetwork.NAME, String.class));

		if (fromNetwork.getSelectedValue().equals(toNetwork.getSelectedValue())) {
			String errorString = "Source and target layout network are the same";
			throw ciExceptionFactory.getCIException(400, new CIError[]{this.buildCIError(400, "copy_layout", TASK_EXECUTION_ERROR_CODE, errorString, null)});		
			
		}
		tunableMap.put("fromNetwork", fromNetwork);
		tunableMap.put("fromColumn", fromColumn);
		tunableMap.put("toNetwork", toNetwork);
		tunableMap.put("toColumn", toColumn);

		TaskIterator taskIterator = copyLayoutTaskFactory.createTaskIterator(cyNetworkView);

		taskManager.setExecutionContext(tunableMap);
		taskManager.execute(taskIterator, taskObserver);
		
		return Response
				.status(taskObserver.response.errors.size() == 0 ? Response.Status.OK
						: Response.Status.INTERNAL_SERVER_ERROR)
				.type(MediaType.APPLICATION_JSON).entity(taskObserver.response).build();
	}
}
