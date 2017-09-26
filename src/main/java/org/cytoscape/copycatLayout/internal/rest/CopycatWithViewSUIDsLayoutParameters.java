package org.cytoscape.copycatLayout.internal.rest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Parameters for passing to CopycatLayoutTask
 * 
 * @author brettjsettle
 *
 */
@ApiModel(value = "Copycat Layout Parameters", description = "Parameters for copying one layout onto another")
public class CopycatWithViewSUIDsLayoutParameters {
	@ApiModelProperty(value = "A node column used to copy nodes in the source network to nodes in the target network", example = "name")
	public String sourceColumn;
	@ApiModelProperty(value = "A node column used to copy nodes in the target network to nodes in the source network", example = "name")
	public String targetColumn;
	@ApiModelProperty(value = "Select unmapped nodes", example = "false")
	public boolean selectUnmapped;
	@ApiModelProperty(value = "Layout unmapped nodes in a grid", example = "false")
	public boolean gridUnmapped;

	public CopycatWithViewSUIDsLayoutParameters() {
		sourceColumn = "name";
		targetColumn = "name";
		selectUnmapped = false;
		gridUnmapped = false;
	}
}