Modifications on gf3 embedded:

1. Updated code to compile against build 10.0-build-20080724
(was 10.0-build-20080430)

2. Updated domain.xml to be able to start GlassFish server.

3. Found a problem in org.jvnet.hk2.component.InjectionManager.
the isOptional method is always returning false.

Because of this in HttpServiceConfigListener the injection for the field

    @Inject(optional=true)
    public AccessLog accessLog;

will thrown an error event if it is marked as optional.

I fixed the problem by adding in domains.xml this line:

<access-log format="%client.name% %auth-user-name% %datetime% %request% %status% %response.length%" rotation-enabled="true" rotation-interval-in-minutes="15" rotation-policy="time" rotation-suffix="yyyy-MM-dd"/>

3. Found a problem in ApplicationLifeCycle.setupContainerInfos()

    Deployer deployer = getDeployer(containerInfo);
    containerInfosByDeployers.put(deployer, containerInfo);
    final MetaData metadata = deployer.getMetaData();
    Class[] requires = (metadata==null?null:metadata.requires());
    Class[] provides = (metadata==null?null:metadata.provides());

In the case of the embedded GlassFish, when deploying a ScatteredWAR a single
sniffer is passed to that method and the requires array will be empty, and the
provides array will contain only one entry of type Application (if I remember
well).

Anyway the mechanism used to compute and sort the deployers for a given archive
is not working correctly because in that case an empty list of ContainerInfo is
returned and the ScatteredWar will not be deployed (the following error is
thrown: "There is no installed container capable of handling this application")

I solved this (at embedded GlassFish level) by replacing the
ApplicationLifeCycle service with a custom ApplicationLifeCycle2 service which
is overriding the setupContainerInfos() method and returns the a correct
ContainerInfo list.

4. Found another problem at embedded GlassFish level:

The deployment of a ScatteredWar is working only if an additional parameter
ParameterNames.VIRTUAL_SERVERS is specified. See:

GFApplication GlassFish.deploy(ReadableArchive a) throws IOException;

        params.put(ParameterNames.NAME,a.getName());
        params.put(ParameterNames.ENABLED,"true");
        params.put(ParameterNames.VIRTUAL_SERVERS, "server");
        //params.put(ParameterNames.CONTEXT_ROOT, "/");

Because of this I think you need to add a method to GlassFish that take a
Properties object as an optional parameter:

GFApplication GlassFish.deploy(ReadableArchive a, Properties params) throws
IOException;

(On my side I fixed this by extending GlassFish class)

This way you can customize your deployment.

5. I think another nice feature will be to let the user customize more easily
the GlassFish embedded application.

For demos or small applications it is ok to instantiate GlassFIsh by giving a
port number.

But when you need to use it on complex projects you need fine tuning on the
server.

So, I think, it will be good to provide an advanced constructor (or a
configuration mechanism) for experimented users where you can specify
additional options like a domain.xml file, whether or not you want domain.xml
persistence etc.

Also, making accessible some private members/methods (like GlassFish.habitat or
GFApplication() ctor) as protected or public may be useful for those who want
to extend the default GlassFish implementation.

I updated GlassFish embedded server so that I can specify the domain.xml and a
default web.xml from outside.

I managed to start our application using embedded GF3 which is pretty cool.
Startup time is ~ 4 seconds on my laptop which is very good.

