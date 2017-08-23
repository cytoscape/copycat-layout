import json

from CyCaller import CyCaller


class CopyLayout:
    """ Cover functions for CopyLayout functions """

    def __init__(self, cy_rest_instance=None):
        """ Constructor remembers CyREST location """
        self._cy_caller = CyCaller(cy_rest_instance)

    def copyLayout(self, toNetwork, fromNetwork=None, fromColumn="name", toColumn="name"):
        """ copy the layout from one network view onto another """
        ids = self._cy_caller.execute_get('/v1/networks.names')
        mapping = {a['name']: a['SUID'] for a in ids}

        params = json.dumps({"fromColumn": fromColumn, "toNetwork": toNetwork, "toColumn": toColumn})

        if fromNetwork is not None:
            for net in (fromNetwork, toNetwork):
                if net not in mapping:
                    raise Exception("Failed to find network with name %s" % net)
            networkSUID = mapping[fromNetwork]
            networkViewSUID = self._cy_caller.execute_get('/v1/networks/%d/views' % networkSUID)
            networkViewSUID = networkViewSUID[0]
            return self._cy_caller.execute_post("/v1/layout/%d/views/%d/copy" % (networkSUID, networkViewSUID), params)
        else:
            return self._cy_caller.execute_post("/v1/layout/currentView/copy", params)
