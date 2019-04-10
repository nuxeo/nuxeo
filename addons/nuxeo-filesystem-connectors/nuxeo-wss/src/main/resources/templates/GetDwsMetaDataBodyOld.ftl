<Results>
    <MtgInstance />
    <SettingUrl>${request.getBaseUrl()}_layouts/settings.aspx</SettingUrl>
    <PermsUrl>${request.getBaseUrl()}_layouts/EditPrms.aspx</PermsUrl>
    <UserInfoUrl>${request.getBaseUrl()}_layouts/UserEdit.aspx</UserInfoUrl>
    <Roles>
        <Role Name="Full Control" Type="Administrator" Description="Has full control." />
        <Role Name="Design" Type="WebDesigner" Description="Can view, add, update, delete, approve, and customize." />
        <Role Name="Contribute" Type="Contributor" Description="Can view, add, update, and delete." />
        <Role Name="Read" Type="Reader" Description="Can view only." />
    </Roles>
    <Permissions>
        <ManageSubwebs />
        <ManageWeb />
        <ManageRoles />
        <ManageLists />
        <InsertListItems />
        <EditListItems />
        <DeleteListItems />
    </Permissions>
    <HasUniquePerm>True</HasUniquePerm>
    <WorkspaceType />
    <IsADMode>False</IsADMode>
    <DocUrl>${request.getBaseUrl()}${doc.getRelativeFilePath(siteRoot)}</DocUrl>
    <Minimal>True</Minimal>
    <Results>
        <Title>Team Site</Title>
        <LastUpdate>633876691128006250</LastUpdate>
        <User>
            <ID>1</ID>
            <Name>Nuxeo</Name>
            <LoginName>${request.getUserName()}</LoginName>
            <Email>someone@example.com</Email>
            <IsDomainGroup>False</IsDomainGroup>
            <IsSiteAdmin>True</IsSiteAdmin>
        </User>
        <Members>
            <Member>
                <ID>5</ID>
                <Name>Team Site Members</Name>
                <Email />
                <LoginName />
                <IsDomainGroup>True</IsDomainGroup>
            </Member>
            <Member>
                <ID>3</ID>
                <Name>Team Site Owners</Name>
                <Email />
                <LoginName />
                <IsDomainGroup>True</IsDomainGroup>
            </Member>
            <Member>
                <ID>4</ID>
                <Name>Team Site Visitors</Name>
                <Email />
                <LoginName />
                <IsDomainGroup>True</IsDomainGroup>
            </Member>
        </Members>
    </Results>
</Results>