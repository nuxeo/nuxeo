package org.nuxeo.opensocial.spaces.webobject;

import java.util.Calendar;
import java.util.StringTokenizer;

import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;

@WebAdapter(name = "verSpace", type = "VersionableSpaceService", targetType = "Space")
public class VersionableSpaceService extends DefaultAdapter {

    /**
       * Space creation in the current univers
       *
       * @return
       */
      @PUT
      public Response createVersion() {
        try {
          Calendar d = getDatePublication(ctx.getForm());
          if (d.compareTo(Calendar.getInstance()) == 1) {
              Space space = (Space) getTarget();
              Space newSpace = space.createVersion(d);
              space.save();


            return Response.ok()
                .entity(
                        //TODO : vérifier ce que ça fait
                    ctx.getModulePath() + "/") //+ univers.getName() + "/"
                        //+ createDoc.getName())
                .build();
          } else {
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .build();
          }
        } catch (Exception e) {
          throw WebException.wrap(e);
        }

      }

      /**
       * update space
       *
       * @return
       */
      @POST
      public Response save() {
        try {
          Calendar d = getDatePublication(ctx.getForm());
          if (d.compareTo(Calendar.getInstance()) == 1) {
            updatePubicationDate(getDatePublication(ctx.getForm()));
            return Response.ok()
                .entity("OK")
                .build();
          } else {
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .build();
          }
        } catch (Exception e) {
          throw WebException.wrap(e);
        }

      }

      public Calendar getDatePublication(FormData formData) {

        StringTokenizer st = new StringTokenizer(formData.getString("dc:valid"),
            "/");
        Calendar date = Calendar.getInstance();
        date.set(Calendar.DAY_OF_MONTH, Integer.parseInt(st.nextToken()));
        date.set(Calendar.MONTH, Integer.parseInt(st.nextToken()) - 1);
        date.set(Calendar.YEAR, Integer.parseInt(st.nextToken()));
        date.set(Calendar.HOUR_OF_DAY,
            Integer.parseInt(formData.getString("hours")));
        date.set(Calendar.MINUTE, Integer.parseInt(formData.getString("minutes")));
        return date;
      }

      /**
       * update space
       *
       * @return
       */
      @Path("publish")
      public Response publishNow() {
        try {
          updatePubicationDate(Calendar.getInstance());
          return redirect(this.path);

//TODO: Vérifier que ça marche
//        		  ctx.getModulePath() + "/" + univers.getName() + "/"
//              + ctx.getForm()
//                  .getString("actualVersionName"));
        } catch (ClientException e) {
          throw WebException.wrap(e);
        }
      }

      public void updatePubicationDate(Calendar cal) throws ClientException {
        try {
          Space space = (Space) getTarget();
          space.setDatePublication(cal);
          space.save();
        } catch (PropertyException e) {
          throw WebException.wrap(e);
        }
      }



}
