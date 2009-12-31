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

package org.nuxeo.ecm.spaces.core.impl;

public interface Constants {// TODO à compléter

  public interface Document {
    String DOCUMENT_TITLE = "dc:title";
    String DOCUMENT_DESCRIPTION = "dc:description";
    String DOCUMENT_CREATOR = "dc:creator";
    String PUBLICATION_DATE = "dc:valid";
    String CREATE_DATE = "dc:created";
  }

  public interface Univers {
    String TYPE = "Univers";
    String ROOT_PATH = "/default-domain/workspaces/galaxy";
  }

  public interface Space {
    String TYPE = "Space";
    String SPACE_THEME = "space:theme";
    String SPACE_LAYOUT = "space:layout";
    String SPACE_CATEGORY = "space:categoryId";
    String SPACE_VERSIONNABLE = "space:versionnable";
  }

  public interface Gadget {
    String TYPE = "Gadget";
    String GADGET_CATEGORY = "gadget:category";
    String GADGET_PLACEID = "gadget:placeID";// html division id
    String GADGET_POSITION = "gadget:position";// position in the div
    String GADGET_COLLAPSED = "gadget:collapsed";// is the gadget collapsed
    String GADGET_PREFERENCES = "gadget:props";
    String GADGET_HEIGHT = "gadget:height";
    String GADGET_HTMLCONTENT = "gadget:htmlContent";
  }
}
