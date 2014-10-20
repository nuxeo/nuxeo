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

class Log {

  constructor(el) {
    this.el = el;
    this._default = '';
  }

  info(msg) {
    this.el.text(msg);
  }

  error(msg) {
    this.el.text(msg);
  }

  default(msg) {
    if (msg !== undefined) {
      this._default = msg;
    }
    this.el.text(this._default);
  }
}

export {Log};