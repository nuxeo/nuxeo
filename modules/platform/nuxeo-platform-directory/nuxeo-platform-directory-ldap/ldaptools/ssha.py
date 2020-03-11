# (C) Copyright 2006 Nuxeo SAS <http://nuxeo.com>
# Authors:
# Tarek Ziade <tziade@nuxeo.com>
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
# $Id: ssha.py 7310 2006-12-04 19:12:26Z ogrisel $
"""ssha digest with salt, compatible with openldap

Usage:

    $ python ssha.py mypassword
"""
import sha, random, base64

def sshaDigest(passphrase, salt=None):
    """ returns a ssha digest (sha-1 with salt)

    this can be used to encrypt a passphrase
    using sha-1 encryption, with salt.
    compatible with openldap fields
    >>> res = sshaDigest('xxx')
    >>> len(res)
    46
    >>> res = sshaDigest('xsazdzxx')
    >>> len(res)
    46
    >>> sshaDigest('xxx').startswith('{SSHA}')
    True
    """
    if salt is None:
        salt = ''
        for i in range(8):
            salt += chr(random.randint(0, 255))
    s = sha.sha()
    s.update(passphrase)
    s.update(salt)
    encoded = base64.encodestring(s.digest()+salt).rstrip()
    crypt = '{SSHA}' + encoded
    return crypt

if __name__ == "__main__":
    import sys
    if len(sys.argv) == 2:
        print sshaDigest(sys.argv[1])
    else:
        print "usage: python ssha.py mypassword"
