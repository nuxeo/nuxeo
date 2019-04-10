/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl.client;

import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Policy;
import org.apache.chemistry.opencmis.client.api.TransientFolder;
import org.apache.chemistry.opencmis.client.api.Tree;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.FailedToDeleteData;
import org.apache.chemistry.opencmis.commons.enums.UnfileObject;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConstraintException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisNotSupportedException;

/**
 * Transient CMIS Folder for Nuxeo.
 */
public class NuxeoTransientFolder extends NuxeoTransientFileableObject
        implements TransientFolder {

    protected boolean isMarkedForDeleteTree;

    protected boolean deleteTreeAllVersions;

    protected UnfileObject deleteTreeUnfile;

    protected boolean deleteTreeContinueOnFailure;

    public NuxeoTransientFolder(NuxeoObject object) {
        super(object);
    }

    @Override
    public Document createDocument(Map<String, ?> properties,
            ContentStream contentStream, VersioningState versioningState,
            List<Policy> policies, List<Ace> addAces, List<Ace> removeAces,
            OperationContext context) {
        return ((NuxeoFolder) object).createDocument(properties, contentStream,
                versioningState, policies, addAces, removeAces, context);
    }

    @Override
    public Document createDocument(Map<String, ?> properties,
            ContentStream contentStream, VersioningState versioningState) {
        return ((NuxeoFolder) object).createDocument(properties, contentStream,
                versioningState);
    }

    @Override
    public Document createDocumentFromSource(ObjectId source,
            Map<String, ?> properties, VersioningState versioningState,
            List<Policy> policies, List<Ace> addAces, List<Ace> removeAces,
            OperationContext context) {
        return ((NuxeoFolder) object).createDocumentFromSource(source,
                properties, versioningState, policies, addAces, removeAces,
                context);
    }

    @Override
    public Document createDocumentFromSource(ObjectId source,
            Map<String, ?> properties, VersioningState versioningState) {
        return ((NuxeoFolder) object).createDocumentFromSource(source,
                properties, versioningState);
    }

    @Override
    public Folder createFolder(Map<String, ?> properties,
            List<Policy> policies, List<Ace> addAces, List<Ace> removeAces,
            OperationContext context) {
        return ((NuxeoFolder) object).createFolder(properties, policies,
                addAces, removeAces, context);
    }

    @Override
    public Folder createFolder(Map<String, ?> properties) {
        return ((NuxeoFolder) object).createFolder(properties);
    }

    @Override
    public Policy createPolicy(Map<String, ?> properties,
            List<Policy> policies, List<Ace> addAces, List<Ace> removeAces,
            OperationContext context) {
        throw new CmisNotSupportedException();
    }

    @Override
    public Policy createPolicy(Map<String, ?> properties) {
        throw new CmisNotSupportedException();
    }

    @Override
    public List<Tree<FileableCmisObject>> getFolderTree(int depth) {
        return ((NuxeoFolder) object).getFolderTree(depth);
    }

    @Override
    public List<Tree<FileableCmisObject>> getFolderTree(int depth,
            OperationContext context) {
        return ((NuxeoFolder) object).getFolderTree(depth, context);
    }

    @Override
    public List<Tree<FileableCmisObject>> getDescendants(int depth) {
        return ((NuxeoFolder) object).getDescendants(depth);
    }

    @Override
    public List<Tree<FileableCmisObject>> getDescendants(int depth,
            OperationContext context) {
        return ((NuxeoFolder) object).getDescendants(depth, context);
    }

    @Override
    public ItemIterable<CmisObject> getChildren() {
        return ((NuxeoFolder) object).getChildren();
    }

    @Override
    public ItemIterable<CmisObject> getChildren(OperationContext context) {
        return ((NuxeoFolder) object).getChildren(context);
    }

    @Override
    public boolean isRootFolder() {
        return ((NuxeoFolder) object).isRootFolder();
    }

    @Override
    public Folder getFolderParent() {
        return ((NuxeoFolder) object).getFolderParent();
    }

    @Override
    public String getPath() {
        return ((NuxeoFolder) object).getPath();
    }

    @Override
    public ItemIterable<Document> getCheckedOutDocs() {
        return ((NuxeoFolder) object).getCheckedOutDocs();
    }

    @Override
    public ItemIterable<Document> getCheckedOutDocs(OperationContext context) {
        return ((NuxeoFolder) object).getCheckedOutDocs(context);
    }

    @Override
    public List<ObjectType> getAllowedChildObjectTypes() {
        return ((NuxeoFolder) object).getAllowedChildObjectTypes();
    }

    @Override
    public void setAllowedChildObjectTypes(List<ObjectType> types) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteTree(boolean allversions, UnfileObject unfile,
            boolean continueOnFailure) {
        deleteTreeAllVersions = allversions;
        deleteTreeUnfile = unfile;
        deleteTreeContinueOnFailure = continueOnFailure;
        isMarkedForDeleteTree = true;
    }

    @Override
    public boolean isMarkedForDelete() {
        return isMarkedForDeleteTree || super.isMarkedForDelete();
    }

    @Override
    public boolean isModified() {
        return isMarkedForDeleteTree || super.isModified();
    }

    @Override
    protected boolean saveDeletes() {
        if (isMarkedForDeleteTree) {
            FailedToDeleteData failed = object.service.deleteTree(
                    object.getRepositoryId(), getId(),
                    Boolean.valueOf(deleteTreeAllVersions), deleteTreeUnfile,
                    Boolean.valueOf(deleteTreeContinueOnFailure), null);
            if (failed != null && !failed.getIds().isEmpty()) {
                throw new CmisConstraintException(
                        "Could not delete some children: " + failed.getIds());
            }
            return true;
        }
        return super.saveDeletes();
    }

    @Override
    public void reset() {
        super.reset();
        isMarkedForDeleteTree = false;
    }

}
