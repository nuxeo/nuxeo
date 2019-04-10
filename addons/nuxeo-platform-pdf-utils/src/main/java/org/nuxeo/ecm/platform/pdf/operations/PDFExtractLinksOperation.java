/*
 * (C) Copyright 2016-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thibaud Arguillere
 *     Miguel Nixo
 */
package org.nuxeo.ecm.platform.pdf.operations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.pdf.LinkInfo;
import org.nuxeo.ecm.platform.pdf.PDFLinks;

/**
 * Returns a JSON string of an array of objects with page, subType, text and link fields.
 * <p>
 * If <code>getAll</code> is <code>false</code>, then <code>type</code> is required.
 *
 * @since 8.10
 */
@Operation(id = PDFExtractLinksOperation.ID, category = Constants.CAT_CONVERSION, label = "PDF: Extract Links",
    description = "Returns a JSON string of an array of objects with page, subType, text and link fields. If getAll" +
        " is true, returns all the links (Remote Go To, Launch and URI in the current version).")
public class PDFExtractLinksOperation {

    public static final String ID = "PDF.ExtractLinks";

    @Param(name = "type", required = false, widget = Constants.W_OPTION, values = { "Launch", "Remote Go To", "URI" })
    protected String type;

    @Param(name = "getAll", required = false)
    protected boolean getAll = false;

    @OperationMethod
    public String run(Blob inBlob) throws IOException, JSONException {
        ArrayList<String> types = new ArrayList<>();
        if (getAll) {
            types.add("Launch");
            types.add("Remote Go To");
            types.add("URI");
        } else {
            if (StringUtils.isBlank(type)) {
                throw new IllegalArgumentException("type cannot be empty if getAll is false");
            }
            types.add(type);
        }
        PDFLinks pdfl = new PDFLinks(inBlob);
        JSONArray array = new JSONArray();
        for (String theType : types) {
            List<LinkInfo> links = new ArrayList<>();
            switch (theType.toLowerCase()) {
            case "remote go to":
                links = pdfl.getRemoteGoToLinks();
                break;
            case "launch":
                links = pdfl.getLaunchLinks();
                break;
            case "uri":
                links = pdfl.getURILinks();
                break;
            }
            for (LinkInfo li : links) {
                JSONObject object = new JSONObject();
                object.put("page", li.getPage());
                object.put("subType", li.getSubType());
                object.put("text", li.getText());
                object.put("link", li.getLink());
                array.put(object);
            }
        }
        pdfl.close();
        return array.toString();
    }

}
