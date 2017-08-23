import os

class CyRESTInstance:
    """ Parameters that describe CyREST """

    _PORT = 1234
    _BASE_URL = "http://localhost"

    def __init__(self, base_url= None, port=None):
        """ Constructor remembers CyREST location """
        if base_url == None:
            base_url = os.getenv("CYREST_URL", CyRESTInstance._BASE_URL)
        if port == None:
            port = os.getenv("CYREST_PORT", CyRESTInstance._PORT)
        self.port = port
        self.base_url = base_url
