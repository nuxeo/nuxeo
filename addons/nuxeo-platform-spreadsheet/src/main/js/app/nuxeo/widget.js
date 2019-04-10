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
import {Connection} from './connection';

class Widget {
  constructor(conn, definition) {
    this.conn = conn;
    this.widget = definition;

    if (!this.widget.fields) {
      return;
    }

    // TODO(nfgs): Handle multiple fields
    this.field = this.widget.fields[0].fieldName;

    // Rename data['schema']['property'] to data.schema.property
    this.field = this.field.replace(/\['/g, '.').replace(/']/g, '');

    // In a listing, the layout is not usually rendered on the document, but on a PageSelection element,
    // wrapping the  DocumentModel to handle selection information.
    // So field binding will look like data.dc.title  instead of dc:title.
    // Let's fix that:
    if (this.field.startsWith('data.')) {
      this.field = this.field.substr(5).replace('.', ':');
    }
  }

  get name() { return this.widget.name; }
  get label() { return this.widget.labels.any; }
  get type() { return this.widget.type; }
  set type(t) { this.widget.type = t; }
  get properties() { return (this.widget.properties) ? this.widget.properties.any : {}; }
  set properties(p) { this.widget.properties = p; }
}

export {Widget};
