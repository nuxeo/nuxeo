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
 * $Id$
 */

package org.nuxeo.ecm.core.repository.jcr;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.model.Document;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class JCRRoot extends JCRDocument {

    public JCRRoot(JCRSession session) throws RepositoryException {
        super(session, initializeRoot(session));
        initRootACP();
    }

    private static Node initializeRoot(JCRSession session) throws RepositoryException {
        Session jcrSession = session.getSession();
        Node jcrRoot = jcrSession.getRootNode();
        if (!jcrRoot.hasNode(NodeConstants.ECM_ROOT.rawname)) {
            // if ecm root not already created, create it now
            Node root = jcrRoot.addNode(NodeConstants.ECM_ROOT.rawname,
                    TypeAdapter.docType2NodeType(NodeConstants.ECM_ROOT_TYPE));
            if (true) {
                // TODO: root is unstructured? Create a doc type to describe the
                // root
                ModelAdapter.setUnstructured(root);
            }
            // XXX : add a migration solution
            jcrRoot.save();
            // create the default infrastructure if any is specified
            createDefaultStructure(jcrRoot);
        } else {
            Node root = jcrRoot.getNode(NodeConstants.ECM_ROOT.rawname);
            NodeType nt = root.getPrimaryNodeType();
            if (!TypeAdapter.nodeType2DocType(nt.getName()).equals(
                    NodeConstants.ECM_ROOT_TYPE)) {
                // migrate the root

                // move the root
                jcrSession.move(root.getPath(), "/tmp_root");
                jcrSession.save();

                // create new root
                Node newRoot = jcrRoot.addNode(
                        NodeConstants.ECM_ROOT.rawname,
                        TypeAdapter.docType2NodeType(NodeConstants.ECM_ROOT_TYPE));

                // get old root
                // Node old_root = jcrRoot.getNode(jcrRoot.getPath() +
                // "/tmp_root");
                Node oldRoot = jcrRoot.getNode("tmp_root");

                // move children
                NodeIterator nodeIt = oldRoot.getNodes();
                while (nodeIt.hasNext()) {
                    Node cNode = nodeIt.nextNode();
                    if (newRoot.hasNode(cNode.getName())) {
                        newRoot.getNode(cNode.getName()).remove();
                    }
                    jcrSession.move(cNode.getPath(), newRoot.getPath() + '/'
                            + cNode.getName());
                }

                // delete old_root
                oldRoot.remove();

                // save
                jcrSession.save();
            }
        }
        return jcrRoot.getNode(NodeConstants.ECM_ROOT.rawname);
    }

    private static void createDefaultStructure(Node jcrRoot) {
//        String docType = documentModel.getType();
//
//        if (DOMAIN_TYPE.equals(docType)) {
//            log.debug("Creating content roots for domain: "
//                    + documentModel.getTitle());
//            TypeService typeService = (TypeService) Framework.getRuntime().getComponent(
//                    TypeService.ID);
//            Type domainType = typeService.getTypeRegistry().getType(docType);
//
//            String sessionId = documentModel.getSessionId();
//            CoreSession session = CoreInstance.getInstance().getSession(
//                    sessionId);
//
//            setDefaultPermissions(documentModel, session);
//
//            String parentPath = documentModel.getPathAsString();
//            List<String> contentRootsList = Arrays.asList(domainType.getAllowedSubTypes());
//            for (String contentRootName : contentRootsList) {
//                DocumentModel contentRootModel = session.createDocumentModel(contentRootName);
//
//                // get title
//                int index;
//                String contentLabel = "";
//                index = contentRootName.indexOf("Root");
//                if (index != -1) {
//                    contentLabel = contentRootName.substring(0, index) + 's';
//                } else {
//                    contentLabel = contentRootName + 's';
//                }
//                // make title i18n
//                // contentLabel = "%i18n" + contentLabel;
//
//                contentRootModel.setProperty("dublincore", "title",
//                        contentLabel);
//                String name = IdUtils.generateId(contentLabel);
//                // set parent path and name for document model
//                contentRootModel.setPathInfo(parentPath, name);
//                session.createDocument(contentRootModel);
//                session.save();
//            }
//        }
    }


//    private void setDefaultPermissions(DocumentModel doc, CoreSession session)
//            throws DocumentException {
//        DefaultPermissionService permissionService = DefaultPermissionServiceHelper.getService();
//        String docType = doc.getType();
//        ACL acl = permissionService.getPermissionsForType(docType);
//        if (acl != null) {
//            log.debug("setting default permissions for [docType=" + docType
//                    + " name=" + doc.getName());
//            DocumentRef docRef = doc.getRef();
//            ACP acp = session.getACP(docRef);
//            acp.addACL(acl);
//            session.setACP(docRef, acp, true);
//        }
//    }

    @Override
    public Document getParent() throws DocumentException {
        return null;
    }

    @Override
    public String getPath() throws DocumentException {
        return "/";
    }

    public void dispose() {
        session = null;
        node = null;
        type = null;
    }

    private void initRootACP() throws RepositoryException {
        try {
            ACP acp = session.getSecurityManager().getACP(this);
            if (acp == null) {
                acp = new ACPImpl();
            }
            if (acp.listUsernamesForPermission(SecurityConstants.EVERYTHING).length == 0) {
                // if nobody has the right to manage the repository set a
                // default ACP that can be later overridden by the content
                // template manager of NXP

                ACL acl = acp.getOrCreateACL();
                acl.add(new ACE(SecurityConstants.ADMINISTRATORS,
                        SecurityConstants.EVERYTHING, true));
                acl.add(new ACE(SecurityConstants.MEMBERS,
                        SecurityConstants.READ, true));
                // temporary hack to have junit tests running
                // TODO - update tests to an user from the administrators group
                acl.add(new ACE(SecurityConstants.ADMINISTRATOR,
                        SecurityConstants.EVERYTHING, true));
                acp.addACL(acl);
                session.getSecurityManager().setACP(this, acp, true);
                // be sure to save the session
                session.jcrSession().save();
            }
        } catch (org.nuxeo.ecm.core.security.SecurityException e) {
            // weird exception handling -> TODO review this
            throw new RepositoryException("Failed to initialize administrator privileges!", e);
        }
    }

}
