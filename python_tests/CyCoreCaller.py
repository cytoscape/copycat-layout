import requests  # http://docs.python-requests.org/en/master/api/
from requests.status_codes import codes

from CyRESTInstance import CyRESTInstance
from CyFailedReqError import CyFailedReqError


class CyCoreCaller:
    """Basic functions for calling CyREST"""

    def __init__(self, cy_rest_instance=None):
        """Constructor remembers CyREST location and NDEx credentials"""
        if cy_rest_instance is None:
            cy_rest_instance = CyRESTInstance()
        self.cy_rest_instance = cy_rest_instance

    @staticmethod
    def _return_json(result):
        """Return JSON if the call was successful, or an exception if not"""
        if result.status_code == codes.OK or result.status_code == codes.CREATED:
            json = result.json()
            errors = json["errors"]
            if len(errors) == 0:
                return json["data"]
            else:
                raise CyFailedReqError(errors)
        else:
            raise CyFailedReqError(result)

    @staticmethod
    def _return(result):
        if result.status_code == codes.OK or result.status_code == codes.CREATED:
            if result.content:
                return result.json()
            else:
                return None
        else:
            raise CyFailedReqError(result)

    def _execute(self, http_method, endpoint, params=None, data=None, ci_json=False):
        """Execute a REST call, choosing whether to get a CI Response return value"""
        fq_endpoint = self.cy_rest_instance.base_url + ":" + str(self.cy_rest_instance.port) + endpoint
        if ci_json:
            result = requests.request(http_method,
                                      fq_endpoint,
                                      params=params,
                                      data=data,
                                      headers={'CIWrapping': 'true'})
            return CyCoreCaller._return_json(result)
        else:
            return CyCoreCaller._return(requests.request(http_method, fq_endpoint, data=data, params=params))

    def execute_get(self, endpoint, params=None, ci_json=False):
        """Execute a REST call, choosing whether to get a CI Response return value"""
        return self._execute("get", endpoint, params=params, ci_json=ci_json)

    def execute_post(self, endpoint, data=None, params=None, ci_json=False):
        """Execute a REST call, choosing whether to get a CI Response return value"""
        return self._execute("post", endpoint, data=data, params=params, ci_json=ci_json)

    def execute_put(self, endpoint, data=None, params=None, ci_json=False):
        """Execute a REST call, choosing whether to get a CI Response return value"""
        return self._execute("put", endpoint, data=data, params=params, ci_json=ci_json)
