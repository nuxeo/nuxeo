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

package org.nuxeo.ecm.spaces.core.impl.docwrapper;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.core.impl.Constants;

public class SpaceDocumentWrapper extends DocumentWrapper implements Space {

  SpaceDocumentWrapper(DocumentModel doc) {
    super(doc);
  }

  public String getLayout() {
    return getInternalStringProperty(Constants.Space.SPACE_LAYOUT);
  }

  public String getCategory() {
    return getInternalStringProperty(Constants.Space.SPACE_CATEGORY);
  }

  public boolean isEqualTo(Space space) {
    return space.getId()!=null && space.getId().equals(getId());
  }

  public String getTheme() {
    return getInternalStringProperty(Constants.Space.SPACE_THEME);
  }

}
