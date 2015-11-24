/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Stéphane Fourrier
 */

package org.nuxeo.opensocial.container.server.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Singleton;

@Singleton
public class BlobServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // CoreSession session = CoreSessionHelper.getSession();
        //
        // ServletRequestContext context = new ServletRequestContext(request);
        // boolean isMultipart = FileUpload.isMultipartContent(context);
        //
        // if (!isMultipart) {
        // throw new ServletException("Request should be multipart");
        // }
        //
        // FileUpload upload = new FileUpload(new FileItemFactory() {
        // public FileItem createItem(String arg0, String arg1, boolean arg2,
        // String arg3) {
        // // TODO Auto-generated method stub
        // return null;
        // }
        // });
        //
        // String title = "";
        // String docId = "";
        // File f = null;
        //
        // response.setContentType("text/html");
        // try {
        // List items = upload.parseRequest(context);
        // Iterator itr = items.iterator();
        // while (itr.hasNext()) {
        // FileItem item = (FileItem) itr.next();
        // if (item.isFormField()) {
        // String fieldName = item.getFieldName();
        //
        // if (fieldName.equals("title"))
        // title = item.getString();
        // else if (fieldName.equals("docid"))
        // docId = item.getString();
        //
        // System.out.println(item.getString());
        // } else {
        // f = new File(item.getName());
        // }
        // }
        //
        // if (f == null || "".equals(docId)) {
        // throw new ServletException(
        // "Unable to get file or id in the request");
        // }
        //
        // DocumentModel doc = session.getDocument(new IdRef(docId));
        // if (!"".equals(title)) {
        // doc.setPropertyValue("dc:title", title);
        // }
        //
        // Blob blob = new FileBlob(f);
        // doc.setProperty("file", "content", blob);
        //
        // doc = session.saveDocument(doc);
        // session.save();
        // } catch (FileUploadException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // } catch (ClientException e) {
        // throw new ServletException(e.getMessage(), e);
        // }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // CoreSession session = CoreSessionHelper.getSession();
        // String docId = req.getParameter("docid");
        //
        // try {
        // IdRef docRef = new IdRef(docId);
        // if (!session.exists(docRef)) {
        // throw new ServletException("Doc not found");
        // }
        //
        // DocumentModel doc = session.getDocument(docRef);
        // Blob blob = (Blob) doc.getProperty("file", "content");
        //
        //
        // InputStream in = blob.getStream();
        // OutputStream out = resp.getOutputStream();
        // resp.setContentLength((int) blob.getLength());
        // resp.setContentType(blob.getMimeType());
        //
        // byte[] buf = new byte[1024];
        // int count = 0;
        // while ((count = in.read(buf)) >= 0) {
        // out.write(buf, 0, count);
        // }
        // in.close();
        // out.close();
        //
        // } catch (ClientException e) {
        // throw new ServletException(e.getMessage(), e);
        // }

    }
}
