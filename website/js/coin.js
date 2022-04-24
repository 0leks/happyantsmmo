
let WORLD_SCALE = 10;
let playerSpeed = 100*WORLD_SCALE;
let tunnelSize = 50*WORLD_SCALE;


let gl = null;
let glCanvas = null;
let textContext = null;
let textCanvas = null;

// Aspect ratio and coordinate system
// details

let aspectRatio;
let currentRotation = [0, 1];
let currentScale = [1.0, 1.0];

// Vertex information

let squareMesh = {};
let circleMesh = {};

let PLAYER_SIZE = 600;
let playerPositions = {};
let playerTargetPositions = {};
let playerInfos = {};
let COIN_SIZE = 200;
let coinPositions = {};
let coinValues = {};

let tunnelNodes = {};
let tunnelSegments = [];

// Rendering data shared with the
// scalers.

let uScalingFactor;
let uGlobalColor;
let uRotationVector;
let aVertexPosition;

// Animation timing

let previousTime = 0.0;
let degreesPerSecond = 90.0;

let loadingMessage = 'Loading... Please wait.';


let wsurl = SERVER_WEBSOCKET_PROTOCOL + SERVER_URL + "coin";
console.log("connecting to websocket at " + wsurl);
let ws = new WebSocket(wsurl);
ws.onmessage = receiveMessage;
ws.onclose = disconnected;
ws.onopen = sendHello;

if (getSessionToken() == null) {
    document.location.href = "/";
}

function disconnected() {
    alert("WebSocket connection closed");
    document.location.href = "/";
}

function exitGame() {
    document.location.href = "/";
}

function sendHello() {
    let jsonData = {
        'type': 'HELLO',
        'session': getSessionToken()
    };
    let tosend = JSON.stringify(jsonData);
    console.log('sending ' + tosend);
    ws.send(tosend);
}

function sendStopGame() {
    let jsonData = {
        'type': 'STOP'
    };
    ws.send(JSON.stringify(jsonData));
}

function homeTeleport() {
    let jsonData = {
        'type': 'TELEPORT',
        'target': 'home'
    };
    ws.send(JSON.stringify(jsonData));
}

function sendMove(x, y) {
    let data = {
        'type': 'MOVE',
        'x': x,
        'y': y
    }
    ws.send(JSON.stringify(data));
}
function sendNewTunnel(nodeid1, nodeposition1, nodeid2, nodeposition2) {
    let data = {'type': 'TUNNEL'};
    if (nodeid1) {
        data.nodeid1 = nodeid1;
    }
    else {
        data.x1 = nodeposition1[0];
        data.y1 = nodeposition1[1];
    }
    if (nodeid2) {
        data.nodeid2 = nodeid2;
    }
    else {
        data.x2 = nodeposition2[0];
        data.y2 = nodeposition2[1];
    }
    ws.send(JSON.stringify(data));
}

id("textCanvas").addEventListener('mousedown', mousePressed, false);
id("textCanvas").addEventListener('mouseup', mouseReleased, false);
id("textCanvas").addEventListener('mousemove', mouseMoved, false);
let mousePos = [0, 0];
function screenToGamePos(screenPos) {
    let mypos = getMyPosition(myID);
    let targetPos = [mypos[0] + (screenPos.x - glCanvas.width / 2) * WORLD_SCALE, mypos[1] - (screenPos.y - glCanvas.height / 2) * WORLD_SCALE];
    return targetPos;
}
function mouseMoved(e) {
    mousePos = screenToGamePos(e);
}
function getTunnelNodeIdAtPoint(point) {
    for (const [id, node] of Object.entries(tunnelNodes)) {
        let distance = Math.sqrt( (node.x - point[0]) ** 2 + (node.y - point[1]) ** 2 );
        let tunnelSize = 50*WORLD_SCALE / 2;
        if (distance < tunnelSize) {
            return node.id;
        }
    }
    return null;
}
function mousePressed(e) {

    if (e.button == 2) {
        placingNewTunnelFromNode = null;
        placingNewTunnelFromLocation = null;
        placingTunnel = false;
        updatePlaceTunnelButton();
        return;
    }

    // console.log(e);
    let mypos = getMyPosition(myID);
    let targetPos = screenToGamePos(e);


    if ( !placingTunnel) {
        // console.log('mypos:' + mypos);
        // playerTargetPositions[myID] = {
        //     'from': new Vector(mypos[0], mypos[1], 0),
        //     'to': new Vector(targetPos[0], targetPos[1], 0),
        //     'previousTime': previousTime
        // };
        sendMove(targetPos[0], targetPos[1]);
    }
    else {
        let tunnelNodeId = getTunnelNodeIdAtPoint(targetPos);
        if (tunnelNodeId) {
            placingNewTunnelFromNode = tunnelNodeId;
        }
        else {
            placingNewTunnelFromLocation = targetPos;
        }
    }
}
function mouseReleased(e) {
    if (placingTunnel) {
        if (placingNewTunnelFromNode || placingNewTunnelFromLocation) {
            let tunnelNodeId = getTunnelNodeIdAtPoint(mousePos);
            sendNewTunnel(placingNewTunnelFromNode, placingNewTunnelFromLocation, tunnelNodeId, mousePos);
            placingNewTunnelFromNode = null;
            placingNewTunnelFromLocation = null;
        }
    }
}

