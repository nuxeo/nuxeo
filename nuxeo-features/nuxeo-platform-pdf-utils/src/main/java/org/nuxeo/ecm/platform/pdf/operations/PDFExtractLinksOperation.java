/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thibaud Arguillere
 *     Miguel Nixo
 */

package org.nuxeo.ecm.platform.pdf.operations;

import java.io.IOException;
import java.util.ArrayList;
import org.apache.commons.lang.StringUtils;
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
 * @since 8.4
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
            ArrayList<LinkInfo> links = new ArrayList<LinkInfo>();
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
