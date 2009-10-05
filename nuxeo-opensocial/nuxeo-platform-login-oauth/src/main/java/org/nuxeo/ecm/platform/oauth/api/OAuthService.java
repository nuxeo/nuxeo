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

package org.nuxeo.ecm.platform.oauth.api;

import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;

public interface OAuthService {
  boolean verify(OAuthMessage message,
      String consumerKey);

  /**
   * Return an OAuthAccessor depending on the key informations that
   * were contributed
   * @param consumerKey
   * @return
   */
  OAuthConsumer getOAuthConsumer(String consumerKey);
}