function receiveHelloMessage(data) {
    console.log(data);
    myID = data.id;

    if (myID == ADMIN_ID) {
        id("stopGameButton").classList.remove('hidden');
    }
}

function receiveCoinMessage(data) {
    data.coins.forEach(coin => {
        // console.log(coin);
        if ('delete' in coin) {
            delete coinPositions[coin.id];
            delete coinValues[coin.id];
        }
        else {
            coinPositions[coin.id] = [coin.x, coin.y];
            if ('value' in coin) {
                coinValues[coin.id] = coin.value;
            }
            else {
                coinValues[coin.id] = '1';
            }
        }
    });
}

function receiveTunnels(data) {
    console.log(data);
    data.tunnels.forEach(tunnel => {
        if (!(tunnel.node1.id in tunnelNodes)) {
            tunnelNodes[tunnel.node1.id] = tunnel.node1;
        }
        if (!(tunnel.node2.id in tunnelNodes)) {
            tunnelNodes[tunnel.node2.id] = tunnel.node2;
        }
        tunnelSegments.push({
            'id': tunnel.id,
            'nodeid1': tunnel.node1.id,
            'nodeid2': tunnel.node2.id
        });
    });
}

var myID;
var lastTimestamp;
function receiveMessage(msg) {
    let data = JSON.parse(msg.data);

    let timestamp = Date.now();
    let status = `${timestamp % 10000} delta ${timestamp - lastTimestamp}: ${data.type}: `;
    lastTimestamp = timestamp;

    if (data.type == 'HELLO') {
        receiveHelloMessage(data);
    }
    else if (data.type == 'BYE') {
        console.log(data);
        signOut();
        alert(data.message);
        document.location.href = "/";
    }
    else if (data.type == 'DC') {
        console.log(data);
        playersDisconnected(data.ids);
    }
    else if (data.type == 'STOP') {
        console.log(data);
        alert(data.message);
    }
    else if (data.type == 'MAPPING') {
        console.log(data);
        for (const [id, handle] of Object.entries(data)) {
            if (id != 'MAPPING') {
                updatePlayerInfo(id, 'handle', handle);
            }
        }
        console.log(playerInfos);
    }
    else if (data.type == 'TUNNEL') {
        receiveTunnels(data);
    }
    else if (data.type == 'COIN') {
        receiveCoinMessage(data);
    }
    else if (data.type == 'MOVE') {
        if ('players' in data) {
            status += `${data.players.length} players, `;
            // console.log(data.players);
            data.players.forEach(player => {
                playerPositions[player.id] = [player.x, player.y];
                updatePlayerInfo(player.id, 'id', player.id);
                updatePlayerInfo(player.id, 'numcoins', player.numcoins);
                updatePlayerInfo(player.id, 'tunnelingLevel', player.tunnelingLevel);


                if ('target' in player) {
                    // if (player.id == myID && playerTargetPositions[myID]) {
                    //     playerTargetPositions[myID].from = new Vector(player.x, player.y, 0);
                    //     playerTargetPositions[myID].previousTime = previousTime;
                    // }
                    // else {
                        playerTargetPositions[player.id] = {
                            'from': new Vector(player.x, player.y, 0),
                            'to': new Vector(player.target.x, player.target.y, 0),
                            'previousTime': previousTime
                        };
                    // }
                }
                else {
                    delete playerTargetPositions[player.id];
                }
            });

        }

        if ('coins' in data) {
            status += `${data.coins.length} coins, `;
            receiveCoinMessage(data);
        }
        loadingMessage = '';
    }
    console.log(status);
}
function playersDisconnected(ids) {
    ids.forEach(id => {
        if (id != myID) {
            delete playerInfos[id];
            delete playerPositions[id];
            delete playerTargetPositions[id];
        }
    });
}

