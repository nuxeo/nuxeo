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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.picture.web;

import static org.jboss.seam.ScopeType.CONVERSATION;

import javax.ejb.Remove;
import javax.faces.event.ActionEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.helpers.EventNames;

/**
 * @author <a href="mailto:ldoguin@nuxeo.com">Laurent Doguin</a>
 *
 */

@Name("slideShowManager")
@Scope(CONVERSATION)
public class SlideShowManagerBean extends InputController implements
        SlideShowManager {

    private static final Log log = LogFactory.getLog(PictureBookManagerBean.class);

    Integer index;

    Integer childrenSize;

    DocumentModel child;

    @Create
    public void initialize() throws Exception {
        log.debug("Initializing...");
        index = 1;
        childrenSize = navigationContext.getCurrentDocumentChildren().size();
    }

    public void firstPic() {
        index = 1;
    }

    public void lastPic() {
        try {
            index = navigationContext.getCurrentDocumentChildren().size();
        } catch (ClientException e) {
            log.error("Catching DocumentChildren size failed", e);
            index = 1;
        }
    }

    @Destroy
    @Remove
    public void destroy() {
        log.debug("Destroy");
        index = null;
        child = null;
        childrenSize = null;
    }

    public Integer getIndex() {
        return index;
    }

    public void decIndex() {
        index--;
    }

    public void incIndex() {
        index++;
    }

    public void setIndex(Integer idx) {
        index = idx;
    }

    @Observer({ EventNames.DOCUMENT_SELECTION_CHANGED })
    public void resetIndex() throws ClientException {
        index = 1;
        child = null;
        childrenSize = navigationContext.getCurrentDocumentChildren().size();
    }

    public void inputValidation(ActionEvent arg0) {
        if (childrenSize < index) {
            index = childrenSize;
        }
        if (index <= 0) {
             index = 1;
        }
    }

    public Integer getChildrenSize() {
        return childrenSize;
    }

    public void setChildrenSize(Integer childrenSize) {
        this.childrenSize = childrenSize;
    }

    public DocumentModel getChild() {
        try {
            return navigationContext.getCurrentDocumentChildren().get(index - 1);
        } catch (ClientException e) {
            log.error("Can't catch Child Document Model ", e);
            return null;
        }
    }

    public void setChild(DocumentModel child) {
        this.child = child;
    }

}
