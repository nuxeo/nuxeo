/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.content.template.service;

import org.nuxeo.ecm.core.api.CoreSession;

/**
 * Handler called after the content creation done by the
 * {@link ContentTemplateService}.
 * <p>
 * The registered handlers are always called when the server starts even if no
 * content creation is done.
 * <p>
 * Useful for packages deployed on an existing Nuxeo that need a default
 * documents structure.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public interface PostContentCreationHandler {

    /**
     * Executes this handler with a system {@code session}.
     */
    void execute(CoreSession session);

}
