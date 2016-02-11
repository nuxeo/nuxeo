/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nelson Silva <nelson.silva@inevo.pt>
 */
package org.nuxeo.ecm.platform.spreadsheet;

import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewService;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewState;
import org.nuxeo.ecm.platform.contentview.json.JSONContentViewState;
import org.nuxeo.ecm.platform.forms.layout.io.Base64;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;

/**
 * Restful actions for Nuxeo Spreadsheet
 *
 * @since 6.0
 */
@Name("spreadsheetActions")
@Scope(EVENT)
public class SpreadsheetActions implements Serializable {

    @In(create = true)
    protected ContentViewService contentViewService;

    public String urlFor(ContentView contentView) throws UnsupportedEncodingException {
        String cv = "";

        // Set the content view state
        ContentViewState state = contentViewService.saveContentView(contentView);
        if (state != null) {
            String json = JSONContentViewState.toJSON(state, false);
            String encoded = Base64.encodeBytes(json.getBytes(), Base64.DONT_BREAK_LINES);
            cv = URLEncoder.encode(encoded, "UTF-8");
        }

        return VirtualHostHelper.getContextPathProperty() + "/spreadsheet/?cv=" + cv;
    }
}
