package org.nuxeo.elasticsearch.audit.pageprovider;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.audit.api.document.AdditionalDocumentAuditParams;
import org.nuxeo.ecm.platform.audit.api.document.DocumentAuditHelper;

public class ESDocumentHistoryPageProvider extends ESAuditPageProvider {

    private static final long serialVersionUID = 1L;

    protected Log log = LogFactory.getLog(ESDocumentHistoryPageProvider.class);

    protected Object[] newParams;

    protected static String singleQuery = "            {\n"
            + "                \"bool\" : {\n"
            + "                  \"must\" : {\n"
            + "                    \"match\" : {\n"
            + "                      \"docUUID\" : {\n"
            + "                        \"query\" : \"?\",\n"
            + "                        \"type\" : \"boolean\"\n"
            + "                      }\n" + "                    }\n"
            + "                  }\n" + "                }\n"
            + "              }          \n" + "";

    protected static String complexQuery = "{\n" + "    \"filtered\" : {\n"
            + "        \"query\" : {\n" + "            \"match_all\" : { }\n"
            + "        },\n" + "        \"filter\" : {\n"
            + "            \"or\" : [\n" + "                {\n"
            + "                    \"term\" : { \"docUUID\" : \"?\" }\n"
            + "                },\n" + "                {\n"
            + "                    \"bool\" : {\n"
            + "                      \"must\" : [{\n"
            + "                        \"term\" : { \"docUUID\" : \"?\" }\n"
            + "                      },\n" + "                      {\n"
            + "                        \"range\" : {\n"
            + "                          \"eventDate\" :  { \"lte\" : \"?\"}\n"
            + "                        }\n" + "                      }]\n"
            + "                  }\n" + "                }\n"
            + "            ]\n" + "        }\n" + "    }\n" + "}\n" + "\n" + "";

    @Override
    protected String getFixedPart() {
        if (getParameters().length == 3) {
            return complexQuery;
        } else {
            return singleQuery;
        }
    }

    @Override
    public List<SortInfo> getSortInfos() {

        List<SortInfo> sort = super.getSortInfos();
        if (sort == null || sort.size() == 0) {
            sort = new ArrayList<SortInfo>();
            sort.add(new SortInfo("eventDate", true));
            sort.add(new SortInfo("id", true));
        }
        return sort;
    }

    @Override
    public Object[] getParameters() {
        if (newParams == null) {
            Object[] params = super.getParameters();
            if (params.length != 1) {
                log.error(this.getClass().getSimpleName()
                        + " Expect only one parameter the document uuid, unexpected behavior may occur");
            }
            CoreSession session = null;
            String uuid = null;
            if (params[0] instanceof DocumentModel) {
                DocumentModel doc = (DocumentModel) params[0];
                uuid = doc.getId();
                session = doc.getCoreSession();
            } else {
                session = (CoreSession) getProperties().get(
                        CORE_SESSION_PROPERTY);
                uuid = params[0].toString();
            }
            if (session != null) {
                try {
                    AdditionalDocumentAuditParams additionalParams = DocumentAuditHelper.getAuditParamsForUUID(
                            uuid, session);
                    if (additionalParams != null) {
                        newParams = new Object[] { uuid,
                                additionalParams.getTargetUUID(),
                                additionalParams.getMaxDate() };
                    } else {
                        newParams = new Object[] { uuid };
                    }
                } catch (Exception e) {
                    log.error(
                            "Error while fetching additional parameters for audit query",
                            e);
                }
            } else {
                log.warn("No core session found: cannot compute all info to get complete audit entries");
                return params;
            }
        }
        return newParams;
    }

    @Override
    public boolean hasChangedParameters(Object[] parameters) {
        return getParametersChanged(this.parameters, parameters);
    }

}
