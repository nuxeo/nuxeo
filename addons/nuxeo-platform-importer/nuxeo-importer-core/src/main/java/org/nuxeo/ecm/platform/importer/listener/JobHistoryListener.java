/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.importer.listener;

import org.nuxeo.ecm.platform.audit.api.job.JobHistoryHelper;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class JobHistoryListener implements ImporterListener {

    protected final JobHistoryHelper jobHelper;

    public JobHistoryListener(String jobName) {
        jobHelper = new JobHistoryHelper(jobName);
    }

    @Override
    public void beforeImport() {
        jobHelper.logJobStarted();
    }

    @Override
    public void afterImport() {
        jobHelper.logJobEnded();
    }

    @Override
    public void importError() {
        jobHelper.logJobFailed("Error during import");
    }

}
