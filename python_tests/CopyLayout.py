import json

from CyCaller import CyCaller


class CopyLayout:
    """ Cover functions for CopyLayout functions """

    def __init__(self, cy_rest_instance=None):
        """ Constructor remembers CyREST location """
        self._cy_caller = CyCaller(cy_rest_instance)

    def copyLayout(self, toSUID=None, fromSUID=None, **args):
        """ copy the layout from one network view onto another """
        

        params = json.dumps(args)
        if (toSUID == None and fromSUID == None):
            raise Exception("No target or source")
        elif toSUID != None and fromSUID != None:
            return self._cy_caller.execute_put("/v1/apply/layouts/copycat/%d/%d" % (fromSUID, toSUID), params)
        elif toSUID == None:
            return self._cy_caller.execute_put("/v1/apply/layouts/copycat/%d/current/" % fromSUID, params)
        else:
            return self._cy_caller.execute_put("/v1/apply/layouts/copycat/current/%d" % toSUID, params)

