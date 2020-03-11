Some notes about the implementation of webengine over JAX-RS
============================================================


WebEngine JAX-RS Goals
----------------------

The main goal is to provide a flexible WEB framework that can be used to build
both traditional Web applications (based on scripting and templates) and also
fully JAX-RS applications

As a side effect of the main goal, we may say a secondary goal is to provide a
pure JAX-RS implementation for Groovy - to be able to build JAX-RS applications
in groovy.

Eric's definition:

"Webengine is the content-centric web framework, from Nuxeo.

At the crossroad of two worlds, Enterprise Java and Lightweight Web Framework
(ala Django and Rails), based on the rock-solid content infrastructure from
Nuxeo, WebEngine bring a light and innovative approach to quickly build and
deliver today's web applications (for the hyped Web2.0 ;-).

From content delivery to highly content-based dynamic web apps,
WebEngine empowers web developers to create a new kind of web apps."


JAX-RS integration
------------------

The currently supported JAX-RS back-ends are:

1. resteasy
2. jersey

Both of these libraries require some 'patching' since WebEngine needs some
extensions over JAX-WS as follows:

1. Declarative registration of root resources.

The only specified way to register root resources is to annotate a resource
class with @Path annotation and specify the binding pattern.

WebEngine needs to define these bindings without using annotations since it may
want to reuse a same resource in another context.

Thus root resources can be declared in a configuration file.

2. Inject custom context objects using @Context annotation. This is needed to
inject the WebContext used by WebEngine.

Currently, the WebContext overrides the context object provided by jersey
and/or Resteasy.

Also, patching is needed to fix some bugs found in these libraries.

The patching is done by overriding classes and not by modifying library JARs.

There are cases when fixing a bug or adding a new feature is not possible
without changing back-end sources (because methods are not visible or
overridable).

For now there is such a problem with Jersey that doesn't support REGEX in @Path
expressions.


WebEngine JAX-RS model
----------------------

WebEngine provides an object model based on JAX-RS that integrate scripting and
some advanced features like actions, security and repository access.

An WebEngine instance is bound to a Java servlet and define a WebEngine
Application.

You can create multiple applications (using isolated WebEngine instances)
by using a servlet by application.

The model of a WebEngine Application is built around three main objects:

1. WebDomain (we may rename it as WebSite)
2. WebObject
3. WebAction


Web Domains
------------

A domain is a JAX-RS root resource that implements WebDomain interface. Its
main goal is to define an entry point into a Web Application.

A Web Application may define multiple domains.

The WebDomain is the entry point in a "domain" controlled by the application.

I used 'domain' as name and not 'site' because a WebEngine application is
composed from multiple domains so that we may think that a WebEngine
application is a 'Site' and its logical sub-parts are 'domains'.

For example in the default WebEngine application we will have the following
domains:

- Main - bound to "/" - contains the scripts and templates used to build the
  'static' part of the application like: Home Page, About, Help, Documentation

- Administration - bound to "/admin" for engine administration (e.g. user admin
  etc)

- Repository - bound to "/repository" - expose the repository for browsing and
  administration

Each domain may specify the directory stack to use for scripting and templates,
a security guard to restrict access to the domain and also a type of resources
it manages.

There are 3 predefined types of domains in WebEngine:

1. The Default domain - is a dynamic domain that may handle resource types as
specified in the configuration

Example:

<domain id="user-admin" type="org.nuxeo.ecm.webengine.UserResource">
  <roots>
    <root>/default</root>
    <root>/user-admin</root>
  </roots>
</domain>

2. The Script Domain - handle ScriptObject resources. Can be used to create a
domain based on full scripting (without repository context)

Example:

<domain id="root">
  <roots>
    <root>/default</root>
  </roots>
</domain>

3. The Document Domain (or Content Domain) - handle DocumentObject resources.
Can be used to access the repository.

Example:

