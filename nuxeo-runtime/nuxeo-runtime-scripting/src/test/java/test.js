importPackage(java.lang);
importPackage(org.nuxeo.runtime);
importPackage(org.nuxeo.runtime.api);
importPackage(org.nuxeo.runtime.api.login);
importPackage(org.nuxeo.runtime.model);

runtime = Framework.getRuntime();
name = runtime.getName();
version = runtime.getVersion();
desc = runtime.getDescription();
println("Remote runtime: " + name + " v." + version);
println(desc);
println("---------------------------------------");
println("Registered components:");
println("---------------------------------------");
regs = runtime.getComponentManager().getRegistrations();
for (var i = 0, size = regs.size(); i < size; i++) {
    println(regs.get(i).getName());
}
println("---------------------------------------");
println("login config:");
println("---------------------------------------");
ls = Framework.getService(LoginService);
domains = ls.getSecurityDomains();
for (var i = 0, size = domains.length; i < size; i++) {
    println(domains[i].getName());
}
println("---------------------------------------");
println("streaming:");
println("---------------------------------------");
println(runtime.getProperty("org.nuxeo.runtime.streaming.serverLocator"));

