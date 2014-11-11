#!/usr/bin/python
import os
import sys
from lxml.etree import iterparse
from lxml.etree import ElementTree
from optparse import OptionParser

def edit_files(options, blacklist = ('.svn',)):
    """Recursively scan xml files and update the given properites"""

    datasource_map = {
        'connection-url': options.url,
        'user-name': options.username,
        'password': options.password,
    }

    repository_map = {
        'url': options.url,
        'user': options.username,
        'password': options.password,
    }

    for dirpath, dirnames, filenames in os.walk('.'):
        filenames = (os.path.join(dirpath, filename) for filename in filenames)


        # do not visit svn subfolders
        for dirname in blacklist:
            if dirname in dirnames:
                dirnames.remove(dirname)

        # inspect xml files
        for filename in filenames:
            if not filename.endswith('.xml'):
                continue

            changed = False

            for event, elem in iterparse(filename):
                if event == 'end':

                    if elem.tag == 'local-tx-datasource':
                        # datasource file
                        for child in elem.getchildren():
                            new_value = datasource_map.get(child.tag)
                            if (new_value is not None
                                and child.text != new_value):
                                child.text = new_value
                                changed = True

                    elif elem.tag == 'PersistenceManager':
                        # repository setup
                        for child in elem.getchildren():
                            new_value = repository_map.get(child.get('name'))
                            if (new_value is not None
                                and child.get('value') != new_value):
                                child.set('value', new_value)
                                changed = True

            if changed:
                # last element is the tree itself: serialize it
                ElementTree(elem).write_c14n(filename)
                print filename, 'updated'

if __name__ == "__main__":
    parser = OptionParser()
    parser.add_option(
        "-d", "--url", dest="url",
        help="database url, eg. jdbc:postgresql://server:port/dbname")
    parser.add_option(
        "-u", "--username", dest="username",
        help="id of the user who owns the database")
    parser.add_option(
        "-p", "--password", dest="password",
        help="password of the user who owns the database")

    (options, args) = parser.parse_args()

    if options.url or options.username or options.password:
        edit_files(options)
    else:
        parser.print_help()


