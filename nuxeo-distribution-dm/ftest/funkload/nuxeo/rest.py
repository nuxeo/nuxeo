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
Nuxeo rest api command.

TODO:
fileUpload
getUid(uid, 'title')
download
export
...
assert
"""
import os
from webunit.utility import Upload
from funkload.utils import Data


class RestAPI:
    """Class to play with nuxeo rest api."""

    def __init__(self, fl, repo=None):
        self.fl = fl
        if repo:
            self.repo = repo
        else:
            self.repo = 'default'
        self.current_uid = None


    def login(self, user, password):
        fl = self.fl
        self.fl.setBasicAuth(user, password)


    def logout(self):
        self.fl.clearBasicAuth()


    def browse(self, uid=None):
        fl = self.fl
        if uid is None:
            uid = self.current_uid or '*'
        resp = fl.get(fl.server_url + '/restAPI/' + self.repo + '/'
                      + uid + '/browse',
                      description='REST browse ' + uid)
        fl.assert_('<document ' in fl.getBody(), fl.getBody())
        return resp.getDOM().getByName('document')[1:]


    def getRootWorkspaceUid(self):
        for item in self.browse():
            if item.type == 'Domain':
                for item in self.browse(item.id):
                    if item.type == 'WorkspaceRoot':
                        return item.id
        return None


    def createDocument(self, parent_uid, doc_type, title, description):
        fl = self.fl
        resp = fl.get(fl.server_url + '/restAPI/' + self.repo + '/'
                      + parent_uid + '/createDocument',
                      params=[['docType', doc_type],
                              ['dublincore:title', title],
                              ['dublincore:description', description]],
                      description='REST create %s: %s' % (doc_type, title))
        fl.assert_('<document>' in fl.getBody(), fl.getBody())
        return resp.getDOM().getByName('docref')[0].getContentString()


    def uploadFile(self, uid, filename):
        # TODO: does not work on 5.2.m[34]
        # ERROR : org.jboss.seam.RequiredException:
        # @In attribute requires non-null value: FileManageActions.typeManager
        fl = self.fl
        fn = os.path.basename(filename)
        resp = fl.post(fl.server_url + '/restAPI/' + self.repo + '/'
                       + uid + '/' + fn + '/upload',
                       params=[['file', Upload(filename)]],
                       description='REST upload file')
        print fl.getBody()
        return resp.getDOM().getByName('docref')[0].getContentString()

    def getChildUid(self, parent_uid, title, doc_type=None):
        """Get the child uid with the title."""
        for item in self.browse(parent_uid):
            if doc_type is None or item.type == doc_type:
                if title == item.title:
                    return item.id
        return None

    def exportSingle(self, uid):
        # TODO return
        # <The Document>Error while calling Seam aware Restlet: null</The Document>
        # same for export
        fl = self.fl
        resp = fl.get(fl.server_url + '/restAPI/' + self.repo + '/'
                      + uid + '/exportSingle',

                      description='REST export ' + uid)
        print resp
        return resp.getDOM()

    def exportTree(self, uid, description=None):
        fl = self.fl
        description = description or 'REST exportTree'
        resp = fl.get(fl.server_url + '/restAPI/' + self.repo + '/'
                      + uid + '/exportTree',
                      description=description )
        fl.assert_('application/zip' in str(resp.headers),
                   'Expecting a zip got: ' + str(resp.headers))
        return resp.body


