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
    // NXP-18425: use both headers to support both nginx (which requires hyphen)
    // and NXP-7.10 with no HF (no hyphen support -> no nginx support)
    var enrichingHeader = enrichers.join(',');
    this.headers[`enrichers.${objectType}`] = enrichingHeader;
    this.headers[`enrichers-${objectType}`] = enrichingHeader;
  }

  fetch(objectType, ...parts) {
    // NXP-18425: use both headers to support both nginx (which requires hyphen)
    // and NXP-7.10 with no HF (no hyphen support -> no nginx support)
    var fetchingHeader = parts.join(',');
    this.headers[`fetch.${objectType}`] = fetchingHeader;
    this.headers[`fetch-${objectType}`] = fetchingHeader;
  }

  translate(objectType, ...elements) {
    // NXP-18425: use both headers to support both nginx (which requires hyphen)
    // and NXP-7.10 with no HF (no hyphen support -> no nginx support)
    var translationHeader = elements.join(',');
    this.headers[`translate.${objectType}`] = translationHeader;
    this.headers[`translate-${objectType}`] = translationHeader;
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
