class CmisException(Exception):

    """
    Common base class for all exceptions.
    """

    pass


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
