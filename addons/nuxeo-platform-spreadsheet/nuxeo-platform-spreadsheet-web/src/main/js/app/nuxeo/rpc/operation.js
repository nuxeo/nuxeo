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

/**
 * Wrapper for nuxeo.Operation
 */
class Operation {
  constructor(conn, opId) {
    this.opId = opId;
    this.conn = conn;
    this._params = {};
    this._headers = {};
  }

  get params() { return this._params; }

  get headers() {
    return this._headers;
  }

  set depth(value) {
    this.headers[`depth`] = value;
  }

  enrich(objectType, ...enrichers) {
    this.headers[`enrichers-${objectType}`] = enrichers.join(',');
  }

  fetch(objectType, ...parts) {
    this.headers[`fetch-${objectType}`] = parts.join(',');
  }

  translate(objectType, ...elements) {
    this.headers[`translate-${objectType}`] = elements.join(',');
  }

  execute() {
    return new Promise((resolve, reject) => {
      this.conn.operation(this.opId)
      .headers(this._headers)
      .params(this._params)
      .execute((error, data) => {
        if (error) {
          reject(Error(error));
        }
        resolve(data);
      });
    });
  }
}

export {Operation};
