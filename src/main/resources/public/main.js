
const ws = new WebSocket("ws://" + location.hostname + ":" + location.port + "/ws");

let time_start = 0;

ws.onopen = () => {
    console.log('Connected');
};

ws.onclose = () => {
    console.log('Disconnected');
};

let messages_to_send = 0;
let message_received = 0;
let messages_sent = 0;
let lost_messages = 0;

const result = document.getElementById('result');
const send_count = document.getElementById('send_count');
const received_count = document.getElementById('received_count');
const lost_count = document.getElementById('lost_count');
const time = document.getElementById('time');

ws.onmessage = (event) => {
    const li = document.createElement('li');
    li.textContent = event.data;
    result.appendChild(li);

    message_received++;
    lost_messages--;

    received_count.textContent = message_received;
    lost_count.textContent = lost_messages;
    time.textContent = `${performance.now() - time_start}ms`;
};

const reverse = () => {
    messages_to_send = document.getElementById('count').value;
    const string = document.getElementById('string').value;
    time_start = performance.now();
    for (let i = 0; i < messages_to_send; i++) {
        ws.send(string);
        messages_sent++;
        send_count.textContent = messages_sent;
    }
    message_received = 0;
    messages_sent = 0;
    lost_messages = messages_to_send;
    received_count.textContent = message_received;
    lost_count.textContent = lost_messages;
}

const clear_result = () => {
    result.innerHTML = '';
}