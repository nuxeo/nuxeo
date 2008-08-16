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
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.atom;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.activation.MimeType;

import org.apache.abdera.Abdera;
import org.apache.abdera.factory.Factory;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Content;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Person;
import org.apache.abdera.model.Text;
import org.apache.abdera.parser.ParseException;
import org.apache.abdera.protocol.server.ProviderHelper;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.ResponseContext;
import org.apache.abdera.protocol.server.context.EmptyResponseContext;
import org.apache.abdera.protocol.server.context.ResponseContextException;
import org.apache.abdera.protocol.server.impl.AbstractEntityCollectionAdapter;
import org.apache.abdera.util.Constants;
import org.apache.abdera.util.MimeTypeHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DocumentCollectionAdapter extends AbstractEntityCollectionAdapter<DocumentModel> {

    private static final Log log = LogFactory.getLog(DocumentCollectionAdapter.class);

    protected DocumentRef root;
    protected String id;

    public DocumentCollectionAdapter(DocumentRef root) throws ClientException {
        this.root = root;
        if (root.type() == DocumentRef.ID) {
            this.id = root.reference().toString();
        }
    }


    public DocumentCollectionAdapter(String path) throws ClientException {
        this.root = new PathRef(path);
    }



    protected CoreSession getSession(RequestContext request) {
        return AbderaHelper.getCoreSession(request);
    }


    @Override
    public String getId(RequestContext request) {
        if (id == null) {
            try {
                id = "nx:"+getSession(request).getDocument(root).getId();
            } catch (ClientException e) {
                id = "failedtogetid"; //TODO
                e.printStackTrace();
            }
        }
        return id;
    }

    public String getTitle(RequestContext request) {
        try {
            return getSession(request).getDocument(root).getTitle();
        } catch (ClientException e) {
            e.printStackTrace();
        }
        return "Collection";
    }

    @Override
    public String getAuthor(RequestContext request) {
        try {
            return (String)getSession(request).getDocument(root).getPropertyValue("dc:creator");
        } catch (ClientException e) {
            e.printStackTrace();
        }
        return "Nuxeo";
    }

//    @Override
//    protected String getFeedIriForEntry(DocumentModel entryObj,
//            RequestContext request) {
//        return basePath;//+URLEncoder.encode(entryObj.getName());
//    }

    @Override
    public Iterable<DocumentModel> getEntries(RequestContext request)
            throws ResponseContextException {
        try {
            return getSession(request).getChildren(root);
        } catch (ClientException e) {
            ResponseContextException ee = new ResponseContextException(500);
            ee.initCause(e);
            throw ee;
        }
    }

    @Override
    public DocumentModel getEntry(String resourceName, RequestContext request)
            throws ResponseContextException {
        try {
            return getSession(request).getChild(root, resourceName);
        } catch (ClientException e) {
            ResponseContextException ee = new ResponseContextException(500);
            ee.initCause(e);
            throw ee;
        }
    }

    @Override
    public Object getContent(DocumentModel entry, RequestContext request)
    throws ResponseContextException {
        String type = entry.getType(); // TODO : add extension point here
        Content content = null;
        try {
            if ("File".equals(type)) {
                Blob blob = (Blob)entry.getPropertyValue("file:content");
                if (blob != null) {
                    content = request.getAbdera().getFactory().newContent(Content.Type.TEXT);
                    content.setText(blob.getString());
                }
            } else if ("Note".equals(type)) {
                Object val = entry.getPropertyValue("note:note");
                if (val != null) {
                    content = request.getAbdera().getFactory().newContent(Content.Type.HTML);
                    content.setText(val.toString());
                }
            } else if ("WikiPage".equals(type)) {
                Object val = entry.getPropertyValue("wiki:content");
                if (val != null) {
                    content = request.getAbdera().getFactory().newContent(Content.Type.TEXT);
                    content.setText(val.toString());
                }
            }
        } catch (Exception e) {
            ResponseContextException ee = new ResponseContextException(500);
            ee.initCause(e);
            throw ee;
        }
        return content;
    }

    @Override
    public List<Person> getAuthors(DocumentModel entry, RequestContext request)
            throws ResponseContextException {
        String[] ar = null;
        try {
            ar = (String[])entry.getPropertyValue("dc:contributors");
        } catch (Exception e) {
            ResponseContextException ee = new ResponseContextException(500);
            ee.initCause(e);
            throw ee;
        }

        if (ar == null || ar.length == 0) {
            return null;
        }
        ArrayList<Person> authors = new ArrayList<Person>();
        for (String c : ar) {
            Person author = request.getAbdera().getFactory().newAuthor();
            author.setName(c);
            authors.add(author);
        }
        return authors.isEmpty() ? null : authors;
    }


    @Override
    public void deleteEntry(String resourceName, RequestContext request)
            throws ResponseContextException {
        try {
            CoreSession session = getSession(request);
            DocumentModel doc = session.getChild(root, resourceName);
            session.removeDocument(doc.getRef());
            session.save();
        } catch (ClientException e) {
            ResponseContextException ee = new ResponseContextException(500);
            ee.initCause(e);
            throw ee;
        }
    }

    @Override
    public DocumentModel postEntry(String title, IRI id, String summary,
            Date updated, List<Person> authors, Content content,
            RequestContext request) throws ResponseContextException {
        CoreSession session = getSession(request);
        try {
            DocumentModel parent = session.getDocument(root);
            System.out.println(">> creating: "+id);
            String name = title.replaceAll("\\s+", "_");
            DocumentModel doc = session.createDocumentModel(parent.getPathAsString(), name, "Note");
            String text = content.getText();
            doc.setPropertyValue("note:note", text);
            doc.setPropertyValue("dc:title", title);
            doc.setPropertyValue("dc:description", summary);
            doc = session.createDocument(doc);
            session.save();
            return doc;
        } catch (Exception e) {
            ResponseContextException ee = new ResponseContextException(500);
            ee.initCause(e);
            throw ee;
        }
    }

    @Override
    public void putEntry(DocumentModel doc, String title, Date updated,
            List<Person> authors, String summary, Content content,
            RequestContext request) throws ResponseContextException {
        CoreSession session = getSession(request);
        try {
            if (doc.getType().equals("Note")) {
                String text = content.getText();
                doc.setPropertyValue("note:note", text);
            } else {
                //TODO
            }
            doc.setPropertyValue("dc:title", title);
            doc.setPropertyValue("dc:description", summary);
            if (authors != null && !authors.isEmpty()) {
                String[] tmp = new String[authors.size()];
                int i = 0;
                for (Person p : authors) {
                    tmp[i++] = p.getName();
                }
                doc.setPropertyValue("dc:contributors", tmp);
            }
            session.saveDocument(doc);
            session.save();
        } catch (Exception e) {
            ResponseContextException ee = new ResponseContextException(500);
            ee.initCause(e);
            throw ee;
        }
    }

    @Override
    public String getId(DocumentModel entry) throws ResponseContextException {
        return "nx:"+entry.getId();
    }

    @Override
    public String getName(DocumentModel entry) throws ResponseContextException {
        return entry.getName();
    }

    @Override
    public Text getSummary(DocumentModel entry, RequestContext request)
            throws ResponseContextException {
        try {
            Text text = request.getAbdera().getFactory().newText(Constants.SUMMARY, Text.Type.TEXT);
            text.setValue((String)entry.getPropertyValue("dc:description"));
            return text;
        } catch (Exception e) {
            ResponseContextException ee = new ResponseContextException(500);
            ee.initCause(e);
            throw ee;
        }
    }

    @Override
    public String getTitle(DocumentModel entry) throws ResponseContextException {
        return entry.getTitle();
    }

    @Override
    public Date getUpdated(DocumentModel entry) throws ResponseContextException {
        try {
            Calendar cal =  (Calendar)entry.getPropertyValue("dc:modified");
            if (cal != null) {
                return cal.getTime();
            }
            return new Date(0);
        } catch (Exception e) {
            ResponseContextException ee = new ResponseContextException(500);
            ee.initCause(e);
            throw ee;
        }
    }


    /** there are some bugs in abdera that break PUT and POST because abdera is
     * trying to validate the entry sent by the client and the validation mechanism is not adapted
     * to real life (tested with firefox atom plugin):
     * 1. on PUT the updated date is checked to exists (but it is not) and after the check
     * that never succeed abdera adds the updated date itself to the entry:

        if (!ProviderHelper.isValidEntry(entry))
          return new EmptyResponseContext(400);
        entry.setUpdated(new Date());

     * 2. you cannot modify an entry if you don't modify all fields in the entry (if you want to just modify the title
     * the check will fail because no content is specified)
     *
     *   The following methods are fixing this
     */

    @Override
    public ResponseContext postEntry(RequestContext request) {
        try {
            Entry entry = getEntryFromRequest(request);
            if (entry != null) {
//              TODO implement a better validate method
//              if (!ProviderHelper.isValidEntry(entry))
//              return new EmptyResponseContext(400);

                entry.setUpdated(new Date());


                DocumentModel entryObj = postEntry(entry.getTitle(),
                        entry.getId(),
                        entry.getSummary(),
                        entry.getUpdated(),
                        entry.getAuthors(),
                        entry.getContentElement(), request);
                entry.getIdElement().setValue(getId(entryObj));

                IRI feedIri = getFeedIRI(entryObj, request);
                String link = getLink(entryObj, feedIri, request);

                entry.addLink(link, "edit");

                return buildCreateEntryResponse(link, entry);
            } else {
                return new EmptyResponseContext(400);
            }
        } catch (ResponseContextException e) {
            return createErrorResponse(e);
        }
    }

    protected IRI getFeedIRI(DocumentModel  entryObj, RequestContext request) {
        String feedIri = getFeedIriForEntry(entryObj, request);
        return new IRI(feedIri).trailingSlash();
    }

    @Override
    public ResponseContext putEntry(RequestContext request) {
        try {
            String id = getResourceName(request);
            DocumentModel entryObj = getEntry(id, request);

            if (entryObj == null) {
                return new EmptyResponseContext(404);
            }

            Entry orig_entry = getEntryFromCollectionProvider(entryObj, new IRI(getFeedIriForEntry(entryObj, request)), request);
            if (orig_entry != null) {

                MimeType contentType = request.getContentType();
                if (contentType != null && !MimeTypeHelper.isAtom(contentType.toString()))
                    return new EmptyResponseContext(415);

                Entry entry = getEntryFromRequest(request);
                if (entry != null) {
                    if (!entry.getId().equals(orig_entry.getId()))
                        return new EmptyResponseContext(409);

// TODO implement better validation
//                    if (!ProviderHelper.isValidEntry(entry))
//                        return new EmptyResponseContext(400);

                    putEntry(entryObj, entry.getTitle(), new Date(), entry.getAuthors(),
                            entry.getSummary(), entry.getContentElement(), request);
                    return new EmptyResponseContext(204);
                } else {
                    return new EmptyResponseContext(400);
                }
            } else {
                return new EmptyResponseContext(404);
            }
        } catch (ResponseContextException e) {
            return createErrorResponse(e);
        } catch (ParseException pe) {
            return new EmptyResponseContext(415);
        } catch (ClassCastException cce) {
            return new EmptyResponseContext(415);
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            return new EmptyResponseContext(400);
        }
    }

    protected Entry getEntryFromCollectionProvider(DocumentModel entryObj, IRI feedIri, RequestContext request)
    throws ResponseContextException {
        Abdera abdera = request.getAbdera();
        Factory factory = abdera.getFactory();
        Entry entry = factory.newEntry();

        return buildEntry(entryObj, entry, feedIri, request);
    }

    private Entry buildEntry(DocumentModel entryObj, Entry entry, IRI feedIri, RequestContext request)
    throws ResponseContextException {
        addEntryDetails(request, entry, feedIri, entryObj);

        if (isMediaEntry(entryObj)) {
            addMediaContent(feedIri, entry, entryObj, request);
        } else {
            addContent(entry, entryObj, request);
        }

        return entry;
    }

}
