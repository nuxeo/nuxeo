package org.nuxeo.ecm.webapp.liveedit;

import static org.jboss.seam.ScopeType.SESSION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

@Scope(SESSION)
@Name("liveEditClientConfig")
@Install(precedence = FRAMEWORK)
public class LiveEditClientConfig implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(LiveEditClientConfig.class);

    protected Boolean clientHasOOLiveEditInstalled=null;
    protected Boolean clientHasMSOLiveEditInstalled=null;
    protected Boolean clientHasLiveEditInstalled=null;

    public static final String LE_MIME_TYPE="application/x-nuxeo-liveedit";
    public static final String OOLE_MIME_STYPE="oo";
    public static final String MSOLE_MIME_STYPE="mso";

    protected void detectLiveEditClientConfig()
    {
        clientHasOOLiveEditInstalled=false;
        clientHasMSOLiveEditInstalled=false;
        clientHasLiveEditInstalled=false;

        FacesContext fContext=FacesContext.getCurrentInstance();
        if (fContext==null)
        {
            log.error("unable to fetch facesContext, can not detect liveEdit client config");
        }
        else
        {
            Map<String,String> headers = fContext.getExternalContext().getRequestHeaderMap();
            String accept = headers.get("Accept");
            String[] accepted = accept.split(",");

            for (int i=0; i<accepted.length;i++)
            {
                if (accepted[i].startsWith(LE_MIME_TYPE))
                {
                    clientHasLiveEditInstalled=true;
                    String[] subTypes = accepted[i].split(";");

                    for (int j=0;j<subTypes.length;j++)
                    {
                        if (subTypes[j].equalsIgnoreCase(MSOLE_MIME_STYPE))
                        {
                            clientHasMSOLiveEditInstalled=true;
                        }
                        else if (subTypes[j].equalsIgnoreCase(OOLE_MIME_STYPE))
                        {
                            clientHasOOLiveEditInstalled=true;
                        }
                    }
                }
            }
        }
    }

    public boolean isLiveEditInstalled()
    {
        if (isMSOLiveEditInstalled() || isOOLiveEditInstalled())
            return true;
        else
            return clientHasLiveEditInstalled;
    }

    public boolean isOOLiveEditInstalled()
    {
        if (clientHasOOLiveEditInstalled==null)
            detectLiveEditClientConfig();

        return clientHasOOLiveEditInstalled;
    }

    public boolean isMSOLiveEditInstalled()
    {
        if (clientHasMSOLiveEditInstalled==null)
            detectLiveEditClientConfig();

        return clientHasMSOLiveEditInstalled;
    }

}
