const express = require('express');
const webSocket = require('ws');
const http = require('http');
const cors = require('cors');
const StreamInfoService = require('./streamInfoService');
const streamInfoService = StreamInfoService();
const nconf = require('nconf').argv();

// TODO : see how to move all redis stuff in the lib & work with ws
const redis = require('redis');


nconf.file({ file: (nconf.get('CONF_FILE') || './config.dev.json' ) });
console.log(`... starting node host :\n\t port: ${nconf.get("PORT")} \n\t Redis : ${nconf.get("REDIS_HOST")}:${nconf.get("REDIS_PORT")}`);

var client = redis.createClient(nconf.get("REDIS_PORT"), nconf.get("REDIS_HOST"));
if (nconf.get('REDIS_PASSWORD')) {
	client.auth(nconf.get("REDIS_PASSWORD"));
}

client.subscribe("app:notifications:consumers");


const port = nconf.get("PORT");

const app = express();
const expressWs = require('express-ws')(app);
app.use(cors());


const server = http.createServer(app);

const wss = new webSocket.Server({
  path: '/ws',
  server,
});

wss.on('connection', (ws) => {
    console.log("Connected");

    client.on('message', function(channel, message){
        console.log(message);
        ws.send(message);
      })
    

});


app.get('/', (req, res) => {
    res.send('Hello World, from express');
});


app.get('/streams/all/:stream_id', (req, res) => {
    let streamdId = req.params.stream_id;
    streamInfoService.getStreamAll(streamdId, function(err, result){
        res.send(result);
    });
});
app.get('/streams/info/:stream_id', (req, res) => {
    let streamdId = req.params.stream_id;
    streamInfoService.getStreamInfo(streamdId, function(err, result){
        res.send(result);
    });
});

app.get('/streams/groups/:stream_id', (req, res) => {
    let streamdId = req.params.stream_id;
    streamInfoService.getGroups(streamdId, function(err, result){
        res.send(result);
    });
});


app.get('/streams/groups/:stream_id/:consumer_group_name', (req, res) => {
    let streamdId = req.params.stream_id;
    let consumerGroupName = req.params.consumer_group_name;
    streamInfoService.getConsumerInfo(streamdId, consumerGroupName,function(err, result){
        res.send(result);
    });
});

app.get('/streams/groups/:stream_id/:consumer_group_name/pending', (req, res) => {
    let streamdId = req.params.stream_id;
    let consumerGroupName = req.params.consumer_group_name;
    streamInfoService.getPendingMessages(streamdId, consumerGroupName,function(err, result){
        res.send(result);
    });
});

app.get('/streams/message/:stream_id/:msg_id', (req, res) => {
    let streamdId = req.params.stream_id;
    let msgId = req.params.msg_id;
    streamInfoService.getMessage(streamdId, msgId,function(err, result){
        res.send(result);
    });
});

server.listen(port, () => console.log(`App listening on port ${port}!`))