let UNLOCK_TUNNELING_COST = 1000;
let TUNNEL_COST = 1100;
let placingTunnel = false;
let placingNewTunnelFromNode = null;
let placingNewTunnelFromLocation = null;
function placeTunnel() {
    if (playerInfos[myID].numcoins >= TUNNEL_COST) {
        console.log("Placing tunnel");
        placingTunnel = true;
        updatePlaceTunnelButton();
    }
}

function unlockTunneling() {
    let tosend = JSON.stringify({
        'type': 'UNLOCK',
        'skill': 'tunneling'
    });
    console.log('sending ' + tosend);
    ws.send(tosend);
}

function openSkillInfo() {
    unhideElement(id("skillInfo"));
}

function closeSkillInfo() {
    hideElement(id("skillInfo"));
}

function getLevelFromExperience(exp) {
    if (exp <= 0) {
        return 0;
    }
    else if (exp >= 969819) {
        return 99;
    }
    else {
        return Math.floor(14*Math.log(exp + 825) - 94);
    }
}

let experienceTable = [0,61,126,197,272,353,441,534,635,743,859,984,1117,1261,1416,1581,1760,1951,2156,2377,
    2614,2869,3142,3436,3751,4090,4454,4845,5265,5716,6200,6720,7279,7879,8523,9215,9958,10757,11614,12535,
    13524,14587,15728,16953,18270,19684,21202,22833,24584,26466,28486,30657,32988,35491,38180,41068,44170,
    47501,51079,54922,59050,63483,68244,73358,78851,84750,91086,97891,105200,113051,121482,130538,140264,
    150710,161930,173981,186924,200825,215755,231791,249014,267513,287381,308720,331639,356255,382694,411090,
    441589,474346,509528,547315,587900,631490,678308,728592,782599,840605,902905,969819, 969819+1];
function getPercentToNextLevel(exp) {
    if (exp >= experienceTable[99]) {
        return {base: experienceTable[99], next: experienceTable[100]}
    }
    let currentLevel = getLevelFromExperience(exp);
    let currentLevelExp = experienceTable[currentLevel];
    let nextLevelExp = experienceTable[currentLevel + 1];
    return {base: currentLevelExp, next: nextLevelExp};
}

function testLevelFromExperience() {
    let previousLevel = -1;
    let previousExp = 0;
    for (let exp = 0; exp < 1000000; exp += 1) {
        var level = getLevelFromExperience(exp);
        let roundedLevel = Math.floor(level);
        
        if (roundedLevel > previousLevel) {
            var deltaExp = exp - previousExp;
            console.log(exp + ' (+' + deltaExp + ') -> ' + level)
            previousLevel = roundedLevel;
            previousExp = exp;
        }
    }
}

window.addEventListener("load", startup, false);

