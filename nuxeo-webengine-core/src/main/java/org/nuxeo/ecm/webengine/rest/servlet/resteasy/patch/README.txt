The following classes were patched to add custom context injection.

ConstructorInjectorImpl
MethodInjectorImpl
PropertyInjectorImpl

Instead of using the static method
InjectorFactoryImpl.getParameterExtractor() to get the correct
ValueInjector the patched files will use a protected method that does the same.
This way one can easily override it.

The ContextParameterInjector was patched to add the WebContext injection.

The patched InjectorFactoryImpl is returning the 3 classes above instead of the original ones
I removed the static method getParameterExtractor()

The class ResourceMethodRegistry was patched to add possibility to register
root resources that are not annotated with @Path and also to create a custom InjectorFactory

The ResourceLocator class was patched to refer to the redefined
ResourceMethodRegistry class instead of using the old one.

The WebEngineServlet is creating a custom dispatcher and context that are
hooking the patched classes.

