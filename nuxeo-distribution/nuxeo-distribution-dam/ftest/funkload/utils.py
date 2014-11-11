#!/usr/bin/python
"""
Misc utils
"""

import os
from random import randint

def extractToken(text, tag_start, tag_end):
    start = text.find(tag_start) + len(tag_start)
    end = text.find(tag_end, start)
    if start < 0 or end < 0:
        return None
    return text[start:end]

def extractJsfState(html):
    state = extractToken(html, '<input type="hidden" name="javax.faces.ViewState"'\
                 ' id="javax.faces.ViewState" value="', '"')
    if not state:
        raise ValueError('No JSF state found in the page.')
    if not state.startswith('j_id') or len(state)>10:
        raise ValueError('Invalid JSF State found: %s.' % str(state))
    return state

def getRandomLines(filename, nb_line):
    """Return a list of lines randomly taken from filename"""
    fd = open(filename, "r")
    filesize = os.stat(filename)[6]
    ret = []
    for i in range(nb_line):
        pos = max(randint(0, filesize - 105), 0)
        fd.seek(pos)
        fd.readline() # skip line
        line = fd.readline().strip()
        if line:
            ret.append(line)
    return ret
