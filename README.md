# Copycat layout
## Now a core app in Cytoscape 3.6.0
Or you can download the app from the [Cytoscape App Store](http://apps.cytoscape.org/apps/copycatLayout)

Copycat layout is an improvement upon the [layoutSaver app](http://apps.cytoscape.org/apps/layoutsaver), with several new features:
1. Network view location and zoom are copied as well, for complete view alignment
1. Option to select unmapped nodes
1. CyREST functionality. Call Copycat in a Cytoscape Automation script

Copycat layout takes two networks and two node columns as input. The source networks is used as a reference layout. Nodes in the source network are paired with nodes in the target network, based on the node attribute specified.  The default mapping column is the `name` column.  Nodes in the target network are relocated to the XY coordinate of their partner node in the source network.  Once all nodes have been moved, the view camera is adjusted to match that of the source.  Optionally, unpaired nodes are selected, and other layouts (eg GridLayout) can be executed for better readability.

Take for example, the source and targets shown below.  The target network is a near exact replica of the source, but a circular layout has been applied. You wouldn't be able to tell just by glancing at the networks because they looks so different.

![Pre-copycat layout](https://github.com/cytoscape/copycat-layout/blob/master/images/pre-copycat.png)

Now apply Copycat layout, using the `name` column in both networks to map nodes. Selecting unmapped nodes will highlight the nodes that appeared in one network and not the other.  Now we can quickly determine what the differences are between our source and target networks.

![Post-copycat layout](https://github.com/cytoscape/copycat-layout/blob/master/images/post-copycat.png)

### Calling via CyREST

You can also call Copycat via its CyREST endpoint, in Python or R scripts
Copycat 1.2.3 offers an endpoint at `/v1/apply/layouts/copycat/{sourceViewSUID}/{targetViewSUID}`, which can be called with the python `requests` module:
```python
REST_PORT='1234'
REST_ENDPOINT = "http://localhost:{}/v1".format(REST_PORT)
import requests

sourceViewSUID = # Add source SUID
targetViewSUID = # Add target SUID

data = {
  "sourceColumn": "name",
  "targetColumn": "name",
  "selectUnmapped": True,
  "gridUnmapped": False
}
resp = requests.put("{}/apply/layouts/copycat/{}/{}".format(REST_ENDPOINT, \
            sourceViewSUID, targetViewSUID), params=data)
data = resp.json()['data']

print("Mapped {} nodes, left {} unmapped in the target network".format(data['mappedNodeCount'], data['unmappedNodeCount']))
```
