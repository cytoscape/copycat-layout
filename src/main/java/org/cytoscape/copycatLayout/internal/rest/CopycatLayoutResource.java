package org.cytoscape.copycatLayout.internal.rest;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
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
import org.cytoscape.copycatLayout.internal.task.CopycatLayoutTaskFactory;
import org.cytoscape.model.CyColumn;
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

@Api(tags = { "Layouts" })
@Path("/v1/apply/layouts/copycat")
public class CopycatLayoutResource {

	private final CyApplicationManager cyApplicationManager;

	private final CyNetworkManager cyNetworkManager;
	private final CyNetworkViewManager cyNetworkViewManager;

	private final CopycatLayoutTaskFactory copycatLayoutTaskFactory;
	private final SynchronousTaskManager<?> taskManager;

	private final CIExceptionFactory ciExceptionFactory;
	private final CIResponseFactory ciResponseFactory;
	private final CIErrorFactory ciErrorFactory;

	public static final String SOURCE_NETWORK_VIEW_NOT_FOUND = "1";
	public static final String SOURCE_COLUMN_NOT_FOUND = "2";
	public static final String SOURCE_COLUMN_UNSUPPORTED = "3";

	public static final String TARGET_NETWORK_VIEW_NOT_FOUND = "4";
	public static final String TARGET_COLUMN_NOT_FOUND = "5";
	public static final String TARGET_COLUMN_UNSUPPORTED = "6";
	public static final String TASK_EXECUTION_ERROR = "7";

	private static final String GENERIC_SWAGGER_NOTES = "Copy one network view layout onto another, "
			+ "setting the node location and view scale to match. This makes visually comparing networks simple." + '\n'
			+ '\n';

	public CopycatLayoutResource(final CyApplicationManager cyApplicationManager,
			final SynchronousTaskManager<?> taskManager, final CyNetworkManager cyNetworkManager,
			final CyNetworkViewManager cyNetworkViewManager, final CopycatLayoutTaskFactory copycatLayoutTaskFactory,
			final CIResponseFactory ciResponseFactory, final CIExceptionFactory ciExceptionFactory,
			final CIErrorFactory ciErrorFactory) {
		this.cyApplicationManager = cyApplicationManager;
		this.taskManager = taskManager;
		this.cyNetworkManager = cyNetworkManager;
		this.cyNetworkViewManager = cyNetworkViewManager;
		this.copycatLayoutTaskFactory = copycatLayoutTaskFactory;
		this.ciExceptionFactory = ciExceptionFactory;
		this.ciResponseFactory = ciResponseFactory;
		this.ciErrorFactory = ciErrorFactory;

	}

	private static final Logger logger = LoggerFactory.getLogger(CopycatLayoutResource.class);

	private final static String resourceErrorRoot = "urn:cytoscape:ci:copycatLayout-app:v1";

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

	public CyNetworkView getCyNetworkView(String resourcePath, String errorType) {
		CyNetworkView cyNetworkView = cyApplicationManager.getCurrentNetworkView();
		if (cyNetworkView == null) {
			String messageString = "Could not find current Network View";
			throw ciExceptionFactory.getCIException(404,
					new CIError[] { this.buildCIError(404, resourcePath, errorType, messageString, null) });
		}
		return cyNetworkView;
	}

	public CyNetworkView getCyNetworkView(String resourcePath, String errorType, long networkViewSUID) {
		for (CyNetwork network : cyNetworkManager.getNetworkSet()) {

			final Collection<CyNetworkView> views = cyNetworkViewManager.getNetworkViews(network);

			for (final CyNetworkView view : views) {
				final Long vid = view.getSUID();
				if (vid.equals(networkViewSUID)) {
					return view;
				}
			}
		}
		throw ciExceptionFactory.getCIException(404, new CIError[] { this.buildCIError(404, resourcePath, errorType,
				"No network view with SUID: " + networkViewSUID, null) });
	}

	@ApiModel(value = "Copycat Layout Response", description = "Number of successfully and unsuccessfully mappes node locations", parent = CIResponse.class)
	public static class CopycatLayoutResponse extends CIResponse<CopycatLayoutResult> {
	}

