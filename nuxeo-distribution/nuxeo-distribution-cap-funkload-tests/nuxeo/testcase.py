# (C) Copyright 2009 Nuxeo SAS <http://nuxeo.com>
# Author: bdelbosc@nuxeo.com
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License version 2 as published
# by the Free Software Foundation.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
# 02111-1307, USA.
#
"""
"""
import os
from time import sleep
import re
from commands import getstatusoutput
import unittest
from funkload.Lipsum import Lipsum
from funkload.FunkLoadTestCase import FunkLoadTestCase
from funkload.utils import xmlrpc_get_credential
from utils import extractJsfState


def command(cmd, do_raise=True, silent=False):
    """Return the status, output as a line list."""
    extra = 'LC_ALL=C '
    print('Run: ' + extra + cmd)
    status, output = getstatusoutput(extra + cmd)
    if status:
        if not silent:
            print('ERROR: [%s] return status: [%d], output: [%s]' %
                  (extra + cmd, status, output))
        if do_raise:
            raise RuntimeError('Invalid return code: %s' % status)
    #if output:
    #    output = output.split('\n')
    return (status, output)


class NuxeoTestCase(FunkLoadTestCase):
    server_url = None
    _lipsum = Lipsum()
    monitor_page = 0

    def setUp(self):
        self.server_url = self.conf_get('main', 'url')
        self.credential_host = self.conf_get('credential', 'host')
        self.credential_port = self.conf_getInt('credential', 'port')
        self.cred_admin = xmlrpc_get_credential(self.credential_host,
                                                self.credential_port,
                                                'admin')
        self.cred_member = xmlrpc_get_credential(self.credential_host,
                                                 self.credential_port,
                                                 'members')
        self.pglog_file = self.conf_get('main', 'pglog', '')
        self.log_dir = self.conf_get('main', 'log_dir', '')
        if not self.in_bench_mode:
            self.monitor_page = self.conf_getInt('main', 'monitor_page', '0')
            if self.monitor_page:
                self.logd("Tracing pg and jvm heap")
                if os.path.exists(self.pglog_file):
                    self.pglogf = open(self.pglog_file)
                    self.pglogf.seek(0, 2)
                else:
                    self.pglog_file = None
        self.logd("setUp")

    def pglog(self):
        """Save the PostgreSQL log for the previous request, logtail."""
        if self.pglog_file is None:
            return
        # wait for async processing
        sleep(4)
        logfile = os.path.join(self.log_dir, "pg-%s-%3.3d.log" % (self.test_name, self.steps))
        lf = open(logfile, "w")
        while True:
            data = self.pglogf.read()
            if data:
                lf.write(data)
            else:
                break
        lf.close()

    def performFullGC(self):
        """Ask monitorctl.sh to perform a full GC."""
        if self.monitorctl_file is not None:
            command(self.monitorctl_file + " invoke-fgc")

    def performHeapHistoStart(self):
        """Perform a heap histo of page."""
        self.performHeapHisto("%s-%3.3d-before" % (self.test_name, self.steps + 1))

    def performHeapHistoEnd(self):
        """Perform a heap histo of page."""
        self.performHeapHisto("%s-%3.3d-end" % (self.test_name, self.steps))

    def performHeapHisto(self, tag):
        """Perform a heap histo."""
        self.monitorctl_file = self.conf_get('main', 'monitorctl_file', '')
        if not os.path.exists(self.monitorctl_file):
            return
        log_dir = self.conf_get('main', 'log_dir', '')
        logfile = os.path.join(log_dir, "hh-%s.txt" % tag)
        status, output = command(self.monitorctl_file + " heap-histo")
        lf = open(logfile, "w")
        lf.write(output)
        lf.close()

    def getLastJsfState(self):
        return extractJsfState(self.getBody())

    def get(self, url, params=None, description=None, ok_codes=None):
        # Override to save pg logs and heap histo
        if self.monitor_page:
            self.performFullGC()
            self.performHeapHistoStart()
        ret = FunkLoadTestCase.get(self, url, params, description, ok_codes)
        if self.monitor_page:
            self.pglog()
            self.performHeapHistoEnd()
        return ret

    def post(self, url, params=None, description=None, ok_codes=None):
        # Override to save pg logs and heap histo
        if self.monitor_page:
            self.performFullGC()
            self.performHeapHistoStart()
        ret = FunkLoadTestCase.post(self, url, params, description, ok_codes)
        if self.monitor_page:
            self.pglog()
            self.performHeapHistoEnd()
        return ret

if __name__ in ('main', '__main__'):
    unittest.main()
