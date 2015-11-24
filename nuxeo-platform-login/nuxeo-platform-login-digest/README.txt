This login module is used to implement HTTP Digest Access Authentication (RFC 2617).
This is mainly used by Microsoft's WSS and WebDAV implementation.

To be able to function with Digest Auth, the plugin requires the UserManager
configuration to have an additional directory and a realm:

  <extension target="org.nuxeo.ecm.platform.usermanager.UserService" point="userManager">
    <userManager>
      <digestAuthDirectory>some_directory</digestAuthDirectory>
      <digestAuthRealm>NUXEO</digestAuthRealm>
    </userManager>
  </extension>

This directory must have an id field and password field configured.
