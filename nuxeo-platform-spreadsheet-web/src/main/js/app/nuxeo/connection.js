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
 * Wrapper for nuxeo.Client
 */
class Connection extends nuxeo.Client {

  constructor(baseURL = '/nuxeo', username = null, password = null) {
    super({
        baseURL,
        username,
        password,
        timeout: 300000
    });
  }

  get baseURL() { return this._baseURL; }

  connect() {
    return new Promise((resolve, reject) => {
      var headers = {
        'Accept': 'application/json'
      };

      var xhrFields = {};
      if (this._username && this._password) {
        headers.Authorization = 'Basic ' + btoa(this._username + ':' + this._password);
        xhrFields = {
          withCredentials: true
        };
      }

      $.ajax({
        type: 'POST',
        url: `${this._automationURL}/login`,
        headers,
        xhrFields
      })
      .done((data, textStatus, jqXHR) => {
        if (data['entity-type'] === 'login') {
          this.connected = true;
          resolve(this);
        } else {
          reject(Error(data));
        }
      })
      .fail(function(jqXHR, textStatus, errorThrown) {
         reject(Error(errorThrown));
      });
    });
  }
}

export {Connection};
