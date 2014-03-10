#!/usr/bin/env python
##
## (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
##
## All rights reserved. This program and the accompanying materials
## are made available under the terms of the GNU Lesser General Public License
## (LGPL) version 2.1 which accompanies this distribution, and is available at
## http://www.gnu.org/licenses/lgpl-2.1.html
##
## This library is distributed in the hope that it will be useful,
## but WITHOUT ANY WARRANTY; without even the implied warranty of
## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
## Lesser General Public License for more details.
##
## Contributors:
##     Julien Carsique
##
## Sources:
## pylint: disable=C0301
##     http://stackoverflow.com/questions/566746/how-to-get-console-window-width-in-python @IgnorePep8
##     http://stackoverflow.com/questions/263890/how-do-i-find-the-width-height-of-a-terminal-window @IgnorePep8
##     https://gist.github.com/jtriley/1108174
##
## Get terminal size like shutil.get_terminal_size() which is only available
## since Python 3.3. This one should work on Linux, Mac OS X, Windows, Cygwin.
##
import os
import platform
import shlex
import struct
import subprocess


#pylint: disable=C0103
def get_terminal_size():
    """Get x/width/columns and y/height/rows/lines of console/terminal"""
    # Use environment variables if defined, not the common case though.
    cols, rows = (os.environ.get('COLUMNS', -1), os.environ.get('LINES', -1))
    if cols > 0 and rows > 0:
        return (cols, rows)

    cr = _get_terminal_size_tput()
    if cr is None:
        current_os = platform.system()
        if current_os == 'Windows':
            cr = _get_terminal_size_windows()
        if (current_os in ['Linux', 'Darwin'] or
            current_os.startswith('CYGWIN')):
            cr = _get_terminal_size_linux()
    if cr is None:
        # common default terminal emulator values
        cr = (80, 24)
    return cr


def _get_terminal_size_tput():
    if not os.environ.get("TERM", None):
        return None
    try:
        cols = int(subprocess.check_output(shlex.split('tput cols')))
        rows = int(subprocess.check_output(shlex.split('tput lines')))
        return (cols, rows)
    except:
        return None


#pylint: disable=C0103,C0301
def _get_terminal_size_windows():
    try:
        from ctypes import windll, create_string_buffer
        # stdin = -10, stdout = -11, stderr = -12
        h = windll.kernel32.GetStdHandle(-12)  #@UndefinedVariable @IgnorePep8
        csbi = create_string_buffer(22)
        res = windll.kernel32.GetConsoleScreenBufferInfo(h, csbi)  # @UndefinedVariable @IgnorePep8
        if res:
            (_, _, _, _, _, left, top, right, bottom, _, _) = struct.unpack(
                                                    "hhhhHhhhhhh", csbi.raw)
            cols = right - left + 1
            rows = bottom - top + 1
            return cols, rows
    except:
        pass


#pylint: disable=C0103
def _get_terminal_size_linux():
    def ioctl_GWINSZ(fd):
        try:
            import fcntl
            import termios  # @UnresolvedImport
            return struct.unpack('hh',
                               fcntl.ioctl(fd, termios.TIOCGWINSZ, '1234'))
        except:
            pass
    rc = ioctl_GWINSZ(0) or ioctl_GWINSZ(1) or ioctl_GWINSZ(2)
    if not rc:
        try:
            fd = os.open(os.ctermid(), os.O_RDONLY)
            rc = ioctl_GWINSZ(fd)
            os.close(fd)
        except:
            return None
    return int(rc[1]), int(rc[0])


def main():
    print  '(width, height) = ', get_terminal_size()

if __name__ == "__main__":
    main()