<domain id="repository">
  <roots>
    <root>/default</root>
  </roots>
  <contentRoot>/default-domain/workspaces</contentRoot>
</domain>

A domain can be bound to several paths using regex expressions
(regex is supported only on resteasy for now)

Example:

<resource domain="repository" pattern="{lang}/repository" />
<resource domain="repository" pattern="/repository" />

You can also bind POJO JAX-RS root resources (not necessarily domains) to path
patterns like this:

<resource class="org.nuxeo.java.MyJavaResource" pattern="/java" />
<resource class="org.nuxeo.groovy.MyGroovyResource" pattern="/groovy" />

This will bind a java and a groovy class as root JAX-RS resources.

*Note*: The Groovy class path is configurable using the Nuxeo Runtime property
(or system property) named 'groovy.classpath'.

By default it points to nxserver/web/classes.

So any class put under this directory will become visible as a Java class
(compilation is done on the fly).

To define a custom WebDomain object you must extend the AsbtractWebDomain
class.

There are also two types of abstract domains that you may extends as
convenience.

These domains are using some predefined rules of dispatching:

1. DefaultWebDomain - it assumes the right part of the path (the one not
matched by the domain itself) is locating a single object 9not a chain of
objects)

2. ChainingWebDomain - will create a chain of objects for each segment in the
path and will invoke the last object. It is extended by DocumentDomain to
handle document requests.

WebEngine Request Dispatch Model
--------------------------------

As we've seen the entry point is the domain. Then, the domain will construct
one or more chained objects.

The HTTP method specified by the request will be invoked on the last object in
the chain.

For ScriptDomains there will be always a single ScriptObject.

For more sophisticated domains like a DocumentDomain we will end up with a
chain of objects.


Example:

The request: GET /myapp/my/domain/my/document
will locate the application bound to /myapp (the servlet mapping)
then will locate the domain bound to /my/domain
then will ask the domain for the resource /my/document.
And finaly the GET method is invoked on the last created object.


Web Objects
------------

A Web Object is a JAX-RS resource managed by a Web Domain and is implementing
the WebObject interface.

A major re-design of the WebObject model consist in the fact that a WebObject
is not linked to a DocumentModel.

This is leveraging the model (including "action" model) to other kind of
resources that are not stored in the repository.

This way the web application will be more consistent since it will share the
same model no matter you use or not DocumentModels.

So the WebObject interface define only the generic model of a web object. This
model includes:

- object web related information (like web path etc.)
- action management.
- type information - each object has a type!

Each object has a type and supports simple inheritance.

An object type is defined by the interface WebType which is managing type
related information as available actions, the super type etc.

Obviously, when deriving objects they will inherit any base type information.

The root object is the root of the object hierarchy tree and is implemented by
the singleton WebType.ROOT. It have no actions defined on it.

WebEngine provides 2 types of objects:

1. ScriptObject represent a script or a template
2. DocumentObject represent a DocumentModel Object.

*Important*: You should note that HTTP requests will be always resolved to an
WebObject.

So that any WebEngine resource accessible from outside will be an WebObject.

Even scripts, and we will see later, even *actions*!

DocumentObject model
---------------------

A special case is the DocumentObject that expose DocumentModels.

To hide any extra concept a WebType will be transparently mapped to a
DocumentType.

So that DocumentObject will have the "Document" type which will wrap a real
DocumentType.

WebType names will always match DocumentType names.

So that you should be careful when creating new WebObject types to not use an
existing DocumentType as the WebObject type.

This is because type names are globals.

For example let say you want to write the User Administration part of your
site.

So that you may want to define a new WebObject that describe your object.

So you will choose a name like User or UserObject or anything you like.

But when doing this you should check the name is not already used by a
DocumentType otherwise you will have type conflicts.

Maybe a good approach will be to use special prefixes like "admin.UserObject".
Or may be to register types at Domain level (to isolate them) and not at
application level.

