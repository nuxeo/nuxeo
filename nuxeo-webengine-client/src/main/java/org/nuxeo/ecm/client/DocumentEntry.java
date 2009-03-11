/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.client;

import java.util.Map;



/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface DocumentEntry extends Entry {

    // client specific properties
    Repository getRepository(); // used by action methods: repo.getNavigationService().getChildren(getId());
    String getId(); // client constructed path or null if unfilled
    boolean isPhantom(); // not yet persisted TODO rename in isTransient() ?
    boolean isDirty(); // modified
    boolean isLocked(); //locked


    String getName(); //cmis:name
    //Type getType();
    String getTypeName(); // link type
    String getParentId();
    DocumentEntry getParent();
    DocumentEntry getChild(String name); // throws Exception if not a folder?


    Object getProperty(String key); // cmis properties
    Map<String, Object> getProperties();

    DocumentList getChildren(); // list entry content
    DocumentList getDescendants();
    DocumentList getParentFolders();
    DocumentList getObjectParents();

    <T> T getTypeAdapter();
    //<T extends MutableDocument> edit() TODO ?

    DocumentEntry newDocument(String type, String name);

    void setProperty(String key, Object value);

    // nuxeo extensions
    void removeContent(String content); //nuxeo specific

    Content[] getContents(); // nuxeo specific

    String getState();//TODO cmis token for state? move this in a property?
    String[] getFacets(); // set of facets of this document
    boolean hasFacet(String facet);

}
