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
import {WIDGETS, WIDGET_TYPES} from './widgets';

 /**
  * Handsontable column options
  */
class Column {
  constructor( connection, def, widget, defaultRenderer = Handsontable.renderers.TextRenderer) {
    Object.assign(this, {connection, def, widget, defaultRenderer});
    // Mixin widget
    if (WIDGETS[widget.name]) {
      Object.assign(this.widget, WIDGETS[widget.name]);
    }
    // Mixin widget type
    if (WIDGET_TYPES[widget.type]) {
      Object.assign(this, WIDGET_TYPES[widget.type]);
    }
    // Mixin special field widget
    if (SPECIAL_FIELDS[this.field] && SPECIAL_FIELDS[this.field].widget) {
      Object.assign(this, SPECIAL_FIELDS[this.field].widget);
    }
    if (SPECIAL_FIELDS[this.field] && SPECIAL_FIELDS[this.field].properties) {
      Object.assign(this.widget.properties, SPECIAL_FIELDS[this.field].properties);
    }
    // Bind the renderer to this column
    if (this.renderer) {
      this.renderer = this.renderer.bind(this);
    } else {
      this.renderer = defaultRenderer;
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
    if (!this.field) {
      return null;
    }
    // Check for special field overrides
    if (SPECIAL_FIELDS[this.field] && SPECIAL_FIELDS[this.field].field) {
      return SPECIAL_FIELDS[this.field].field;
    }
    return `properties.${this.field}`;
  }

  get header() {
    var header = this.def.properties.any.label || this.field;
    if (this.def.properties.any.useFirstWidgetLabelAsColumnHeader) {
      header = this.widget.label;
    }
    return header;
  }

  get hasSupportedWidgetType() {
    return !!WIDGET_TYPES[this.widget.type];
  }
}

const SPECIAL_FIELDS = {

  // system metadata fields
  'dc:created': {
    widget: {
      readOnly: true
    }
  },
  'dc:modified': {
    widget: {
      readOnly: true
    }
  },
  'dc:creator': {
    widget: {
      readOnly: true
    }
  },
  'dc:lastContributor': {
    widget: {
      readOnly: true
    }
  },
  'dc:contributors': {
    widget: {
      readOnly: true
    }
  },
  'currentLifeCycleState': {
    widget: {
      readOnly: true
    },
    field: 'state'
  },
  'type': {
    widget: {
      readOnly: true
    },
    field: 'type'
  },
  'versionLabel': {
    widget: {
      readOnly: true
    },
    field: 'versionLabel'
  },
  'dc:nature': {
    properties: {
      dbl10n: false,
      localize: true
    }
  }
};

export {Column};