let placeTunnelButton = id("tunnelButton");
let unlockTunnelingButton = id("unlockTunnelingButton");
let tunnelingSkillButton = id("skillInfoButton");
function updatePlaceTunnelButton() {
    if ('tunnelingLevel' in playerInfos[myID]) {
        let currentExp = playerInfos[myID].tunnelingLevel;
        let currentLevel = getLevelFromExperience(currentExp);
        let experienceRange = getPercentToNextLevel(currentExp);
        id("tunnelingLevel").innerHTML = currentLevel;
        id("tunnelingLevelProgress").min = experienceRange.base;
        id("tunnelingLevelProgress").max = experienceRange.next;
        id("tunnelingLevelProgress").value = currentExp;
        id("tunnelingLevelProgress").title = currentExp + '/' + experienceRange.next;
    }

    if (placingTunnel) {
        placeTunnelButton.disabled = true;
        return;
    }
    if (playerInfos[myID].tunnelingLevel > 0) {
        placeTunnelButton.disabled = false;
        unhideElement(placeTunnelButton)
        
        unlockTunnelingButton.disabled = true;
        hideElement(unlockTunnelingButton);

        if (playerInfos[myID].tunnelingLevel > 1) {
            unhideElement(tunnelingSkillButton);
            tunnelingSkillButton.disabled = false;
        }
        unhideElement(tunnelingSkillButton);
        tunnelingSkillButton.disabled = false;

    }
    else {
        placeTunnelButton.disabled = true;
        
        if (playerInfos[myID].numcoins >= UNLOCK_TUNNELING_COST) {
            unhideElement(unlockTunnelingButton);
            unlockTunnelingButton.disabled = false;
        }
    }
}

function updatePlayerInfo(pid, key, value) {
    if (!(pid in playerInfos)) {
        playerInfos[pid] = {};
    }
    playerInfos[pid][key] = value;

    if (pid == myID) {
        if(key == 'numcoins' || key == 'tunnelingLevel') {
            updatePlaceTunnelButton();
        }
    }
}

function getPlayerInfo(id, key) {
    if (id in playerInfos) {
        if (key in playerInfos[id]) {
            return playerInfos[id][key];
        }
        else {
            return 'null';
        }
    }
    else {
        return 'player not found';
    }
}

function getMyPosition(playerid) {
    if (playerid in playerPositions) {
        let delta2 = new Vector(0, 0);
        if (playerid in playerTargetPositions) {
            let current = new Vector(playerTargetPositions[playerid].from.x, playerTargetPositions[playerid].from.y, 0);
            let target = new Vector(playerTargetPositions[playerid].to.x, playerTargetPositions[playerid].to.y, 0);

            let deltaTime = previousTime - playerTargetPositions[playerid].previousTime;
            let secondsElapsed = deltaTime / 1000;

            let deltaVector = target.subtract(current);
                delta2 = deltaVector.unit().multiply(secondsElapsed * playerSpeed);
                if (delta2.length() > deltaVector.length()) {
                    delta2 = deltaVector;
                    delete playerTargetPositions[playerid];
                }
                let estimatedPos = current.add(delta2);
                playerPositions[playerid][0] = estimatedPos.x;
                playerPositions[playerid][1] = estimatedPos.y;
        }
        return [playerPositions[playerid][0], playerPositions[playerid][1]];
    }
    return [0, 0];
}

function makeSquareMesh() {
    let size = 1;
    let vertices = [
        - size / 2, + size / 2,
        + size / 2, + size / 2,
        + size / 2, - size / 2,
        - size / 2, - size / 2];
    let indices = [
        0, 1, 2,
        0, 2, 3
    ];

    squareMesh.vertexArray = Float32Array.from(vertices);
    squareMesh.vertexBuffer = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, squareMesh.vertexBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, squareMesh.vertexArray, gl.STATIC_DRAW);
    squareMesh.vertexNumComponents = 2;
    squareMesh.vertexCount = squareMesh.vertexArray.length / squareMesh.vertexNumComponents;

    squareMesh.indexArray = Uint16Array.from(indices);
    squareMesh.indexBuffer = gl.createBuffer();
    gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, squareMesh.indexBuffer);
    gl.bufferData(gl.ELEMENT_ARRAY_BUFFER, squareMesh.indexArray, gl.STATIC_DRAW);
    squareMesh.indexCount = squareMesh.indexArray.length;
}

