
let endpoint = "ws://localhost:8080/user";
console.log("connecting to websocket at " + endpoint);
let ws = new WebSocket(endpoint);
ws.onmessage = msg => {
    console.log(msg);
};
ws.onclose = disconnected;
ws.onopen = sendHello;
function disconnected() {
    // alert("WebSocket connection closed");
    console.log("WebSocket connection closed");
}
function sendHello() {
    console.log("connected");
    ws.send("some message asdf");
}
