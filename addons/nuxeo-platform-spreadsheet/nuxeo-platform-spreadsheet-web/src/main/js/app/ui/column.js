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

    // These widgets use all the properties for rendering
    // so we must either replicate the logic here or just fall back to the sort property
    if (this.widget.field === 'data') {
      this.widget.field = this.def.properties.any.sortPropertyName;
    }

    // Mixin widget
    if (WIDGETS[this.widget.name]) {
      Object.assign(this.widget, WIDGETS[widget.name]);
    }

    // Mixin custom field widget
    const field = this.widget.field;
    if (CUSTOM_FIELDS[field] && CUSTOM_FIELDS[field].widget) {
      Object.assign(this.widget, CUSTOM_FIELDS[field].widget);
    }
    if (CUSTOM_FIELDS[field] && CUSTOM_FIELDS[field].properties) {
      Object.assign(this.widget.properties, CUSTOM_FIELDS[field].properties);
    }

    // Mixin widget type
    if (WIDGET_TYPES[this.widget.type]) {
      // reset custom type since it's not known to handsontable
      const type = this.widget.type;
      delete this.widget.type;
      Object.assign(this.widget, WIDGET_TYPES[type]);
    }

    // Make widget properties available in the columns
    Object.assign(this, this.widget);

    // Bind the renderer to this column
    if (this.renderer) {
      this.renderer = this.renderer.bind(this);
    } else {
      this.renderer = defaultRenderer;
    }
  }

  get data() {
    if (!this.field) {
      return null;
    }
    // Check for special field overrides
    if (CUSTOM_FIELDS[this.field] && CUSTOM_FIELDS[this.field].field) {
      return CUSTOM_FIELDS[this.field].field;
    }
    return `properties.${this.field}`;
  }

  get header() {
    var header = this.def.properties.any.label || this.field;
    if (this.def.properties.any.useFirstWidgetLabelAsColumnHeader) {
      header = this.widget.label || this.widget.labels.any;
    }
    return header;
  }

  get hasSupportedWidgetType() {
    return !!WIDGET_TYPES[this.widget.type];
  }
}

const CUSTOM_FIELDS = {

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
  },
  'thumb:thumbnail': {
    widget: {
      readOnly: true,
      type: 'image'
    }
  },
};

export {Column};
