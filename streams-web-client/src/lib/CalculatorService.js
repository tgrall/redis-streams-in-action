import axios from "axios";

const url = "";
export const Calculator = {

    async sendNumbers(n1,n2){
        let retValue = {};
        try {
            retValue = await axios.get(`http://localhost:8081/api/send-message?n1=${n1}&n2=${n2}`);
        } catch(e){
            console.log(`Error calling ${url}`);
            retValue.status = "ERROR";
        }
        return retValue;
  