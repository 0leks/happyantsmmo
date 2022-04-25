
let WORLD_SCALE = 10;
let playerSpeed = 100*WORLD_SCALE;
let tunnelSize = 49*WORLD_SCALE;


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

let centeredSquareMesh = {};
let cornerSquareMesh = {};
let circleMesh = {};

let PLAYER_SIZE = 600;
let playerPositions = {};
let playerTargetPositions = {};
let playerInfos = {};
let COIN_SIZE = 200;
let coinPositions = {};
let coinValues = {};

let tunnelNodes = {};
let tunnelSegments = {};

let myPosition;
class TunnelingStatus {
    tunnelingLevel;
    startTunnelNodeId;
    tunnelStartPosition;
    endTunnelNodeId;
    tunnelEndPosition;
}

let myTunnelingStatus = new TunnelingStatus();

class Room {
    x;
    y;
    width;
    height;
    color1;
    color2;
    constructor(x, y, width, height, color1, color2) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color1 = color1;
        this.color2 = color2;
    }
}

let cameraTranslate = [0, 0];

let colorA = [0.8, 0.9, 1.0, 1.0];
let colorB = [0.7, 0.8, 0.9, 1.0];
let colorC = [0.7, 0.9, 0.7, 1.0];
let colorD = [0.7, 0.6, 0.5, 1.0];
let rooms = [
    new Room(-10000, -10000, 20000, 20000, colorA, colorB),
    new Room( 25000, -20000, 10000, 10000, colorC, colorD),
    new Room( 25000, -5000, 10000, 10000, colorC, colorD),
    new Room( 25000, 10000, 10000, 10000, colorC, colorD),
];

// let startTunnelNodeId = getTunnelNodeIdAtPoint(playerPositions[myID]);
// let endTunnelNodeId = getTunnelNodeIdAtPoint(mousePos);
// let tunnelStart = {x: playerPositions[myID][0], y: playerPositions[myID][1]};
// let tunnelEnd = {x: mousePos[0], y: mousePos[1]};
// if (startTunnelNodeId) {
//     tunnelStart = tunnelNodes[startTunnelNodeId];
// }
// if (endTunnelNodeId) {
//     tunnelEnd = tunnelNodes[endTunnelNodeId];
// }



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

function openAdminPanel() {
    unhideElement(id("adminPanel"));
}

