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

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.gadgets.GadgetContext;

public class NxGadgetContext extends GadgetContext {

  private String gadgetDef;
  private SecurityToken securityToken;

  public NxGadgetContext(String gadgetDef, String viewer, String owner) {
    this.gadgetDef = gadgetDef;
    this.securityToken = new NxSecurityToken(viewer, owner);
  }

  @Override
  public URI getUrl() {
    try {
      return new URI(gadgetDef);
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public SecurityToken getToken() {
    return this.securityToken;
  }

}
