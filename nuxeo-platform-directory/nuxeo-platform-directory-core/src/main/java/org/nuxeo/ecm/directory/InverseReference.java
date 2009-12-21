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

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Reference that uses the matching reference of the target directory to
 * actually do the job.
 *
 * @author ogrisel
 */
@XObject(value = "inverseReference")
public class InverseReference extends AbstractReference {

    @XNode("@dualReferenceField")
    protected String dualReferenceName;

    protected Reference dualReference;

    @XNode("@field")
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    @XNode("@directory")
    public void setTargetDirectoryName(String targetDirectoryName) {
        this.targetDirectoryName = targetDirectoryName;
    }

    protected void checkDualReference() throws DirectoryException {
        if (dualReference == null) {
            dualReference = getTargetDirectory().getReference(dualReferenceName);
        }
        if (dualReference == null) {
            throw new DirectoryException("could not find reference "
                    + dualReferenceName);
        }
        if (dualReference instanceof InverseReference) {
            throw new DirectoryException(String.format(
                    "InverseReference %s cannot refere to InverseReference %s",
                    getFieldName(), dualReferenceName));
        }
    }

    public void addLinks(String sourceId, List<String> targetIds)
            throws DirectoryException {
        checkDualReference();
        dualReference.addLinks(targetIds, sourceId);
    }

    public void addLinks(List<String> sourceIds, String targetId)
            throws DirectoryException {
        checkDualReference();
        dualReference.addLinks(targetId, sourceIds);
    }

    public void removeLinksForTarget(String targetId) throws DirectoryException {
        checkDualReference();
        dualReference.removeLinksForSource(targetId);
    }

    public void removeLinksForSource(String sourceId) throws DirectoryException {
        checkDualReference();
        dualReference.removeLinksForTarget(sourceId);
    }

    public List<String> getSourceIdsForTarget(String targetId)
            throws DirectoryException {
        checkDualReference();
        return dualReference.getTargetIdsForSource(targetId);
    }

    public List<String> getTargetIdsForSource(String sourceId)
            throws DirectoryException {
        checkDualReference();
        return dualReference.getSourceIdsForTarget(sourceId);
    }

    public void setTargetIdsForSource(String sourceId, List<String> targetIds)
            throws DirectoryException {
        checkDualReference();
        dualReference.setSourceIdsForTarget(sourceId, targetIds);
    }

    public void setSourceIdsForTarget(String targetId, List<String> sourceIds)
            throws DirectoryException {
        checkDualReference();
        dualReference.setTargetIdsForSource(targetId, sourceIds);
    }

}
