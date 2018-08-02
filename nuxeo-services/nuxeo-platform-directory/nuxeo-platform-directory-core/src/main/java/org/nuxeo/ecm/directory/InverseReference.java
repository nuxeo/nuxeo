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
 * Reference that uses the matching reference of the target directory to actually do the job.
 *
 * @author ogrisel
 */
public class InverseReference extends AbstractReference {

    /**
     * Indicates if the target directory can be updated from the current reference
     *
     * @since 5.7
     */
    protected boolean readOnly = false;

    protected String dualReferenceName;

    protected Reference dualReference;

    public InverseReference(InverseReferenceDescriptor referenceDescriptor) {
        super(referenceDescriptor.getFieldName(), referenceDescriptor.getDirectory());
        dualReferenceName = referenceDescriptor.getDualReferenceName();
        readOnly = referenceDescriptor.isReadOnly();
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    protected void checkDualReference() {
        if (dualReference == null) {
            List<Reference> references = getTargetDirectory().getReferences(dualReferenceName);
            if (references.size() == 0) {
                dualReference = null;
            } else if (references.size() == 1) {
                dualReference = references.get(0);
            } else {
                for (Reference ref : references) {
                    if (ref instanceof InverseReference) {
                        continue;
                    } else if (sourceDirectoryName.equals(ref.getTargetDirectory().getName())
                            && targetDirectoryName.equals(ref.getSourceDirectory().getName())) {
                        if (dualReference == null) {
                            dualReference = ref;
                        } else {
                            throw new DirectoryException(
                                    "More than one reference: could not find reference " + dualReferenceName);
                        }
                    }
                }
            }
        }
        if (dualReference == null) {
            throw new DirectoryException("could not find reference " + dualReferenceName);
        }
        if (dualReference instanceof InverseReference) {
            throw new DirectoryException(String.format("InverseReference %s cannot refer to InverseReference %s",
                    getFieldName(), dualReferenceName));
        }
    }

    @Override
    public void addLinks(String sourceId, List<String> targetIds) {
        if (readOnly) {
            return;
        }
        checkDualReference();
        dualReference.addLinks(targetIds, sourceId);
    }

    @Override
    public void addLinks(List<String> sourceIds, String targetId) {
        if (readOnly) {
            return;
        }
        checkDualReference();
        dualReference.addLinks(targetId, sourceIds);
    }

    @Override
    public void removeLinksForTarget(String targetId) {
        if (readOnly) {
            return;
        }
        checkDualReference();
        dualReference.removeLinksForSource(targetId);
    }

    @Override
    public void removeLinksForTarget(String targetId, Session session) {
        if (readOnly) {
            return;
        }
        checkDualReference();
        dualReference.removeLinksForSource(targetId, session);
    }

    @Override
    public void removeLinksForSource(String sourceId) {
        if (readOnly) {
            return;
        }
        checkDualReference();
        dualReference.removeLinksForTarget(sourceId);
    }

    @Override
    public void removeLinksForSource(String sourceId, Session session) {
        if (readOnly) {
            return;
        }
        checkDualReference();
        dualReference.removeLinksForTarget(sourceId, session);
    }

    @Override
    public List<String> getSourceIdsForTarget(String targetId) {
        checkDualReference();
        return dualReference.getTargetIdsForSource(targetId);
    }

    @Override
    public List<String> getTargetIdsForSource(String sourceId) {
        checkDualReference();
        return dualReference.getSourceIdsForTarget(sourceId);
    }

    @Override
    public void setTargetIdsForSource(String sourceId, List<String> targetIds) {
        if (readOnly) {
            return;
        }
        checkDualReference();
        dualReference.setSourceIdsForTarget(sourceId, targetIds);
    }

    @Override
    public void setTargetIdsForSource(String sourceId, List<String> targetIds, Session session) {
        if (readOnly) {
            return;
        }
        checkDualReference();
        dualReference.setSourceIdsForTarget(sourceId, targetIds, session);
    }

    @Override
    public void setSourceIdsForTarget(String targetId, List<String> sourceIds) {
        if (readOnly) {
            return;
        }
        checkDualReference();
        dualReference.setTargetIdsForSource(targetId, sourceIds);
    }

    @Override
    public void setSourceIdsForTarget(String targetId, List<String> sourceIds, Session session) {
        if (readOnly) {
            return;
        }
        checkDualReference();
        dualReference.setTargetIdsForSource(targetId, sourceIds, session);
    }

    @Override
    public void addLinks(String sourceId, List<String> targetIds, Session session) {
        if (readOnly) {
            return;
        }
        checkDualReference();
        dualReference.addLinks(targetIds, sourceId, session);
    }

    @Override
    public void addLinks(List<String> sourceIds, String targetId, Session session) {
        if (readOnly) {
            return;
        }
        checkDualReference();
        dualReference.addLinks(targetId, sourceIds, session);
    }
}
