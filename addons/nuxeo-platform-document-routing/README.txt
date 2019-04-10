Case Management Document Routing Addon
----------------------------------------

This addon adds the "routing" functionality to Nuxeo.
A "route" is a list of steps that a list of documents will execute. 
Steps can be:

    * distribute the documents
    * modify some metadata
    * create task for the document to be reviewed

See: https://doc.nuxeo.com/display/CMDOC/Document+Routing

Build with::

  $ mvn clean install 

And deploy the three jars from the target subfolders into the nuxeo.ear/plugins or
nxserver/bundles folder of your nuxeo server (and restart).


Routing/Task integration
========================

Each time a route step is set to running, an operation chain is launched acoording to the following extension point:

  <extension target="org.nuxeo.ecm.platform.routing.service"
    point="chainsToType">
    <mapping documentType="SimpleTask" chainId="simpleTask"
      undoChainIdFromRunning="simpleUndo" undoChainIdFromDone="simpleUndo" />
  </extension>

If you want user validation, or user input, you need to create a task in that chain. For instance like

    <chain id="simpleTask">
      <operation id="Context.FetchDocument" />
      <operation id="Workflow.CreateRoutingTask">
        <param type="string" name="accept operation chain">
          setTaskDone
        </param>
        <param type="string" name="reject operation chain">
          setTaskDone
        </param>
      </operation>
    </chain>

This task first fetch the document associated to the Route, than create a Task on that document. The Workflow.CreateRoutingTask takes needed information (like actors, directive etc...) in the Step document metadatas. For that purpose, you can use the TaskStep facet and its associated task_step layout.
A task document currently only accept two operation chain. One for the accept button and one for the reject button. It's your responsability to resume the route in this operation. The easiest way being to add the Document.Routing.Task.Done operation at the end of thoses chains.


Decisional Step Implementation in CMF
======================================

    Added a new doc type DecisionalDistributionTask and overridden the ConditionalfolderFactory contribution to create a ConditionalFolder containing a DecisionalDistributionTask as the decisional step.
    the DecisionalDistributionTask is mapped to the DecisionalDistributionTaskChain (this is the automation chain executed when the step is run)
    the DecisionalDistributionTask, sets the next option to run to 1 if the current task is validated, or to 2 if the current task is refused
    <chain id="DecisionalDistributionTaskChain">
    <operation id="Case.Management.CreateCaseLink" />
    <operation id="Case.Management.Step.CaseLink.Mapping">
    <param name="actionnable" type="boolean">true</param>
    <param name="mappingProperties" type="Properties">
    <property key="dc:title">Case:dc:title</property>
    <property key="acslk:dueDate">Step:rtsk:dueDate</property>
    <property key="acslk:automaticValidation">
    Step:rtsk:automaticValidation
    </property>
    </param>
    <param name="leavingChainsProperties" type="Properties">
    <property key="validate">validateAndRemoveLinkAndExecuteOption1</property>
    <property key="refuse">refuseAndRemoveLinkAndExecuteOption2</property>
    </param>
    </operation>
    <operation id="Case.Management.Distribution" />
    </chain>


