import axios from "axios";

export const StreamInfo = {

    async getStreamInfo(){
        let retValue = {};
        try {
            retValue = await axios.get("/streams/all/app:calculator-events");
        } catch(e){
            console.log(`Error calling getStreamInfo`);
            retValue.status = "ERROR";
        }
        return retValue;
    },

    async getMessage(id){
        let retValue = {};
        try {
            retValue = await axios.get("/streams/message/app:calculator-events/"+ id);
        } catch(e){
            console.log(`Error calling getMessage`);
            retValue.status = "ERROR";
        }
        return retValue;
    }
}