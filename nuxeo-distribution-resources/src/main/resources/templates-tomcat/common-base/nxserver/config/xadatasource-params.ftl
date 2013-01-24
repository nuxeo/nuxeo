<#if "${nuxeo.db.type}" == "postgresql">
        <property name="ServerName">${nuxeo.db.host}</property>
        <property name="PortNumber">${nuxeo.db.port}</property>
        <property name="DatabaseName">${nuxeo.db.name}</property>
        <property name="User">${nuxeo.db.user}</property>
        <property name="Password">${nuxeo.db.password}</property>
<#elseif "${nuxeo.db.type}" == "oracle">
        <property name="URL">jdbc:oracle:thin:@${nuxeo.db.host}:${nuxeo.db.port}:${nuxeo.db.name}</property>
        <property name="User">${nuxeo.db.user}</property>
        <property name="Password">${nuxeo.db.password}</property>
<#elseif "${nuxeo.db.type}" == "mssql">
        <property name="ServerName">${nuxeo.db.host}</property>
        <property name="PortNumber">${nuxeo.db.port}</property>
        <property name="DatabaseName">${nuxeo.db.name}</property>
        <property name="User">${nuxeo.db.user}</property>
        <property name="Password">${nuxeo.db.password}</property>
        <property name="UseCursors">true</property>
<#elseif "${nuxeo.db.type}" == "mysql">
        <property name="URL">jdbc:mysql://${nuxeo.db.host}:${nuxeo.db.port}/${nuxeo.db.name}?relaxAutoCommit=true</property>
        <property name="User">${nuxeo.db.user}</property>
        <property name="Password">${nuxeo.db.password}</property>
<#else>
        <property name="URL">jdbc:h2:${nuxeo.db.name}</property>
        <property name="User">${nuxeo.db.user}</property>
        <property name="Password">${nuxeo.db.password}</property>
</#if>
