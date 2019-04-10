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
import {Layout} from './layout';

/**
 * Content view
 */
class ContentView {
    constructor(conn) {
      this.conn = conn;
      this.resultLayouts = {};
    }

    set resultLayout(layoutName) {
      this._resultLayout = layoutName;
    }

    getResultLayout(layoutName = this._resultLayout) {
      return new Promise((resolve, reject) => {
        if (this.resultLayouts[layoutName]) {
          resolve(this.resultLayouts[layoutName]);
        }
        new Layout(this.conn, layoutName).fetch().then((layout) => {
          this.resultLayouts[layoutName] = layout;
          resolve(layout);
        });
      });
    }
}

export {ContentView};
