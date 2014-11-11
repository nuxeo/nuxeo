/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.ui.web.component.tree;

import java.io.Serializable;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.event.ActionEvent;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.ui.web.component.list.UIEditableList;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

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
    protected String selectedPath;

    @SuppressWarnings("unchecked")
    public void addSelectionToList(ActionEvent event) {
        UIComponent component = event.getComponent();
        if (component == null) {
            return;
        }
        UIComponent base = ComponentUtils.getBase(component);
        UIEditableList list = ComponentUtils.getComponent(base,
                selectionListId, UIEditableList.class);

        if (list != null) {
            List<String> values = (List<String>) list.getEditableModel().getWrappedData();
            // add selected value to the list
            if (!values.contains(selectedPath)) {
                list.addValue(selectedPath);
            }
        }
    }

    /**
     * Returns the {@code DocumentModel} referenced by the given path if exists,
     * {@code null} otherwise.
     */
    public DocumentModel getDocumentFromPath(String path)
            throws ClientException {
        DocumentRef ref = new PathRef(path);
        return documentManager.exists(ref) ? documentManager.getDocument(new PathRef(
                path))
                : null;
    }

}
