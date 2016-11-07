#
#      Licensed to the Apache Software Foundation (ASF) under one
#      or more contributor license agreements.  See the NOTICE file
#      distributed with this work for additional information
#      regarding copyright ownership.  The ASF licenses this file
#      to you under the Apache License, Version 2.0 (the
#      "License"); you may not use this file except in compliance
#      with the License.  You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
#      Unless required by applicable law or agreed to in writing,
#      software distributed under the License is distributed on an
#      "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#      KIND, either express or implied.  See the License for the
#      specific language governing permissions and limitations
#      under the License.
#

"""
Module containing the base Binding class and other service objects.
"""
from cmislib.exceptions import CmisException, RuntimeException, \
    ObjectNotFoundException, InvalidArgumentException, \
    PermissionDeniedException, NotSupportedException, \
    UpdateConflictException


class Binding(object):

    """
    Represents the binding used to communicate with the CMIS server.
    """

    def getRepositoryService(self):

        """
        Returns the repository service specific to this binding.
        """

        pass

    def _processCommonErrors(self, error, url):

        """
        Maps HTTPErrors that are common to all to exceptions. Only errors
        that are truly global, like 401 not authorized, should be handled
        here. Callers should handle the rest.
        """

        if error['status'] == '401':
            raise PermissionDeniedException(error['status'], url)
        elif error['status'] == '400':
            raise InvalidArgumentException(error['status'], url)
        elif error['status'] == '404':
            raise ObjectNotFoundException(error['status'], url)
        elif error['status'] == '403':
            raise PermissionDeniedException(error['status'], url)
        elif error['status'] == '405':
            raise NotSupportedException(error['status'], url)
        elif error['status'] == '409':
            raise UpdateConflictException(error['status'], url)
        elif error['status'] == '500':
            raise RuntimeException(error['status'], url)
        else:
            raise CmisException(error['status'], url)


class RepositoryServiceIfc(object):

    """
    Defines the interface for the repository service.
    """

    def getRepositories(self, client):

        """
        Returns a list of repositories for this server.
        """

        pass

    def getRepositoryInfo(self):

        """
        Returns the repository information for this server.
        """

        pass
