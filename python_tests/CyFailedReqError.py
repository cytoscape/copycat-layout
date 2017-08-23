class CyFailedReqError(Exception):
    def __init__(self, exception_text):
        self.args = exception_text

