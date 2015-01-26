/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo
 */

package org.nuxeo.ecm.automation.core.util;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.automation.core.util.Paginable;

/**
 * Adds page support on {@link RecordSet}
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.7
 */
public interface PaginableRecordSet extends Paginable<Map<String, Serializable>>, RecordSet {

}
