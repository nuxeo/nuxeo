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
 *     Mike Obrebski <mobrebski@nuxeo.com>
 */

import {DirectoryEditor} from './directory';

class ProdLineDirectoryEditor extends DirectoryEditor {

	// Let's override prepare and just pass set the select2 options ourselves
  prepare(row, col, prop, td, originalValue, cellProperties) {

  // setup the label cache
    this._labels = {};

    // flatten our values to a list of ids
    var value = (Array.isArray(originalValue)) ? originalValue.map(this.prepareEntity.bind(this)) : this.prepareEntity(originalValue);

    super.prepare(row, col, prop, td, value, cellProperties);
  }


}

export {ProdLineDirectoryEditor};
