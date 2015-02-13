/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.rendition;

import org.nuxeo.ecm.core.api.ClientException;

/**
 * Exception thrown by the {@link org.nuxeo.ecm.platform.rendition.service.RenditionService}
 * if any error occurred.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.4.1
 */
public class RenditionException extends ClientException {

    private static final long serialVersionUID = 1L;

    protected String messageLabel;

    public RenditionException(String message, String messageLabel, Exception e) {
        super(message, e);
        this.messageLabel = messageLabel;
    }

    public RenditionException(String message, Exception e) {
        super(message, e);
    }

    public RenditionException(String message, String messageLabel) {
        super(message);
        this.messageLabel = messageLabel;
    }

    public RenditionException(String message) {
        super(message);
    }

    @Override
    public String getLocalizedMessage() {
        return messageLabel != null ? messageLabel : getMessage();
    }

}
