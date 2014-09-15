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
 * Paginated Query
 */
class Query extends Operation {

  constructor(connection) {
    super(connection, 'Document.PageProvider');
  }

  set nxql(q) {
    this.params.language = 'nxql';
    this.params.query = q;
  }

  set queryParams(p) {
    this.params.queryParams = p;
  }

  set pageProvider(name) {
    this.params.name = name;
  }

  set page(p) {
    this.params.page = p;
  }

  set pageSize(s) {
    this.params.pageSize = s;
  }

  run() {
    return this.execute();
  }
}

export {Query};
