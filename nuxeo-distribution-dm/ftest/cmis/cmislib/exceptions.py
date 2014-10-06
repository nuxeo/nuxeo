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
Module containing exceptions.
"""
class CmisException(Exception):

    """
    Common base class for all exceptions.
    """

    def __init__(self, status=None, url=None):
        Exception.__init__(self, "Error %s at %s" % (status, url))
        self.status = status
        self.url = url


class InvalidArgumentException(CmisException):

    """ InvalidArgumentException """

    pass


class ObjectNotFoundException(CmisException):

    """ ObjectNotFoundException """

    pass


class NotSupportedException(CmisException):

    """ NotSupportedException """

    pass


class PermissionDeniedException(CmisException):

    """ PermissionDeniedException """

    pass


class RuntimeException(CmisException):

    """ RuntimeException """

    pass


class ConstraintException(CmisException):

    """ ConstraintException """

    pass


class ContentAlreadyExistsException(CmisException):

    """ContentAlreadyExistsException """

    pass


class FilterNotValidException(CmisException):

    """FilterNotValidException """

    pass


class NameConstraintViolationException(CmisException):

    """NameConstraintViolationException """

    pass


class StorageException(CmisException):

    """StorageException """

    pass


class StreamNotSupportedException(CmisException):

    """ StreamNotSupportedException """

    pass


class UpdateConflictException(CmisException):

    """ UpdateConflictException """

    pass


class VersioningException(CmisException):

    """ VersioningException """

    pass
