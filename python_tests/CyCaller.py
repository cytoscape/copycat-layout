import requests  # http://docs.python-requests.org/en/master/api/

from CyRESTInstance import CyRESTInstance
from CyFailedCIError import CyFailedCIError


class CyCaller:
    """Basic functions for calling CyREST"""

    def __init__(self, cy_rest_instance=None):
        """Constructor remembers CyREST location and NDEx credentials"""
        if cy_rest_instance is None:
            cy_rest_instance = CyRESTInstance()
        self.cy_rest_instance = cy_rest_instance

    @staticmethod
    def _return_json(result):
        """Return JSON if the call was successful, or an exception if not"""
        json = result.json()
        if type(json) == dict and 'errors' in json:
            errors = json["errors"]
            if len(errors) == 0:
                return json["data"]
            else:
                raise CyFailedCIError(errors)
        return json

    def _execute(self, http_method, endpoint, data=None, params=None):
        """Set up a REST call with appropriate headers, then return result"""
        headers = {'Content-type': 'application/json', 'Accept': 'application/json'}
        result = requests.request(http_method,
                                  self.cy_rest_instance.base_url + ":" + str(self.cy_rest_instance.port) + endpoint,
                                  data=data,
                                  params=params,
                                  headers=headers)
        return CyCaller._return_json(result)

    def execute_post(self, endpoint, data=None):
        """Execute a REST call using POST"""
        return self._execute("post", endpoint, data)

    def execute_put(self, endpoint, data=None):
        """Execute a REST call using PUT"""
        return self._execute("put", endpoint, data)

    def execute_get(self, endpoint, params=None):
        """Execute a REST call using GET"""
        return self._execute("get", endpoint, params=params)
