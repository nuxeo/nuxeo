/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.importer.threading;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

/**
 * Default multi-threading policy (creates a new thread on each branch until the
 * max nb thread is reached)
 *
 * @author Thierry Delprat
 *
 */
public class DefaultMultiThreadingPolicy implements ImporterThreadingPolicy {

    public boolean needToCreateThreadAfterNewFolderishNode(
            DocumentModel parent, SourceNode node, long uploadedSources,
            int batchSize, int scheduledTasks) {

        if (uploadedSources < (batchSize / 3)) {
            return false;
        }

        if (scheduledTasks >= 5) {
            return false;
        } else {
            return true;
        }
    }

}
