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
import {Query} from '../../nuxeo/rpc/query';

class DocumentEditor extends Select2Editor {

  query(connection, properties, term) {
    var q = new Query(connection);
    // Set the properties
    Object.assign(q.params, properties);
    q.nxql = properties.query;
    q.params.searchTerm = term + '%';
    q.pageProvider = properties.pageProviderName || 'default_document_suggestion';
    q.page = 0;
    q.pageSize = 20;
    // Execute the query
    return q.run().then((result) => result.entries);
  }

  formatter(doc) {
    return doc.text || doc.title;
  }

  getEntryId(item) {
    return item.uid;
  }
}

export {DocumentEditor};
