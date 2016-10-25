#!/usr/bin/python
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

import os
import json
from urllib import pathname2url
from utils import truncate

def getNuxeoPayload(docType, name, properties):
    """ Returns the Nuxeo REST API payload to create a document"""
    return '{ "entity-type": "document", "name":"%s", "type": "%s","properties": %s}' % (
        name, docType, json.dumps(properties, ensure_ascii=False))


def getNuxeoFolderPayload(name):
    """ Create a payload for a folder"""
    return getNuxeoPayload("Folder", name, {"dc:title": name})


class NuxeoWriter(object):
    """ Helper to write a document and its parents into Redis
    """

    def __init__(self, writer):
        self.writer = writer

    def addDocument(self, name, docType, parentPath, properties):
        """ Add a document and all its parents
        """
        assert not '/' in name, 'name with slashes is forbiden: %s' % name
        self.addParents(parentPath)
        payload = getNuxeoPayload(docType, name, properties)
        key = "/".join((parentPath, name))
        self.writer.sadd("doc", key)
        self.writer.hmset("data:" + key, {"parentPath": pathname2url(parentPath), "type": docType, "name": name,
                                          "payload": payload, "key": key, "url": pathname2url(key)})

    def addParents(self, path):
        """ Add all parents folder for this path
        """
        segments = path.split("/")
        for i in range(len(segments)):
            self.addFolder("/".join(segments[0:i + 1]))

    def addFolder(self, path):
        """ Add a folder if it does not exists already
        """
        level = len(path.split("/"))
        name = os.path.basename(path)
        if (level > 1):
            parentPath = os.path.dirname(path)
        else:
            parentPath = ""
        payload = getNuxeoFolderPayload(name)
        self.writer.zadd("folder", level, path)
        key = "data:" + path
        self.writer.hsetnx(key, "parentPath", pathname2url(parentPath))
        self.writer.hsetnx(key, "name", name)
        self.writer.hsetnx(key, "type", "Folder")
        self.writer.hsetnx(key, "payload", payload)
        self.writer.hsetnx(key, "key", key)
        self.writer.hsetnx(key, "url", pathname2url(key))

