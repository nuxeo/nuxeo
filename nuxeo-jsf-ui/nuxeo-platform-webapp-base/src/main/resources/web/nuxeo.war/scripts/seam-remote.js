// Copied from Seam 2.2.1
// to get the bugfix for JBSEAM-3721

// Init base-level objects
var Seam = new Object();
Seam.Remoting = new Object();
Seam.Component = new Object();
Seam.pageContext = new Object();

// Components registered here
Seam.Component.components = new Array();
Seam.Component.instances = new Array();

//Nuxeo specific
Seam.Remoting.contextPath = '/nuxeo';

Seam.Component.newInstance = function(name)
{
  for (var i = 0; i < Seam.Component.components.length; i++)
  {
    if (Seam.Component.components[i].__name == name)
      return new Seam.Component.components[i];
  }
}

Seam.Component.getInstance = function(name)
{
  for (var i = 0; i < Seam.Component.components.length; i++)
  {
    if (Seam.Component.components[i].__name == name)
    {
      if (Seam.Component.components[i].__instance == null)
        Seam.Component.components[i].__instance = new Seam.Component.components[i]();
      return Seam.Component.components[i].__instance;
    }
  }
  return null;
}

Seam.Component.getComponentType = function(obj)
{
  for (var i = 0; i < Seam.Component.components.length; i++)
  {
    if (obj instanceof Seam.Component.components[i])
      return Seam.Component.components[i];
  }
  return null;
}

Seam.Component.getComponentName = function(obj)
{
  var componentType = Seam.Component.getComponentType(obj);
  return componentType ? componentType.__name : null;
}

Seam.Component.register = function(component)
{
  for (var i = 0; i < Seam.Component.components.length; i++)
  {
    if (Seam.Component.components[i].__name == component.__name)
    {
      // Replace the existing component with the new one
      Seam.Component.components[i] = component;
      return;
    }
  }
  Seam.Component.components.push(component);
  component.__instance = null;
}

Seam.Component.isRegistered = function(name)
{
  for (var i = 0; i < Seam.Component.components.length; i++)
  {
    if (Seam.Component.components[i].__name == name)
      return true;
  }
  return false;
}

Seam.Component.getMetadata = function(obj)
{
  for (var i = 0; i < Seam.Component.components.length; i++)
  {
    if (obj instanceof Seam.Component.components[i])
      return Seam.Component.components[i].__metadata;
  }
  return null;
}

Seam.Remoting.extractEncodedSessionId = function(url)
{
  var sessionId = null;
  if (url.indexOf(';jsessionid=') >= 0)
  {
    var qpos = url.indexOf('?');
    sessionId = url.substring(url.indexOf(';jsessionid=') + 12, qpos >= 0 ? qpos : url.length);
  }
  return sessionId;
}

Seam.Remoting.PATH_EXECUTE = "/execute";
Seam.Remoting.PATH_SUBSCRIPTION = "/subscription";
Seam.Remoting.PATH_POLL = "/poll";

Seam.Remoting.encodedSessionId = Seam.Remoting.extractEncodedSessionId(window.location.href);

// Type declarations will live in this namespace
Seam.Remoting.type = new Object();

// Types are registered in an array
Seam.Remoting.types = new Array();

Seam.Remoting.debug = false;
Seam.Remoting.debugWindow = null;

Seam.Remoting.setDebug = function(val)
{
  Seam.Remoting.debug = val;
}

// Log a message to a popup debug window
Seam.Remoting.log = function(msg)
{
  if (!Seam.Remoting.debug)
    return;

  if (!Seam.Remoting.debugWindow || Seam.Remoting.debugWindow.document == null)
  {
    var attr = "left=400,top=400,resizable=yes,scrollbars=yes,width=400,height=400";
    Seam.Remoting.debugWindow = window.open("", "__seamDebugWindow", attr);
    if (Seam.Remoting.debugWindow)
    {
      Seam.Remoting.debugWindow.document.write("<html><head><title>Seam Debug Window</title></head><body></body></html>");
      var bodyTag = Seam.Remoting.debugWindow.document.getElementsByTagName("body").item(0);
      bodyTag.style.fontFamily = "arial";
      bodyTag.style.fontSize = "8pt";
    }
  }

  if (Seam.Remoting.debugWindow)
  {
    msg = msg.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
    Seam.Remoting.debugWindow.document.write("<pre>" + (new Date()) + ": " + msg + "</pre><br/>");
  }
}

