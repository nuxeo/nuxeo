/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Dragos Mihalache
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.versioning.service;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.platform.versioning.api.VersionIncEditOptions;
import org.nuxeo.ecm.platform.versioning.api.VersioningActions;
import org.nuxeo.ecm.platform.versioning.api.VersioningManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Versions management component implementation.
 */
public class VersioningManagerImpl extends DefaultComponent implements VersioningManager {

    public static final String COMPONENT_ID = "org.nuxeo.ecm.platform.versioning.VersioningManager";

    @Override
    public VersionIncEditOptions getVersionIncEditOptions(DocumentModel doc) {
        VersionIncEditOptions options = new VersionIncEditOptions();
        VersioningService service = Framework.getService(VersioningService.class);
        for (VersioningOption option : service.getSaveOptions(doc)) {
            VersioningActions action;
            switch (option) {
            case MINOR:
                action = VersioningActions.ACTION_INCREMENT_MINOR;
                break;
            case MAJOR:
                action = VersioningActions.ACTION_INCREMENT_MAJOR;
                break;
            default:
                action = VersioningActions.ACTION_NO_INCREMENT;
            }
            if (option == service.getSaveOptions(doc).get(0)) {
                options.setDefaultVersioningAction(action);
            }
            options.addOption(action);
        }

        return options;
    }

    @Override
    public String getVersionLabel(DocumentModel doc) {
        return doc.getVersionLabel();
    }

}
