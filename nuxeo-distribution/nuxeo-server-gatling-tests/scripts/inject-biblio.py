#!/usr/bin/env python
# -*- coding: utf-8 -*-

# (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the GNU Lesser General Public License
# (LGPL) version 2.1 which accompanies this distribution, and is available at
# http://www.gnu.org/licenses/lgpl-2.1.html
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
# Lesser General Public License for more details.
#
# Contributors:
#     Delbosc Benoit
#

import csv
import sys
from injector import Injector
from utils import nuxeoName

class InjectorBiblio(Injector):

    def downloadInfo(self):
        return ['tous-les-documents-des-bibliotheques-de-pret.csv',
                'http://opendata.paris.fr/explore/dataset/tous-les-documents-des-bibliotheques-de-pret/download/?format=csv&use_labels_for_header=true']

    def parse(self, input, writer):
        for row in csv.reader(iter(input.readline, ''), delimiter=';', doublequote=False, quotechar='"'):
            # for i in range(0, 30):
            #   print(str(i) + " " + clean(row[i]))
            props = {}
            props["dc:title"] = row[3]
            props["dc:description"] = row[5] + " " + row[9] + " " + row[20]
            name = nuxeoName(props["dc:title"])
            cat = row[26] if row[25].startswith("pas") else row[25]
            parentPath = self.createPathFromCategory(nuxeoName(cat))
            writer.addDocument(name, "File", parentPath, props)


    def createPathFromCategory(self, cat):
        """hmmm weird encoding"""
        parent = cat
        root = parent.split(' ')[0]
        if (len(root) == 4):
            level0 = root[:2] + '00'
            level1 = root[:3] + '0'
            parent_path = "/".join((level0, level1, parent))
        else:
            parent_path = "/".join(("Misc", parent))
        return parent_path


if __name__ == '__main__':
    sys.exit(InjectorBiblio().run())