Seam.Remoting.createNamespace = function(namespace)
{
  var parts = namespace.split(".");
  var base = Seam.Remoting.type;

  for(var i = 0; i < parts.length; i++)
  {
    if (typeof base[parts[i]] == "undefined")
      base[parts[i]] = new Object();
    base = base[parts[i]];
  }
}

Seam.Remoting.__Context = function()
{
  this.conversationId = null;

  Seam.Remoting.__Context.prototype.setConversationId = function(conversationId)
  {
    this.conversationId = conversationId;
  }

  Seam.Remoting.__Context.prototype.getConversationId = function()
  {
    return this.conversationId;
  }
}

Seam.Remoting.Exception = function(msg)
{
  this.message = msg;

  Seam.Remoting.Exception.prototype.getMessage = function()
  {
    return this.message;
  }
}

Seam.Remoting.context = new Seam.Remoting.__Context();

Seam.Remoting.getContext = function()
{
  return Seam.Remoting.context;
}

Seam.Remoting.Map = function()
{
  this.elements = new Array();

  Seam.Remoting.Map.prototype.size = function()
  {
    return this.elements.length;
  }

  Seam.Remoting.Map.prototype.isEmpty = function()
  {
    return this.elements.length == 0;
  }

  Seam.Remoting.Map.prototype.keySet = function()
  {
    var keySet = new Array();
    for (var i = 0; i < this.elements.length; i++)
      keySet[keySet.length] = this.elements[i].key;
    return keySet;
  }

  Seam.Remoting.Map.prototype.values = function()
  {
    var values = new Array();
    for (var i = 0; i < this.elements.length; i++)
      values[values.length] = this.elements[i].value;
    return values;
  }

  Seam.Remoting.Map.prototype.get = function(key)
  {
    for (var i = 0; i < this.elements.length; i++)
    {
      if (this.elements[i].key == key)
        return this.elements[i].value;
    }
    return null;
  }

  Seam.Remoting.Map.prototype.put = function(key, value)
  {
    for (var i = 0; i < this.elements.length; i++)
    {
      if (this.elements[i].key == key)
      {
        this.elements[i].value = value;
        return;
      }
    }
    this.elements.push({key:key,value:value});
  }

  Seam.Remoting.Map.prototype.remove = function(key)
  {
    for (var i = 0; i < this.elements.length; i++)
    {
      if (this.elements[i].key == key)
        this.elements.splice(i, 1);
    }
  }

  Seam.Remoting.Map.prototype.contains = function(key)
  {
    for (var i = 0; i < this.elements.length; i++)
    {
      if (this.elements[i].key == key)
        return true;
    }
    return false;
  }
}

Seam.Remoting.registerType = function(type)
{
  for (var i = 0; i < Seam.Remoting.types.length; i++)
  {
    if (Seam.Remoting.types[i].__name == type.__name)
    {
      Seam.Remoting.types[i] = type;
      return;
    }
  }
  Seam.Remoting.types.push(type);
}

Seam.Remoting.createType = function(name)
{
  for (var i = 0; i < Seam.Remoting.types.length; i++)
  {
    if (Seam.Remoting.types[i].__name == name)
      return new Seam.Remoting.types[i];
  }
}

Seam.Remoting.getType = function(obj)
{
  for (var i = 0; i < Seam.Remoting.types.length; i++)
  {
    if (obj instanceof Seam.Remoting.types[i])
      return Seam.Remoting.types[i];
  }
  return null;
}

Seam.Remoting.getTypeName = function(obj)
{
  var type = Seam.Remoting.getType(obj);
  return type ? type.__name : null;
}

Seam.Remoting.getMetadata = function(obj)
{
  for (var i = 0; i < Seam.Remoting.types.length; i++)
  {
    if (obj instanceof Seam.Remoting.types[i])
      return Seam.Remoting.types[i].__metadata;
  }
  return null;
}

