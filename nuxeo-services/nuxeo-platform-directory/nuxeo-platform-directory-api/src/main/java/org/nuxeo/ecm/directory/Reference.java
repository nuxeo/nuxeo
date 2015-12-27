/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * References are used to leverage SQL joins or attributes that store a list of distinguished names in LDAP servers
 * (e.g. uniqueMember).
 * <p>
 * In nuxeo directories, references are special entry fields that are string list of entry ids of a target directory.
 *
 * @author ogrisel
 */
public interface Reference {

    String getFieldName();

    Directory getSourceDirectory() throws DirectoryException;

    void setSourceDirectoryName(String sourceDirectoryName);

    Directory getTargetDirectory() throws DirectoryException;

    void setTargetDirectoryName(String targetDirectoryName);

    void addLinks(String sourceId, List<String> targetIds) throws DirectoryException;

    void addLinks(List<String> sourceIds, String targetId) throws DirectoryException;

    void removeLinksForSource(String sourceId) throws DirectoryException;

    void removeLinksForTarget(String targetId) throws DirectoryException;

    List<String> getTargetIdsForSource(String sourceId) throws DirectoryException;

    List<String> getSourceIdsForTarget(String targetId) throws DirectoryException;

    void setTargetIdsForSource(String sourceId, List<String> targetIds) throws DirectoryException;

    void setSourceIdsForTarget(String targetId, List<String> sourceIds) throws DirectoryException;

    /**
     * Returns a clone, added for hot reload support.
     *
     * @since 5.6
     */
    Reference clone();
}
