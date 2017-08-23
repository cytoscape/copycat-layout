from CyCoreCaller import CyCoreCaller
import json


class Core:
    """ Cover functions for Cytoscape Core functions """

    TABLE_DEFAULT_NODE = "defaultnode"
    TABLE_DEFAULT_EDGE = "defaultedge"
    TABLE_DEFAULT_NETWORK = "defaultnetwork"

    DATA_STRING = "String"
    DATA_DOUBLE = "Double"
    DATA_LONG = "Long"
    DATA_INTEGER = "Integer"
    DATA_BOOLEAN = "Boolean"

    def __init__(self, cy_rest_instance=None):
        """ Constructor remembers CyREST location """
        self._cy_caller = CyCoreCaller(cy_rest_instance)

    def get_version(self, ci_json=False):
        """ Get the CyREST version """
        return self._cy_caller.execute_get("/v1", ci_json=ci_json)

    def read_session_file(self, file_name, ci_json=False):
        """ Read a session from an absolute file name """
        return self._cy_caller.execute_get("/v1/session", {"file": file_name}, ci_json=ci_json)

    def get_network_suids(self, column=None, query=None, ci_json=False):
        """ Get a list of SUIDs identifying loaded networks """
        return self._cy_caller.execute_get("/v1/networks", {"column": column, "query": query}, ci_json=ci_json)

    def create_table_column(self, network_id, name, data_type=DATA_STRING, immutable=False, list=False, local=False,
                            table_type=TABLE_DEFAULT_NODE, ci_json=False):
        """ Add a single column to a table """
        _column_def = json.dumps(
            {"name": name, "type": data_type, "immutable": immutable, "list": list, "local": local})
        return self._cy_caller.execute_post(
            "/v1/networks/" + str(network_id) + "/tables/" + table_type + "/columns", _column_def, ci_json=ci_json)

    def fill_table_column(self, network_id, name, value, table_type=TABLE_DEFAULT_NODE, ci_json=False):
        """ Add values to a column in a table """
        return self._cy_caller.execute_put(
            "/v1/networks/" + str(network_id) + "/tables/" + table_type + "/columns/" + str(name),
            params={"default": value}, ci_json=ci_json)
