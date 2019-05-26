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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.picture.web;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.helpers.EventNames;

/**
 * @author <a href="mailto:ldoguin@nuxeo.com">Laurent Doguin</a>
 * @deprecated since 6.0. See NXP-15370.
 */
@Name("slideShowManager")
@Scope(CONVERSATION)
@Deprecated
public class SlideShowManagerBean implements SlideShowManager, Serializable {

    private static final long serialVersionUID = -3281363416111697725L;

    private static final Log log = LogFactory.getLog(PictureBookManagerBean.class);

    @In(create = true)
    protected CoreSession documentManager;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    protected List<DocumentModel> children;

    protected Integer childrenSize;

    protected DocumentModel child;

    protected Integer index;

    protected Boolean stopped;

    protected Boolean repeat;

    @Create
    public void initialize() {
        log.debug("Initializing...");
        index = 1;
        childrenSize = null;
        stopped = false;
        repeat = false;
    }

    @Override
    public void firstPic() {
        index = 1;
    }

    @Override
    public void lastPic() {
        index = getChildren().size();
    }

    @Destroy
    public void destroy() {
        log.debug("Destroy");
        index = null;
        child = null;
        childrenSize = null;
        stopped = null;
        repeat = null;
    }

    @Override
    public Integer getIndex() {
        return index;
    }

    @Override
    public void decIndex() {
        index--;
    }

    @Override
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

    @Override
    public void setIndex(Integer idx) {
        index = idx;
    }

    @Override
    @Observer({ EventNames.DOCUMENT_SELECTION_CHANGED, EventNames.DOCUMENT_CHILDREN_CHANGED })
    @BypassInterceptors
    public void resetIndex() {
        index = 1;
        child = null;
        children = null;
        childrenSize = null;
        stopped = false;
        repeat = false;
    }

    @Override
    public void inputValidation(ActionEvent arg0) {
        if (getChildrenSize() < index) {
            index = getChildrenSize();
        }
        if (index <= 0) {
            index = 1;
        }
    }

    @Override
    public Integer getChildrenSize() {
        if (childrenSize == null) {
            childrenSize = getChildren().size();
        }
        return childrenSize;
    }

    @Override
    public DocumentModel getChild() {
        if (index > getChildrenSize()) {
            index = childrenSize;
        }
        if (!getChildren().isEmpty()) {
            return getChildren().get(index - 1);
        }
        return null;
    }

    protected List<DocumentModel> getChildren() {
        if (children == null) {
            DocumentModel currentDoc = navigationContext.getCurrentDocument();
            if (currentDoc != null) {
                children = documentManager.getChildren(currentDoc.getRef());
            } else {
                children = Collections.emptyList();
            }
        }
        return children;
    }

    @Override
    public void setChild(DocumentModel child) {
        this.child = child;
    }

    public void togglePause() {
        stopped = true;
    }

    public void stop() {
        index = 1;
        stopped = true;
    }

    public void start() {
        stopped = false;
    }

    public Boolean getStopped() {
        return stopped;
    }

    public void toggleRepeat() {
        repeat = !repeat;
    }

    public void setStopped(Boolean stopped) {
        this.stopped = stopped;
    }

    public Boolean getRepeat() {
        return repeat;
    }

    public void setRepeat(Boolean repeat) {
        this.repeat = repeat;
    }

}