function makeCircleMesh() {
    let size = 1;
    let numSegments = 16;
    let vertices = [0, 0];
    let indices = [];
    for (let i = 0; i < numSegments; i++) {
        let radians = i * 2 * Math.PI / numSegments;
        let x = Math.cos(radians) * size/2;
        let y = Math.sin(radians) * size/2;
        vertices.push(...[x, y]);

        if (i > 0) {
            indices.push(...[0, i+1, i]);
        }
    }
    indices.push(...[0, 1, numSegments]);

    circleMesh.vertexArray = Float32Array.from(vertices);
    circleMesh.vertexBuffer = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, circleMesh.vertexBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, circleMesh.vertexArray, gl.STATIC_DRAW);
    circleMesh.vertexNumComponents = 2;
    circleMesh.vertexCount = circleMesh.vertexArray.length / circleMesh.vertexNumComponents;

    circleMesh.indexArray = Uint16Array.from(indices);
    circleMesh.indexBuffer = gl.createBuffer();
    gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, circleMesh.indexBuffer);
    gl.bufferData(gl.ELEMENT_ARRAY_BUFFER, circleMesh.indexArray, gl.STATIC_DRAW);
    circleMesh.indexCount = circleMesh.indexArray.length;
}

function startup() {
    glCanvas = document.getElementById("glcanvas");
    glCanvas.width = document.body.clientWidth;
    glCanvas.height = document.body.clientHeight;
    gl = glCanvas.getContext("webgl");

    const shaderSet = [
        {
            type: gl.VERTEX_SHADER,
            id: "vertex-shader"
        },
        {
            type: gl.FRAGMENT_SHADER,
            id: "fragment-shader"
        }
    ];

    shaderProgram = buildShaderProgram(shaderSet);

    aspectRatio = glCanvas.width / glCanvas.height;
    currentRotation = [0, 1];
    // currentScale = [1.0, aspectRatio];
    let scale = 1;
    currentScale = [2 * scale / glCanvas.width / WORLD_SCALE, 2 * scale / glCanvas.height / WORLD_SCALE];

    makeSquareMesh(0, 0);
    makeCircleMesh();

    currentAngle = 0.0;
    
    textCanvas = id("textCanvas");
    textCanvas.width = document.body.clientWidth;
    textCanvas.height = document.body.clientHeight;
    textContext = textCanvas.getContext("2d");
    textContext.textBaseline = "middle";
    textContext.textAlign = "center";

    animateScene();
}

function buildShaderProgram(shaderInfo) {
    let program = gl.createProgram();

    shaderInfo.forEach(function (desc) {
        let shader = compileShader(desc.id, desc.type);

        if (shader) {
            gl.attachShader(program, shader);
        }
    });

    gl.linkProgram(program)

    if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
        console.log("Error linking shader program:");
        console.log(gl.getProgramInfoLog(program));
    }

    return program;
}

function compileShader(id, type) {
    let code = document.getElementById(id).firstChild.nodeValue;
    let shader = gl.createShader(type);

    gl.shaderSource(shader, code);
    gl.compileShader(shader);

    if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
        console.log(`Error compiling ${type === gl.VERTEX_SHADER ? "vertex" : "fragment"} shader:`);
        console.log(gl.getShaderInfoLog(shader));
    }
    return shader;
}

function drawMeshes(mesh, positions, scale) {

    let uScale = gl.getUniformLocation(shaderProgram, "uScale");
    gl.uniform1f(uScale, scale);


    gl.bindBuffer(gl.ARRAY_BUFFER, mesh.vertexBuffer);
    aVertexPosition = gl.getAttribLocation(shaderProgram, "aVertexPosition");
    gl.enableVertexAttribArray(aVertexPosition);
    gl.vertexAttribPointer(aVertexPosition, mesh.vertexNumComponents, gl.FLOAT, false, 0, 0);

    gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, mesh.indexBuffer);

    uPositionVector = gl.getUniformLocation(shaderProgram, "uPositionVector");
    for (const [id, pos] of Object.entries(positions)) {
        gl.uniform2fv(uPositionVector, pos);
        gl.drawElements(gl.TRIANGLES, mesh.indexCount, gl.UNSIGNED_SHORT, 0);
    }
}

