/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nelson Silva <nelson.silva@inevo.pt>
 */
package org.nuxeo.ecm.platform.spreadsheet;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.jboss.seam.ScopeType.EVENT;

import java.io.IOException;
import java.io.Serializable;
import java.net.URLEncoder;
import java.util.Base64;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewService;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewState;
import org.nuxeo.ecm.platform.contentview.json.JSONContentViewState;
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

    public String urlFor(ContentView contentView) throws IOException {
        String cv = "";

        // Set the content view state
        ContentViewState state = contentViewService.saveContentView(contentView);
        if (state != null) {
            String json = JSONContentViewState.toJSON(state, false);
            String encoded = Base64.getEncoder().encodeToString(json.getBytes(UTF_8));
            cv = URLEncoder.encode(encoded, "UTF-8");
        }

        return VirtualHostHelper.getContextPathProperty() + "/spreadsheet/?cv=" + cv;
    }
}
