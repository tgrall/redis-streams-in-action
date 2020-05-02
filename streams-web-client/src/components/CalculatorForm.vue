<template>
    <div>
      <h3>Send data to the services</h3>

      <b-row>
        <b-col>
        <div>
          <b-form inline >
          <div class="mx-auto">
          <label class="sr-only" for="inline-form-input-name">Name</label>
          <b-input
          id="n1"
          class="mb-2 mr-sm-2 mb-sm-0"
          placeholder="Number 1"
          v-model="form.n1"
          ></b-input>

          <b-input
          id="n1"
          class="mb-2 mr-sm-2 mb-sm-0"
          placeholder="Number 2"
          v-model="form.n2"
          ></b-input>

          <b-button variant="dark" @click="postMessage">Post</b-button>
          </div>
          </b-form>
        </div>
        </b-col>
      </b-row>

      <b-row class="mt-2">
        <b-col>
        This form will send the 2 values (numbers) to a Redis Streams. 
        Then some "services" (consumer groups/consumers) will process them.
        </b-col>
      </b-row>
    </div>
</template>

<script>
import axios from "axios";

  export default {
    name: "Calculator",
    components :{
    },
    data() {
      return {
        form: {
          n1: '',
          n2: ''
        },
        msg : null,
        show: true
      }
    },
    created() {
      const ws = new WebSocket("ws://localhost:3000/ws");
      ws.onmessage = ({data}) => {
        const event =  JSON.parse(data);
        this.msg = event;
      };
    },
    methods: {
      async postMessage() {
        await axios.get(`/api/send-message?n1=${this.form.n1}&n2=${this.form.n2}`);
      }
    }
  }
</script>