/*
 * (C) Copyright 2002 - 2006 Nuxeo SARL <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 *
 * $Id: Plugin.java 4449 2006-10-19 11:51:56Z janguenot $
 */

package org.nuxeo.ecm.platform.filemanager.service.extension;

/**
 * FileManagerServiceCommon plugin default interface.
 * <p>
 * Responsible for converting given sources to a given type of Document using
 * default.
 *
 * @author <a href="mailto:andreas.kalogeropoulos@nuxeo.com">Andreas
 *         Kalogeropoulos</a>
 *
 * @deprecated use {@link FileImporter} instead
 */
@Deprecated
// TODO: remove (not used)
public interface Plugin extends FileImporter {

}
