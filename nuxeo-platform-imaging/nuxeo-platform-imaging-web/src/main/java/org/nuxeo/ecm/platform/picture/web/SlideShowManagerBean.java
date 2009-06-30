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

import java.io.Serializable;

import javax.ejb.Remove;
import javax.faces.event.ActionEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.helpers.EventNames;

/**
 * @author <a href="mailto:ldoguin@nuxeo.com">Laurent Doguin</a>
 *
 */

@Name("slideShowManager")
@Scope(CONVERSATION)
public class SlideShowManagerBean implements
        SlideShowManager, Serializable {

    private static final long serialVersionUID = -3281363416111697725L;

    private static final Log log = LogFactory.getLog(PictureBookManagerBean.class);

    Integer index;

    Integer childrenSize=null;

    Boolean stoped;

    Boolean repeat;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    DocumentModel child;

    @Create
    public void initialize() throws Exception {
        log.debug("Initializing...");
        index = 1;
        childrenSize = null;
        stoped = false;
        repeat = false;
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
        stoped = null;
        repeat = null;
    }

    public Integer getIndex() {
        return index;
    }

    public void decIndex() {
        index--;
    }

    public void incIndex() {
        index++;
        if ((index) > getChildrenSize()) {
            if (repeat) {
                index = 1;
            } else {
                index = childrenSize;
            }
        }
    }

    public void setIndex(Integer idx) {
        index = idx;
    }

    @Observer({ EventNames.DOCUMENT_SELECTION_CHANGED })
    @BypassInterceptors
    public void resetIndex() throws ClientException {
        index = 1;
        child = null;
        childrenSize = null;
        stoped = false;
        repeat = false;
    }

    public void inputValidation(ActionEvent arg0) {
        if (getChildrenSize() < index) {
            index = getChildrenSize();
        }
        if (index <= 0) {
             index = 1;
        }
    }

    public Integer getChildrenSize() {
        if (childrenSize==null) {
            try {
                childrenSize = navigationContext.getCurrentDocumentChildren().size();
            } catch (ClientException e) {
                log.error("Error while calculating size of picturebook", e);
                childrenSize=0;
            }
        }
        return childrenSize;
    }

    public void setChildrenSize(Integer childrenSize) {
        this.childrenSize = childrenSize;
    }

    public DocumentModel getChild() {
        try {
            if ((index) > getChildrenSize()) {
                    index = childrenSize;
            }
            return navigationContext.getCurrentDocumentChildren().get(index - 1);
        } catch (ClientException e) {
            log.error("Can't catch Child Document Model ", e);
            return null;
        }
    }

    public void setChild(DocumentModel child) {
        this.child = child;
    }

    public void togglePause() {
        stoped = true;
    }

    public void stop() {
        index = 1;
        stoped = true;
    }

    public void start(){
            stoped = false;
    }

    public Boolean getStoped() {
        return stoped;
    }

    public void toggleRepeat(){
        if (repeat) {
            repeat = false;
        } else {
            repeat = true;
        }
    }

    public void setStoped(Boolean stoped) {
        this.stoped = stoped;
    }

    public Boolean getRepeat() {
        return repeat;
    }

    public void setRepeat(Boolean repeat) {
        this.repeat = repeat;
    }

}
