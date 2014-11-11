<Results>
<Title>${site.name}</Title>
        <LastUpdate>${updateTS}</LastUpdate>
        <User>
            <ID>${currentUser.id}</ID>
            <Name>${currentUser.name}</Name>
            <LoginName>${currentUser.login}</LoginName>
            <Email>${currentUser.email}</Email>
            <#if currentUser.domainGroup>
            <IsDomainGroup>True</IsDomainGroup>
            <#else>
            <IsDomainGroup>False</IsDomainGroup>
            </#if>
            <#if currentUser.siteAdmin>
            <IsSiteAdmin>True</IsSiteAdmin>
            <#else>
            <IsSiteAdmin>False</IsSiteAdmin>
            </#if>
        </User>
        <Members>
            <#list users as user>
            <#if (user.id!=currentUser.id)>
            <Member>
                <ID>${user.id}</ID>
                <Name>${user.name}</Name>
                <Email>${user.email}</Email>
                <LoginName>${user.login}</LoginName>
                <#if user.domainGroup>
                <IsDomainGroup>True</IsDomainGroup>
                <#else>
                <IsDomainGroup>False</IsDomainGroup>
                </#if>
            </Member>
            </#if>
           </#list>
        </Members>
        <Assignees>
            <#list assignees as user>
            <Member>
                <ID>${user.id}</ID>
                <Name>${user.name}</Name>
                <LoginName>${user.login}</LoginName>
            </Member>
            </#list>
        </Assignees>
    <List Name="Tasks">
        <NoChanges />
    </List>
    <List Name="Documents">
        <NoChanges />
    </List>
    <List Name="Links">
        <NoChanges />
    </List>
</Results>