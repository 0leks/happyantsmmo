

function connectLocalhost() {
    connectToServer("localhost");
}
function connectAzure() {
    connectToServer("spring-boot-complete-1647250737544.azurewebsites.net");
}


function connectToServer(path) {
    let endpoint = "ws://" + path + "/user";
    console.log("connecting to websocket at " + endpoint);
    let ws = new WebSocket(endpoint);
    ws.onmessage = msg => {
        console.log(msg);
    };
    ws.onclose = disconnected;
    ws.onopen = sendHello;
    ws.onerror = err => {
        console.log(err);
    }
    function disconnected() {
        // alert("WebSocket connection closed");
        console.log("WebSocket connection closed");
    }
    function sendHello() {
        console.log("connected");
        ws.send("some message asdf");
    }
}