package org.cytoscape.copycatLayout.internal.rest;

import io.swagger.annotations.ApiModelProperty;

public class CopycatLayoutResult {
	@ApiModelProperty(value = "Number of successfully mapped nodes", required=true)
	public int mappedNodeCount;
	@ApiModelProperty(value = "Number of unmapped nodes in the target network.", required=true)
	public int unmappedNodeCount;
}
