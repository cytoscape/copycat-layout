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
import org.osgi.util.tracker.ServiceTracker;
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
	private final SynchronousTaskManager<Object> taskManager;

	private final ServiceTracker ciResponseFactoryTracker;

	private CIResponseFactory getCIResponseFactory() {
		return (CIResponseFactory) ciResponseFactoryTracker.getService();
	}
	// private final CIResponseFactory ciResponseFactory;

	private final ServiceTracker ciExceptionFactoryTracker;

	private CIExceptionFactory getCIExceptionFactory() {
		return (CIExceptionFactory) ciExceptionFactoryTracker.getService();
	}
	// private final CIExceptionFactory ciExceptionFactory;

	private final ServiceTracker ciErrorFactoryTracker;

	private CIErrorFactory getCIErrorFactory() {
		return (CIErrorFactory) ciErrorFactoryTracker.getService();
	}
	// private final CIErrorFactory ciErrorFactory;

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
			final SynchronousTaskManager<Object> taskManager, final CyNetworkManager cyNetworkManager,
			final CyNetworkViewManager cyNetworkViewManager, final CopycatLayoutTaskFactory copycatLayoutTaskFactory,
			final ServiceTracker ciResponseFactoryTracker, final ServiceTracker ciExceptionFactoryTracker,
			final ServiceTracker ciErrorFactoryTracker) {
		this.cyApplicationManager = cyApplicationManager;
		this.taskManager = taskManager;
		this.cyNetworkManager = cyNetworkManager;
		this.cyNetworkViewManager = cyNetworkViewManager;
		this.copycatLayoutTaskFactory = copycatLayoutTaskFactory;
		this.ciExceptionFactoryTracker = ciExceptionFactoryTracker;
		this.ciResponseFactoryTracker = ciResponseFactoryTracker;
		this.ciErrorFactoryTracker = ciErrorFactoryTracker;

	}

	private static final Logger logger = LoggerFactory.getLogger(CopycatLayoutResource.class);

	private final static String resourceErrorRoot = "urn:cytoscape:ci:copycatLayout-app:v1";

	private CIError buildCIError(int status, String resourcePath, String code, String message, Exception e) {
		return getCIErrorFactory().getCIError(status, resourceErrorRoot + ":" + resourcePath + ":" + code, message);
	}

	CIResponse<Object> buildCIErrorResponse(int status, String resourcePath, String code, String message, Exception e) {
		CIResponse<Object> response = getCIResponseFactory().getCIResponse(new Object());

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
			throw getCIExceptionFactory().getCIException(404, new CIError[] {
					this.buildCIError(404, resourcePath, errorType, messageString, new Exception(messageString)) });
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
		throw getCIExceptionFactory()
				.getCIException(404,
						new CIError[] { this.buildCIError(404, resourcePath, errorType,
								"No network view with SUID: " + networkViewSUID,
								new Exception("No network view found with SUID: " + networkViewSUID)) });
	}

	@ApiModel(value = "Copycat Layout Response", description = "Number of successfully and unsuccessfully mappes node locations", parent = CIResponse.class)
	public static class CopycatLayoutResponse extends CIResponse<CopycatLayoutResult> {
	}

	private String validateNetworkName(long viewSUID, String column, String networkType) {
		String name = null;
		try {
			name = getNetworkNameWithColumn(viewSUID, column);
		} catch (NetworkViewNotFoundException e) {
			throw getCIExceptionFactory().getCIException(404,
					new CIError[] { buildCIError(404, "copycat-layout",
							networkType == "Source" ? SOURCE_NETWORK_VIEW_NOT_FOUND : TARGET_NETWORK_VIEW_NOT_FOUND,
							networkType + " network view with SUID " + viewSUID + " does not exist.", e) });
		} catch (NodeColumnNotFoundException e) {
			throw getCIExceptionFactory().getCIException(404,
					new CIError[] { buildCIError(404, "copycat-layout",
							networkType == "Source" ? SOURCE_COLUMN_NOT_FOUND : TARGET_COLUMN_NOT_FOUND,
							networkType + " column " + column + " not found in network.", e) });

		} catch (UnsupportedColumnTypeException e) {
			throw getCIExceptionFactory().getCIException(404,
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
	public Response copyCurrentLayout(
			@PathParam("sourceViewSUID") @ApiParam(value = "Source network view SUID (or \"current\")") String sourceViewSUID,
			@PathParam("targetViewSUID") @ApiParam(value = "Target network view SUID (or \"current\")") String targetViewSUID,
			@ApiParam(value = "Clone the specified network view layout onto another network view") CopycatWithViewSUIDsLayoutParameters params) {
		if (params == null)
			params = new CopycatWithViewSUIDsLayoutParameters();
		try {
			Long sViewSUID = parseSUIDFromString(sourceViewSUID);
			Long tViewSUID = parseSUIDFromString(sourceViewSUID);
			return copyLayout(sViewSUID, params.sourceColumn, tViewSUID, params.targetColumn, params.selectUnmapped,
					params.gridUnmapped);
		} catch (Exception e) {
			// THROW EXCEPTION
		}
		return null;
	}

	public Long getCurrentNetworkViewSUID() throws Exception {
		CyNetworkView view = cyApplicationManager.getCurrentNetworkView();
		if (view == null) {
			throw new Exception("No current network view");
		}
		return view.getSUID();
	}

	public Long parseSUIDFromString(String suid) throws Exception {
		if (suid.toLowerCase().equals("current")) {
			return getCurrentNetworkViewSUID();
		} else if (suid.matches("\\d+")) {
			return Long.parseLong(suid);
		}
		throw new Exception("Cannot parse SUID from " + suid);
	}

	private Response copyLayout(long sourceViewSUID, String sourceColumn, long targetViewSUID, String targetColumn,
			boolean selectUnmapped, boolean gridUnmapped) {
		CopycatLayoutTaskObserver taskObserver = new CopycatLayoutTaskObserver(this, "copycat-layout",
				TASK_EXECUTION_ERROR);
		Map<String, Object> tunableMap = new HashMap<String, Object>();

		TaskIterator taskIterator = copycatLayoutTaskFactory.createTaskIterator();

		String sourceName = validateNetworkName(sourceViewSUID, sourceColumn, "Source");
		String targetName = validateNetworkName(targetViewSUID, targetColumn, "Target");
		if (sourceViewSUID == targetViewSUID) {
			throw getCIExceptionFactory().getCIException(400,
					new CIError[] { buildCIError(400, "copycat-layout", TASK_EXECUTION_ERROR,
							"Source and destination network views cannot be the same.",
							new Exception("Source and destination network views cannot be the same.")) });
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