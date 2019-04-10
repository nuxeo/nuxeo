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
 * Wrapper for nuxeo.Request
 */
class Request {
  constructor(conn, path = '') {
    this.path = path;
    this.conn = conn;
    this._params = {};
    this._headers = {};
  }

  get params() {
    return this._params;
  }

  get headers() {
    return this._headers;
  }

  set enrichers(lst) {
    this.headers['X-NXContext-Category'] = lst.join(',');
  }

  execute(method = 'get', path) {
    return new Promise((resolve, reject) => {
      this.conn.request(path || this.path)
        .repositoryName(undefined)
        .headers(this._headers)
        .query(this._params)
        [method]((error, data) => {
          if (error) {
            reject(Error(error));
          }
          resolve(data);
        });
    });
  }
}

export {Request};
