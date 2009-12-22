/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.webapp.search;

import java.util.List;
import java.util.Map;

import javax.annotation.security.PermitAll;
import javax.ejb.Local;
import javax.ejb.Remove;

import org.jboss.seam.annotations.Destroy;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.types.FieldWidget;
import org.nuxeo.ecm.platform.ui.web.directory.VocabularyEntryList;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
@Local
public interface SearchColumns {

    /**
     * Declaration for [Seam]Create method.
     */
    void init();

    @Remove
    @Destroy
    @PermitAll
    void destroy();

    /**
     * The list of schemas to be displayed.
     */
    List<FieldWidget> getResultColumns();

    List<FieldWidget> getDefaultResultColumns();

    void setResultColumns(List<FieldWidget> resultList);

    VocabularyEntryList getFieldGroupEntries();

    VocabularyEntryList getFieldEntries();

    String addField();

    String removeField();

    String getNewField();

    void setNewField(String newField);

    String reset();

    String swapColumns() throws ClientException;

    String getFieldRef1();

    void setFieldRef1(String fieldRef1);

    String getFieldRef2();

    void setFieldRef2(String fieldRef2);

    Map<String, FieldWidget> getFieldMap();

    boolean getSortAscending();

    void setSortAscending(boolean sortAscending);

    String getSortColumn();

    void setSortColumn(String sortColumn);

    void setDefaultResultColumnList(String[] resultColumnList);

}
