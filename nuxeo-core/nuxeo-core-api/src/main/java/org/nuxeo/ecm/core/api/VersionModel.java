/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api;

import java.io.Serializable;
import java.util.Calendar;

/*
 * TODO VersionModel should be replaced with a DocumentVersionModel that extends
 * DocumentModel
 */
/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface VersionModel extends Serializable {

    String getId();

    void setId(String id);

    Calendar getCreated();

    void setCreated(Calendar created);

    String getDescription();

    void setDescription(String description);

    String getLabel();

    void setLabel(String label);

}
