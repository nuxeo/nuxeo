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
 * $Id: ChainSelectActionsBean.java 28950 2008-01-11 13:35:06Z tdelprat $
 */

package org.nuxeo.ecm.webapp.directory;

import static org.jboss.seam.ScopeType.SESSION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.platform.ui.web.directory.ChainSelect;
import org.nuxeo.ecm.platform.ui.web.directory.ChainSelectStatus;
import org.nuxeo.ecm.platform.ui.web.directory.Selection;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;

/**
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
@Name("chainSelectActions")
@Scope(SESSION)
public class ChainSelectActionsBean implements ChainSelectActions, Serializable {

    private static final long serialVersionUID = 27502317512904295L;

    private static final Log log = LogFactory.getLog(ChainSelectActionsBean.class);

    private ChainSelect getChainSelect(ActionEvent event) {
        UIComponent component = event.getComponent();
        while (!(component instanceof ChainSelect)) {
            component = component.getParent();
        }
        return (ChainSelect) component;
    }

    @Override
    public void add(ActionEvent event) {
        ChainSelect chainSelect = getChainSelect(event);
        FacesContext context = FacesContext.getCurrentInstance();
        boolean allowBranchSelection = chainSelect.getBooleanProperty("allowBranchSelection", false);
        boolean allowRootSelection = chainSelect.getBooleanProperty("allowRootSelection", false);
        int size = chainSelect.getSize();
        String clientId = chainSelect.getClientId(context);

        LinkedHashMap<String, Selection> map = new LinkedHashMap<>();
        for (Selection selection : chainSelect.getComponentValue()) {
            map.put(selection.getValue(chainSelect.getKeySeparator()), selection);
        }
        for (Selection selection : chainSelect.getSelections()) {
            int selectionSize = selection.getSize();
            if (!allowRootSelection && selectionSize == 0) {
                String messageStr = ComponentUtils.translate(context, "label.chainSelect.empty_selection");
                FacesMessage message = new FacesMessage(messageStr);
                context.addMessage(clientId, message);
                chainSelect.setValid(false);
                return;
            }
            if (!allowBranchSelection && selectionSize > 0 && selectionSize != size) {
                String messageStr = ComponentUtils.translate(context, "label.chainSelect.incomplete_selection");
                FacesMessage message = new FacesMessage(messageStr);
                context.addMessage(clientId, message);
                chainSelect.setValid(false);
                return;
            }

            map.put(selection.getValue(chainSelect.getKeySeparator()), selection);
        }

        Selection[] componentValue = map.values().toArray(new Selection[0]);

        String[] submittedValue;
        if (componentValue.length == 0) {
            submittedValue = null;
        } else {
            submittedValue = new String[componentValue.length];
            for (int i = 0; i < componentValue.length; i++) {
                submittedValue[i] = componentValue[i].getValue(chainSelect.getKeySeparator());
            }
        }

        chainSelect.setComponentValue(componentValue);
        chainSelect.setSubmittedValue(submittedValue);
        context.renderResponse();
        log.debug("add: submittedValue=" + ChainSelect.format(submittedValue));
    }

    @Override
    public void delete(ActionEvent event) {
        FacesContext context = FacesContext.getCurrentInstance();
        ChainSelect chainSelect = getChainSelect(event);
        List<Selection> componentValueList = new ArrayList<>();
        componentValueList.addAll(Arrays.asList(chainSelect.getComponentValue()));

        String value = context.getExternalContext().getRequestParameterMap().get(ChainSelectStatus.REMOVE_ID);

        for (Iterator<Selection> i = componentValueList.iterator(); i.hasNext();) {
            Selection selection = i.next();
            if (selection.getValue(chainSelect.getKeySeparator()).equals(value)) {
                i.remove();
            }
        }
        Selection[] componentValue = componentValueList.toArray(new Selection[0]);
        String[] submittedValue = null;
        if (componentValue.length != 0) {
            submittedValue = new String[componentValue.length];
            for (int i = 0; i < componentValue.length; i++) {
                submittedValue[i] = componentValue[i].getValue(chainSelect.getKeySeparator());
            }
        }

        chainSelect.setComponentValue(componentValue);
        chainSelect.setSubmittedValue(submittedValue);
        context.renderResponse();
    }
}