To create a new DocumentObject simply extends the existing Document Object like
DocumentObject and bind it to a type.

Binding objects on types it is not yet defined - in a first step I will
probably use a naming rule or extension points. Later I will may be switch to
annotations or use both methods.

Discovering WebObjects is also not yet specified. If using annotations we may
generate at build time a list of available objects or at runtime scanning JARs
to discover them.

You can mix these approaches with extension point configuration.

As WebObjects are JAX-RS resources you will use annotations on methods to
define the REST handlers.

Also you can define new WebObjects in Groovy. (using Groovy annotations)

So, to recapitulate, an WebObject is a JAX-RS resource that may respond to HTTP
methods and may return web views to be rendered to clients.

We will see more about WebViews later.


WebAction
----------

One of the major re-factoring is the WebAction concept.

WebActions are implemented as JAX-RS resources like WebDomain and WebObject!

This is clarifying the problem we had previously with actions concept:

- What is the difference between an action and a HTTP method?
- What is the difference between an action and a view?
- etc.

By defining actions as JAX-RS resources we have no more these conceptual
conflicts.

But, practically what this means?

It means that WebActions as WebObjects may handle HTTP methods and may define
views to be rendered to the client.

Because of this approach we must change the action marker from @@ to /@.

Otherwise JAX-RS will not correctly dispatch actions. (actions must match a
path segment).

A difference between WebObjects and WebActions is that actions can be used only
as leaf resources. They cannot be the parent of a WebObject or another
WebAction resource.

You can even use WebActions directly under a WebDomain it the domain support
actions.

Let's see some examples:

GET /my/doc
Will retrieve the /my/doc document and return the default view of the document to the client

GET /my/doc?view=edit
Will retrieve the /my/doc document and return the "edit" view of the document to the client

POST /my/doc/child
Will create a new 'child' document

PUT /my/doc/child
Will update the 'child' document

DELETE /my/doc
Will delete a document

etc.

GET /my/doc/@lock
Will get the lock status on the document

POST /my/doc/@lock
Will put a lock on the document

GET /my/doc/@acp
Will get the acp of the document

PUT /my/doc/@acp
Will update the acp of the document

etc.


Creating Response and Template Rendering
-----------------------------------------

Another big change is the way the response is sent to the client.

When processing an action you do not have anymore access to the response output
stream.

This is imposed by JAX-RS but I think is a good think since it is clearly
separate scripts from templates.

This means a script or a java method that wants to send a response must return
an object.

JAX-RS will try to find out what writer it should use to write this object to
the response stream.

It will look into the map of registered writers until it find a matching one.

If any is found it will thrown an exception.

So that to write a custom response you must register a MessageBodyWriter that
knows how to write your object.

There are various writers already available in JAX-WS that knows how to write
regular java objects like:

1. Primitives, Strings
2. InputStream  and File objects
3. Response objects - these are special objects that can be used to have full
   control on the response (to change headers etc)
4. JAXB annotated objects
etc.

Let say you want to write a very large object as the response (for example a
big file).

To avoid putting the object in memory you can either return a File or an
InputStream that points to your object content either a reference object and
register your own writer that knows how to write it down.

WebEngine provides a Template object (it will be renamed to WebView) that
identify a freemarker template and also a MessageBodyWriter that knows how to
write it down.

So if you want to render a template from a script you should return a Template
object. The rest is done in background for you.

Example:

return Context.getTemplate("my/template.ftl")

The Template is then intercepted by the TemplateMessageBodyWriter that will
locate the template file using the current domain stacking directories and will
render the template to the output stream.

WebEngine is also providing a WebObjectMessageBodyWriter that will render
WebObject using context information.

For example, if you want to render a WebObject in the default way (using
default template discovery), you simply return the object instance from you
method. Example:

@GET
public WebObject get() {
  return this;
}

The default template name is "view" and you can change this using the "view"
query string variables.