	private String validateNetworkName(long viewSUID, String column, String networkType) {
		String name = null;
		try {
			name = getNetworkNameWithColumn(viewSUID, column);
		} catch (NetworkViewNotFoundException e) {
			throw ciExceptionFactory.getCIException(404,
					new CIError[] { buildCIError(404, "copycat-layout",
							networkType == "Source" ? SOURCE_NETWORK_VIEW_NOT_FOUND : TARGET_NETWORK_VIEW_NOT_FOUND,
							networkType + " network view with SUID " + viewSUID + " does not exist.", e) });
		} catch (NodeColumnNotFoundException e) {
			throw ciExceptionFactory.getCIException(404,
					new CIError[] { buildCIError(404, "copycat-layout",
							networkType == "Source" ? SOURCE_COLUMN_NOT_FOUND : TARGET_COLUMN_NOT_FOUND,
							networkType + " column " + column + " not found in network.", e) });

		} catch (UnsupportedColumnTypeException e) {
			throw ciExceptionFactory.getCIException(404,
					new CIError[] { buildCIError(404, "copycat-layout",
							networkType == "Source" ? SOURCE_COLUMN_UNSUPPORTED : TARGET_COLUMN_UNSUPPORTED,
							networkType + " column " + column + " must be of type String or Integer.", e) });
		}
		return name;
	}
	

	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{sourceViewSUID}/{targetViewSUID}")
	@ApiOperation(value = "Copy network view layout to another view", notes = GENERIC_SWAGGER_NOTES, response = CopycatLayoutResponse.class)
	@ApiResponses(value = {
			@ApiResponse(code = 404, message = "Network View does not exist", response = CIResponse.class) })
	public Response copyCurrentLayout(@PathParam("sourceViewSUID") Long sourceViewSUID, @PathParam("targetViewSUID") Long targetViewSUID,
			@ApiParam(value = "Clone the specified network view layout onto another network view", required = true) CopycatWithViewSUIDsLayoutParameters params) {
		return copyLayout(sourceViewSUID, params.sourceColumn, targetViewSUID, params.targetColumn,
				params.selectUnmapped, params.gridUnmapped);
	}
	
	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/current/{targetViewSUID}")
	@ApiOperation(hidden=true, value = "Copy current network view layout to another view", notes = GENERIC_SWAGGER_NOTES, response = CopycatLayoutResponse.class)
	@ApiResponses(value = {
			@ApiResponse(code = 404, message = "Network View does not exist", response = CIResponse.class) })
	public Response copyCurrentLayout(@PathParam("targetViewSUID") Long targetViewSUID,
			@ApiParam(value = "Clone the current network view layout onto another network view", required = true) CopycatWithViewSUIDsLayoutParameters params) {
		CyNetworkView sourceView = cyApplicationManager.getCurrentNetworkView();
		if (sourceView == null) {
			throw ciExceptionFactory.getCIException(404, new CIError[] { buildCIError(404, "copycat-current-layout",
					SOURCE_NETWORK_VIEW_NOT_FOUND, "No current network selected", null) });
		}
		long sourceViewSUID = sourceView.getSUID();
		return copyLayout(sourceViewSUID, params.sourceColumn, targetViewSUID, params.targetColumn,
				params.selectUnmapped, params.gridUnmapped);
	}
	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{sourceViewSUID}/current")
	@ApiOperation(hidden=true, value = "Copy a network view layout to the current view", notes = GENERIC_SWAGGER_NOTES, response = CopycatLayoutResponse.class)
	@ApiResponses(value = {
			@ApiResponse(code = 404, message = "Network View does not exist", response = CIResponse.class) })
	public Response copyToCurrentLayout(@PathParam("sourceViewSUID") Long sourceViewSUID,
			@ApiParam(value = "Clone the specified network view layout onto the current network view", required = true) CopycatWithViewSUIDsLayoutParameters params) {
		CyNetworkView targetView = cyApplicationManager.getCurrentNetworkView();
		if (targetView == null) {
			throw ciExceptionFactory.getCIException(404, new CIError[] { buildCIError(404, "copycat-to-current-layout",
					TARGET_NETWORK_VIEW_NOT_FOUND, "No current network selected", null) });
		}
		long targetViewSUID = targetView.getSUID();
		return copyLayout(sourceViewSUID, params.sourceColumn, targetViewSUID, params.targetColumn,
				params.selectUnmapped, params.gridUnmapped);
	}
	/*
	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/")
	@ApiOperation(value = "Copy one network view layout to another view", notes = GENERIC_SWAGGER_NOTES, response = CopycatLayoutResult.class)
	@ApiResponses(value = {
			@ApiResponse(code = 404, message = "Network View does not exist", response = CIResponse.class) })
	public Response copyLayout(
			@ApiParam(value = "Copycat Layout Parameters", required = true) CopycatLayoutParameters params) {
		return copyLayout(params.sourceNetworkViewSUID, params.sourceColumn, params.targetNetworkViewSUID,
				params.targetColumn, params.selectUnmapped, params.gridUnmapped);

	}
	*/
	
