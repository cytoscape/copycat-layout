package org.cytoscape.layoutMapper.internal.rest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Parameters for passing to MapLayoutTask
 * 
 * @author brettjsettle
 *
 */
@ApiModel(value="Map Layout Parameters", description="Parameters for clone layout")
public class MapLayoutParameters {
	@ApiModelProperty(value = "The network whose layout is being mapped")
	public String fromNetworkName;
	@ApiModelProperty(value = "A node column used to map nodes in the source network to nodes in the target network", example="name")
	public String fromColumnName;
	@ApiModelProperty(value = "The network view that is receiving the mapped layout")
	public String toNetworkName;
	@ApiModelProperty(value = "A node column used to map nodes in the target network to nodes in the source network", example="name")
	public String toColumnName;
}
