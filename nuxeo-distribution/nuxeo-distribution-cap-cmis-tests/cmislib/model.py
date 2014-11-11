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
Module containing the CmisClient object, which is responsible for
keeping track of connection information. The name 'model' is no longer
really appropriate, but it is kept for backwards compatibility.
"""
import logging

from cmislib.atompub.binding import AtomPubBinding
from cmislib.cmis_services import Binding


moduleLogger = logging.getLogger('cmislib.model')


class CmisClient(object):

    """
    Handles all communication with the CMIS provider.
    """

    def __init__(self, repositoryUrl, username, password, **kwargs):

        """
        This is the entry point to the API. You need to know the
        :param repositoryUrl: The service URL of the CMIS provider
        :param username: Username
        :param password: Password

        >>> client = CmisClient('http://localhost:8080/alfresco/s/cmis', 'admin', 'admin')
        """

        self.repositoryUrl = repositoryUrl
        self.username = username
        self.password = password
        self.extArgs = kwargs
        if kwargs.has_key('binding') and (isinstance(kwargs['binding'], Binding)):
            self.binding = kwargs['binding']
        else:
            self.binding = AtomPubBinding(**kwargs)
        self.logger = logging.getLogger('cmislib.model.CmisClient')
        self.logger.info('Creating an instance of CmisClient')

    def __str__(self):
        """To string"""
        return 'CMIS client connection to %s' % self.repositoryUrl

    def getRepositories(self):

        """
        Returns a dict of high-level info about the repositories available at
        this service. The dict contains entries for 'repositoryId' and
        'repositoryName'.

        >>> client.getRepositories()
        [{'repositoryName': u'Main Repository', 'repositoryId': u'83beb297-a6fa-4ac5-844b-98c871c0eea9'}]
        """

        return self.binding.getRepositoryService().getRepositories(self)

    def getRepository(self, repositoryId):

        """
        Returns the repository identified by the specified repositoryId.

        >>> repo = client.getRepository('83beb297-a6fa-4ac5-844b-98c871c0eea9')
        >>> repo.getRepositoryName()
        u'Main Repository'
        """
        return self.binding.getRepositoryService().getRepository(self, repositoryId)

    def getDefaultRepository(self):

        """
        There does not appear to be anything in the spec that identifies
        a repository as being the default, so we'll define it to be the
        first one in the list.

        >>> repo = client.getDefaultRepository()
        >>> repo.getRepositoryId()
        u'83beb297-a6fa-4ac5-844b-98c871c0eea9'
        """

        return self.binding.getRepositoryService().getDefaultRepository(self)

    defaultRepository = property(getDefaultRepository)
    repositories = property(getRepositories)
