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
This modules is tied with the Nuxeo Webengine application.

"""

from urllib import quote

class BasePage:
    """Base class for nuxeo ep page."""
    fl = None

    def __init__(self, fl):
        self.fl = fl

    # helpers
    def available(self):
        """Check if the server is available."""
        fl = self.fl
        fl.get(fl.server_url + '/login.jsp',
               description="Check if the server is alive")

    # pages
    def home(self):
        self.fl.get(self.fl.server_url + "/site", description="View home page")
        return self

    def login(self, user, password):
        fl = self.fl
        fl.post(fl.server_url + "/nxstartup.faces", params=[
            ['user_name', user],
            ['user_password', password],
            ['form_submitted_marker', ''],
            ['Submit', 'Connexion']],
            description="Login " + user)
        fl.assert_('login.jsp' not in fl.getLastUrl(),
                   'Login failed for %s:%s' % (user, password))
        return self

    def loginInvalid(self, user, password):
        fl = self.fl
        fl.post(fl.server_url + "/nxstartup.faces", params=[
            ['user_name', user],
            ['user_password', password],
            ['form_submitted_marker', ''],
            ['Submit', 'Connexion']],
            description="Login invalid user " + user)
        fl.assert_('login.jsp' in fl.getLastUrl(),
                   'Invalid login expected for %s:%s.' %  (user, password))
        return self

    def viewDocumentPath(self, path, description=None, raiseOn404=True):
        """This method return None when raiseOn404 is False and the document
        does not exist"""
        fl = self.fl
        if not description:
            description = "View document path:" + path
        ok_codes = [200, 301, 302, 303, 307]
        if not raiseOn404:
            ok_codes.append(404)
        resp = fl.get(fl.server_url +
                      "/site/admin/repository/default-domain/" + quote(path) +
                      "/@views/content_page?context=tab",
                      description=description, ok_codes=ok_codes)
        if resp.code == 404:
            fl.logi('Document ' + path + ' does not exists.')
            return None
        return self

    def createDocument(self, parent, title, description=None,
                       doc_type='Note', doc_id=None):
        fl = self.fl
        fl.get(fl.server_url + "/site/admin/repository/default-domain/" + quote(parent) + "/@views/create",
               description="View create form")
        if not doc_id:
            doc_id = title.replace('/', '-').strip()
        fl.post(fl.server_url + "/site/admin/repository/default-domain/" + quote(parent), params=[
            ['name', doc_id],
            ['doctype', doc_type],
            ['dc:title', title],
            ['dc:description', description]],
            description="Create a new " + doc_type)
        return self

    def deleteDocument(self, path):
        fl = self.fl
        fl.get(fl.server_url + "/site/admin/repository/default-domain/" +
               quote(path) + "/@delete",
            description="Delete " + path)
        return self

    def createUser(self, username, password, firstname='',
                   lastname='', groups=''):
        """This method does not raise exception if user already exists"""
        fl = self.fl
        fl.get(fl.server_url + '/site/admin/users/@views/create_user',
               description="Create user form")

        fl.post(fl.server_url + "/site/admin/users/user", params=[
            ['username', username],
            ['password', password],
            ['firstName', firstname],
            ['lastName', lastname],
            ['groups', groups]],
            description="Create users")
        return self

    def deleteUser(self, user):
        fl = self.fl
        fl.get(fl.server_url + "/site/admin/users/user/" + quote(user)
               + "/@delete",
               description="Delete user " + user)
        return self

    def admin(self):
        fl = self.fl
        fl.get(fl.server_url + '/site/admin',
               description="View administration page")
        return self

    def user_management(self):
        fl = self.fl
        fl.get(fl.server_url + '/site/admin/users',
               description="View user management page")
        return self

    def logout(self):
        fl = self.fl
        fl.get(fl.server_url + "/logout", description="Logout")
        return self


class WebenginePage(BasePage):
    """Default web engine page."""
