/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.ui.web.component.tree;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.event.ActionEvent;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.ui.web.component.list.UIEditableList;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;

/**
 * Action to handle tree widget.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.4
 */
@Name("treeWidgetActions")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class TreeWidgetActions implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @RequestParameter
    protected String selectionListId;

    @RequestParameter
    protected String selectionInputId;

    @RequestParameter
    protected String selectedPath;

    @SuppressWarnings("unchecked")
    public void addSelectionToList(ActionEvent event) {
        UIComponent component = event.getComponent();
        if (component == null) {
            return;
        }
        UIComponent base = ComponentUtils.getBase(component);
        UIEditableList list = ComponentUtils.getComponent(base, selectionListId, UIEditableList.class);

        if (list != null) {
            List<String> values = (List<String>) list.getEditableModel().getWrappedData();
            // add selected value to the list
            if (!values.contains(selectedPath)) {
                list.addValue(selectedPath);
            }
        }
    }

    public void setUIInputValue(ActionEvent event) {
        UIComponent component = event.getComponent();
        if (component == null) {
            return;
        }
        UIComponent base = ComponentUtils.getBase(component);
        UIInput uiInput = ComponentUtils.getComponent(base, selectionInputId, UIInput.class);

        if (uiInput != null) {
            uiInput.setSubmittedValue(selectedPath);
        }
    }

    /**
     * Returns the {@code DocumentModel} referenced by the given path if exists, {@code null} otherwise.
     */
    public DocumentModel getDocumentFromPath(String path) {
        // handle root document differently as user may not have browse rights
        // on it
        if ("/".equals(path)) {
            return documentManager.getRootDocument();
        }
        DocumentRef ref = new PathRef(path);
        return documentManager.exists(ref) ? documentManager.getDocument(new PathRef(path)) : null;
    }

}