function drawRoom(gl, uGlobalColor, color1, color2, position, scale) {
    gl.uniform4fv(uGlobalColor, color1);
    drawMeshes(squareMesh, {0: position}, WORLD_SCALE * scale);
    
    gl.uniform4fv(uGlobalColor, color2);
    drawMeshes(squareMesh, {0: position}, WORLD_SCALE * scale * 16/20);

    gl.uniform4fv(uGlobalColor, color1);
    drawMeshes(squareMesh, {0: position}, WORLD_SCALE * scale * 8/20);

    gl.uniform4fv(uGlobalColor, color2);
    drawMeshes(squareMesh, {0: position}, WORLD_SCALE * scale / 20);
}

function drawTunnelNode(node) {
    textContext.fillStyle = 'rgba(100, 100, 100, 0.5)';
    textContext.strokeStyle = 'rgba(255, 255, 255, 0.6)';
    textContext.lineWidth = WORLD_SCALE;
    textContext.beginPath();
    textContext.arc(node.x, -node.y, tunnelSize/2, 0, 2 * Math.PI);
    textContext.fill();
    textContext.stroke();
    textContext.fillStyle = 'rgba(255, 255, 255, 1)';
    textContext.beginPath();
    textContext.arc(node.x, -node.y, tunnelSize/10, 0, 2 * Math.PI);
    textContext.fill();
}

function drawTunnelSegment(node1, node2) {
    textContext.strokeStyle = 'rgba(100, 100, 100, 0.5)';
    textContext.lineWidth = tunnelSize;
    // textContext.lineCap = 'square';
    textContext.beginPath();
    textContext.moveTo(node1.x, -node1.y);
    textContext.lineTo(node2.x, -node2.y);
    textContext.stroke();
    textContext.strokeStyle = 'rgba(255, 255, 255, 1)';
    textContext.lineWidth = WORLD_SCALE;
    textContext.stroke();
}

function drawTargetX(position) {
    let x_size = PLAYER_SIZE/2;
    textContext.strokeStyle = 'rgba(255, 60, 60, 0.5)';
    textContext.lineWidth = 50;

    textContext.beginPath();
    textContext.moveTo(position.x + x_size, -position.y + x_size);
    textContext.lineTo(position.x - x_size, -position.y - x_size);

    textContext.moveTo(position.x + x_size, -position.y - x_size);
    textContext.lineTo(position.x - x_size, -position.y + x_size);
    textContext.stroke(); 
}



