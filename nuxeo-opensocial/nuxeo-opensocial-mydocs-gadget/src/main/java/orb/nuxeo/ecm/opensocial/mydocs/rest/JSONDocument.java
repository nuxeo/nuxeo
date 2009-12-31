package orb.nuxeo.ecm.opensocial.mydocs.rest;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelIterator;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.CompoundFilter;
import org.nuxeo.ecm.core.api.impl.DocumentsPageProvider;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.api.impl.LifeCycleFilter;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.rest.DocumentObject;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.runtime.api.Framework;

@WebObject(type = "JSONDocument")
public class JSONDocument extends DocumentObject {
  private static final int PAGE_SIZE = 10;
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat(
      "yyyy-MM-dd HH:mm:ss");

  private static final Log log = LogFactory.getLog(JSONDocument.class);

  @GET
  @Produces("text/html; charset=UTF-8")
  public Object doGet() {

    String currentPage = ctx.getRequest()
        .getParameter("page");

    Integer index;
    try {
      index = Integer.valueOf(currentPage);
    } catch (Exception e) {
      index = 0;
    }

    Map<String, Object> all = new HashMap<String, Object>();
    Map<String, Object> summary = new HashMap<String, Object>();
    summary.put("id", getDocument().getId());
    try {
      summary.put("title", getDocument().getTitle());
    } catch (Exception e) {
      e.printStackTrace();
    }
    ;

    CoreSession session = ctx.getCoreSession();
    try {

      PagedDocumentsProvider provider = getResProviderForDocChildren(
          getDocument().getRef(), session);

      summary.put("pages", provider.getNumberOfPages());
      summary.put("pageNumber", index);
      summary.put("id", getDocument().getRef()
          .toString());

      all.put("summary", summary);

      List<Object> docs = new ArrayList<Object>();

      for (DocumentModel child : provider.getPage(index)) {

        // FIXME
        if (!"Space".equals(child.getType())) {
          try {
            docs.add(getDocItem(child));
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
      all.put("document", docs);
    } catch (ClientException e) {
      e.printStackTrace();
    }

    return makeJSON(all);
  }

  @POST
  public Object addDocument() throws Exception {
    FileManager fm = Framework.getService(FileManager.class);
    CoreSession session = ctx.getCoreSession();
    FormData form = ctx.getForm();
    Blob blob = form.getFirstBlob();
    if (blob == null) {
        throw new IllegalArgumentException(
                "Could not find any uploaded file");
    }
    fm.createDocumentFromBlob(session, blob, doc.getPathAsString(), true, blob.getFilename());
    return doGet();

  }

  private Map<String, Object> getDocItem(DocumentModel doc) throws Exception {
    Map<String, Object> docItem = new HashMap<String, Object>();
    docItem.put("id", doc.getId());
    docItem.put("name", doc.getName());
    docItem.put("url", getDocumentURL(doc));
    docItem.put("icon", doc.getPropertyValue("common:icon"));
    docItem.put("title", doc.getTitle());
    docItem.put("creator", doc.getPropertyValue("dublincore:creator"));
    docItem.put(
        "modified",
        DATE_FORMAT.format(((GregorianCalendar) doc.getPropertyValue("dublincore:modified")).getTime()));
    if (doc.hasFacet("Folderish")) {
      docItem.put("folderish", "1");
    } else {
      docItem.put("folderish", "0");
    }

    return docItem;
  }

  @Path(value = "{path}")
  public Resource traverse(@PathParam("path") String path) {
    return newDocument(path);
  }

  public DocumentObject newDocument(String path) {
    try {
      PathRef pathRef = new PathRef(doc.getPath()
          .append(path)
          .toString());
      DocumentModel doc = ctx.getCoreSession()
          .getDocument(pathRef);
      return (DocumentObject) newObject("JSONDocument", doc);
    } catch (Exception e) {
      throw WebException.wrap(e);
    }
  }

  private PagedDocumentsProvider getResProviderForDocChildren(
      DocumentRef docRef, CoreSession session) throws ClientException {
    FacetFilter facetFilter = new FacetFilter(FacetNames.HIDDEN_IN_NAVIGATION,
        false);
    LifeCycleFilter lifeCycleFilter = new LifeCycleFilter(
        LifeCycleConstants.DELETED_STATE, false);
    CompoundFilter filter = new CompoundFilter(facetFilter, lifeCycleFilter);
    DocumentModelIterator resultDocsIt = session.getChildrenIterator(docRef,
        null, SecurityConstants.READ, filter);

    return new DocumentsPageProvider(resultDocsIt, PAGE_SIZE);
  }

  protected static String makeJSON(Map<String, Object> all) {
    JSON jsonRes = JSONSerializer.toJSON(all);
    if (jsonRes instanceof JSONObject) {
      JSONObject jsonOb = (JSONObject) jsonRes;
      return jsonOb.toString(2);
    } else if (jsonRes instanceof JSONArray) {
      JSONArray jsonOb = (JSONArray) jsonRes;
      return jsonOb.toString(2);
    } else {
      return null;
    }
  }

  protected static String getDocumentURL(DocumentModel doc) {
    DocumentViewCodecManager dvcm;
    try {
      dvcm = Framework.getService(DocumentViewCodecManager.class);
    } catch (Exception e) {
      return null;
    }
    return dvcm.getUrlFromDocumentView(new DocumentViewImpl(doc), false, null);
  }

}
