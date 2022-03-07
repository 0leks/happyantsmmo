

//Establish the WebSocket connection and set up event handlers
console.log("connecting to websocket at /play");
let ws = new WebSocket("ws://" + location.hostname + ":7070/play");
ws.onmessage = receiveMessage;
// ws.onmessage = msg => updateChat(msg);
ws.onclose = () => alert("WebSocket connection closed");
ws.onopen = sendHello;


const accessToken = getAccessToken();
function sendHello() {
    let jsonData = {
        'type': 'HELLO',
        'token': accessToken
    };
    let tosend = JSON.stringify(jsonData);
    console.log('sending ' + tosend);
    ws.send(tosend);
}

function sendMove(x, y) {
    let data = {
        'type': 'MOVE',
        'x': x,
        'y': y
    }
    console.log(data);
    ws.send(JSON.stringify(data));
}

document.addEventListener('click', logMouseEvent);
function logMouseEvent(e) {
    console.log(e);
    // let circleElem = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
    // circleElem.setAttribute('cx', e.x);
    // circleElem.setAttribute('cy', e.y);
    // circleElem.setAttribute('r', 5);

    // id("gamesvg").appendChild(circleElem);

    sendMove(e.x, e.y);
}

document.addEventListener('keydown', logKey);
function logKey(e) {
  console.log(e.code);
}

// function sendAndClear(message) {
//     if (message !== "") {
//         ws.send(message);
//         id("message").value = "";
//     }
// }

function receiveMessage(msg) {
    let data = JSON.parse(msg.data);
    
    if (data.type == 'HELLO') {
        console.log(data);
        id("me").setAttribute("cx", data.x);
        id("me").setAttribute("cy", data.y);
    }
    else if (data.type == 'MOVE') {
        console.log(data.players.length);
        data.players.forEach(player => {
            let group = id(player.id);
            if (group == null) {
                group = document.createElementNS('http://www.w3.org/2000/svg', 'g');
                group.setAttribute('id', player.id);

                let circleElem = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
                circleElem.setAttribute('r', 50);
                group.appendChild(circleElem);

                let handleText = document.createElementNS('http://www.w3.org/2000/svg', 'text');
                handleText.innerHTML = player.handle;
                handleText.classList.add('handleText');
                group.appendChild(handleText);

                id("gamesvg").appendChild(group);
            }

            group.setAttribute('transform', 'translate(' + player.x + ',' + player.y + ')');
            // console.log('handling player info');
            // console.log(player);
        });
    }
    // id("chat").insertAdjacentHTML("afterbegin", data.userMessage);
    // id("userlist").innerHTML = data.userlist.map(user => "<li>" + user + "</li>").join("");
}
