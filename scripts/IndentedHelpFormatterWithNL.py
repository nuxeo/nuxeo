#!/usr/bin/env python
"""
(C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.

All rights reserved. This program and the accompanying materials
are made available under the terms of the GNU Lesser General Public License
(LGPL) version 2.1 which accompanies this distribution, and is available at
http://www.gnu.org/licenses/lgpl-2.1.html

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
Lesser General Public License for more details.

Contributors:
    Julien Carsique

Sources:
    http://groups.google.com/group/comp.lang.python/browse_frm/thread/6df6e6b541a15bc2 @IgnorePep8

This is a formatter for optparse which replace double line breaks in help
strings with one line break (whereas IndentedHelpFormatter removes all)."""

from optparse import IndentedHelpFormatter
import textwrap

#pylint: skip-file
class IndentedHelpFormatterWithNL(IndentedHelpFormatter):

    def format_description(self, description):
        if not description:
            return ""
        desc_width = self.width - self.current_indent
        indent = " " * self.current_indent
        # CHANGES BEGIN
        desc_lines = [
            textwrap.fill(line, desc_width, initial_indent=indent,
                          subsequent_indent=indent)
            for line in description.split("\n\n")]
        result = "\n".join(desc_lines) + "\n"
        # CHANGES END
        return result

    def format_option(self, option):
        result = []
        opts = self.option_strings[option]
        opt_width = self.help_position - self.current_indent - 2
        if len(opts) > opt_width:
            opts = "%*s%s\n" % (self.current_indent, "", opts)
            indent_first = self.help_position
        else:  # start help on same line as opts
            opts = "%*s%-*s  " % (self.current_indent, "", opt_width, opts)
            indent_first = 0
        result.append(opts)
        if option.help:
            help_text = self.expand_default(option)
            # CHANGES BEGIN
            help_lines = []
            for para in help_text.split("\n\n"):
                help_lines.extend(textwrap.wrap(para, self.help_width))
            # CHANGES END
            result.append("%*s%s\n" % (indent_first, "", help_lines[0]))
            result.extend(["%*s%s\n" % (self.help_position, "", line)
                for line in help_lines[1:]])
        elif opts[-1] != "\n":
            result.append("\n")
        return "".join(result)
