

let fullButton;



function init() {
    console.log('Main: init.');
    remoteVideo = document.querySelector('#screen');
    fullButton = document.querySelector('#fullscreen');
    fullButton.addEventListener("click", onFullScreenAction);
}

function uninit() {
    console.log('Main: uninit');
    fullButton = null;

}

function onFullScreenAction(event) {
    toggleFullScreen();
}

function toggleFullScreen() {
    var elem = document.getElementById("screen");
    if (elem.requestFullscreen) {
        elem.requestFullscreen();
    } else {
        console.log("ok");
    }
}


window.onload = init;
window.onbeforeunload = uninit;