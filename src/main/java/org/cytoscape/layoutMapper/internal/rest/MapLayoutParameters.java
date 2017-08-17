package org.cytoscape.layoutMapper.internal.rest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Parameters for passing to MapLayoutTask
 * 
 * @author brettjsettle
 *
 */
@ApiModel(value="Map Layout Parameters", description="Parameters for mapping one layout onto another")
public class MapLayoutParameters {
	@ApiModelProperty(value = "A node column used to map nodes in the source network to nodes in the target network")
	public String fromColumn;
	@ApiModelProperty(value = "The name of the network view that is receiving the mapped layout")
	public String toNetwork;
	@ApiModelProperty(value = "A node column used to map nodes in the target network to nodes in the source network")
	public String toColumn;
}
