
console.log("connecting to websocket at /coin");
let ws = new WebSocket("ws://" + location.hostname + ":7070/coin");
ws.onmessage = receiveMessage;
ws.onclose = disconnected;
ws.onopen = sendHello;


const accessToken = getAccessToken();
if (accessToken == null) {
    document.location.href = "/";
}

function disconnected() {
    alert("WebSocket connection closed");
    document.location.href = "/";
}

function sendHello() {
    let jsonData = {
        'type': 'HELLO',
        'token': accessToken
    };
    let tosend = JSON.stringify(jsonData);
    console.log('sending ' + tosend);
    ws.send(tosend);
}

function sendStopGame() {
    let jsonData = {
        'type': 'STOP'
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
    // console.log(data);
    ws.send(JSON.stringify(data));
}

document.addEventListener('click', logMouseEvent);
function logMouseEvent(e) {
    // console.log(e);
    let mypos = getMyPosition();
    let targetPos = [mypos[0] + (e.x - glCanvas.width / 2), mypos[1] - (e.y - glCanvas.height / 2)];
    // console.log('mypos:' + mypos);
    playerTargetPositions[myID] = {
        'from': new Vector(mypos[0], mypos[1], 0),
        'to': new Vector(targetPos[0], targetPos[1], 0),
        'previousTime': previousTime
    };
    sendMove(targetPos[0], targetPos[1]);
}

document.addEventListener('keydown', logKey);
function logKey(e) {
    console.log(e);
    if (e.code == 'Escape') {
        sendStopGame();
    }
}

var myID;
var lastTimestamp;
function receiveMessage(msg) {
    let data = JSON.parse(msg.data);

    let timestamp = Date.now();
    let status = `${timestamp % 10000} delta ${timestamp - lastTimestamp}: ${data.type}: `;
    lastTimestamp = timestamp;

    if (data.type == 'HELLO') {
        console.log(data);
        myID = data.id;
        status += myID;
    }
    else if (data.type == 'STOP') {
        console.log(data);
        alert(data.message);
    }
    else if (data.type == 'MOVE') {
        if ('players' in data) {
            status += `${data.players.length} players, `;
            // console.log(data.players);
            data.players.forEach(player => {
                playerPositions[player.id] = [player.x, player.y];
                playerInfos[player.id] = player;

                if (player.id == myID && playerTargetPositions[myID]) {
                    playerTargetPositions[myID].from = new Vector(player.x, player.y, 0);
                    playerTargetPositions[myID].previousTime = previousTime;
                }
            });

        }

        if ('coins' in data) {
            status += `${data.coins.length} coins, `;
            data.coins.forEach(coin => {
                // console.log(coin);
                if ('delete' in coin) {
                    delete coinPositions[coin.id];
                }
                else {
                    coinPositions[coin.id] = [coin.x, coin.y];
                }
            });
        }
    }
    console.log(status);
}

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

let PLAYER_SIZE = 60;
let playerPositions = {};
let playerTargetPositions = {};
let playerInfos = {};
let COIN_SIZE = 20;
let coinPositions = {};

// Rendering data shared with the
// scalers.

let uScalingFactor;
let uGlobalColor;
let uRotationVector;
let aVertexPosition;

// Animation timing

let previousTime = 0.0;
let degreesPerSecond = 90.0;

window.addEventListener("load", startup, false);

function getMyPosition() {
    if (myID in playerPositions) {
        let delta2 = new Vector(0, 0);
        if (myID in playerTargetPositions) {
            let current = new Vector(playerTargetPositions[myID].from.x, playerTargetPositions[myID].from.y, 0);
            let target = new Vector(playerTargetPositions[myID].to.x, playerTargetPositions[myID].to.y, 0);

            let deltaTime = previousTime - playerTargetPositions[myID].previousTime;
            let playerSpeed = 100;
            let secondsElapsed = deltaTime / 1000;

            let deltaVector = target.subtract(current);
                delta2 = deltaVector.unit().multiply(secondsElapsed * playerSpeed);
                if (delta2.length() > deltaVector.length()) {
                    delta2 = deltaVector;
                    delete playerTargetPositions[myID];
                }
                let estimatedPos = current.add(delta2);
                playerPositions[myID][0] = estimatedPos.x;
                playerPositions[myID][1] = estimatedPos.y;
        }
        return [playerPositions[myID][0], playerPositions[myID][1]];
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
    currentScale = [2 * scale / glCanvas.width, 2 * scale / glCanvas.height];

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

    gl.uniform2fv(uCameraTranslate, getMyPosition());

    gl.uniform4fv(uGlobalColor, [0.8, 0.9, 1.0, 1.0]);
    drawMeshes(squareMesh, {0: [0,0]}, 2000);
    
    gl.uniform4fv(uGlobalColor, [0.7, 0.8, 0.9, 1.0]);
    drawMeshes(squareMesh, {0: [0,0]}, 1600);

    gl.uniform4fv(uGlobalColor, [0.8, 0.9, 1.0, 1.0]);
    drawMeshes(squareMesh, {0: [0,0]}, 800);

    gl.uniform4fv(uGlobalColor, [0.7, 0.8, 0.9, 1.0]);
    drawMeshes(squareMesh, {0: [0,0]}, 200);

    
    
    let radians = currentAngle * Math.PI / 180.0;
    currentRotation[0] = Math.sin(radians);
    currentRotation[1] = Math.cos(radians);
    gl.uniform2fv(uRotationVector, currentRotation);

    gl.uniform4fv(uGlobalColor, [0.1, 0.7, 0.2, 1.0]);
    drawMeshes(squareMesh, playerPositions, PLAYER_SIZE);
    gl.uniform4fv(uGlobalColor, [.9, 0.6, 0.2, 1.0]);
    drawMeshes(circleMesh, coinPositions, COIN_SIZE);
    
    gl.uniform2fv(uRotationVector, [1, 0]);

    textContext.clearRect(0, 0, textContext.canvas.width, textContext.canvas.height);
    textContext.strokeRect(0, 0, textContext.canvas.width, textContext.canvas.height);

    textContext.save();
    let mypos = getMyPosition();
    textContext.translate(-mypos[0] + textContext.canvas.width/2, mypos[1] + textContext.canvas.height/2);

    textContext.font = '16px serif';
    for (const [id, coinPos] of Object.entries(coinPositions)) {
        textContext.fillText('1', coinPos[0], -coinPos[1]);
    }
    textContext.font = '24px serif';
    for (const [id, playerinfo] of Object.entries(playerInfos)) {
        textContext.fillText('id' + playerinfo.id, playerPositions[id][0], -playerPositions[id][1] - 10);
        textContext.fillText('' + playerinfo.numcoins, playerPositions[id][0], -playerPositions[id][1] + 15);
    }
    textContext.restore();




    window.requestAnimationFrame(function (currentTime) {
        //   let deltaAngle = ((currentTime - previousTime) / 1000.0) * degreesPerSecond;
        // currentAngle = (currentAngle + deltaAngle) % 360;

        previousTime = currentTime;
        animateScene();
    });
}

