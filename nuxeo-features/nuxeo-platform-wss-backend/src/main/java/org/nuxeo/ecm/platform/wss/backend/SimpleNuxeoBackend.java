/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */

package org.nuxeo.ecm.platform.wss.backend;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.platform.jbpm.JbpmListFilter;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.relations.api.Literal;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.Subject;
import org.nuxeo.ecm.platform.relations.api.impl.RelationDate;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.wss.service.WSSPlugableBackendManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.wss.WSSException;
import org.nuxeo.wss.servlet.WSSFilter;
import org.nuxeo.wss.servlet.WSSRequest;
import org.nuxeo.wss.spi.WSSBackend;
import org.nuxeo.wss.spi.WSSListItem;
import org.nuxeo.wss.spi.dws.DWSMetaData;
import org.nuxeo.wss.spi.dws.DWSMetaDataImpl;
import org.nuxeo.wss.spi.dws.Link;
import org.nuxeo.wss.spi.dws.LinkImpl;
import org.nuxeo.wss.spi.dws.Site;
import org.nuxeo.wss.spi.dws.SiteImpl;
import org.nuxeo.wss.spi.dws.Task;
import org.nuxeo.wss.spi.dws.User;
import org.nuxeo.wss.spi.dws.UserImpl;

public class SimpleNuxeoBackend extends AbstractNuxeoCoreBackend implements WSSBackend {

    private static final Log log = LogFactory.getLog(SimpleNuxeoBackend.class);

    protected String corePathPrefix;
    protected String urlRoot;

    public SimpleNuxeoBackend(String corePathPrefix, String urlRoot) {
        this.corePathPrefix=corePathPrefix;
        this.urlRoot = urlRoot;
    }

    public SimpleNuxeoBackend(String corePathPrefix, String urlRoot, CoreSession session) {
        this(corePathPrefix,urlRoot);
        this.session = session;
    }

    protected DocumentModel resolveLocation(String location) throws ClientException, WSSException, Exception {
        Path strPath = new Path(corePathPrefix);
        strPath = strPath.append(location);
        DocumentRef docRef = new PathRef(strPath.toString());
        DocumentModel doc = null;

        if (getCoreSession().exists(docRef)) {
            doc = getCoreSession().getDocument(docRef);
        } else {
            Path path = new Path(location);
            String parentSubPath = path.removeLastSegments(1).toString();
            Path parentPath = new Path(corePathPrefix);

            String filename = path.lastSegment();

            // first try with spaces (for create New Folder)
            String folderName = filename.replace(" ", "-");
            DocumentRef folderRef = new PathRef(parentPath.append(folderName).toString());
            if (getCoreSession().exists(folderRef)) {
                return getCoreSession().getDocument(folderRef);
            }
            // look for a child
            parentPath = parentPath.append(parentSubPath);
            docRef = new PathRef(parentPath.toString());
            if (!getCoreSession().exists(docRef)) {
                throw new WSSException("Unable to find parent for item " + location);
            }
            List<DocumentModel> children = getCoreSession().getChildren(docRef);
            for (DocumentModel child : children) {
                BlobHolder bh = child.getAdapter(BlobHolder.class);
                if (bh!=null) {
                    Blob blob = bh.getBlob();
                    if (blob!=null && filename.equals(blob.getFilename())) {
                        doc = child;
                        break;
                    }
                    else if (blob!=null && URLEncoder.encode(filename).equals(blob.getFilename())) {
                        doc = child;
                        break;
                    }
                }
            }
        }

        return doc;
    }

    @Override
    public boolean exists(String location) {
        return super.exists(location); // XXX todo
    }

    public WSSListItem getItem(String location) throws WSSException {

        try {
            DocumentModel doc = resolveLocation(location);
            if (doc!=null) {
                return WSSPlugableBackendManager.instance().createItem(doc, corePathPrefix, urlRoot);
            }
            else {
                throw new WSSException("Unable to find item " + location);
            }
        }
        catch (Exception e) {
            throw new WSSException("Error while getting item", e);
        }
    }

