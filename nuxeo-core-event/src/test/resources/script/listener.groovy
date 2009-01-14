
org.nuxeo.ecm.core.event.test.EventListenerTest.SCRIPT_CNT++;

System.out.println("Helo from groovy listener. Event name: "
    + event.name + ". CNT: "+org.nuxeo.ecm.core.event.test.EventListenerTest.SCRIPT_CNT);