Like: GET /my/doc?view=edit


Processing Input Forms
----------------------

I've not yet worked on forms but the mechanism is the same as for rendering
responses.

let say you have a method:

@POST
public void post(DocumentModel doc) {
   //...
   getSession().saveDocument(doc);
}

When submiting a document using a web form you method will be called and JAX-RS
will see that you expect a DocumentModel object.

It will look then into the MessageBodyReader registry for a reader that knows
how to transform the form input in a DocumentModel. Then, in your
implementation you simply save the already built document.

WebEngine will provide all Readers and Writers you need when dealing with
documents.

This is making life easier for those writing WebObjects...

Mime-Types
-----------

A feature not discussed until now is the Mime-Type of the output and input.

What if I want to change the way a document is serialized?

How can I do this without breaking the flow described above?

The response is simple. JAX-RS is using mime-types to detect what Reader and
Writer should be used to handle a request/response.

On the MessageBodyWriter and MessageBodyReaders you can specify what mime-type
you can handle.

Example:

@ConsumeMime({"application/atom+xml"})
public class DocumentAtomWriter implements MessageBodyWriter<DocumentModel> {
...
}

This way when outputting the returned object JAX-RS will try to guess what mime
type it needs to serve to the client (when guessing it is basing on the request
Accept header) and then it choose the right Writer that accept this mime-type.

The same for Readers.

Ok. But what if I want to override the detected mime type?

Then you should return a "Response" object that is specifying the mime type you
want.

See ScriptObject for an example.

Example: Let say you want output a file (download) and use the mime type
corresponding to the file extension:

@GET
public File getFile(@Context ServletContext servletCtx) {
  File file = new File(path);
  String ctype = servletCtx.getMimeType(f.getName());
  return Response.ok(file, ctype).build();
}


Building RESTful applications
-----------------------------

Building RESTful applications around Nuxeo becomes very simple.

As WebEngine implements all the bricks needed (Writers, Readers,
DocumentObjects etc) it becomes very simple to build a REST application.

For example. Let say you want an ATOM API for DocumentModel.

How you can implement it using WebEngine?

The response is - you do nothing! :) Just use an atom client that will send the
correct mime-type through "Accept" headers and WebEngine will use proper Writer
and Readers to encode/decode requests.

All other functionality like accessing documents, locking, unlocking, deleting
etc. will be already implemented by the DocumentObejct

If you want to create a feed view for a Nuxeo Folder then you should simply
write a freemarker template that is generating the feed and bind a ScriptObject
(or WebObject) to an URL or something like that..

What is really good is that the same infrastructure is reused to build REST
applications and pure web application!


Scripting improvements
-----------------------

There are also improvements on scripting side.

The Groovy scripting is now the only supported to build WebObject, WebAction
and WebDomains.  (Since you should use extended scripting capabilities like
annotations etc)

To be able to make full use of Groovy features we are directly using Groovy
API.  (so we avoid using javax.script in Groovy case) You can still use plain
scripting (using javax.script) in python, javascript or other language if you
want.

This enable us for example to control the groovy class path and to hot redeploy
groovy classes.

Also, we *can* now use Groovy classes from Groovy scripts (or even from python
or other script languages!) In fact Groovy classes will be compiled as regular
java classes and WebEngine will take care of the cache and hot redeploy.

A planned feature is to support real hot deploy for WebDomain, WebObject and
WebAction objects coded in Java.

So that you will code these objects in eclipse and you can test them
immediately wihtout recompiling them.

This feature is easy to implement since we already have groovy class caching
and hot deploy mechanism. We need just to compile at demand java files. This
can be easily done using javac.

For other type of objects we will not be able to perform hot deployment.

The WebDomain, WebObject and WebAction objects are special because they exists
only the time of the request which means you will never have persistent
references to them.

Also, these objects are managed by WebEngine so that their creation (and thus
recompilation) can be done on the fly.
