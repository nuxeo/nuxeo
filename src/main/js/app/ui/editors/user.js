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
import {Operation} from '../../nuxeo/rpc/operation';

class UserEditor extends Select2Editor {
  query(connection, properties, term) {
    var op = new Operation(connection, 'UserGroup.Suggestion');
    // Set the properties
    Object.assign(op.params, properties);
    op.params.searchTerm = term;
    op.params.searchType = properties.userSuggestionSearchType;
    // Perform the search
    return op.execute();
  }

  formatter(entry) {
    return entry.displayLabel;
  }
}

export {UserEditor};
