/*
* JBoss, Home of Professional Open Source
* Copyright 2005, JBoss Inc., and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/

package org.jboss.remoting.transport.servlet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvocationResponse;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.marshal.MarshalFactory;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.remoting.marshal.http.HTTPMarshaller;
import org.jboss.remoting.marshal.http.HTTPUnMarshaller;
import org.jboss.remoting.transport.http.HTTPMetadataConstants;
import org.jboss.remoting.transport.servlet.ServletServerInvokerMBean;
import org.jboss.remoting.transport.web.WebServerInvoker;
import org.jboss.remoting.transport.web.WebUtil;

/**
 * The servlet based server invoker that receives the original http request
 * from the ServerInvokerServlet.
 *
 * PATCHED to send exceptions back to the client instead of sending an error message which is breaking EJB calls - see line 135
 * To apply that patch you should copy this class over the original one in jboss remoting 1.4.3-GA
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class ServletServerInvoker extends WebServerInvoker implements ServletServerInvokerMBean
{
   public ServletServerInvoker(InvokerLocator locator)
   {
      super(locator);
   }

   public ServletServerInvoker(InvokerLocator locator, Map configuration)
   {
      super(locator, configuration);
   }

   protected String getDefaultDataType()
   {
      return HTTPMarshaller.DATATYPE;
   }

   public String getMBeanObjectName()
   {
      return "jboss.remoting:service=invoker,transport=servlet";
   }

   public void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
   {
      Map metadata = new HashMap();

      Enumeration enumer = request.getHeaderNames();
      while(enumer.hasMoreElements())
      {
         Object obj = enumer.nextElement();
         String headerKey = (String) obj;
         String headerValue = request.getHeader(headerKey);
         metadata.put(headerKey, headerValue);
      }

      Map urlParams = request.getParameterMap();
      metadata.putAll(urlParams);

      String requestContentType = request.getContentType();


      try
      {
         Object invocationResponse = null;

         ServletInputStream inputStream = request.getInputStream();
         UnMarshaller unmarshaller = MarshalFactory.getUnMarshaller(HTTPUnMarshaller.DATATYPE, getSerializationType());
         Object obj = unmarshaller.read(inputStream, metadata);
         inputStream.close();

         InvocationRequest invocationRequest = null;

         if(obj instanceof InvocationRequest)
         {
            invocationRequest = (InvocationRequest) obj;
         }
         else
         {
            if(WebUtil.isBinary(requestContentType))
            {
               invocationRequest = getInvocationRequest(metadata, obj);
            }
            else
            {
               invocationRequest = createNewInvocationRequest(metadata, obj);
            }
         }

         try
         {
            // call transport on the subclass, get the result to handback
            invocationResponse = invoke(invocationRequest);
         }
         catch(Throwable ex)
         {
            log.debug("Error thrown calling invoke on server invoker.", ex);
            invocationResponse = new InvocationResponse(invocationRequest.getSessionId(), ex, true, invocationRequest.getReturnPayload());
         }

         if(invocationResponse != null)
         {
            response.setContentType(requestContentType);
            int iContentLength = getContentLength(invocationResponse);
            response.setContentLength(iContentLength);
            ServletOutputStream outputStream = response.getOutputStream();
            Marshaller marshaller = MarshalFactory.getMarshaller(HTTPMarshaller.DATATYPE, getSerializationType());
            marshaller.write(invocationResponse, outputStream);
            outputStream.close();
         }

      }
      catch(ClassNotFoundException e)
      {
         log.error("Error processing invocation request due to class not being found.", e);
         response.sendError(500, "Error processing invocation request due to class not being found.  " + e.getMessage());

      }

   }

   public byte[] processRequest(HttpServletRequest request, byte[] requestByte,
                                HttpServletResponse response)
         throws ServletException, IOException
   {
      byte[] retval = null;

      Map metadata = new HashMap();

      Enumeration enumer = request.getHeaderNames();
      while(enumer.hasMoreElements())
      {
         Object obj = enumer.nextElement();
         String headerKey = (String) obj;
         String headerValue = request.getHeader(headerKey);
         metadata.put(headerKey, headerValue);
      }

      Map urlParams = request.getParameterMap();
      metadata.putAll(urlParams);

      metadata.put(HTTPMetadataConstants.METHODTYPE, request.getMethod());
      metadata.put(HTTPMetadataConstants.PATH, request.getPathTranslated());

      String requestContentType = request.getContentType();


      try
      {
         Object invocationResponse = null;

         ServletInputStream inputStream = request.getInputStream();
         UnMarshaller unmarshaller = getUnMarshaller();
         Object obj = unmarshaller.read(new ByteArrayInputStream(requestByte), metadata);
         inputStream.close();

         boolean isError = false;
         InvocationRequest invocationRequest = null;

         if(obj instanceof InvocationRequest)
         {
            invocationRequest = (InvocationRequest) obj;
         }
         else
         {
            if(WebUtil.isBinary(requestContentType))
            {
               invocationRequest = getInvocationRequest(metadata, obj);
            }
            else
            {
               invocationRequest = createNewInvocationRequest(metadata, obj);
            }
         }

         try
         {
            // call transport on the subclass, get the result to handback
            invocationResponse = invoke(invocationRequest);
         }
         catch(Throwable ex)
         {
            log.debug("Error thrown calling invoke on server invoker.", ex);
            invocationResponse = new InvocationResponse(invocationRequest.getSessionId(), ex, true, invocationRequest.getReturnPayload());
            isError = true;
         }

         //Start with response code of 204 (no content), then if is a return from handler, change to 200 (ok)
         int status = 204;
         if(invocationResponse != null)
         {
            if(isError)
            {
                status = 200;
               //response.sendError(500, "Error occurred processing invocation request. ");
            }
            else
            {
               status = 200;
            }
         }

         // extract response code/message if exists
         Map responseMap = invocationRequest.getReturnPayload();
         if(responseMap != null)
         {
            Integer handlerStatus = (Integer) responseMap.get(HTTPMetadataConstants.RESPONSE_CODE);
            if(handlerStatus != null)
            {
               status = handlerStatus.intValue();
            }
         }

         // can't set message anymore as is depricated
         response.setStatus(status);

         if(invocationResponse != null)
         {
            String responseContentType = invocationResponse == null ? requestContentType : WebUtil.getContentType(invocationResponse);
            response.setContentType(responseContentType);
            //int iContentLength = getContentLength(invocationResponse);
            //response.setContentLength(iContentLength);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Marshaller marshaller = getMarshaller();
            marshaller.write(invocationResponse, outputStream);
            retval = outputStream.toByteArray();
            response.setContentLength(retval.length);
         }

      }
      catch(ClassNotFoundException e)
      {
         log.error("Error processing invocation request due to class not being found.", e);
         response.sendError(500, "Error processing invocation request due to class not being found.  " + e.getMessage());
      }

      return retval;
   }
}