    public List<WSSListItem> listItems(String location) throws WSSException {

        Path strPath = new Path(corePathPrefix);
        strPath = strPath.append(location);
        DocumentRef docRef = new PathRef(strPath.toString());
        List<WSSListItem> items = new ArrayList<WSSListItem>();

        try {

            if (! getCoreSession().exists(docRef)) {
                throw new WSSException("Unable to find item " + location);
            }

            List<DocumentModel> children = getCoreSession().getChildren(docRef);
            for (DocumentModel child : children) {
                if (child.hasFacet(FacetNames.HIDDEN_IN_NAVIGATION)) {
                    log.debug("Skipping hidden doc");
                } else if (LifeCycleConstants.DELETED_STATE.equals(child.getCurrentLifeCycleState())) {
                    log.debug("Skipping deleted doc");
                } else {
                    items.add(WSSPlugableBackendManager.instance().createItem(child, corePathPrefix, urlRoot));
                }
            }
            return items;
        }
        catch (Exception e) {
            throw new WSSException("Error while getting children for " + location, e);
        }
    }


    // called when moving from one backend to an other
    public WSSListItem moveDocument(DocumentModel source, String newLocation) throws WSSException {

        Path destinationPath = new Path(corePathPrefix);
        destinationPath = destinationPath.append(newLocation);

        String name = source.getName();
        DocumentRef targetRef = new PathRef(destinationPath.removeLastSegments(1).toString());
        DocumentModel movedDoc = null;

        checkAccess(targetRef, SecurityConstants.ADD_CHILDREN);
        checkAccess(source.getRef(), SecurityConstants.REMOVE);
        //checkAccess(source.parent.getRef(), SecurityConstants.REMOVE_CHILDREN);

        try {
            movedDoc = getCoreSession().move(source.getRef(), targetRef, name);
        } catch (Exception e) {
            log.error("Error while moving " + source.getPathAsString() + " to " + newLocation, e);
            throw new WSSException("Error while moving " + source.getPathAsString() + " to " + newLocation, e);
        }

        if (movedDoc!=null) {
            return WSSPlugableBackendManager.instance().createItem(movedDoc, corePathPrefix, urlRoot);
        } else {
            throw new WSSException("No resulting doc found !!!");
        }
    }

    public WSSListItem moveItem(String oldLocation, String newLocation) throws WSSException {

         Path sourcePath = new Path(corePathPrefix);
         sourcePath = sourcePath.append(oldLocation);

         Path destinationPath = new Path(corePathPrefix);
         destinationPath = destinationPath.append(newLocation);

         DocumentModel movedDoc = null;

         try {
             if (sourcePath.removeLastSegments(1).toString().equals(destinationPath.removeLastSegments(1).toString())) {
                 // rename !!

                 DocumentModel source = resolveLocation(oldLocation);
                 String dstName = destinationPath.lastSegment();

                 if (source.isFolder()) {
                     source.setPropertyValue("dc:title", dstName);
                     checkAccess(source.getRef(), SecurityConstants.WRITE_PROPERTIES);

                     getCoreSession().saveDocument(source);
                     movedDoc = getCoreSession().move(source.getRef(), source.getParentRef(), cleanName(dstName));
                 } else {
                     BlobHolder bh = source.getAdapter(BlobHolder.class);
                     boolean blobUpdated=false;
                     if (bh!=null) {
                         Blob blob = bh.getBlob();
                         if (blob!=null) {
                             blob.setFilename(dstName);
                             blobUpdated=true;
                             // XXXX should be done via blob holder !!!
                             if (source.hasSchema("file")) {
                                 source.setProperty("file", "content", blob);
                             }
                             movedDoc = getCoreSession().saveDocument(source);
                         }
                     }
                     if (!blobUpdated) {
                         source.setPropertyValue("dc:title", dstName);
                         source = getCoreSession().saveDocument(source);
                         movedDoc = getCoreSession().move(source.getRef(), source.getParentRef(), cleanName(dstName));
                     }
                 }
             } else {
                 // move
                 DocumentModel source = resolveLocation(oldLocation);
                 String name = source.getName();
                 DocumentRef targetRef = new PathRef(destinationPath.removeLastSegments(1).toString());
                 movedDoc = getCoreSession().move(source.getRef(), targetRef, name);
             }
         }
         catch (Exception e) {
             throw new WSSException("Error while doing move", e);
        }
        if (movedDoc != null) {
            return WSSPlugableBackendManager.instance().createItem(movedDoc, corePathPrefix, urlRoot);
        } else {
            throw new WSSException("No resulting doc found !!!");
        }
    }

