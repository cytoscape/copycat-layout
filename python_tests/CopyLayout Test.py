from CopyLayout import CopyLayout
from CyRESTInstance import CyRESTInstance
from TestConfig import BASE_URL, IMPOSSIBLE_URL
from Core import Core
from CyFailedCIError import CyFailedCIError
from requests.status_codes import codes

""" Built from http://pyunit.sourceforge.net/pyunit.html """


import unittest

resourceErrorRoot = "urn:cytoscape:ci:copycatLayout-app:v1"

_copyLayout = CopyLayout(CyRESTInstance(base_url=BASE_URL))  # assumes Cytoscape answers at base_url
_bad_copyLayout = CopyLayout(CyRESTInstance(base_url=IMPOSSIBLE_URL))  # # for verifying proper exception thrown

_core = Core(CyRESTInstance(base_url=BASE_URL))  # assumes Cytoscape answers at base_url


class CopyLayoutTestCase(unittest.TestCase):

    def setUp(self):
        pass

    def tearDown(self):
        pass

    def get_network_view_suids(self):
        viewSUIDs = []
        netSUIDs = _core._cy_caller.execute_get('/v1/networks')
        for suid in netSUIDs:
            viewSUIDs.extend(_core._cy_caller.execute_get('/v1/networks/%d/views' % suid))
        return viewSUIDs

    def test_copyLayout_no_source_network(self):
        input("On Cytoscape, deselect all networks and hit <enter>")
        netName = self.get_network_view_suids()[0]
        try:
            _copyLayout.copyLayout(toSUID=netName)
        except CyFailedCIError as e:
            error = e.args[0]
            assert error["type"] == resourceErrorRoot + ":copycat-current-layout:1" \
                   and error["status"] == codes.NOT_FOUND \
                   and error["message"] is not None \
                   and error["link"] is not None, "test_copyLayout_no_source_network returned invalid CyFaileError: " + str(e)
        else:
            assert False, "test_copyLayout_no_source_network did not get the expected CyFailedError exception"

    def test_copyLayout_no_target_network(self):
        input("On Cytoscape, deselect all networks and hit <enter>")
        netName = self.get_network_view_suids()[0]
        try:
            _copyLayout.copyLayout(fromSUID=netName)
        except CyFailedCIError as e:
            error = e.args[0]
            assert error["type"] == resourceErrorRoot + ":copycat-to-current-layout:4" \
                   and error["status"] == codes.NOT_FOUND \
                   and error["message"] is not None \
                   and error["link"] is not None, "test_copyLayout_no_target_network returned invalid CyFaileError: " + str(e)
        else:
            assert False, "test_copyLayout_no_target_network did not get the expected CyFailedError exception"

    def test_copyLayout_no_source_column(self):
        nets = self.get_network_view_suids()
        source = nets[0]
        target = nets[1]
        try:
            _copyLayout.copyLayout(fromSUID=source, toSUID=target, sourceColumn='NONEXISTANT')
        except CyFailedCIError as e:
            error = e.args[0]
            assert error["type"] == resourceErrorRoot + ":copycat-layout:2" \
                   and error["status"] == codes.NOT_FOUND \
                   and error["message"] is not None \
                   and error["link"] is not None, "test_copyLayout_no_source_column returned invalid CyFaileError: " + str(e)
        else:
            assert False, "test_copyLayout_no_source_column did not get the expected CyFailedError exception"

    def test_copyLayout_no_target_column(self):
        nets = self.get_network_view_suids()
        source = nets[0]
        target = nets[1]
        try:
            _copyLayout.copyLayout(fromSUID=source, toSUID=target, targetColumn='NONEXISTANT')
        except CyFailedCIError as e:
            error = e.args[0]
            assert error["type"] == resourceErrorRoot + ":copycat-layout:5" \
                   and error["status"] == codes.NOT_FOUND \
                   and error["message"] is not None \
                   and error["link"] is not None, "test_copyLayout_no_target_column returned invalid CyFaileError: " + str(e)
        else:
            assert False, "test_copyLayout_no_target_column did not get the expected CyFailedError exception"


    def test_copyLayout(self):
        nets = self.get_network_view_suids()
        source = nets[0]
        target = nets[1]
        _copyLayout.copyLayout(fromSUID=source, toSUID=target)

    def test_copyLayout_to_itself(self):
        net = self.get_network_view_suids()[0]
        try:
            result = _copyLayout.copyLayout(fromSUID=net, toSUID=net)
        except CyFailedCIError as e:
            error = e.args[0]
            print(error)
            assert error['type'] == resourceErrorRoot + ':copycat-layout:7' \
                and error['status'] == 400 \
                and error['message'] == "Source and destination network views cannot be the same.", \
                "test_copyLayout_to_itself returned invalid CyFailedCIError:" + str(e)

    
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
