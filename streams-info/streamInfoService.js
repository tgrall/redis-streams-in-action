
const redis = require('redis');
const async = require('async');
let moment = require('moment');
const nconf = require('nconf').argv();

nconf.file({ file: (nconf.get('CONF_FILE') || './config.dev.json' ) });

console.log(`... \t Redis : ${nconf.get("REDIS_HOST")}:${nconf.get("REDIS_PORT")}`);


var client = redis.createClient(nconf.get("REDIS_PORT"), nconf.get("REDIS_HOST"));
if (nconf.get('REDIS_PASSWORD')) {
	client.auth(nconf.get("REDIS_PASSWORD"));
}

/**
 * This class is just to use with a small numbers of groups/pending messages 
 * since itis not usign any pagination
 * This is made for educational/learning purpose
 * 
 * Almost 0 error management ;)
 */
var StreamInfoService = function () {

    var _message = function(entry) {
        if (entry) {
            let msg = {
                header : {
                    id : entry[0],
                    date_of_msg : _getDateFromId( entry[0])
                },
            }  
            const idxMax = entry[1].length;
            msg.body = {};
            for (let index = 0; index < idxMax; index++) {
                msg.body[ entry[1][index] ] = entry[1][index+1];
                index++; 
            }
            return msg;
        } else {
            return {};
        }
    }

    var _pendingMessage = function(entry) {
        let pendinMessage = {
            id : entry[0],
            consumer : entry[1],
            time_since_deliver_ms : entry[2],
            deliver_counts : entry[3],
            date_of_msg : _getDateFromId( entry[0]),
            time_since_deliver : moment.utc(entry[2]).format('HH:mm:ss')
        }  
        return pendinMessage;
    }

    var _consumerEntry = function(entry) {
        let consumerEntry = {
            name : entry[1],
            pending : entry[3],
            idle : entry[5],
        }  
        return consumerEntry;
    }

    var _groupEntry = function(entry) {
        let groupEntry = {
            name : entry[1],
            consumers : entry[3],
            pending : entry[5],
            last_delivered_id : entry[7],
        }  
        return groupEntry;
    }

    var _getStreamEntry = function(entry){
        let streamEntry = {
        }
        if (entry) {
            streamEntry.id = entry[0]
            const idxMax = entry[1].length;
            for (let index = 0; index < idxMax; index++) {
                streamEntry[ entry[1][index] ] = entry[1][index+1];
                index++; 
            }
        }

        return streamEntry;
    }

    var _getDateFromId = function(id) {
        let ts = id.split("-")[0];
        let m = moment.unix( ts/1000 );
        let s = m.format("DD-MMM-YYYY H:mm:ss");
        return s;
    }

    /**
     * @param {} streamId 
     * @param {*} callback 
     */
    var _getStreamAll = function(streamId, callback){
        _getStreamInfo(streamId, function(err, streamInfo){
            _getGroups(streamId, function(err, groups){
                //streamInfo.groups = groups;
                let groupsWithConsumer = [];
                let idx = 0;
                async.eachSeries(groups, function iteratee(item,callback){
                    async.series([
                        function(callback){
                            _getConsumerInfo(streamId, item.name, function(err, consumers){
                                item.consumers = consumers
                                groupsWithConsumer[idx] = item;
                                callback(err, null);  
                            })
                        },
                        function(callback){
                            _getPendingMessages(streamId, item.name, function(err, pendingMsgs){
                                item.pending_messages = pendingMsgs;
                                callback(err, null);  

                                //groupsWithConsumer[idx] = item;
                                //callback(err, item);  
                                //idx++
                            })
                        },                        
                    ],
                    function(err, results) {
                        idx++
                        callback(err, results);  
                    });
                }, function done(){
                    streamInfo.key = streamId;
                    // add metadata
                    if (streamInfo.first_entry && streamInfo.first_entry.id) {
                        streamInfo.first_entry.msg_date = _getDateFromId(streamInfo.first_entry.id.split("-")[0]);
                    }
                    if (streamInfo.last_entry && streamInfo.last_entry.id) {
                        streamInfo.last_entry.msg_date = _getDateFromId(streamInfo.last_entry.id.split("-")[0]);
                    }

                    streamInfo.groups = groupsWithConsumer;
                    callback(err,streamInfo);                  
                });
            });
        });

    }

    var _getStreamInfo = function(streamdId, callback){
        client.sendCommand("XINFO", ["STREAM", streamdId], function(err, result){
            let streamInfo = {
                "last_generated_id" : null,
                "first_entry" : null,
                "last_entry" : null,
                "length" : 0,
                "groups" : 0,
                "radix_tree_keys" : 0,
                "radix_tree_nodes" : 0,
                "raw" : result
            }
            if (result) {
                let arrayLeng = result.length;
                for (let index = 0; index < arrayLeng; index++) {
                    const element = result[index];
                    if (element == "length") {
                        streamInfo.length = result[index+1];
                        index++;
                    } else if (element == "radix-tree-keys") {
                        streamInfo.radix_tree_keys = result[index+1];
                        index++;
                    } else if (element == "radix-tree-nodes") {
                        streamInfo.radix_tree_nodes = result[index+1];
                        index++;
                    } else if (element == "groups") {
                        streamInfo.groups = result[index+1];
                        index++;
                    }  else if (element == "last-generated-id") {
                        streamInfo.last_generated_id = result[index+1];
                        index++;
                    } else if (element == "first-entry") {
                        const streamEntry = _getStreamEntry(result[index+1]);
                        streamInfo.first_entry = streamEntry;
                        index++;
                    } else if (element == "last-entry") {
                        const streamEntry = _getStreamEntry(result[index+1]);
                        streamInfo.last_entry = streamEntry;
                        index++;
                    }
                }
            }

        
            callback(err,streamInfo);  
        })
    }

    var _getGroups = function(streamId, callback){
        client.sendCommand("XINFO", ["GROUPS", streamId], function(err, result){
            var groups = [];
            if (result) {
                let arrayLeng = result.length;
                for (let index = 0; index < arrayLeng; index++) {
                    groups.push(_groupEntry(result[index]));
                }    
            }
            callback(err,groups);  
        })
    }

    var _getConsumerInfo = function(streamId, groupName, callback){
        client.sendCommand("XINFO", ["CONSUMERS", streamId, groupName], function(err, result){
            var consumers = [];
            let arrayLeng = result.length;
            for (let index = 0; index < arrayLeng; index++) {
                consumers.push(_consumerEntry(result[index]));
            }
            callback(err, consumers );
        })
    }

    var _getPendingMessages = function(streamId, groupName, callback){
        client.sendCommand("XPENDING", [streamId, groupName, "-", "+", "200"], function(err, result){
            var retValue = [];
            let arrayLeng = result.length;
            for (let index = 0; index < arrayLeng; index++) {
                retValue.push(_pendingMessage(result[index]));
            }
            callback(err, retValue );
        })
    }


    var _getMessage = function(streamId, msgId, callback){
        console.log(   )
        client.sendCommand("XRANGE", [ streamId, msgId, msgId], function(err, result){
            var retValue = {};
            if (result && result[0]) {
                retValue = _message(result[0])
            }
            callback(err, retValue );
        })
    }

    return {
        getStreamAll: _getStreamAll,
        getStreamInfo: _getStreamInfo,
        getGroups: _getGroups,
        getConsumerInfo: _getConsumerInfo,
        getPendingMessages: _getPendingMessages,
        getMessage: _getMessage,
    }


}

module.exports = StreamInfoService;
