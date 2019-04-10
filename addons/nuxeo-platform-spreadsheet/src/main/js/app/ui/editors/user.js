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

  prepare(row, col, prop, td, originalValue, cellProperties) {
    // flatten our values to a list of ids
    var value = (Array.isArray(originalValue)) ? originalValue.map((u) => u.id) : originalValue.id;
    super.prepare(row, col, prop, td, value, cellProperties);
  }

  saveValue(val, ctrlDown) {
    // create directory entries again on save
    var value = val[0][0];

    if (value) {
      value = value.split(',').map(function (id) {
        return {
          'entity-type': 'user',
          id: id
        };
      }.bind(this));
      // unwrap the map result if not multiple
      if (!this.cellProperties.multiple) {
        value = value[0];
      }
    } else {
      value = this.cellProperties.multiple ? [] : null;
    }

    super.saveValue([[value]], ctrlDown);
  }

  query(connection, properties, term) {
    var op = new Operation(connection, 'UserGroup.Suggestion');
    // Set the properties
    Object.assign(op.params, properties);
    op.params.searchTerm = term;
    op.params.searchType = 'USER_TYPE';
    // Perform the search
    return op.execute();
  }

  formatter(entry) {
    return entry.text || entry.displayLabel;
  }
}

function UserRenderer(instance, td, row, col, prop, value, cellProperties) {
  if (value) {
    if (!Array.isArray(value)) {
      value = [value];
    }
    arguments[5] = value.map((u) =>  u.id).join(','); // jshint ignore:line
  }
  cellProperties.defaultRenderer.apply(this, arguments);
}

export {UserEditor, UserRenderer};
