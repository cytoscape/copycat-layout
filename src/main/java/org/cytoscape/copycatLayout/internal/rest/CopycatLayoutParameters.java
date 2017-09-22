package org.cytoscape.copycatLayout.internal.rest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Parameters for passing to CopycatLayoutTask
 * 
 * @author brettjsettle
 *
 */
@ApiModel(value="Copycat Layout Parameters", description="Parameters for copying one layout onto another")
public class CopycatLayoutParameters extends CopycatWithViewSUIDsLayoutParameters {
	@ApiModelProperty(value = "The SUID of the network view whose layout is being copied", required=true)
	public long sourceNetworkViewSUID;
	@ApiModelProperty(value = "The SUID of the network view that is receiving the copied layout", required = true)
	public long targetNetworkViewSUID;
}