Seam.Remoting.serializeValue = function(value, type, refs)
{
  if (value == null)
    return "<null/>";
  else if (type)
  {
    switch (type) {
      // Boolean
      case "bool": return "<bool>" + (value ? "true" : "false") + "</bool>";

      // Numerical types
      case "number": return "<number>" + value + "</number>";

      // Date
      case "date": return Seam.Remoting.serializeDate(value);
      // Beans
      case "bean": return Seam.Remoting.getTypeRef(value, refs);

      // Collections
      case "bag": return Seam.Remoting.serializeBag(value, refs);
      case "map": return Seam.Remoting.serializeMap(value, refs);

      default: return "<str>" + encodeURIComponent(value) + "</str>";
    }
  }
  else // We don't know the type.. try to guess
  {
    switch (typeof(value)) {
      case "number":
        return "<number>" + value + "</number>";
      case "boolean":
        return "<bool>" + (value ? "true" : "false") + "</bool>";
      case "object":
        if (value instanceof Array)
          return Seam.Remoting.serializeBag(value, refs);
        else if (value instanceof Date)
          return Seam.Remoting.serializeDate(value);
        else if (value instanceof Seam.Remoting.Map)
          return Seam.Remoting.serializeMap(value, refs);
        else
          return Seam.Remoting.getTypeRef(value, refs);
      default:
        return "<str>" + encodeURIComponent(value) + "</str>"; // Default to String
    }
  }
}

Seam.Remoting.serializeBag = function(value, refs)
{
  var data = "<bag>";

  for (var i = 0; i < value.length; i++)
  {
    data += "<element>";
    data += Seam.Remoting.serializeValue(value[i], null, refs);
    data += "</element>";
  }

  data += "</bag>";
  return data;
}

Seam.Remoting.serializeMap = function(value, refs)
{
  var data = "<map>";

  var keyset = value.keySet();
  for (var i = 0; i < keyset.length; i++)
  {
    data += "<element><k>";
    data += Seam.Remoting.serializeValue(keyset[i], null, refs);
    data += "</k><v>";
    data += Seam.Remoting.serializeValue(value.get(keyset[i]), null, refs);
    data += "</v></element>";
  }

  data += "</map>";
  return data;
}

Seam.Remoting.serializeDate = function(value)
{
  var zeroPad = function(val, digits) { while (("" + val).length < digits) val = "0" + val; return val; };

  var data = "<date>";
  data += value.getFullYear();
  data += zeroPad(value.getMonth() + 1, 2);
  data += zeroPad(value.getDate(), 2);
  data += zeroPad(value.getHours(), 2);
  data += zeroPad(value.getMinutes(), 2);
  data += zeroPad(value.getSeconds(), 2);
  data += zeroPad(value.getMilliseconds(), 3);
  data += "</date>";
  return data;
}

Seam.Remoting.getTypeRef = function(obj, refs)
{
  var refId = -1;

  for (var i = 0; i < refs.length; i++)
  {
    if (refs[i] == obj)
    {
      refId = i;
      break;
    }
  }

  if (refId == -1)
  {
    refId = refs.length;
    refs[refId] = obj;
  }

  return "<ref id=\"" + refId + "\"/>";
}

Seam.Remoting.serializeType = function(obj, refs)
{
  var data = "<bean type=\"";

  var objType = Seam.Component.getComponentType(obj);
  var isComponent = objType != null;

  if (!isComponent)
    objType = Seam.Remoting.getType(obj);

  if (!objType)
  {
    alert("Unknown Type error.");
    return null;
  }

  data += objType.__name;
  data += "\">\n";

  var meta = isComponent ? Seam.Component.getMetadata(obj) : Seam.Remoting.getMetadata(obj);
  for (var i = 0; i < meta.length; i++)
  {
    data += "<member name=\"";
    data += meta[i].field;
    data += "\">";
    data += Seam.Remoting.serializeValue(obj[meta[i].field], meta[i].type, refs);
    data += "</member>\n";
  }

  data += "</bean>";

  return data;
}

Seam.Remoting.__callId = 0;

// eval() disabled until security issues resolved.

//Seam.Remoting.eval = function(expression, callback)
//{
//  var callId = "" + Seam.Remoting.__callId++;
//  var data = "<eval expr=\"";
//  data += expression;
//  data += "\" id=\"";
//  data += callId;
//  data += "\"/>";
//  var call = {data: data, id: callId, callback: callback};

//  var envelope = Seam.Remoting.createEnvelope(Seam.Remoting.createHeader(), data);
//  Seam.Remoting.pendingCalls.put(call.id, call);

//  call.asyncReq = Seam.Remoting.sendAjaxRequest(envelope, Seam.Remoting.PATH_EXECUTE, Seam.Remoting.processResponse, false);
//}

