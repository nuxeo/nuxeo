/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
package org.nuxeo.ecm.user.center.profile;

import org.nuxeo.common.annotation.Experimental;
import org.nuxeo.ecm.core.work.AbstractWork;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 7.2
 */
@Experimental(comment="https://jira.nuxeo.com/browse/NXP-12200")
public class UserProfileImporterWork extends AbstractWork {

    private static final long serialVersionUID = 1L;

    protected transient UserProfileImporter importer;

    @Override
    public String getTitle() {
        return "Userprofile Importer";
    }

    @Override
    public Progress getProgress() {
        if (importer != null && importer.totalRecords > 0) {
            return new Progress(importer.currentRecord, importer.totalRecords);
        }
        return super.getProgress();
    }

    @Override
    public void work() {
        openSystemSession();
        importer = new UserProfileImporter();
        importer.doImport(session);
    }

}
