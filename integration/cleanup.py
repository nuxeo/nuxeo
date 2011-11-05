#!/usr/bin/env python

VCSPROPS="nuxeo-test-cvs.properties"

import os

home = os.getenv('USERPROFILE') or os.getenv('HOME')
fname = os.path.join(home, VCSPROPS)
if os.path.isfile(fname):
    os.remove(fname)

