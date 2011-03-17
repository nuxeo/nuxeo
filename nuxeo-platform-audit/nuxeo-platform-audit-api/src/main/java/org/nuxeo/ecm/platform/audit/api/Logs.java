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
 * $Id: LogEntry.java 1362 2006-07-26 14:26:03Z sfermigier $
 */

package org.nuxeo.ecm.platform.audit.api;


/**
 * Logs interface.
 * <p>
 * {@see http://jira.nuxeo.org/browse/NXP-514}
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface Logs extends AuditReader, AuditLogger, AuditAdmin {

}
