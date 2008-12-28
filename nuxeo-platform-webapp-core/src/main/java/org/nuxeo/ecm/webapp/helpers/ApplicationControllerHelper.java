/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.helpers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeView;

/**
 * Encapsulates the page handling logic. Based on a document type, computes what
 * page should be displayed to the user.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 * @deprecated
 */
@Deprecated
public class ApplicationControllerHelper {

    protected static final String DEFAULT_VIEW = "view_documents";
    protected static final String CREATE_VIEW = "create_document";
    protected static final String EDIT_VIEW = "edit_document";

    private static final Log log = LogFactory.getLog(ApplicationControllerHelper.class);

    // Utility class.
    private ApplicationControllerHelper() {}

    public static String getPageOnSelectedDocumentType(Type docType) {
        String returnPage = null;
        if (docType != null) {
            String view = docType.getDefaultView();
            if (view != null) {
                returnPage = view;
            } else {
                returnPage = DEFAULT_VIEW;
            }
        }
        log.debug("Return page -> " + returnPage);
        return returnPage;
    }

    // Not used.
    public static String getPageOnEditDocumentType(Type docType) {
        String returnPage = null;
        if (docType != null) {
            String view = docType.getEditView();
            if (view != null) {
                returnPage = view;
            } else {
                returnPage = EDIT_VIEW;
            }
        }
        log.debug("Return page -> " + returnPage);
        return returnPage;
    }

    public static String getPageOnEditedDocumentType(Type docType) {
        String returnPage = null;
        if (docType != null) {
            TypeView view = docType.getView("after-edit");
            if (view == null) {
                returnPage = getPageOnSelectedDocumentType(docType);
            } else {
                returnPage = view.getValue();
            }
        }
        log.debug("Return page -> " + returnPage);
        return returnPage;
    }

    public static String getPageOnCreateDocumentType(Type docType) {
        String returnPage = null;
        if (docType != null) {
            String view = docType.getCreateView();
            if (view != null) {
                returnPage = view;
            } else {
                returnPage = CREATE_VIEW;
            }
        }
        log.debug("Return page -> " + returnPage);
        return returnPage;
    }

    public static String getPageOnCreatedDocumentType(Type docType) {
        String returnPage = null;
        if (docType != null) {
            TypeView view = docType.getView("after-create");
            if (view == null) {
                returnPage = getPageOnSelectedDocumentType(docType);
            } else {
                returnPage = view.getValue();
            }
        }
        log.debug("Return page -> " + returnPage);
        return returnPage;
    }

}
