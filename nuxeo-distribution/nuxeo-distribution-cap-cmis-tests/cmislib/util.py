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
Module containing handy utility functions.
"""
import re
import iso8601
import logging
import datetime
from cmislib.domain import CmisId

moduleLogger = logging.getLogger('cmislib.util')


def multiple_replace(aDict, text):

    """
    Replace in 'text' all occurences of any key in the given
    dictionary by its corresponding value.  Returns the new string.

    See http://code.activestate.com/recipes/81330/
    """

    # Create a regular expression  from the dictionary keys
    regex = re.compile("(%s)" % "|".join(map(re.escape, aDict.keys())))

    # For each match, look-up corresponding value in dictionary
    return regex.sub(lambda mo: aDict[mo.string[mo.start():mo.end()]], text)


def parsePropValue(value, nodeName):

    """
    Returns a properly-typed object based on the type as specified in the
    node's element name.
    """

    moduleLogger.debug('Inside parsePropValue')

    if nodeName == 'propertyId':
        return CmisId(value)
    elif nodeName == 'propertyString':
        return value
    elif nodeName == 'propertyBoolean':
        bDict = {'false': False, 'true': True}
        return bDict[value.lower()]
    elif nodeName == 'propertyInteger':
        return int(value)
    elif nodeName == 'propertyDecimal':
        return float(value)
    elif nodeName == 'propertyDateTime':
        # %z doesn't seem to work, so I'm going to trunc the offset
        # not all servers return microseconds, so those go too
        return parseDateTimeValue(value)
    else:
        return value


def parsePropValueByType(value, typeName):

    """
    Returns a properly-typed object based on the type as specified in the
    node's property definition.
    """

    moduleLogger.debug('Inside parsePropValueByType: %s: %s', typeName, value)

    if typeName == 'id':
        if value:
            return CmisId(value)
        else:
            return None
    elif typeName == 'string':
        return value
    elif typeName == 'boolean':
        if not value:
            return False
        if type(value) == bool:
            return value
        else:
            bDict = {'false': False, 'true': True}
            return bDict[value.lower()]
    elif typeName == 'integer':
        if value:
            return int(value)
        else:
            return 0
    elif typeName == 'decimal':
        if value:
            # search result relevance is returning as an arrary of decimals
            # in the browser binding for some reason
            if isinstance(value, list):
                return float(value[0])
            else:
                return float(value)
        else:
            return 0.0
    elif typeName == 'datetime':
        # %z doesn't seem to work, so I'm going to trunc the offset
        # not all servers return microseconds, so those go too
        return parseDateTimeValue(value)
    else:
        return value


def parseDateTimeValue(value):

    """
    Utility function to return a datetime from a string.
    """
    if type(value) == str:
        return iso8601.parse_date(value)
    elif type(value) == int:
        return datetime.datetime.fromtimestamp(value / 1000)
    else:
        return


def parseBoolValue(value):

    """
    Utility function to parse booleans and none from strings
    """

    if value == 'false':
        return False
    elif value == 'true':
        return True
    elif value == 'none':
        return None
    else:
        return value


def toCMISValue(value):

    """
    Utility function to convert Python values to CMIS string values
    """

    if value is False:
        return 'false'
    elif value is True:
        return 'true'
    elif value is None:
        return 'none'
    else:
        return value