Seam.Remoting.createCall = function(component, methodName, params, callback, exceptionHandler)
{
  var callId = "" + Seam.Remoting.__callId++;
  if (!callback)
    callback = component.__callback[methodName];

  var data = "<call component=\"";
  data += Seam.Component.getComponentType(component).__name;
  data += "\" method=\"";
  data += methodName;
  data += "\" id=\"";
  data += callId;
  data += "\">\n";

  // Add parameters
  data += "<params>";

  var refs = new Array();

  for (var i = 0; i < params.length; i++)
  {
    data += "<param>";
    data += Seam.Remoting.serializeValue(params[i], null, refs);
    data += "</param>";
  }

  data += "</params>";

  // Add refs
  data += "<refs>";
  for (var i = 0; i < refs.length; i++)
  {
    data += "<ref id=\"" + i + "\">";
    data += Seam.Remoting.serializeType(refs[i], refs);
    data += "</ref>";
  }
  data += "</refs>";

  data += "</call>";

  return {data: data, id: callId, callback: callback, exceptionHandler: exceptionHandler};
}

Seam.Remoting.createHeader = function()
{
  var header = "";

  header += "<context>";
  if (Seam.Remoting.getContext().getConversationId())
  {
    header += "<conversationId>";
    header += Seam.Remoting.getContext().getConversationId();
    header += "</conversationId>";
  }
  header += "</context>";

  return header;
}

Seam.Remoting.createEnvelope = function(header, body)
{
  var data = "<envelope>";

  if (header)
  {
    data += "<header>";
    data += header;
    data += "</header>";
  }

  if (body)
  {
    data += "<body>";
    data += body;
    data += "</body>";
  }

  data += "</envelope>";

  return data;
}

Seam.Remoting.pendingCalls = new Seam.Remoting.Map();
Seam.Remoting.inBatch = false;
Seam.Remoting.batchedCalls = new Array();

Seam.Remoting.startBatch = function()
{
  Seam.Remoting.inBatch = true;
  Seam.Remoting.batchedCalls.length = 0;
}

Seam.Remoting.executeBatch = function()
{
  if (!Seam.Remoting.inBatch)
    return;

  var data = "";
  for (var i = 0; i < Seam.Remoting.batchedCalls.length; i++)
  {
    Seam.Remoting.pendingCalls.put(Seam.Remoting.batchedCalls[i].id, Seam.Remoting.batchedCalls[i]);
    data += Seam.Remoting.batchedCalls[i].data;
  }

  var envelope = Seam.Remoting.createEnvelope(Seam.Remoting.createHeader(), data);
  Seam.Remoting.batchAsyncReq = Seam.Remoting.sendAjaxRequest(envelope, Seam.Remoting.PATH_EXECUTE, Seam.Remoting.processResponse, false);
  Seam.Remoting.inBatch = false;
}

Seam.Remoting.cancelBatch = function()
{
  Seam.Remoting.inBatch = false;
  for (var i = 0; i < Seam.Remoting.batchedCalls.length; i++)
    Seam.Remoting.pendingCalls.remove(Seam.Remoting.batchedCalls[i].id);
}

Seam.Remoting.cancelCall = function(callId)
{
  var call = Seam.Remoting.pendingCalls.get(callId);
  Seam.Remoting.pendingCalls.remove(callId);
  if (call && call.asyncReq)
  {
    if (Seam.Remoting.pendingCalls.isEmpty())
      Seam.Remoting.hideLoadingMessage();
    window.setTimeout(function() {
      call.asyncReq.onreadystatechange = function() {};
    }, 0);
    call.asyncReq.abort();
  }
}

Seam.Remoting.execute = function(component, methodName, params, callback, exceptionHandler)
{
  var call = Seam.Remoting.createCall(component, methodName, params, callback, exceptionHandler);

  if (Seam.Remoting.inBatch)
  {
    Seam.Remoting.batchedCalls[Seam.Remoting.batchedCalls.length] = call;
  }
  else
  {
    // Marshal the request
    var envelope = Seam.Remoting.createEnvelope(Seam.Remoting.createHeader(), call.data);
    Seam.Remoting.pendingCalls.put(call.id, call);
    Seam.Remoting.sendAjaxRequest(envelope, Seam.Remoting.PATH_EXECUTE, Seam.Remoting.processResponse, false);
  }

  return call;
}

