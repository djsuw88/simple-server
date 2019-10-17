/*
 * SessionDispatcher.java February 2014
 *
 * Copyright (C) 2014, Niall Gallagher <niallg@users.sf.net>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or 
 * implied. See the License for the specific language governing 
 * permissions and limitations under the License.
 */

package org.simpleframework.http.socket.service;

import static org.simpleframework.http.socket.service.ServiceEvent.DISPATCH_SOCKET;
import static org.simpleframework.http.socket.service.ServiceEvent.ERROR;
import static org.simpleframework.http.socket.service.ServiceEvent.TERMINATE_SOCKET;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.socket.Session;
import org.simpleframework.transport.Channel;
import org.simpleframework.transport.trace.Trace;

/**
 * The <code>SessionDispatcher</code> object is used to perform the
 * opening handshake for a WebSocket session. Once the session has been
 * established it is connected to a <code>Service</code> where frames
 * can be sent and received. If for any reason the handshake fails
 * this will terminated the connection with a HTTP 400 response.
 * 
 * @author Niall Gallagher
 */
class SessionDispatcher {

   /**
    * This is used to create the session for the WebSocket.
    */
   private final SessionBuilder builder;
   
   /**
    * This is used to select the service to dispatch to.
    */
   private final Router router;
   
   /**
    * Constructor for the <code>SessionDispatcher</code> object. The
    * dispatcher created will dispatch WebSocket sessions to a service
    * using the provided <code>Router</code> instance. 
    * 
    * @param builder this is used to build the WebSocket session
    * @param router this is used to select the service 
    */
   public SessionDispatcher(SessionBuilder builder, Router router) {
      this.builder = builder;
      this.router = router;
   }
   
   /**
    * This method is used to create a dispatch a <code>Session</code> to
    * a specific service selected by a router. If the session initiating
    * handshake fails for any reason this will close the underlying TCP
    * connection and send a HTTP 400 response back to the client. 
    * 
    * @param request this is the session initiating request
    * @param response this is the session initiating response
    */
   public void dispatch(Request request, Response response) {
      Channel channel = request.getChannel();
      Trace trace = channel.getTrace();
      
      try {
         Service service = router.route(request, response);
         Session session = builder.create(request, response, service);      
        
         trace.trace(DISPATCH_SOCKET);
      } catch(Exception cause) {
         trace.trace(ERROR, cause);
         terminate(request, response);
      }
   }
   
   /**
    * This method is used to terminate the connection and commit the
    * response. Terminating the session before it has been dispatched
    * is done when there is a protocol or an unexpected I/O error with
    * the underlying TCP channel. 
    * 
    * @param request this is the session initiating request
    * @param response this is the session initiating response
    */
   public void terminate(Request request, Response response) {
      Channel channel = request.getChannel();
      Trace trace = channel.getTrace();
      
      try {
         response.close();
         channel.close();
         trace.trace(TERMINATE_SOCKET);
      } catch(Exception cause) {
         trace.trace(ERROR, cause);
      }
   }
}
