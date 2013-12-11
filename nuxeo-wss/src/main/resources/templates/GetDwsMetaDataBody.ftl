<Results>
    <MtgInstance />
    <SettingUrl>${request.getBaseUrl()}${site.accessUrl}</SettingUrl>
    <PermsUrl>${request.getBaseUrl()}_layouts/EditPrms.aspx</PermsUrl>
    <UserInfoUrl>${request.getBaseUrl()}${site.userManagementUrl}</UserInfoUrl>
    <Roles>
        <Role Name="Full Control" Type="Administrator" Description="Has full control." />
        <Role Name="Design" Type="WebDesigner" Description="Can view, add, update, delete, approve, and customize." />
        <Role Name="Contribute" Type="Contributor" Description="Can view, add, update, and delete." />
        <Role Name="Read" Type="Reader" Description="Can view only." />
    </Roles>
    <Schema Name="Tasks">
        <Field Name="Title" Type="Text" Required="True">
            <Choices />
        </Field>
        <Field Name="Priority" Type="Choice" Required="False">
            <Choices>
                <Choice>(1) High</Choice>
                <Choice>(2) Normal</Choice>
                <Choice>(3) Low</Choice>
            </Choices>
        </Field>
        <Field Name="Status" Type="Choice" Required="False">
            <Choices>
                <Choice>Not Started</Choice>
                <Choice>In Progress</Choice>
                <Choice>Completed</Choice>
                <Choice>Deferred</Choice>
                <Choice>Waiting on someone else</Choice>
            </Choices>
        </Field>
        <Field Name="PercentComplete" Type="Number" Required="False">
            <Choices />
        </Field>
        <Field Name="AssignedTo" Type="User" Required="False">
            <Choices />
        </Field>
        <Field Name="Body" Type="Note" Required="False">
            <Choices />
        </Field>
        <Field Name="StartDate" Type="DateTime" Required="False">
            <Choices />
        </Field>
        <Field Name="DueDate" Type="DateTime" Required="False">
            <Choices />
        </Field>
    </Schema>
    <Schema Name="Documents" Url="${siteUrl}">
        <Field Name="FileLeafRef" Type="File" Required="True">
            <Choices />
        </Field>
        <Field Name="Title" Type="Text" Required="False">
            <Choices />
        </Field>
    </Schema>
    <Schema Name="Links">
        <Field Name="URL" Type="URL" Required="True">
            <Choices />
        </Field>
        <Field Name="Comments" Type="Note" Required="False">
            <Choices />
        </Field>
    </Schema>
    <ListInfo Name="Tasks">
        <Moderated>False</Moderated>
        <ListPermissions>
        </ListPermissions>
    </ListInfo>
    <ListInfo Name="Documents">
        <Moderated>False</Moderated>
        <ListPermissions>
            <EditListItems />
            <InsertListItems />
            <ManageLists />
        </ListPermissions>
    </ListInfo>
    <ListInfo Name="Links">
        <Moderated>False</Moderated>
        <ListPermissions>
        </ListPermissions>
    </ListInfo>
    <Permissions>
        <ManageSubwebs />
        <ManageWeb />
        <ManageRoles />
        <ManageLists />
        <InsertListItems />
        <EditListItems />
        <DeleteListItems />
    </Permissions>
    <HasUniquePerm>False</HasUniquePerm>
    <WorkspaceType>DWS</WorkspaceType>
    <IsADMode>False</IsADMode>
    <DocUrl>${request.getBaseUrl()}${doc.getRelativeFilePath(siteRoot)}</DocUrl>
    <Minimal>False</Minimal>
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
            <ID>{845AED3A-1548-4508-A6A9-2F352429901E}</ID>
            <#list tasks as task>
            <z:row
                ows_Title="${task.title}"
                ows_AssignedTo="${task.assigneeRef}"
                ows_Status="${task.status}"
                ows_Priority="${task.priority}"
                ows_DueDate="${task.dueDateTS}"
                ows_Body="${task.body}"
                ows_Created="${task.createdTS}"
                ows_Author="${task.authorRef}"
                ows_Modified="${task.modifiedTS}"
                ows_Editor="${task.editorRef}"
                ows_owshiddenversion="1"
                ows__ModerationStatus="0"
                ows__Level="1"
                ows_ID="${task.id}"
                ows_UniqueId="${task.uniqueId}"
                ows_FSObjType="1;#0"
                ows_FileRef="${task.fileRef}"
                ows_MetaInfo="1;#"
                xmlns:z="#RowsetSchema" />
            </#list>
        </List>
        <List Name="Documents">
            <ID>{${site.listUUID}}</ID>
            <#list docs as doc>
              <#if doc.folderish>
                <z:row
                    ows_FileRef="${doc.fileRef}"
                    ows_FSObjType="1"
                    xmlns:z="#RowsetSchema" />
              <#else>
                <z:row
                    ows_FileRef="${doc.fileRef}"
                    ows_Title="${doc.title}"
                    ows_FSObjType="0"
                    ows_Created="${doc.createdTS}"
                    ows_Author="${doc.authorRef}"
                    ows_Modified="${doc.modifiedTS}"
                    ows_Editor="${doc.editorRef}"
                    ows_ID="${doc.id}"
                    ows_ProgID=""
                    xmlns:z="#RowsetSchema" />
               </#if>
             </#list>
        </List>
        <List Name="Links">
            <ID>{5E00A9E6-9781-4B79-B39C-A1FBB63B2A1A}</ID>
            <#list links as link>
            <z:row
                ows_URL="${link.url}"
                ows_Comments="${link.comments}"
                ows_Created="${link.createdTS}"
                ows_Author="${link.authorRef}"
                ows_Modified="${link.modifiedTS}"
                ows_Editor="${link.editorRef}"
                ows_owshiddenversion="1"
                ows_MetaInfo="1;#"
                ows__ModerationStatus="0"
                ows__Level="1"
                ows_ID="${link.id}"
                ows_UniqueId="${link.uniqueId}"
                ows_FSObjType="1;#0"
                ows_FileRef="${link.fileRef}"
                xmlns:z="#RowsetSchema" />
              </#list>
        </List>
    </Results>
</Results>