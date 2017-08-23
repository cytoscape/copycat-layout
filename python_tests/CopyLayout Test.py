from CopyLayout import CopyLayout
from CyRESTInstance import CyRESTInstance
from TestConfig import BASE_URL, IMPOSSIBLE_URL
from Core import Core
from CyFailedCIError import CyFailedCIError
from requests.status_codes import codes

""" Built from http://pyunit.sourceforge.net/pyunit.html """


import unittest

_copyLayout = CopyLayout(CyRESTInstance(base_url=BASE_URL))  # assumes Cytoscape answers at base_url
_bad_copyLayout = CopyLayout(CyRESTInstance(base_url=IMPOSSIBLE_URL))  # # for verifying proper exception thrown

_core = Core(CyRESTInstance(base_url=BASE_URL))  # assumes Cytoscape answers at base_url


class CopyLayoutTestCase(unittest.TestCase):

    def setUp(self):
        pass

    def tearDown(self):
        pass

    def test_copyLayout_no_network(self):
        input("On Cytoscape, deselect all networks and hit <enter>")
        netName = _core._cy_caller.execute_get('/v1/networks.names')[0]['name']
        try:
            _copyLayout.copyLayout(toNetwork=netName)
        except CyFailedCIError as e:
            error = e.args[0]
            assert error["type"] == "urn:cytoscape:ci:copyLayout-app:v1:copy_current_layout:1" \
                   and error["status"] == codes.NOT_FOUND \
                   and error["message"] is not None \
                   and error["link"] is not None, "test_copyLayout_no_network returned invalid CyFaileError: " + str(e)
        else:
            assert False, "test_copyLayout_no_network did not get the expected CyFailedError exception"

    def test_copyLayout(self):
        input("On Cytoscape, select a network and hit <enter>")
        names = _core._cy_caller.execute_get('/v1/networks.names')
        toName = names[0]['name']
        fromName = names[1]['name']
        _copyLayout.copyLayout(fromNetwork=fromName, toNetwork=toName)

    def test_copyLayout_to_itself(self):
        input("On Cytoscape, select a network and hit <enter>")
        netName = _core._cy_caller.execute_get('/v1/networks.names')[0]['name']
        try:
            result = _copyLayout.copyLayout(fromNetwork=netName, toNetwork=netName)
        except CyFailedCIError as e:
            error = e.args[0]
            assert error['type'] == 'urn:cytoscape:ci:copyLayout-app:v1:copy_layout:3' \
                and error['status'] == 400 \
                and error['message'] == "Source and target layout network are the same", "test_copyLayout_to_itself returned invalid CyFailedCIError:" + str(e)

    def test_copyLayout_incorrect_column(self):
        names = _core._cy_caller.execute_get('/v1/networks.names')
        toName = names[0]['name']
        fromName = names[1]['name']
        try:
            _copyLayout.copyLayout(toColumn="TOTALLY NONEXISTANT COLUMN", fromColumn=fromName, toNetwork=toName)
        except CyFailedCIError as e:
            error = e.args[0]
            assert error["type"] == "urn:cytoscape:ci:copyLayout-app:v1:copy_layout:3" \
                   and error["status"] == 404, "test_copyLayout_incorrect_column returned invalid CyFaileError: " + str(e)
        else:
            assert False, "test_copyLayout_incorrect_column did not get the expected CyFailedError exception"


    def test_copyLayout_exception(self):
        try:
            _bad_copyLayout.copyLayout()
        except CyFailedCIError:
            assert False, "test_copyLayout_exception got unexpected CyFailedError"
        except BaseException:
            pass
        else:
            assert False, "test_copyLayout_exception expected exception"


def suite():
    copyLayout_suite = unittest.makeSuite(CopyLayoutTestCase, "test")
    return unittest.TestSuite((copyLayout_suite))


if __name__ == "__main__":
    unittest.TextTestRunner().run(suite())
