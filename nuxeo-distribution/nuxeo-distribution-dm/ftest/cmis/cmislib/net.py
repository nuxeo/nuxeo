#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
#
#   Authors:
#    Jeff Potts, Optaros
#
'''
Module that knows how to connect to the AtomPub Binding of a CMIS repo
'''

from urllib import urlencode
from urllib2 import HTTPBasicAuthHandler, \
                    HTTPPasswordMgrWithDefaultRealm, \
                    HTTPRedirectHandler, \
                    HTTPDefaultErrorHandler, \
                    HTTPError, \
                    Request, \
                    build_opener


class SmartRedirectHandler(HTTPRedirectHandler):

    """ Handles 301 and 302 redirects """

    def http_error_301(self, req, fp, code, msg, headers):
        """ Handle a 301 error """
        result = HTTPRedirectHandler.http_error_301(
            self, req, fp, code, msg, headers)
        result.status = code
        return result

    def http_error_302(self, req, fp, code, msg, headers):
        """ Handle a 302 error """
        result = HTTPRedirectHandler.http_error_302(
            self, req, fp, code, msg, headers)
        result.status = code
        return result


class DefaultErrorHandler(HTTPDefaultErrorHandler):

    """ Default error handler """

    def http_error_default(self, req, fp, code, msg, headers):
        """Provide an implementation for the default handler"""
        result = HTTPError(
            req.get_full_url(), code, msg, headers, fp)
        result.status = code
        return result


class RESTService(object):

    """
    Generic service for interacting with an HTTP end point. Sets headers
    such as the USER_AGENT and builds the basic auth handler.
    """

    def __init__(self):
        self.user_agent = 'cmislib/%s +http://code.google.com/p/cmislib/'

    def get(self,
            url,
            username=None,
            password=None,
            **kwargs):

        """ Makes a get request to the URL specified."""

        if kwargs:
            if url.find('?') >= 0:
                url = url + '&' + urlencode(kwargs)
            else:
                url = url + '?' + urlencode(kwargs)

        request = RESTRequest(url, method='GET')

        # add a user-agent
        request.add_header('User-Agent', self.user_agent)

        # create a password manager
        passwordManager = HTTPPasswordMgrWithDefaultRealm()
        passwordManager.add_password(None, url, username, password)

        opener = build_opener(SmartRedirectHandler(),
                                      DefaultErrorHandler(),
                                      HTTPBasicAuthHandler(passwordManager))

        return opener.open(request)

    def delete(self, url, username=None, password=None, **kwargs):

        """ Makes a delete request to the URL specified. """

        if kwargs:
            if url.find('?') >= 0:
                url = url + '&' + urlencode(kwargs)
            else:
                url = url + '?' + urlencode(kwargs)

        request = RESTRequest(url, method='DELETE')

        # add a user-agent
        request.add_header('User-Agent', self.user_agent)

        # create a password manager
        passwordManager = HTTPPasswordMgrWithDefaultRealm()
        passwordManager.add_password(None, url, username, password)

        opener = build_opener(SmartRedirectHandler(),
                                      DefaultErrorHandler(),
                                      HTTPBasicAuthHandler(passwordManager))

        #try:
        #    opener.open(request)
        #except urllib2.HTTPError, e:
        #    if e.code is not 204:
        #        raise e
        #return None
        return opener.open(request)

    def put(self,
            url,
            payload,
            contentType,
            username=None,
            password=None,
            **kwargs):

        """
        Makes a PUT request to the URL specified and includes the payload
        that gets passed in. The content type header gets set to the
        specified content type.
        """

        if kwargs:
            if url.find('?') >= 0:
                url = url + '&' + urlencode(kwargs)
            else:
                url = url + '?' + urlencode(kwargs)

        request = RESTRequest(url, payload, method='PUT')

        # set the content type header
        request.add_header('Content-Type', contentType)

        # add a user-agent
        request.add_header('User-Agent', self.user_agent)
        # create a password manager
        passwordManager = HTTPPasswordMgrWithDefaultRealm()
        passwordManager.add_password(None, url, username, password)

        opener = build_opener(SmartRedirectHandler(),
                                      DefaultErrorHandler(),
                                      HTTPBasicAuthHandler(passwordManager))

        return opener.open(request)

    def post(self,
             url,
             payload,
             contentType,
             username=None,
             password=None,
             **kwargs):

        """
        Makes a POST request to the URL specified and posts the payload
        that gets passed in. The content type header gets set to the
        specified content type.
        """

        if kwargs:
            if url.find('?') >= 0:
                url = url + '&' + urlencode(kwargs)
            else:
                url = url + '?' + urlencode(kwargs)

        request = RESTRequest(url, payload, method='POST')

        # set the content type header
        request.add_header('Content-Type', contentType)

        # add a user-agent
        request.add_header('User-Agent', self.user_agent)

        # create a password manager
        passwordManager = HTTPPasswordMgrWithDefaultRealm()
        passwordManager.add_password(None, url, username, password)

        opener = build_opener(SmartRedirectHandler(),
                                      DefaultErrorHandler(),
                                      HTTPBasicAuthHandler(passwordManager))

        try:
            return opener.open(request)
        except HTTPError, e:
            if e.code is not 201:
                return e
            else:
                return e.read()


class RESTRequest(Request):

    """
    Overrides urllib's request default behavior
    """

    def __init__(self, *args, **kwargs):
        """ Constructor """
        self._method = kwargs.pop('method', 'GET')
        assert self._method in ['GET', 'POST', 'PUT', 'DELETE']
        Request.__init__(self, *args, **kwargs)

    def get_method(self):
        """ Override the get method """
        return self._method
