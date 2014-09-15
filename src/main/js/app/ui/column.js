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
import {WIDGETS} from './widgets';
 /**
  * Handsontable column options
  */
class Column {
  constructor( connection, def, widget, renderer) {
    Object.assign(this, {connection, def, widget, renderer});
    if (WIDGETS[widget.type]) {
      Object.assign(this, WIDGETS[widget.type]);
    }
  }

  get field() {
    var field = this.widget.field;

    // These widgets use all the properties for rendering
    // so we must either replicate the logic here or just fall back to the sort property
    if (field === 'data') {
      field = this.def.properties.any.sortPropertyName;
    }

    return field;
  }

  get data() {
    return (this.field) ? `properties.${this.field}`: null;
  }

  get header() {
    var header = this.def.properties.any.label || this.field;
    if (this.def.properties.any.useFirstWidgetLabelAsColumnHeader) {
      header = this.widget.label;
    }
    return header;
  }

  get width() { return 200; }
}

export {Column};