Seam.Remoting.sendAjaxRequest = function(envelope, path, callback, silent)
{
  Seam.Remoting.log("Request packet:\n" + envelope);

  if (!silent)
    Seam.Remoting.displayLoadingMessage();

  var asyncReq;

  if (window.XMLHttpRequest)
  {
    asyncReq = new XMLHttpRequest();
    if (asyncReq.overrideMimeType)
      asyncReq.overrideMimeType('text/xml');
  }
  else
  {
    asyncReq = new ActiveXObject("Microsoft.XMLHTTP");
  }

  asyncReq.onreadystatechange = function()
  {
    if (asyncReq.readyState == 4)
    {
      var inScope = typeof(Seam) == "undefined" ? false : true;

      if (inScope) Seam.Remoting.hideLoadingMessage();

      if (asyncReq.status == 200)
      {
        // We do this to avoid a memory leak
        window.setTimeout(function() {
          asyncReq.onreadystatechange = function() {};
        }, 0);

        if (inScope) Seam.Remoting.log("Response packet:\n" + asyncReq.responseText);

        if (callback)
        {
          // The following code deals with a Firefox security issue.  It reparses the XML
          // response if accessing the documentElement throws an exception
          try
          {
            asyncReq.responseXML.documentElement;
            //Seam.Remoting.processResponse(asyncReq.responseXML);
      callback(asyncReq.responseXML);
          }
          catch (ex)
          {
             try
             {
               // Try it the IE way first...
               var doc = new ActiveXObject("Microsoft.XMLDOM");
               doc.async = "false";
               doc.loadXML(asyncReq.responseText);
               callback(doc);
             }
             catch (e)
             {
               // If that fails, use standards
               var parser = new DOMParser();
               //Seam.Remoting.processResponse(parser.parseFromString(asyncReq.responseText, "text/xml"));
         callback(parser.parseFromString(asyncReq.responseText, "text/xml"));
             }
          }
        }
      }
      else
      {
        Seam.Remoting.displayError(asyncReq.status);
      }
    }
  }

  if (Seam.Remoting.encodedSessionId)
  {
    path += ';jsessionid=' + Seam.Remoting.encodedSessionId;
  }

  asyncReq.open("POST", Seam.Remoting.resourcePath + path, true);
  asyncReq.send(envelope);
}

Seam.Remoting.displayError = function(code)
{
  alert("There was an error processing your request.  Error code: " + code);
}

Seam.Remoting.setCallback = function(component, methodName, callback)
{
  component.__callback[methodName] = callback;
}

Seam.Remoting.processResponse = function(doc)
{
  var headerNode;
  var bodyNode;

  var inScope = typeof(Seam) == "undefined" ? false : true;
  if (!inScope) return;

  var context = new Seam.Remoting.__Context;

  if (doc.documentElement)
  {
    for (var i = 0; i < doc.documentElement.childNodes.length; i++)
    {
      var node = doc.documentElement.childNodes.item(i);
      if (node.tagName == "header")
        headerNode = node;
      else if (node.tagName == "body")
        bodyNode = node;
    }
  }

  if (headerNode)
  {
    var contextNode;
    for (var i = 0; i < headerNode.childNodes.length; i++)
    {
      var node = headerNode.childNodes.item(i);
      if (node.tagName == "context")
      {
        contextNode = node;
        break;
      }
    }
    if (contextNode && context)
    {
      Seam.Remoting.unmarshalContext(contextNode, context);
      if (context.getConversationId() && Seam.Remoting.getContext().getConversationId() == null)
        Seam.Remoting.getContext().setConversationId(context.getConversationId());
    }
  }

  if (bodyNode)
  {
    for (var i = 0; i < bodyNode.childNodes.length; i++)
    {
      var node = bodyNode.childNodes.item(i);
      if (node.tagName == "result")
        Seam.Remoting.processResult(node, context);
    }
  }
}

Seam.Remoting.processResult = function(result, context)
{
  var callId = result.getAttribute("id");
  var call = Seam.Remoting.pendingCalls.get(callId);
  Seam.Remoting.pendingCalls.remove(callId);

  if (call && (call.callback || call.exceptionHandler))
  {
    var valueNode = null;
    var refsNode = null;
    var exceptionNode = null;

    var children = result.childNodes;
    for (var i = 0; i < children.length; i++)
    {
      var tag = children.item(i).tagName;
      if (tag == "value")
        valueNode = children.item(i);
      else if (tag == "refs")
        refsNode = children.item(i);
      else if (tag == "exception")
        exceptionNode = children.item(i);
    }

    if (exceptionNode != null)
    {
      var msgNode = null;
      var children = exceptionNode.childNodes;
      for (var i = 0; i < children.length; i++)
      {
        var tag = children.item(i).tagName;
        if (tag == "message")
          msgNode = children.item(i);
      }

      var msg = Seam.Remoting.unmarshalValue(msgNode.firstChild);
      var ex = new Seam.Remoting.Exception(msg);
      call.exceptionHandler(ex);
    }
    else
    {
      var refs = new Array();
      if (refsNode)
        Seam.Remoting.unmarshalRefs(refsNode, refs);

      var value = Seam.Remoting.unmarshalValue(valueNode.firstChild, refs);

      call.callback(value, context, callId);
    }
  }
}

