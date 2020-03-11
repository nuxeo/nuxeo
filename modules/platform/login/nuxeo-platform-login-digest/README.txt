This login module is used to implement HTTP Digest Access Authentication (RFC 2617).
This is mainly used by Microsoft's WSS and WebDAV implementation.

To be able to function with Digest Auth, the plugin requires the UserManager
configuration to have an additional directory and a realm:

  <extension target="org.nuxeo.ecm.directory.sql.SQLDirectoryFactory"
    point="directories">
    <directory name="digestauth">
      <schema>digestauth</schema>
      <table>digestauth</table>
      <autoincrementIdField>false</autoincrementIdField>
      <dataSource>java:/nxsqldirectory</dataSource>
      <idField>username</idField>
      <passwordField>password</passwordField>
      <createTablePolicy>on_missing_columns</createTablePolicy>
    </directory>
  </extension>

  <extension target="org.nuxeo.ecm.platform.usermanager.UserService" point="userManager">
    <userManager>
      <digestAuthDirectory>digestauth</digestAuthDirectory>
      <digestAuthRealm>NUXEO</digestAuthRealm>
    </userManager>
  </extension>