    public void removeItem(String location) throws WSSException {

        DocumentModel docToRemove = null;
        try {
            docToRemove = resolveLocation(location);
        } catch (Exception e) {
            throw new WSSException("Error while resolving document path", e);
        }
        if (docToRemove == null) {
            throw new WSSException("Document path not found");
        }
        try {
            checkAccess(docToRemove.getParentRef(), SecurityConstants.REMOVE_CHILDREN);
            getCoreSession().removeDocument(docToRemove.getRef());
        } catch (Exception e) {
            throw new WSSException("Error while deleting doc " + docToRemove.getRef() , e);
        }
    }

    protected DocumentModel createNode(String parentPath, String name, boolean folderish) throws WSSException {

        DocumentModel parent;
        try {
            parent = resolveLocation(parentPath);
        } catch (Exception e) {
            throw new WSSException("Error while resolving parent path", e);

        }
        if (parent == null) {
            throw new WSSException("Error while resolving parent path");
        }
        if (!parent.isFolder()) {
            throw new WSSException("Can not create a child in a non folderish node");
        }

        String targetType = WSSPlugableBackendManager.folderishDocType;
        if (folderish) {
            if ("WorkspaceRoot".equals(parent.getType())) {
                targetType = "Workspace";
            }
        } else {
            targetType = WSSPlugableBackendManager.leafDocType;
        }

        String nodeName = cleanName(name);

        try {
            checkAccess(parent.getRef(), SecurityConstants.ADD_CHILDREN);
            DocumentModel newDoc = getCoreSession().createDocumentModel(parent.getPathAsString(), nodeName, targetType);
            newDoc.setPropertyValue("dc:title", name);
            newDoc = getCoreSession().createDocument(newDoc);
            return newDoc;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            throw new WSSException("Error child creating new folder", e);
        }
    }

    public WSSListItem createFolder(String parentPath, String name)
            throws WSSException {
        DocumentModel newFolder = createNode(parentPath, name, true);
        return WSSPlugableBackendManager.instance().createItem(newFolder, corePathPrefix, urlRoot);
    }

    protected String cleanName(String name) {
        // XXX
        String s = name.replaceAll(" ", "-");
        s = s.replaceAll("[èéêë]","e");
        s = s.replaceAll("[ûù]","u");
        s = s.replaceAll("[ïî]","i");
        s = s.replaceAll("[àâ]","a");
        s = s.replaceAll("Ô","o");
        s = s.replaceAll("ç","c");
        s = s.replaceAll("[ÈÉÊË]","E");
        s = s.replaceAll("[ÛÙ]","U");
        s = s.replaceAll("[ÏÎ]","I");
        s = s.replaceAll("[ÀÂ]","A");
        s = s.replaceAll("Ô","O");
        s = s.replaceAll("Ç","C");
        return s;
    }

    public WSSListItem createFileItem(String parentPath, String name)
            throws WSSException {
        DocumentModel newFolder = createNode(parentPath, name, false);
        return WSSPlugableBackendManager.instance().createItem(newFolder, corePathPrefix, urlRoot);
    }

    public Site getSite(String location) throws WSSException {

        String parentPath = new Path(location).removeLastSegments(1).toString();
        NuxeoListItem parent = (NuxeoListItem) getItem(parentPath);
        String siteName = parent.getDisplayName();
        SiteImpl site = new SiteImpl(siteName);
        String nxUrl = urlRoot + "/nxpath/default" + parent.getDoc().getPathAsString() + "@view_documents";
        try {
            site.setAccessUrl("?"+ WSSFilter.FILTER_FORWARD_PARAM +  "=" + URLEncoder.encode(nxUrl, "UTF-8"));
            //site.setAccessUrl(URLEncoder.encode(nxUrl, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new WSSException("Error encoding url", e);
        }
        //site.setAccessUrl(parent.getRelativeFilePath(""));
        //site.setAccessUrl();
        site.setUserManagementUrl("");
        site.setListUUID(parent.getEtag());
        site.setItem(parent);
        return site;
    }