Seam.Remoting.unmarshalContext = function(ctxNode, context)
{
  for (var i = 0; i < ctxNode.childNodes.length; i++)
  {
    var tag = ctxNode.childNodes.item(i).tagName;
    if (tag == "conversationId")
      context.setConversationId(ctxNode.childNodes.item(i).firstChild.nodeValue);
  }
}

Seam.Remoting.unmarshalRefs = function(refsNode, refs)
{
  var objs = new Array();

  // Pass 1 - create the reference objects
  for (var i = 0; i < refsNode.childNodes.length; i++)
  {
    if (refsNode.childNodes.item(i).tagName == "ref")
    {
      var refNode = refsNode.childNodes.item(i);
      var refId = parseInt(refNode.getAttribute("id"));

      var valueNode = refNode.firstChild;
      if (valueNode.tagName == "bean")
      {
        var obj = null;
        var typeName = valueNode.getAttribute("type");
        if (Seam.Component.isRegistered(typeName))
          obj = Seam.Component.newInstance(typeName);
        else
          obj = Seam.Remoting.createType(typeName);
        if (obj)
        {
          refs[refId] = obj;
          objs[objs.length] = {obj: obj, node: valueNode};
        }
      }
    }
  }

  // Pass 2 - populate the object members
  for (var i = 0; i < objs.length; i++)
  {
    for (var j = 0; j < objs[i].node.childNodes.length; j++)
    {
      var child = objs[i].node.childNodes.item(j);
      if (child.tagName == "member")
      {
        var name = child.getAttribute("name");
        objs[i].obj[name] = Seam.Remoting.unmarshalValue(child.firstChild, refs);
      }
    }
  }
}

Seam.Remoting.unmarshalValue = function(element, refs)
{
  var tag = element.tagName;

  switch (tag)
  {
    case "bool": return element.firstChild.nodeValue == "true";
    case "number":
      if (element.firstChild.nodeValue.indexOf(".") == -1)
        return parseInt(element.firstChild.nodeValue);
      else
        return parseFloat(element.firstChild.nodeValue);
    case "str":
      var data = "";
      for (var i = 0; i < element.childNodes.length; i++)
      {
        if (element.childNodes[i].nodeType == 3) // NODE_TEXT
          data += element.childNodes[i].nodeValue;
      }
      return decodeURIComponent(data);
    case "ref": return refs[parseInt(element.getAttribute("id"))];
    case "bag":
      var value = new Array();
      for (var i = 0; i < element.childNodes.length; i++)
      {
        if (element.childNodes.item(i).tagName == "element")
          value[value.length] = Seam.Remoting.unmarshalValue(element.childNodes.item(i).firstChild, refs);
      }
      return value;
    case "map":
      var map = new Seam.Remoting.Map();
      for (var i = 0; i < element.childNodes.length; i++)
      {
        var childNode = element.childNodes.item(i);
        if (childNode.tagName == "element")
        {
          var key = null
          var value = null;

          for (var j = 0; j < childNode.childNodes.length; j++)
          {
            if (key == null && childNode.childNodes.item(j).tagName == "k")
              key = Seam.Remoting.unmarshalValue(childNode.childNodes.item(j).firstChild, refs);
            else if (value == null && childNode.childNodes.item(j).tagName == "v")
              value = Seam.Remoting.unmarshalValue(childNode.childNodes.item(j).firstChild, refs);
          }

          if (key != null)
            map.put(key, value);
        }
      }
      return map;
    case "date": return Seam.Remoting.deserializeDate(element.firstChild.nodeValue);
    default: return null;
  }
}

Seam.Remoting.deserializeDate = function(val)
{
  var dte = new Date();
  dte.setFullYear(parseInt(val.substring(0,4), 10),
                  parseInt(val.substring(4,6), 10) - 1,
                  parseInt(val.substring(6,8), 10));
  dte.setHours(parseInt(val.substring(8,10), 10));
  dte.setMinutes(parseInt(val.substring(10,12), 10));
  dte.setSeconds(parseInt(val.substring(12,14), 10));
  dte.setMilliseconds(parseInt(val.substring(14,17), 10));
  return dte;
}

