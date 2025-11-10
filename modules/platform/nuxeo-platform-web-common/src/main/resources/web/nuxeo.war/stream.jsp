<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%
  String context = request.getContextPath();
%>
<html>
<head>
  <title>Stream Management</title>
  <meta charset="utf-8">
  <script type="text/javascript">
    const STREAMS_URL = "<%=context%>/api/v1/management/stream/streams";
    const CONSUMERS_URL = "<%=context%>/api/v1/management/stream/consumers";
    const STREAM_ESS_URL = "<%=context%>/api/v1/management/stream/cat";

    let currentStream = "";
    let currentConsumer = "";
    let recordCounter = 0;
    let disconnect = false;
    const validConsumerURLs = {}

    window.onload = function () {
      const streamSelect = document.getElementById("streams");
      streamSelect.onchange = function () {
        currentStream = streamSelect.value;
        const xmlhttp2 = new XMLHttpRequest();
        xmlhttp2.onreadystatechange = function () {
          if (this.readyState == 4 && this.status == 200) {
            const myArr = validateInput(JSON.parse(this.responseText));
            parseConsumerList(myArr);
          }
        };
        xmlhttp2.open("GET", validConsumerURLs[currentStream], true);
        xmlhttp2.send();
      }

      const xmlhttp = new XMLHttpRequest();
      xmlhttp.onreadystatechange = function () {
        if (this.readyState == 4 && this.status == 200) {
          let myArr = validateInput(JSON.parse(this.responseText));
          myArr = myArr.sort((a, b) => a.name.localeCompare(b.name));
          parseStreamList(myArr);
        }
      };
      xmlhttp.open("GET", STREAMS_URL, true);
      xmlhttp.send();
    }

    function validateInput(arr) {
      if (!Array.isArray(arr)) {
        throw new Error("Invalid input: " + JSON.stringify(arr));
      }
      arr.forEach((element) => {
        Object.entries(element).forEach(([key, value]) => {
          if (!isValidInput(key) || !isValidInput(value)) {
            throw new Error("Invalid input: " + JSON.stringify(arr));
          }
        });
      });
      return arr;
    }

    function isValidInput(input) {
      return /^[a-z0-9/_-]+$/i.test(input)
    }

    function parseStreamList(arr) {
      const select = document.getElementById("streams");
      arr.forEach((element) => {
        const streamName = element.name;
        const streamPartitions = element.partitions;
        const option = document.createElement("option");
        option.setAttribute("value", streamName);
        option.textContent = streamName + " (" + streamPartitions + " partitions)";
        select.add(option);
        validConsumerURLs[streamName] = CONSUMERS_URL + "?stream=" + streamName
      });
      // Explicitly add log4j stream, because it is not part of a stream processor
      const option = document.createElement("option");
      option.setAttribute("value", "source/log4j");
      option.textContent = "source/log4j";
      select.add(option);
      select.value = arr[0].name;
      select.dispatchEvent(new Event('change'));
    }

    function parseConsumerList(arr) {
      const select = document.getElementById("consumers");
      select.options.length = 0;
      arr.forEach((element) => {
        const consumer = element.consumer;
        const option = document.createElement("option");
        option.setAttribute("value", consumer);
        option.textContent = consumer;
        select.add(option);
      });
    }

    var es;

    function startES() {
      console.log("Starting ES");
      if (es !== undefined) {
        es.close();
      }
      disconnect = false;
      currentCounter = 0;
      let url = STREAM_ESS_URL + "?stream=" + currentStream;
      if (document.getElementById("fromGroup").checked) {
        url = url + "&fromGroup=" + document.getElementById("consumers").value;
      } else if (document.getElementById("fromOffset").checked) {
        url = url + "&fromOffset=" + document.getElementById("offset").value;
        url = url + "&partition=" + document.getElementById("partition").value;
      } else if (document.getElementById("fromTail").checked) {
        url = url + "&fromTail=true"
      }
      url = url + "&rewind=" + document.getElementById("rewind").value;
      url = url + "&timeout=" + document.getElementById("timeout").value;
      url = url + "&limit=" + document.getElementById("limit").value;
      document.getElementById("message").innerText = "Fetching records ...";
      es = new EventSource(url, {withCredentials: true});
      es.onopen = e => console.log("ES open " + url);
      es.onerror = e => stopES(e);
      es.onmessage = e => displayMessage(e.data);
    }

    function stopES(error) {
      if (es !== undefined) {
        console.log("Stop ES ", error);
        es.close();
        delete es;
        let message = "Fetched: " + currentCounter + " records."
        if (error != null && !disconnect) {
          message = message + " <b>Terminate on error see console log.</b>";
        }
        document.getElementById("message").innerHTML = message;
      }
    }

    function clearMessages() {
      document.getElementById("records").innerText = "";
    }

    function displayMessage(event) {
      // console.log("Got data: " + event);
      let json = JSON.parse(event);
      if (json.type == "record") {
        currentCounter++;
      }
      document.getElementById("records").insertAdjacentText('afterbegin', JSON.stringify(json, undefined, 2) + ",\n");
      if (json.type == "disconnect") {
        disconnect = true;
      }
    }

  </script>
</head>
<body>
<h1>Stream Management</h1>

Streams:
<select id="streams">
</select>

<fieldset>
  <legend>Position</legend>
  <div>
    <input type="radio" name="from" id="fromGroup" checked>
    <label for="fromGroup">From a consumer position:</label><select id="consumers"></select>
  </div>
  <div>
    <input type="radio" name="from" id="fromBeginning">
    <label for="fromBeginning">From beginning</label>
  </div>
  <div>
    <input type="radio" name="from" id="fromTail">
    <label for="fromTail">From tail</label>
  </div>
  <div>
    <input type="radio" name="from" id="fromOffset">
    <label for="fromOffset">From offset:</label><input type="number" id="offset" min="0">
    <label for="partition">partition: </label><input type="number" id="partition" min="0" max="125">
  </div>

  <div>
    <label for="rewind">Rewind: </label>
    <select id="rewind">
      <option value="0">0</option>
      <option value="1">1</option>
      <option value="2">2</option>
      <option value="3">3</option>
      <option value="4">4</option>
      <option value="5">5</option>
      <option value="10">10</option>
      <option value="20">20</option>
    </select>
    <label for="limit">Limit: </label>
    <select id="limit">
      <option value="1">1</option>
      <option value="2">2</option>
      <option value="3">3</option>
      <option value="4">4</option>
      <option value="5">5</option>
      <option value="10">10</option>
      <option value="20">20</option>
      <option value="50">50</option>
      <option value="100">100</option>
      <option value="1000">1000</option>
      <!-- option value="-1">Unlimited</option-->
    </select>
    <label for="timeout">Timeout: </label>
    <select id="timeout">
      <option value="1ms">0s</option>
      <option value="5s">5s</option>
      <option value="10s">10s</option>
      <option value="30s">30s</option>
      <option value="60s">1min</option>
      <option value="300s">5min</option>
      <option value="600s">10min</option>
    </select>
  </div>

</fieldset>
<div>
  <button onclick="startES()">View Records</button>
  <button onclick="stopES(null)">Stop</button>
  <button onclick="clearMessages()">Clear</button>
  <span id="message"></span>
</div>
<div>
  <textarea id="records" cols="80" rows="20"></textarea>
</div>
</body>
</html>
