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

  constructor(baseURL = 'http://localhost:8080/nuxeo', username = 'Administrator', password = 'Administrator') {
    this.baseURL = baseURL;
    this.username = username;
    this.password = password;
    super({
        baseURL: this.baseURL,
        username: this.username,
        password: this.password,
        timeout: 300000
    });
  }

  connect() {
    return new Promise((resolve, reject) => {
      super.connect((error, client) => {
        if (error) {
          reject(Error(error));
        }
        resolve(this);
      });
    });
  }
}

export {Connection};