Seam.Remoting.loadingMsgDiv = null;
Seam.Remoting.loadingMessage = "Please wait...";
Seam.Remoting.displayLoadingMessage = function()
{
  if (!Seam.Remoting.loadingMsgDiv)
  {
    Seam.Remoting.loadingMsgDiv = document.createElement('div');
    var msgDiv = Seam.Remoting.loadingMsgDiv;
    msgDiv.setAttribute('id', 'loadingMsg');

    msgDiv.style.position = "absolute";
    msgDiv.style.top = "0px";
    msgDiv.style.right = "0px";
    msgDiv.style.background = "red";
    msgDiv.style.color = "white";
    msgDiv.style.fontFamily = "Verdana,Helvetica,Arial";
    msgDiv.style.fontSize = "small";
    msgDiv.style.padding = "2px";
    msgDiv.style.border = "1px solid black";

    document.body.appendChild(msgDiv);

    var text = document.createTextNode(Seam.Remoting.loadingMessage);
    msgDiv.appendChild(text);
  }
  else
  {
    Seam.Remoting.loadingMsgDiv.innerHTML = Seam.Remoting.loadingMessage;
    Seam.Remoting.loadingMsgDiv.style.visibility = 'visible';
  }
}

Seam.Remoting.hideLoadingMessage = function()
{
  if (Seam.Remoting.loadingMsgDiv)
    Seam.Remoting.loadingMsgDiv.style.visibility = 'hidden';
}

/* Messaging API */

Seam.Remoting.pollInterval = 10; // Default poll interval of 10 seconds
Seam.Remoting.pollTimeout = 0; // Default timeout of 0 seconds
Seam.Remoting.polling = false;

Seam.Remoting.setPollInterval = function(interval)
{
  Seam.Remoting.pollInterval = interval;
}

Seam.Remoting.setPollTimeout = function(timeout)
{
  Seam.Remoting.pollTimeout = timeout;
}

Seam.Remoting.subscriptionRegistry = new Array();

Seam.Remoting.subscribe = function(topicName, callback)
{
  for (var i = 0; i < Seam.Remoting.subscriptionRegistry.length; i++)
  {
    if (Seam.Remoting.subscriptionRegistry[i].topic == topicName)
      return;
  }

  var body = "<subscribe topic=\"" + topicName + "\"/>";
  var env = Seam.Remoting.createEnvelope(null, body);
  Seam.Remoting.subscriptionRegistry.push({topic:topicName, callback:callback});
  Seam.Remoting.sendAjaxRequest(env, Seam.Remoting.PATH_SUBSCRIPTION, Seam.Remoting.subscriptionCallback, false);
}

Seam.Remoting.unsubscribe = function(topicName)
{
  var token = null;

  for (var i = 0; i < Seam.Remoting.subscriptionRegistry.length; i++)
  {
    if (Seam.Remoting.subscriptionRegistry[i].topic == topicName)
    {
      token = Seam.Remoting.subscriptionRegistry[i].token;
      Seam.Remoting.subscriptionRegistry.splice(i, 1);
    }
  }

  if (token)
  {
    var body = "<unsubscribe token=\"" + token + "\"/>";
    var env = Seam.Remoting.createEnvelope(null, body);
    Seam.Remoting.sendAjaxRequest(env, Seam.Remoting.PATH_SUBSCRIPTION, null, false);
  }
}

Seam.Remoting.subscriptionCallback = function(doc)
{
  var body = doc.documentElement.firstChild;
  for (var i = 0; i < body.childNodes.length; i++)
  {
    var node = body.childNodes.item(i);
    if (node.tagName == "subscription")
    {
      var topic = node.getAttribute("topic");
      var token = node.getAttribute("token");
      for (var i = 0; i < Seam.Remoting.subscriptionRegistry.length; i++)
      {
        if (Seam.Remoting.subscriptionRegistry[i].topic == topic)
        {
          Seam.Remoting.subscriptionRegistry[i].token = token;
          Seam.Remoting.poll();
          break;
        }
      }
    }
  }
}

Seam.Remoting.pollTimeoutFunction = null;

