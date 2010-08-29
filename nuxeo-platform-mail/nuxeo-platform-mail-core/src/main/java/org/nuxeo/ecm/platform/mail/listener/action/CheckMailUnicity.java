/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Vilogia - Mail dedupe
 *
 */

package org.nuxeo.ecm.platform.mail.listener.action;

import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.PARENT_PATH_KEY;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.query.sql.SQLQueryParser;
import org.nuxeo.ecm.platform.mail.action.ExecutionContext;
import org.nuxeo.ecm.platform.mail.utils.MailCoreConstants;

/**
 * This class checks whether a mail is a duplicate of a previously stored mail
 * document. The mail is considered a duplicate if the sender, sending_date,
 * title, text and containing folder are the same. This should fit for most
 * uses.
 *
 * @author <a href="mailto:christophe.capon@vilogia.fr">Christophe Capon</a>
 * @author <a href="mailto:ldoguin@nuxeo.com">Laurent Doguin</a>
 */
public class CheckMailUnicity extends AbstractMailAction {

    private static final Log log = LogFactory.getLog(CheckMailUnicity.class);

    public static final String MAIL_SEARCH_QUERY = "SELECT * FROM MailMessage "
            + "WHERE mail:messageId = %s AND ecm:path STARTSWITH %s AND ecm:isProxy = 0 ";

    @Override
    public boolean execute(ExecutionContext context) throws Exception {

        CoreSession session = getCoreSession(context);
        if (session == null) {
            log.error("Could not open CoreSession");
            return false;
        }

        ExecutionContext initialContext = context.getInitialContext();
        Path parentPath = new Path((String) initialContext.get(PARENT_PATH_KEY));

        // Get the fields used for dedupe
        String messageId = (String) context.get(MailCoreConstants.MESSAGE_ID_KEY);

        // Build the query
        // We intentionally include the deleted documents in the search
        // If they have been deleted we do not reinject them in the
        // mail folder
        StringBuilder query = new StringBuilder();
        query.append(String.format(MAIL_SEARCH_QUERY,
                SQLQueryParser.prepareStringLiteral(messageId),
                SQLQueryParser.prepareStringLiteral(parentPath.toString())));

        DocumentModelList duplicatedMail = session.query(query.toString());

        if (!duplicatedMail.isEmpty()) {
            // Current Mail is a duplicate
            return false;
        }
        return true;
    }

}