let cameraTranslate = [0, 0];
function animateScene() {

    gl.viewport(0, 0, glCanvas.width, glCanvas.height);
    gl.clearColor(0, 0, 0, 1);
    gl.clear(gl.COLOR_BUFFER_BIT);


    gl.useProgram(shaderProgram);

    uRotationVector = gl.getUniformLocation(shaderProgram, "uRotationVector");
    uScalingFactor = gl.getUniformLocation(shaderProgram, "uScalingFactor");
    uGlobalColor = gl.getUniformLocation(shaderProgram, "uGlobalColor");

    gl.uniform2fv(uScalingFactor, currentScale);

    uCameraTranslate = gl.getUniformLocation(shaderProgram, "uCameraTranslate");
    gl.uniform2fv(uCameraTranslate, cameraTranslate);


    // DRAW BACKGROUND SQUARES
    drawRoom(gl, uGlobalColor, [0.8, 0.9, 1.0, 1.0], [0.7, 0.8, 0.9, 1.0], [0, 0], 2000);
    drawRoom(gl, uGlobalColor, [0.7, 0.9, 0.7, 1.0], [0.7, 0.6, 0.5, 1.0], [30000, -15000], 1000);
    drawRoom(gl, uGlobalColor, [0.7, 0.9, 0.7, 1.0], [0.7, 0.6, 0.5, 1.0], [30000, 0], 1000);
    drawRoom(gl, uGlobalColor, [0.7, 0.9, 0.7, 1.0], [0.7, 0.6, 0.5, 1.0], [30000, 15000], 1000);

    let radians = currentAngle * Math.PI / 180.0;
    currentRotation[0] = Math.sin(radians);
    currentRotation[1] = Math.cos(radians);
    gl.uniform2fv(uRotationVector, currentRotation);

    // DRAW PLAYERS
    gl.uniform4fv(uGlobalColor, [0.1, 0.7, 0.2, 1.0]);
    let pos = [];
    for (playerid in playerPositions) {
        pos.push(getMyPosition(playerid));
    }
    drawMeshes(squareMesh, pos, PLAYER_SIZE);

    // DRAW COINS
    gl.uniform4fv(uGlobalColor, [.9, 0.6, 0.2, 1.0]);
    drawMeshes(circleMesh, coinPositions, COIN_SIZE);
    
    gl.uniform2fv(uRotationVector, [1, 0]);

    


    textContext.clearRect(0, 0, textContext.canvas.width, textContext.canvas.height);
    textContext.strokeRect(0, 0, textContext.canvas.width, textContext.canvas.height);

    textContext.save();
    textContext.translate(textContext.canvas.width/2, textContext.canvas.height/2);
    textContext.scale(1/WORLD_SCALE, 1/WORLD_SCALE);
    textContext.translate(-cameraTranslate[0], cameraTranslate[1]);

    textContext.font = '160px serif';
    for (const [id, coinPos] of Object.entries(coinPositions)) {
        textContext.fillText(coinValues[id], coinPos[0], -coinPos[1]);
    }
    textContext.font = '240px serif';
    for (const [id, playerinfo] of Object.entries(playerInfos)) {
        if (id in playerPositions) {
            // textContext.fillText('id' + playerinfo.id, playerPositions[id][0], -playerPositions[id][1] - 10*WORLD_SCALE);
            textContext.fillText(getPlayerInfo(id, 'handle'), playerPositions[id][0], -playerPositions[id][1] - 10*WORLD_SCALE);
            textContext.fillText('' + getPlayerInfo(id, 'numcoins'), playerPositions[id][0], -playerPositions[id][1] + 15*WORLD_SCALE);
            // textContext.fillText('tunnel: ' + getPlayerInfo(id, 'tunnelingLevel'), playerPositions[id][0], -playerPositions[id][1] + 40*WORLD_SCALE);
        }
    }
    textContext.fillText(loadingMessage, 0, 0);
    
    tunnelSegments.forEach(tunnel => {
        drawTunnelSegment(tunnelNodes[tunnel.nodeid1], tunnelNodes[tunnel.nodeid2]);
    });

    for (const [id, node] of Object.entries(tunnelNodes)) {
        drawTunnelNode(node);
    }

    if (placingTunnel && placingNewTunnelFromNode) {
        drawTunnelSegment(tunnelNodes[placingNewTunnelFromNode], {x: mousePos[0], y: mousePos[1]});
    }
    if (placingTunnel && placingNewTunnelFromLocation) {
        drawTunnelSegment({x: placingNewTunnelFromLocation[0], y: placingNewTunnelFromLocation[1]}, 
                        {x: mousePos[0], y: mousePos[1]});
        drawTunnelNode({x: placingNewTunnelFromLocation[0], y: placingNewTunnelFromLocation[1]})
    }
    
    if (placingTunnel) {
        drawTunnelNode({x: mousePos[0], y: mousePos[1]})
    }

    if (myID in playerTargetPositions) {
        drawTargetX(playerTargetPositions[myID].to);
    }

    textContext.restore();






    window.requestAnimationFrame(function (currentTime) {
        //   let deltaAngle = ((currentTime - previousTime) / 1000.0) * degreesPerSecond;
        // currentAngle = (currentAngle + deltaAngle) % 360;

        
        let deltaTime = currentTime - previousTime;
        previousTime = currentTime;



        let mypos = getMyPosition(myID);
        let posVec = new Vector(mypos[0], mypos[1], 0);
        let cameraVec = new Vector(cameraTranslate[0], cameraTranslate[1], 0);
        let adjustmentVec = posVec.subtract(cameraVec);
        let distance = adjustmentVec.length();
        let maxDistance = deltaTime * playerSpeed / 1000;
        // console.log("distance: " + distance + ', maxDistance: ' + maxDistance);
        if (distance <= maxDistance) {
            cameraTranslate = mypos;
        }
        else {
            let ratio = distance/maxDistance;
            let newCameraVec = adjustmentVec.unit().multiply(maxDistance*Math.sqrt(ratio));
            cameraTranslate = [cameraTranslate[0] + newCameraVec.x, cameraTranslate[1] + newCameraVec.y];
        }
        animateScene();
    });
}

