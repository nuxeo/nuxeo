/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * Encapsulates the page handling logic. Based on a document type, computes what page should be displayed to the user.
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
    private ApplicationControllerHelper() {
    }

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