    public DWSMetaData getMetaData(String location, WSSRequest request)
            throws WSSException {

        try {
            DWSMetaDataImpl metadata = new DWSMetaDataImpl();

            String parentPath = new Path(location).removeLastSegments(1).toString();
            List<WSSListItem> documents  = listItems(parentPath);
            NuxeoListItem doc = (NuxeoListItem) getItem(location);

            metadata.setDocuments(documents);

            metadata.setSite(getSite(location));
            String cUserName = getCoreSession().getPrincipal().getName();

            List<String> userNames = new ArrayList<String>();
            for (WSSListItem item : documents) {
                if (!userNames.contains(item.getAuthor())) {
                    userNames.add(item.getAuthor());
                }
                String[] contributors = (String[]) (((NuxeoListItem) item).getDoc().getPropertyValue("dc:contributors"));
                if (contributors!=null) {
                    for (String contributor : contributors) {
                        if (!userNames.contains(contributor)) {
                            userNames.add(contributor);
                        }
                    }
                }
            }

            User currentUser = getUserFromLogin(cUserName, 1);
            metadata.setCurrentUser(currentUser);

            // get links
            List<Link> links = getDocumentLinks(doc.getDoc(), userNames, request);
            metadata.setLinks(links);

            // get tasks
            List<Task> tasks = getTasks(doc.getDoc(), userNames, request);
            metadata.setTasks(tasks);

            // manage users
            List<User> users = new ArrayList<User>();
            users.add(currentUser);
            int i = 2;
            for (String name : userNames) {
                if (!name.equals(cUserName)) {
                    users.add(getUserFromLogin(name, i));
                    i++;
                }
            }
            metadata.setUsers(users);

            return metadata;

        }
        catch (Exception e) {
            throw new WSSException("Error in getMetadata", e);
        }
    }

    protected List<Task> getTasks(DocumentModel doc, List<String> userNames, WSSRequest request) throws Exception {

        List<Task> tasks = new ArrayList<Task>();
        JbpmService jbpmService = Framework.getService(JbpmService.class);
        List<TaskInstance> jbpmTasks = jbpmService.getTaskInstances(doc, null, (JbpmListFilter) null);

        for (TaskInstance jbpmTask : jbpmTasks) {
            NuxeoTask task = new NuxeoTask(jbpmTask,"");
            task.translateDirective(request);
            tasks.add(task);
            String author = task.getAuthorLogin();
            if (author!=null && !userNames.contains(author)) {
                userNames.add(author);
            }
            String editor = task.getEditorLogin();
            if (editor !=null && !userNames.contains(editor)) {
                userNames.add(editor);
            }
            String assignee = task.getAssigneeLogin();
            if (assignee!=null && !userNames.contains(assignee)) {
                userNames.add(assignee);
            }
        }

        return tasks;
    }

    protected List<Link> getDocumentLinks(DocumentModel doc, List<String> userNames, WSSRequest request) throws Exception  {

        List<Link> links = new ArrayList<Link>();

        RelationManager relationManager = Framework.getService(RelationManager.class);
        QNameResource documentResource = (QNameResource) relationManager.getResource(
                RelationConstants.DOCUMENT_NAMESPACE, doc, null);

        Statement pattern = new StatementImpl(null, null, documentResource);
        List<Statement> inStatements = relationManager.getStatements(RelationConstants.GRAPH_NAME, pattern);
        links.addAll(computeLinks(inStatements, userNames, true, request));

        pattern = new StatementImpl(documentResource, null, null );
        List<Statement> outStatements = relationManager.getStatements(RelationConstants.GRAPH_NAME, pattern);
        links.addAll(computeLinks(outStatements, userNames, false, request));

        return links;
    }