function sendTunnelingExp() {
    let jsonData = {
        'type': 'TEST',
        'setTunnelingExp': id("adminInput").value
    };
    let tosend = JSON.stringify(jsonData);
    console.log('sending ' + tosend);
    ws.send(tosend);
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
function sendNewTunnel(myTunnelingStatus) {
    let data = {'type': 'TUNNEL'};
    if (myTunnelingStatus.startTunnelNodeId) {
        data.nodeid1 = myTunnelingStatus.startTunnelNodeId;
    }
    else {
        data.x1 = myTunnelingStatus.tunnelStartPosition.x;
        data.y1 = myTunnelingStatus.tunnelStartPosition.y;
    }
    if (myTunnelingStatus.endTunnelNodeId) {
        data.nodeid2 = myTunnelingStatus.endTunnelNodeId;
    }
    else {
        data.x2 = myTunnelingStatus.tunnelEndPosition.x;
        data.y2 = myTunnelingStatus.tunnelEndPosition.y;
    }
    ws.send(JSON.stringify(data));
    placingTunnel = false;
}

function sendCollapseTunnel(myTunnelingStatus) {
    if (myTunnelingStatus.startTunnelNodeId 
        && myTunnelingStatus.endTunnelNodeId) {
        let data = {
            'type': 'TUNNEL',
            'collapse': {
                nodeid1: myTunnelingStatus.startTunnelNodeId,
                nodeid2: myTunnelingStatus.endTunnelNodeId
            }
        };
        ws.send(JSON.stringify(data));
        collapsingTunnel = false;
    }
}

id("textCanvas").addEventListener('mousedown', mousePressed, false);
id("textCanvas").addEventListener('mouseup', mouseReleased, false);
id("textCanvas").addEventListener('mousemove', mouseMoved, false);
let mousePos = [0, 0];
function screenToGamePos(screenPos) {
    let mypos = getPlayerPosition(myID);
    let targetPos = [mypos[0] + (screenPos.x - glCanvas.width / 2) * WORLD_SCALE, mypos[1] - (screenPos.y - glCanvas.height / 2) * WORLD_SCALE];
    return targetPos;
}
function mouseMoved(e) {
    mousePos = screenToGamePos(e);
}
function getTunnelNodeIdAtPoint(point) {
    for (const [id, node] of Object.entries(tunnelNodes)) {
        if ('delete' in node) {
            continue;
        }
        let distance = Math.sqrt( (node.x - point[0]) ** 2 + (node.y - point[1]) ** 2 );
        let tunnelSize = 50*WORLD_SCALE / 2;
        if (distance < tunnelSize) {
            return node.id;
        }
    }
    return null;
}
function isInRoom(point) {
    for (const room of rooms) {
        if (room.x <= point.x && point.x <= room.x + room.width
            && room.y <= point.y && point.y <= room.y + room.height) {
                return true;
        }
    }
    return false;
}
function mousePressed(e) {
    mouseMoved(e);
    if (e.button == 2) {
        placingTunnel = false;
        collapsingTunnel = false;
        updatePlaceTunnelButton();
        return;
    }
    if ( !placingTunnel && !collapsingTunnel) {
        sendMove(mousePos[0], mousePos[1]);
    }
}
function mouseReleased(e) {
    mouseMoved(e);
    if (placingTunnel) {
        sendNewTunnel(myTunnelingStatus);
    }
    if (collapsingTunnel) {
        sendCollapseTunnel(myTunnelingStatus);
    }
}

function receiveHelloMessage(data) {
    console.log(data);
    myID = data.id;

    if (myID == ADMIN_ID) {
        unhideElement(id("stopGameButton"));
        unhideElement(id("adminButton"));
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
    if ('tunnelnodes' in data) {
        data.tunnelnodes.forEach(node => {
            if ('delete' in node) {
                tunnelNodes[node.id] = node;
                // TODO need to make sure this doesnt break any segments
                // delete tunnelNodes[node.id];
            }
            else {
                tunnelNodes[node.id] = node;
            }
        });
    }
    if ('tunnelsegments' in data) {
        data.tunnelsegments.forEach(tunnel => {
            if ('delete' in tunnel) {
                delete tunnelSegments[tunnel.id];
            }
            else {
                tunnelSegments[tunnel.id] = {
                    'id': tunnel.id,
                    'nodeid1': tunnel.node1,
                    'nodeid2': tunnel.node2
                };
            }
        });
    }
}

function myInfoUpdated() {
    
    updatePlaceTunnelButton();
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
                updatePlayerInfo(player.id, 'tunnelingExp', player.tunnelingExp);

                if ('target' in player) {
                    playerTargetPositions[player.id] = {
                        'from': new Vector(player.x, player.y, 0),
                        'to': new Vector(player.target.x, player.target.y, 0),
                        'previousTime': previousTime
                    };
                }
                else {
                    delete playerTargetPositions[player.id];
                }

                if (player.id == myID) {
                    myInfoUpdated();
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
let TUNNEL_COST = 0;
let placingTunnel = false;
let collapsingTunnel = false;
function placeTunnel() {
    if (playerInfos[myID].numcoins >= TUNNEL_COST) {
        console.log("Placing tunnel");
        placingTunnel = true;
        updatePlaceTunnelButton();
    }
}

function collapseTunnel() {
    collapsingTunnel = true;
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

function closeParent(element) {
    hideElement(element.parentNode);
}

let experienceTable = [0,1,66,137,212,293,381,474,575,683,799,924,1057,1201,1356,1521,1700,1891,2096,2317,
    2554,2809,3082,3376,3691,4030,4394,4785,5205,5656,6140,6660,7219,7819,8463,9155,9898,10697,11554,12475,
    13464,14527,15668,16893,18210,19624,21142,22773,24524,26406,28426,30597,32928,35431,38120,41008,44110,
    47441,51019,54862,58990,63423,68184,73298,78791,84690,91026,97831,105140,112991,121422,130478,140204,
    150650,161870,173921,186864,200765,215695,231731,248954,267453,287321,308660,331579,356195,382634,411030,
    441529,474286,509468,547255,587840,631430,678248,728532,782539,840545,902845,969759,969759+1];

function getLevelFromExperience(exp) {
    if (exp <= 0) {
        return 0;
    }
    else if (exp >= experienceTable[99]) {
        return 99;
    }
    else {
        return Math.floor(14*Math.log(exp + 885) - 94);
    }
}
function getPercentToNextLevel(exp) {
    if (exp >= experienceTable[99]) {
        return {base: experienceTable[99], next: experienceTable[100]}
    }
    let currentLevel = getLevelFromExperience(exp);
    let currentLevelExp = experienceTable[currentLevel];
    let nextLevelExp = experienceTable[currentLevel + 1];
    return {base: currentLevelExp, next: nextLevelExp};
}
function getMaxSegmentLength(tunnelingLevel) {
    return 3000 + 50*tunnelingLevel;
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
let collapseTunnelButton = id("collapseTunnelButton");
let unlockTunnelingButton = id("unlockTunnelingButton");
let tunnelingSkillButton = id("skillInfoButton");
function updatePlaceTunnelButton() {
    if ('tunnelingExp' in playerInfos[myID]) {
        let currentExp = getPlayerInfo(myID, 'tunnelingExp');
        myTunnelingStatus.tunnelingLevel = getLevelFromExperience(currentExp);
        let experienceRange = getPercentToNextLevel(currentExp);
        id("tunnelingLevel").innerHTML = myTunnelingStatus.tunnelingLevel;
        id("tunnelingLevelProgress").min = experienceRange.base;
        id("tunnelingLevelProgress").max = experienceRange.next;
        id("tunnelingLevelProgress").value = currentExp;
        tunnelingSkillButton.title = currentExp + '/' + experienceRange.next;
    }

    if (placingTunnel) {
        placeTunnelButton.disabled = true;
        return;
    }
    if (playerInfos[myID].tunnelingExp > 0) {
        placeTunnelButton.disabled = false;
        unhideElement(placeTunnelButton)
        
        unlockTunnelingButton.disabled = true;
        hideElement(unlockTunnelingButton);

        if (playerInfos[myID].tunnelingExp > 1) {
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

    if (playerInfos[myID].tunnelingExp > 0
            && Object.keys(tunnelSegments).length > 0) {
        unhideElement(collapseTunnelButton);
        collapseTunnelButton.disabled = false;
    }
}

function updatePlayerInfo(pid, key, value) {
    if (!(pid in playerInfos)) {
        playerInfos[pid] = {};
    }
    playerInfos[pid][key] = value;

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

function getPlayerPosition(playerid) {
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

function makemesh(mesh, vertices, indices) {
    mesh.vertexArray = Float32Array.from(vertices);
    mesh.vertexBuffer = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, mesh.vertexBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, mesh.vertexArray, gl.STATIC_DRAW);
    mesh.vertexNumComponents = 2;
    mesh.vertexCount = mesh.vertexArray.length / mesh.vertexNumComponents;

    mesh.indexArray = Uint16Array.from(indices);
    mesh.indexBuffer = gl.createBuffer();
    gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, mesh.indexBuffer);
    gl.bufferData(gl.ELEMENT_ARRAY_BUFFER, mesh.indexArray, gl.STATIC_DRAW);
    mesh.indexCount = mesh.indexArray.length;
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
    makemesh(centeredSquareMesh, vertices, indices);

    let verticesCorner = [
        0, size,
        size, size,
        size, 0,
        0, 0];
    makemesh(cornerSquareMesh, verticesCorner, indices);
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

    makemesh(circleMesh, vertices, indices);
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

    makeSquareMesh();
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

function drawRoom(gl, uGlobalColor, room) {

    let center = [room.x + room.width/2, room.y + room.height/2];

    gl.uniform4fv(uGlobalColor, room.color1);
    drawMeshes(centeredSquareMesh, {0: center}, room.width);
    
    gl.uniform4fv(uGlobalColor, room.color2);
    drawMeshes(centeredSquareMesh, {0: center}, room.width * 16/20);

    gl.uniform4fv(uGlobalColor, room.color1);
    drawMeshes(centeredSquareMesh, {0: center}, room.width * 8/20);

    gl.uniform4fv(uGlobalColor, room.color2);
    drawMeshes(centeredSquareMesh, {0: center}, room.width / 20);
}

let tunnelBackgroundColor = 'rgba(100, 100, 100, 0.5)';
let tunnelLineColor = 'rgba(255, 255, 255, 1)';
let tunnelInvalidLineColor = 'rgba(255, 0, 0, 1)';
let tunnelNodeOutlineColor = 'rgba(255, 255, 255, 0.6)';

function drawTunnelNode(node, invalidSegment) {
    textContext.fillStyle = tunnelBackgroundColor;
    textContext.lineWidth = WORLD_SCALE;
    textContext.beginPath();
    textContext.arc(node.x, -node.y, tunnelSize/2, 0, 2 * Math.PI);
    if (!invalidSegment) {
        textContext.fill();
    }

    if (!('delete' in node)) {
        textContext.strokeStyle = invalidSegment ? tunnelInvalidLineColor : tunnelNodeOutlineColor;
        textContext.stroke();
        textContext.fillStyle = invalidSegment ? tunnelInvalidLineColor : tunnelLineColor;
        textContext.beginPath();
        textContext.arc(node.x, -node.y, tunnelSize/10, 0, 2 * Math.PI);
        textContext.fill();
    }
}

function drawTunnelSegment(node1, node2, invalidSegment) {
    textContext.strokeStyle = tunnelBackgroundColor;
    textContext.lineWidth = tunnelSize;
    // textContext.lineCap = 'square';
    textContext.beginPath();
    textContext.moveTo(node1.x, -node1.y);
    textContext.lineTo(node2.x, -node2.y);
    if (!invalidSegment) {
        textContext.stroke();
    }
    textContext.strokeStyle = invalidSegment ? tunnelInvalidLineColor : tunnelLineColor;
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
    for( const room of rooms) {
        drawRoom(gl, uGlobalColor, room);
    }

    let radians = currentAngle * Math.PI / 180.0;
    currentRotation[0] = Math.sin(radians);
    currentRotation[1] = Math.cos(radians);
    gl.uniform2fv(uRotationVector, currentRotation);

    // DRAW PLAYERS
    gl.uniform4fv(uGlobalColor, [0.1, 0.7, 0.2, 1.0]);
    let pos = [];
    for (playerid in playerPositions) {
        pos.push(getPlayerPosition(playerid));
    }
    drawMeshes(centeredSquareMesh, pos, PLAYER_SIZE);

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
            // textContext.fillText('tunnel: ' + getPlayerInfo(id, 'tunnelingExp'), playerPositions[id][0], -playerPositions[id][1] + 40*WORLD_SCALE);
        }
    }
    textContext.fillText(loadingMessage, 0, 0);
    
    for (const [id, segment] of Object.entries(tunnelSegments)) {
        drawTunnelSegment(tunnelNodes[segment.nodeid1], tunnelNodes[segment.nodeid2]);
    };

    for (const [id, node] of Object.entries(tunnelNodes)) {
        drawTunnelNode(node);
    }

    if (placingTunnel || collapsingTunnel) {
        myPosition = getPlayerPosition(myID);

        myTunnelingStatus.startTunnelNodeId = getTunnelNodeIdAtPoint(myPosition);
        if (myTunnelingStatus.startTunnelNodeId) {
            myTunnelingStatus.tunnelStartPosition = tunnelNodes[myTunnelingStatus.startTunnelNodeId];
        }
        else {
            myTunnelingStatus.tunnelStartPosition = {x: playerPositions[myID][0], y: playerPositions[myID][1]};
        }

        myTunnelingStatus.endTunnelNodeId = getTunnelNodeIdAtPoint(mousePos);
        if (myTunnelingStatus.endTunnelNodeId) {
            myTunnelingStatus.tunnelEndPosition = tunnelNodes[myTunnelingStatus.endTunnelNodeId];
        }
        else {
            myTunnelingStatus.tunnelEndPosition = {x: mousePos[0], y: mousePos[1]};
        }
    }
        
    if (placingTunnel) {
        let startVec = new Vector(myTunnelingStatus.tunnelStartPosition.x, 
                                myTunnelingStatus.tunnelStartPosition.y, 
                                0);
        let endVec = new Vector(myTunnelingStatus.tunnelEndPosition.x, 
                                myTunnelingStatus.tunnelEndPosition.y, 
                                0);
        let deltaVec = endVec.subtract(startVec);
        let maxSegmentLength = getMaxSegmentLength(myTunnelingStatus.tunnelingLevel) - 1;
        if (myTunnelingStatus.endTunnelNodeId && deltaVec.length() > maxSegmentLength) {
            myTunnelingStatus.endTunnelNodeId = null;
            myTunnelingStatus.tunnelEndPosition = {x: mousePos[0], y: mousePos[1]};
            endVec.x = myTunnelingStatus.tunnelEndPosition.x;
            endVec.y = myTunnelingStatus.tunnelEndPosition.y;
            deltaVec = endVec.subtract(startVec);
        }
        if (deltaVec.length() > maxSegmentLength) {
            let croppedVec = deltaVec.multiply(maxSegmentLength / deltaVec.length());
            endVec = startVec.add(croppedVec);
            myTunnelingStatus.tunnelEndPosition = endVec;
            deltaVec = endVec.subtract(startVec);
        }

        let invalidSegment = false;
        if (!myTunnelingStatus.startTunnelNodeId 
                && !isInRoom(myTunnelingStatus.tunnelStartPosition)) {
            invalidSegment = true;
        }

        drawTunnelSegment(startVec, endVec, invalidSegment);
        if (!myTunnelingStatus.startTunnelNodeId) {
            drawTunnelNode(startVec, invalidSegment);
        }
        if (!myTunnelingStatus.endTunnelNodeId) {
            drawTunnelNode(endVec);
        }

        let midPoint = startVec.add(endVec).multiply(0.5);
        let cost = 10 + 50 + 10 * Object.keys(tunnelSegments).length 
                    + Math.floor(Math.sqrt(deltaVec.length() / WORLD_SCALE));
        textContext.fillText('~$' + cost, midPoint.x, -midPoint.y);
    }

    if (collapsingTunnel) {
        if (myTunnelingStatus.startTunnelNodeId 
            && myTunnelingStatus.endTunnelNodeId) {
            drawTunnelSegment(myTunnelingStatus.tunnelStartPosition, 
                                myTunnelingStatus.tunnelEndPosition, 
                                true);
            drawTunnelNode(myTunnelingStatus.tunnelStartPosition, true);
            drawTunnelNode(myTunnelingStatus.tunnelEndPosition, true);
        }
        else {
            drawTunnelSegment(myTunnelingStatus.tunnelStartPosition, 
                                myTunnelingStatus.tunnelEndPosition, 
                                true);
            drawTunnelNode(myTunnelingStatus.tunnelEndPosition, false);
        }
    }

    if (myID in playerTargetPositions) {
        drawTargetX(playerTargetPositions[myID].to);
    }

    textContext.restore();


    window.requestAnimationFrame(function (currentTime) {
        let deltaTime = currentTime - previousTime;
        previousTime = currentTime;

        let mypos = getPlayerPosition(myID);
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

