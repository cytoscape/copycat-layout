package org.cytoscape.layoutMapper.internal.rest;

import java.util.ArrayList;

import javax.ws.rs.core.Response;

import org.cytoscape.ci.model.CIError;
import org.cytoscape.ci.model.CIResponse;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskObserver;

public class MapLayoutTaskObserver implements TaskObserver {

	/**
	 * 
	 */
	private final MapLayoutResource mapLayoutResource;
	CIResponse<?> response;

	public CIResponse<?> getResponse() {
		return response;
	}

	private String result;
	private String resourcePath;
	private String errorCode;

	public MapLayoutTaskObserver(MapLayoutResource mapLayoutResource, String resourcePath, String errorCode) {
		this.mapLayoutResource = mapLayoutResource;
		response = null;
		this.resourcePath = resourcePath;
		this.errorCode = errorCode;
	}

	public void allFinished(FinishStatus arg0) {

		if (arg0.getType() == FinishStatus.Type.SUCCEEDED || arg0.getType() == FinishStatus.Type.CANCELLED) {
			response = new CIResponse<String>();
			((CIResponse<String>) response).data = result;
			response.errors = new ArrayList<CIError>();
		} else {
			response = this.mapLayoutResource.buildCIErrorResponse(
					Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), resourcePath, errorCode,
					arg0.getException().getMessage(), arg0.getException());
		}
	}

	public void taskFinished(ObservableTask arg0) {
		String jsonResult = arg0.getResults(String.class);
		result = jsonResult;
	}
}
