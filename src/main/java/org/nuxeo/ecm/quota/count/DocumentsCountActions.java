/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.quota.count;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.*;

import java.io.Serializable;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.quota.QuotaStatsService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
@Name("documentsCountActions")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class DocumentsCountActions implements Serializable {

    private static final long serialVersionUID = -1L;

    @In(create = true)
    protected transient CoreSession documentManager;

    public void computeInitialDocumentsCountStatistics() {
        QuotaStatsService quotaStatsService = Framework.getLocalService(QuotaStatsService.class);
        quotaStatsService.computeInitialStatistics(documentManager.getRepositoryName());
    }

}
