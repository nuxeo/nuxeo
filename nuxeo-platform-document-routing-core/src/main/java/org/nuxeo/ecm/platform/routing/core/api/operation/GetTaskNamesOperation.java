/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     guillaume
 */
package org.nuxeo.ecm.platform.routing.core.api.operation;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.ui.select2.common.Select2Common;

/**
 *
 *
 * @since 5.8
 */
@Operation(id = GetTaskNamesOperation.ID, category = Constants.CAT_WORKFLOW, label = "xxx", description = "xxxx")
public class GetTaskNamesOperation {

    public static final String ID = "Context.GetTaskNames";

    @Context
    protected OperationContext ctx;

    @Param(name = "lang", required = false)
    protected String lang;

    @Param(name = "searchTerm", required = false)
    protected String searchTerm;

    @OperationMethod
    public Blob run() {
        JSONArray result = new JSONArray();

        // XXX Fill the array with suggestion
        DocumentModelList list = new DocumentModelListImpl();

        for (DocumentModel doc : list) {
            String translatedLabel = "";// Translate your label
            if (translatedLabel.startsWith(searchTerm)) {
                JSONObject obj = new JSONObject();
                obj.element(Select2Common.ID, ""/* put the ref of the doc you want to submit */);
                obj.element(Select2Common.LABEL, translatedLabel);
                result.add(obj);
            }
        }

        return new StringBlob(result.toString(), "application/json");
    }

}
