/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nelson Silva <nelson.silva@inevo.pt>
 */
import {Select2Editor} from './select2';
import {Directory} from '../../nuxeo/rpc/directory';

class DirectoryEditor extends Select2Editor {
  query(connection, properties, term) {
    var directory = new Directory(connection); // Directory name is a widget property
    // Set the properties
    Object.assign(directory, properties);
    // Perform the search
    return directory.search(term);
  }

  formatter(entry) {
    return entry.displayLabel;
  }

  getEntryId(item) {
    if (item.computedId) {
      return item.computedId;
    } else {
      return item.id;
    }
  }
}

export {DirectoryEditor};
