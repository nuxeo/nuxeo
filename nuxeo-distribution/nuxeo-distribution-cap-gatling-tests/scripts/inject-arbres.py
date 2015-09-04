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
import geohash
from injector import Injector

class InjectorArbre(Injector):

    def downloadInfo(self):
        return ['les-arbres.csv',
                'http://opendata.paris.fr/explore/dataset/les-arbres/download/?format=csv&use_labels_for_header=true']

    def parse(self, input, writer):
        for row in csv.reader(iter(input.readline, ''), delimiter=';', doublequote=False, quotechar='"'):
            # Geo Point;LOCALISATI;LIEU / ADRESSE;CIRCONFERENCE (cm);HAUTEUR (m);ESPECE;VARIETE;DATE PLANTATION
            try:
                lat, lon = row[0].split(", ")
                name = geohash.encode((float(lon), float(lat)))
            except ValueError:
                self.log.warn("Skipping line with invalid geohash: " + row[0])
                continue
            props = {}
            parentPath = self.createPathFromGeoHash(name)
            props["dc:title"] = row[5] + ((" - " + row[6]) if row[6] != "" else "")
            props["dc:description"] = row[1] + " " + row[2] +  " plantation: " + row[7]
            props["dc:issued"] = row[7]
            props["dc:format"] = "Hauteur: " + row[4] + "m, circonférence: " + row[3] + " cm"
            props["dc:source"] = row[0]
            writer.addDocument(name, "File", parentPath, props)

    def createPathFromGeoHash(self, hash):
        # geohash level 5 is ~2,4km tile per leaf folder
        return hash[0:3] + "/" + hash[3] + "/" + hash[4] + "/" + hash[5]


if __name__ == '__main__':
    sys.exit(InjectorArbre().run())
