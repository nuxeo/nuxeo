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
import {Operation} from './operation';

/**
 * Directory
 */
class Directory extends Operation {

  constructor(connection, directoryName, language) {
    super(connection, 'Directory.SuggestEntries');
    this.directoryName = directoryName;
    this.language = language;
  }

  get directoryName() { return this.params.directoryName; }
  set directoryName(name) {
    this.params.directoryName = name;
  }

  get translateLabels() { return this.params.translateLabels; }
  set translateLabels(flag) {
    this.params.translateLabels = flag;
  }

  set dbl10n(flag) {
    this.params.dbl10n = flag;
  }

  set localize(flag) {
    this.params.localize = flag;
  }

  set language(lang) {
    this.params.lang = lang;
  }

  entries() {
    return this.search();
  }

  search(term) {
    this.params.searchTerm = term;
    return this.execute();
  }
}

export {Directory};
