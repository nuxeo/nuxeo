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
import {Widget} from './widget';

/**
 * Layout
 */
class Layout {
  constructor(conn, name, lang) {
    this.conn = conn;
    this.name = name;
    this.lang = lang;
    this.widgets = {};
  }

  fetch() {
    return new Promise((resolve, reject) => {
      var request = this.conn.request('/site/layout-manager/layouts/json?layoutName=' + this.name + '&lang=' + this.lang).repositoryName(undefined);
      request._url = this.conn.baseURL;	

      request.get((error, def) => {
        if (error) {
          reject(Error(error));
        }
        // Prepare a hashmap with the widgets
        this.widgets = {};
        def.widgets.forEach((widget) => this.widgets[widget.name] = new Widget(this.conn, widget));

        this.columns = def.rows;

        resolve(this);
      });
    });
  }

}

export {Layout};