	private Response copyLayout(long sourceViewSUID, String sourceColumn, long targetViewSUID, String targetColumn,
			boolean selectUnmapped, boolean gridUnmapped) {
		CopycatLayoutTaskObserver taskObserver = new CopycatLayoutTaskObserver(this, "copycat_layout",
				TASK_EXECUTION_ERROR);
		Map<String, Object> tunableMap = new HashMap<String, Object>();
		
		TaskIterator taskIterator = copycatLayoutTaskFactory.createTaskIterator();
		
		String sourceName = validateNetworkName(sourceViewSUID, sourceColumn, "Source");
		String targetName = validateNetworkName(targetViewSUID, targetColumn, "Target");

		if (sourceViewSUID == targetViewSUID) {
			throw ciExceptionFactory.getCIException(400, new CIError[] { buildCIError(400, "copycat-layout",
					TASK_EXECUTION_ERROR, "Source and destination network views cannot be the same.", null) });
		}
		
		ListSingleSelection<String> sourceList = new ListSingleSelection<String>(sourceName);
		sourceList.setSelectedValue(sourceName);
		
		ListSingleSelection<String> sourceColumnList = new ListSingleSelection<String>(sourceColumn);
		sourceColumnList.setSelectedValue(sourceColumn);
		
		ListSingleSelection<String> targetList = new ListSingleSelection<String>(targetName);
		targetList.setSelectedValue(targetName);
		
		ListSingleSelection<String> targetColumnList = new ListSingleSelection<String>(targetColumn);
		targetColumnList.setSelectedValue(targetColumn);
		
		
		tunableMap.put("sourceNetwork", sourceList);
		tunableMap.put("targetNetwork", targetList);

		tunableMap.put("sourceColumn", sourceColumnList);
		tunableMap.put("targetColumn", targetColumnList);
		
		tunableMap.put("selectUnmapped", selectUnmapped);
		tunableMap.put("gridUnmapped", gridUnmapped);

		taskManager.setExecutionContext(tunableMap);
		taskManager.execute(taskIterator, taskObserver);
		
		return Response
				.status(taskObserver.response.errors.size() == 0 ? Response.Status.OK
						: Response.Status.INTERNAL_SERVER_ERROR)
				.type(MediaType.APPLICATION_JSON).entity(taskObserver.response).build();
	}

	private String getNetworkNameWithColumn(long networkViewSUID, String column)
			throws NetworkViewNotFoundException, UnsupportedColumnTypeException, NodeColumnNotFoundException {
		for (CyNetwork net : cyNetworkManager.getNetworkSet()) {
			for (CyNetworkView view : cyNetworkViewManager.getNetworkViews(net)) {
				if (view.getSUID() == networkViewSUID) {
					CyColumn col = net.getDefaultNodeTable().getColumn(column);
					if (col == null)
						throw new NodeColumnNotFoundException();
					if (col.getType() != String.class && col.getType() != Integer.class) {
						throw new UnsupportedColumnTypeException();
					}

					return net.getRow(net).get(CyNetwork.NAME, String.class);
				}
			}
		}
		throw new NetworkViewNotFoundException();
	}

	@SuppressWarnings("serial")
	private class NetworkViewNotFoundException extends Exception {
	}

	@SuppressWarnings("serial")
	private class NodeColumnNotFoundException extends Exception {
	}

	@SuppressWarnings("serial")
	private class UnsupportedColumnTypeException extends Exception {
	}
}