

//Establish the WebSocket connection and set up event handlers
let ws = new WebSocket("ws://" + location.hostname + ":7070/chat");
ws.onmessage = msg => updateChat(msg);
ws.onclose = () => alert("WebSocket connection closed");

// Add event listeners to button and input field
id("send").addEventListener("click", () => sendAndClear(id("message").value));
id("message").addEventListener("keypress", function (e) {
    if (e.keyCode === 13) { // Send message if enter is pressed in input field
        sendAndClear(e.target.value);
    }
});

function sendAndClear(message) {
    if (message !== "") {
        ws.send(message);
        id("message").value = "";
    }
}

function updateChat(msg) { // Update chat-panel and list of connected users
    let data = JSON.parse(msg.data);
    id("chat").insertAdjacentHTML("afterbegin", data.userMessage);
    id("userlist").innerHTML = data.userlist.map(user => "<li>" + user + "</li>").join("");
}



id("sign-out").addEventListener("click", () => {
    signOut();
    refreshPageStatus();
});
id("sign-in").addEventListener("click", () => document.location.href="/signin");


function refreshPageStatus() {
    console.log(document.cookie);
    
    let accessToken = getAccessToken();
    console.log(accessToken);
    if (accessToken) {
        id("sign-out").classList.remove('hidden');
        id("sign-in").classList.add('hidden');
        requestAccountInfo(accessToken).then(result => {
            console.log(result);
            if (result) {
                id("handle").innerHTML = result;
            }
            else {
                signOut();
                refreshPageStatus();
            }
        })
    }
    else {
        id("sign-out").classList.add('hidden');
        id("sign-in").classList.remove('hidden');
        id("handle").innerHTML = '';
    }
    
}

refreshPageStatus();
