
let mirrorButton;
let fullButton;
let streamCanvas;
let imageWebsocket = null;
let urlCreator = window.URL || window.webkitURL;
let imageQueue = [];
let freshhandle;
let canvasContext;
function init() {
    mirrorButton = document.getElementById("join");
    fullButton = document.getElementById("fullscreen");
    streamCanvas = document.getElementById("screen");
    canvasContext = streamCanvas.getContext("2d");
    registerEvents();
    registerDrawEvent();
}

window.onload = init;

function unInit() {
    unregisterDrawEvent();
    removeEvents();
    unInitWebsocket();
    mirrorButton = null;
    fullButton = null;
    canvasContext = null;
    streamCanvas = null;
}

window.onbeforeunload = unInit;

function registerEvents() {
    mirrorButton.addEventListener("click", onMirrorButtonClick);
    fullButton.addEventListener("click", onFullButtonClick);
}

function removeEvents() {
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

function initWebsocket() {
    console.log('initWebsocket: init.');
    imageWebsocket = new WebSocket('ws://' + window.location.host + '/tesla');
    imageWebsocket.binaryType = "arraybuffer";
    imageWebsocket.addEventListener("open", (event) => { console.log("websocket was opened.", event); });
    imageWebsocket.addEventListener("message", (event) => { prepareImage(event.data); });
    imageWebsocket.addEventListener("error", (event) => { console.log("websocket fatal error occured.", event); });
    imageWebsocket.addEventListener("close", (event) => { console.log("websocket was closed.", event); });
}

function unInitWebsocket() {
    if (imageWebsocket == null) return;
    imageWebsocket.close();
    imageWebsocket = null;
}

function prepareImage(bytearray) {
    const blob = new Blob([bytearray], { type: "image/jpeg" });
    if (imageQueue.length > 8) {
        console.log("trigger empty blob!!!! ");
        imageQueue = [];
    }
    //console.log("image to queue: ", imageQueue.length);
    imageQueue.push(blob);
}

function registerDrawEvent() {
    if (!freshhandle) {
        freshhandle = setInterval(drawImage, 32);
    }
}

function unregisterDrawEvent() {
    clearInterval(freshhandle);
    freshhandle = null;
}

function drawImage() {
    if (imageQueue.length == 0) {
        return;
    }

    var blob = imageQueue.shift();
    var imageURL = urlCreator.createObjectURL(blob);
    var img = new Image();
    img.onload = function () {
        streamCanvas.width = img.naturalWidth;
        streamCanvas.height = img.naturalHeight;
        urlCreator.revokeObjectURL(imageURL);
        imageURL = null;
        var srcRect = {
            x: 0, y: 0,
            width: img.naturalWidth,
            height: img.naturalHeight
        };
        var dstRect = srcRect;
        //var canvasContext = streamCanvas.getContext("2d");
        try {
            canvasContext.drawImage(img,
                srcRect.x,
                srcRect.y,
                srcRect.width,
                srcRect.height,
                dstRect.x,
                dstRect.y,
                dstRect.width,
                dstRect.height
            );

        } catch (e) {
            console.log("draw image failed", e);
        }    
        img = null;
        blob = null;
        //canvasContext = null;
    }
    img.src = imageURL;
}

