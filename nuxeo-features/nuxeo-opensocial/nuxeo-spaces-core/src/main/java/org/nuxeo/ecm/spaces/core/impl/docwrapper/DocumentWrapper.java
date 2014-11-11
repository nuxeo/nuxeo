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

import java.io.Serializable;
import java.util.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.spaces.core.impl.Constants;

public class DocumentWrapper {

  private static final Log LOGGER = LogFactory.getLog(DocumentWrapper.class);

  DocumentModel internalDoc = null;

  public DocumentModel getInternalDocument() {
    return internalDoc;
  }

  protected DocumentWrapper(DocumentModel doc) {
    this.internalDoc = doc;
  }

  public final String getId() {
    return internalDoc.getId();
  }

  public final String getName() {
    return internalDoc.getName();
  }

  public final String getTitle() {
    try {
      return internalDoc.getTitle();
    } catch (ClientException e) {
      LOGGER.error("Unable to retrieve document title, returns null", e);
      return null;
    }
  }

  public String getOwner() {
    return getInternalStringProperty(Constants.Document.DOCUMENT_CREATOR);
  }

  public Calendar getDatePublication() {
    return getInternalCalendarProperty(Constants.Document.PUBLICATION_DATE);
  }

  public final void setTitle(String title) throws ClientException {
    internalDoc.setPropertyValue(Constants.Document.DOCUMENT_TITLE, title);
  }

  public final String getDescription() {
    return getInternalStringProperty(Constants.Document.DOCUMENT_DESCRIPTION);
  }

  public final void setDescription(String description) throws ClientException {
    setStringProperty(Constants.Document.DOCUMENT_DESCRIPTION, description);
  }

  public final void setStringProperty(String key, String title)
      throws ClientException {
    internalDoc.setPropertyValue(key, title);
  }

  protected String getInternalStringProperty(String key) {
    try {
      return (String) internalDoc.getProperty(key)
          .getValue();
    } catch (ClientException e) {
      LOGGER.error("Unable to retrieve property '" + key + "', returns null", e);
      return null;
    }
  }

  protected int getInternalIntegerProperty(String key) {
    try {
      Serializable ser = internalDoc.getProperty(key)
          .getValue();
      if (ser == null)
        return 0;
      return ((Long) ser).intValue();
    } catch (ClientException e) {
      LOGGER.error("Unable to retrieve integer property '" + key
          + "', returns 0", e);
      return 0;
    }
  }

  protected boolean getInternalBooleanProperty(String key) {
    try {
      Serializable ser = internalDoc.getProperty(key)
          .getValue();
      if (ser == null)
        return false;
      return (Boolean) ser;
    } catch (ClientException e) {
      LOGGER.error("Unable to retrieve boolean property '" + key
          + "', returns false", e);
      return false;
    }
  }

  private Calendar getInternalCalendarProperty(String key) {
    try {
      Serializable ser = internalDoc.getProperty(key)
          .getValue();
      if (ser != null)
        return (Calendar) ser;
    } catch (ClientException e) {
      LOGGER.error("Unable to retrieve calendar property '" + key
          + "', returns null", e);
    }
    return null;
  }

}
