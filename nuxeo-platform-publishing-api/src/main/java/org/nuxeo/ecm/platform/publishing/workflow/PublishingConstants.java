/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 * $Id: PublishingConstants.java 29075 2008-01-16 09:12:59Z jcarsique $
 */

package org.nuxeo.ecm.platform.publishing.workflow;


/**
 * Publishing related constants.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public final class PublishingConstants {

    // Constant utility class.
    private PublishingConstants() {
    }

    public static final String WORKFLOW_REVIEWERS = "workflowReviewers";

    public static final String WORKFLOW_TRANSITION_TO_PUBLISH = "publish";

    public static final String WORKFLOW_TRANSITION_TO_RIGHTS = "setupRights";

    public static final String WORKFLOW_TRANSITION_TO_REJECT = "reject";

    public static final String WORKFLOW_DEFINITION_NAME = "document_publishing";

    public static final String SUBMITTED_BY = "submitted_by";

}
