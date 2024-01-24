
let mirrorButton;
let fullButton;
let streamCanvas;
let imageWebsocket = null;
function init() {
    mirrorButton = document.getElementById("join");
    fullButton = document.getElementById("fullscreen");
    streamCanvas = document.getElementById("screen");
    registerEvents();
}

window.onload = init;

function unInit() {
    removeEvents();
    unInitWebsocket();
    mirrorButton = null;
    fullButton = null;
    streamCanvas = null;
}
window.onbeforeunload = unInit;

function registerEvents() {
    mirrorButton.addEventListener("click", onMirrorButtonClick);
    fullButton.addEventListener("click", onFullButtonClick);
}

function removeEvents(){
    mirrorButton.addEventListener("click", null);
    fullButton.addEventListener("click", null);
}

function onMirrorButtonClick(event) {
    unInitWebsocket();
    initWebsocket();
}

function onFullButtonClick(event) {
    toggleFullScreen();
}

function toggleFullScreen() {
    if (streamCanvas == null) return;
    if (streamCanvas.requestFullscreen) {
        streamCanvas.requestFullscreen();
    } else {
        console.log("ok");
    }
}

function initWebsocket(){
    console.log('initWebsocket: init.');
    imageWebsocket = new WebSocket('ws://' + window.location.host + '/tesla');
    imageWebsocket.binaryType = "arraybuffer";
    imageWebsocket.addEventListener("open", (event) => {console.log("websocket was opened.", event);});
    imageWebsocket.addEventListener("message", (event) => {drawFrame(event.data);});
    imageWebsocket.addEventListener("error", (event) => {console.log("websocket fatal error occured.", event);});
    imageWebsocket.addEventListener("close", (event) => {console.log("websocket was closed.", event);});
}

function unInitWebsocket(){
    if (imageWebsocket == null) return;
    imageWebsocket.close();
    imageWebsocket = null;
}

function drawFrame(bytearray){
    const blob = new Blob([bytearray], { type: "image/jpeg" });
    var urlCreator = window.URL || window.webkitURL;
    const blobURL = urlCreator.createObjectURL(blob);
    streamCanvas.src = blobURL;
}