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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.directory;

import java.util.List;

/**
 * Base interface for references between directory fields.
 * <p>
 * References are used to leverage SQL joins or attributes that store a list of
 * distinguished names in LDAP servers (e.g. uniqueMember).
 * <p>
 * In nuxeo directories, references are special entry fields that are string
 * list of entry ids of a target directory.
 *
 * @author ogrisel
 */
public interface Reference {

    String getFieldName();

    Directory getSourceDirectory() throws DirectoryException;

    void setSourceDirectoryName(String sourceDirectoryName);

    Directory getTargetDirectory() throws DirectoryException;

    void setTargetDirectoryName(String targetDirectoryName);

    void addLinks(String sourceId, List<String> targetIds)
            throws DirectoryException;

    void addLinks(List<String> sourceIds, String targetId)
            throws DirectoryException;

    void removeLinksForSource(String sourceId) throws DirectoryException;

    void removeLinksForTarget(String targetId) throws DirectoryException;

    List<String> getTargetIdsForSource(String sourceId)
            throws DirectoryException;

    List<String> getSourceIdsForTarget(String targetId)
            throws DirectoryException;

    void setTargetIdsForSource(String sourceId, List<String> targetIds)
            throws DirectoryException;

    void setSourceIdsForTarget(String targetId, List<String> sourceIds)
            throws DirectoryException;

}
