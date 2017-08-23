package org.cytoscape.copyLayout.internal.rest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Parameters for passing to MapLayoutTask
 * 
 * @author brettjsettle
 *
 */
@ApiModel(value="Copy Layout Parameters", description="Parameters for copying one layout onto another")
public class CopyLayoutParameters {
	@ApiModelProperty(value = "A node column used to copy nodes in the source network to nodes in the target network")
	public String fromColumn;
	@ApiModelProperty(value = "The name of the network view that is receiving the copied layout")
	public String toNetwork;
	@ApiModelProperty(value = "A node column used to copy nodes in the target network to nodes in the source network")
	public String toColumn;
}