Seam.Remoting.poll = function()
{
  if (Seam.Remoting.polling)
    return;

  Seam.Remoting.polling = true;
  clearTimeout(Seam.Remoting.pollTimeoutFunction);

  var body = "";

  if (Seam.Remoting.subscriptionRegistry.length == 0)
  {
    Seam.Remoting.polling = false;
    return;
  }

  for (var i = 0; i < Seam.Remoting.subscriptionRegistry.length; i++)
  {
    body += "<poll token=\"" + Seam.Remoting.subscriptionRegistry[i].token + "\" ";
    body += "timeout=\"" + Seam.Remoting.pollTimeout + "\"/>";
  }

  var env = Seam.Remoting.createEnvelope(null, body);
  Seam.Remoting.sendAjaxRequest(env, Seam.Remoting.PATH_POLL, Seam.Remoting.pollCallback, true);
}

Seam.Remoting.pollCallback = function(doc)
{
  Seam.Remoting.polling = false;

  var body = doc.documentElement.firstChild;
  for (var i = 0; i < body.childNodes.length; i++)
  {
    var node = body.childNodes.item(i);
    if (node.tagName == "messages")
      Seam.Remoting.processMessages(node);
    else if (node.tagName == "errors")
      Seam.Remoting.processPollErrors(node);
  }

  Seam.Remoting.pollTimeoutFunction = setTimeout("Seam.Remoting.poll()", Math.max(Seam.Remoting.pollInterval * 1000, 1000));
}

Seam.Remoting.processMessages = function(messages)
{
  var token = messages.getAttribute("token");

  var callback = null;
  for (var i = 0; i < Seam.Remoting.subscriptionRegistry.length; i++)
  {
    if (Seam.Remoting.subscriptionRegistry[i].token == token)
    {
      callback = Seam.Remoting.subscriptionRegistry[i].callback;
      break;
    }
  }

  if (callback != null)
  {
    var messageNode = null;

    var children = messages.childNodes;
    for (var i = 0; i < children.length; i++)
    {
      if (children.item(i).tagName == "message")
      {
        messageNode = children.item(i);
        var messageType = messageNode.getAttribute("type");

        var valueNode = null;
        var refsNode = null;
        for (var j = 0; j < messageNode.childNodes.length; j++)
        {
          var node = messageNode.childNodes.item(j);
          if (node.tagName == "value")
            valueNode = node;
          else if (node.tagName == "refs")
            refsNode = node;
        }

        var refs = new Array();
        if (refsNode)
          Seam.Remoting.unmarshalRefs(refsNode, refs);

        var value = Seam.Remoting.unmarshalValue(valueNode.firstChild, refs);

        callback(Seam.Remoting.createMessage(messageType, value));
      }
    }
  }
}

Seam.Remoting.processErrors = function(errors)
{
  var token = errors.getAttribute("token");

  // Unsubscribe to the topic
  for (var i = 0; i < Seam.Remoting.subscriptionRegistry.length; i++)
  {
    if (Seam.Remoting.subscriptionRegistry[i].token == token)
    {
      Seam.Remoting.subscriptionRegistry.splice(i, 1);
      break;
    }
  }

  for (var i = 0; i < errors.childNodes.length; i++)
  {
    if (errors.childNodes.item(i).tagName == "error")
    {
      var errorNode = errors.childNodes.item(i);
      var code = errorNode.getAttribute("code");
      var message = errorNode.firstChild.nodeValue;

      if (Seam.Remoting.onPollError)
        Seam.Remoting.onPollError(code, message);
      else
        alert("A polling error occurred: " + code + " " + message);
    }
  }
}

Seam.Remoting.ObjectMessage = function()
{
  this.value = null;

  Seam.Remoting.ObjectMessage.prototype.getValue = function()
  {
    return this.value;
  }

  Seam.Remoting.ObjectMessage.prototype.setValue = function(value)
  {
    this.value = value;
  }
}

Seam.Remoting.TextMessage = function()
{
  this.text = null;

  Seam.Remoting.TextMessage.prototype.getText = function()
  {
    return this.text;
  }

  Seam.Remoting.TextMessage.prototype.setText = function(text)
  {
    this.text = text;
  }
}

Seam.Remoting.createMessage = function(messageType, value)
{
  switch (messageType)
  {
    case "object":
      var msg = new Seam.Remoting.ObjectMessage();
      msg.setValue(value);
      return msg;
    case "text":
      var msg = new Seam.Remoting.TextMessage();
      msg.setText(value);
      return msg;
  }
  return null;
}