    protected List<Link> computeLinks(List<Statement> statements, List<String> userNames, boolean incomming, WSSRequest request ) throws Exception  {

        List<Link> links = new ArrayList<Link>();

        int counter = 1;

        String baseUrl = request.getBaseUrl();
        String targetDocTitle="";
        for (Statement stmt : statements) {

            String url = "";
            if (incomming) {
                Subject subject = stmt.getSubject();
                if (subject.isLiteral()) {
                    url = ((Literal) subject).getValue();
                } else if (subject.isQNameResource()) {
                    if (RelationConstants.DOCUMENT_NAMESPACE.equals(((QNameResource) subject).getNamespace())) {
                        url = mkDocLink(baseUrl, ((QNameResource) subject).getLocalName());
                        targetDocTitle = getDocTitle(((QNameResource) subject).getLocalName());
                    } else {
                        url = ((QNameResource) subject).getUri();
                    }
                } else if (subject.isResource()) {
                    url = ((Resource) subject).getUri();
                }
            }
            else {
                Node object = stmt.getObject();
                if (object.isLiteral()) {
                    url = ((Literal) object).getValue();
                } else if (object.isQNameResource()) {
                    if (RelationConstants.DOCUMENT_NAMESPACE.equals(((QNameResource) object).getNamespace())) {
                        url = mkDocLink(baseUrl, ((QNameResource) object).getLocalName());
                        targetDocTitle = getDocTitle(((QNameResource) object).getLocalName());
                    } else {
                        url = ((QNameResource) object).getUri();
                    }
                } else if (object.isResource()) {
                    url = ((Resource) object).getUri();
                }
            }

            String comment = "";
            Node node = stmt.getProperty(RelationConstants.COMMENT);
            if (node != null && node.isLiteral()) {
                comment = ((Literal) node).getValue();
            }
            Date creationDate = null;

            Node dateNode = stmt.getProperty(RelationConstants.CREATION_DATE);
            if (dateNode != null && dateNode.isLiteral()) {
                creationDate = RelationDate.getDate((Literal) dateNode);
            }
            dateNode = stmt.getProperty(RelationConstants.MODIFICATION_DATE);

            Date modificationDate = null;
            if (dateNode != null && dateNode.isLiteral()) {
                modificationDate = RelationDate.getDate((Literal) dateNode);
            }
            node = stmt.getProperty(RelationConstants.AUTHOR);

            String author;
            if (node != null && node.isLiteral()) {
                author = ((Literal) node).getValue();
            } else {
                author = "";
            }

            if (author!=null && !userNames.contains(author)) {
                 userNames.add(author);
            }

            String predicateURL = stmt.getPredicate().getUri();
            String predicate = new Path(predicateURL).lastSegment();
            String labelKey = "label.relation.predicate." + predicate;
            String predicateLabel = TranslationHelper.getLabel(labelKey, request);

            comment = predicateLabel + "\n" + targetDocTitle + "\n" + comment;

            String id = "";
            if (incomming) {
                id = "1" + counter;
            } else {
                id = "2" + counter;
            }

            Link link = new LinkImpl(id,author, creationDate, modificationDate, "",comment,url);
            links.add(link);

            counter++;
        }

        return links;
    }

    protected String mkDocLink(String baseURL, String uri) {
        return baseURL + "nuxeo/nxdoc/" + uri + "/view_documents";
    }

    protected String getDocTitle( String uri) {
        String uuid = uri.split("/")[1];
        DocumentRef ref = new IdRef(uuid);

        try {
            if (getCoreSession().exists(ref)) {
                return getCoreSession().getDocument(ref).getTitle();
            } else {
                return "";
            }
        } catch (Exception e) {
            return "";
        }
    }


    protected User getUserFromLogin(String userLogin, int idx) throws Exception {

        User user=null;
        UserManager um = Framework.getService(UserManager.class);
        NuxeoPrincipal principal = um.getPrincipal(userLogin);
        if (principal!=null) {
            String email = (String) principal.getModel().getProperty(um.getUserSchemaName(), um.getUserEmailField());
            String fullname = principal.getFirstName() + " "+ principal.getLastName();
            if (fullname.equals(" ")) {
                fullname = userLogin;
            }
            user = new UserImpl(""+idx, userLogin, fullname, email);
        }
        else {
            user = new UserImpl(""+idx, userLogin, userLogin, "");
        }
        return user;
    }

}
