
const ws = new WebSocket("ws://" + location.hostname + ":" + location.port + "/ws");
// get most precise time now

let time_start = 0;

ws.onopen = () => {
    console.log('Connected');
};

ws.onclose = () => {
    console.log('Disconnected');
};

ws.onmessage = (event) => {
    let time_end = performance.now();
    const result = document.getElementById('result');
    const li = document.createElement('li');
    li.textContent = event.data + ` : ${time_end - time_start}ms`;
    result.appendChild(li);

//    window.scrollTo(0, document.body.scrollHeight);
};

const reverse = () => {
    const string = document.getElementById('string').value;
    time_start = performance.now();
    ws.send(string);
}
