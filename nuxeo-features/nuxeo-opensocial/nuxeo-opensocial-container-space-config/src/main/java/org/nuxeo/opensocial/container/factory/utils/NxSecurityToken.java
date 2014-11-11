/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.opensocial.container.factory.utils;

import org.apache.shindig.auth.SecurityToken;

/**
 * Create Security Token
 * @author 10044826
 *
 */
public class NxSecurityToken implements SecurityToken {

  private String viewer;
  private String owner;

  public NxSecurityToken(String viewer, String owner) {
    this.viewer = viewer;
    this.owner = owner;
  }

  public String getAppId() {
    // TODO Auto-generated method stub
    return null;
  }

  public String getAppUrl() {
    // TODO Auto-generated method stub
    return null;
  }

  public String getDomain() {
    // TODO Auto-generated method stub
    return null;
  }

  public long getModuleId() {
    // TODO Auto-generated method stub
    return 0;
  }

  public String getOwnerId() {
    return owner;
  }

  public String getTrustedJson() {
    // TODO Auto-generated method stub
    return null;
  }

  public String getUpdatedToken() {
    // TODO Auto-generated method stub
    return null;
  }

  public String getViewerId() {
    return viewer;
  }

  public boolean isAnonymous() {
    // TODO Auto-generated method stub
    return false;
  }

}
