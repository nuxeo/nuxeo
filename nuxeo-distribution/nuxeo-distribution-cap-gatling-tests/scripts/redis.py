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

import sys

def encodeUsingPipeProtocol(cmd):
    ret = "*%s\r\n" % str(len(cmd))
    for arg in cmd:
        ret += "$%s\r\n%s\r\n" % (str(len(arg)), arg)
    return ret

def escapeRedisValue(value):
    value.replace('"', '\\"')
    return value


class RedisWriter(object):
    """ Output redis command encoded or not, taking care of key prefixes
    """
    def __init__(self, out=sys.stdout, prefix="imp", usePipeProtocol=True):
        self.out = out
        self.prefix = prefix
        self.usePipeProtocol = usePipeProtocol

    def set(self, key, value):
        self.write(["SET", self._addPrefix(key), escapeRedisValue(value)])

    def sadd(self, key, value):
        self.write(["SADD", self._addPrefix(key), escapeRedisValue(value)])

    def zadd(self, key, level, value):
        self.write(["ZADD", self._addPrefix(key), str(level), escapeRedisValue(value)])

    def rpush(self, key, value):
        self.write(["RPUSH", self._addPrefix(key), escapeRedisValue(value)])

    def hmset(self, key, values):
        cmd = ["HMSET", self._addPrefix(key)]
        for field, value in values.items():
            cmd.append(field)
            cmd.append(escapeRedisValue(value))
        self.write(cmd)

    def hsetnx(self, key, field, value):
        cmd = ["HSETNX", self._addPrefix(key), field, escapeRedisValue(value)]
        self.write(cmd)

    def _addPrefix(self, key):
        if self.prefix is not None:
            return self.prefix + ":" + key
        return key

    def write(self, cmd):
        """ Returns the Redis commmand as string or encodede as Redis protocol for mass import using --pipe
        Don't take care about prefix or escaping values
        """
        if (self.usePipeProtocol == True):
            return self.out.write(encodeUsingPipeProtocol(cmd))
        return self.out.write(" ".join(cmd) + '\n')
