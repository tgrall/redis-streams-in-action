<template>
  <div class="mt-5">
   <h2>Processing Status</h2>
   <b-card class="mt-3" v-if="streamInfo">
       <b-row>
        <div 
            v-for="group in streamInfo.groups"
            :key="group.name"
            class="col-md-6 col-3 my-1">

            <b-card>
              <b-card-title >{{ group.name }}</b-card-title>
              <b-card-sub-title class="mb-2"><hr/></b-card-sub-title>
              <b-alert show :variant="(group.pending === 0)?'info':'danger'" >
                  <div v-if="(group.pending === 0)">
                      Processing OK
                  </div>
                  <div v-else  :id="group.name">
                      {{group.pending}} messages in error, waiting for retry.
                      <b-badge pill @click="showPendingMessages" >
                        {{ (showPending)?"Hide":"View"  }}
                          
                      </b-badge>
                    <transition name="fade">
                    <div v-if="showPending" class="mt-2">
                    <b-list-group 
                        :key="msg.id" 
                        v-for="msg in group.pending_messages" >
                        <b-list-group-item class="small d-flex justify-content-between align-items-center ">
                            <span>Id: <b-link @click="showMessage(msg.id)" >{{msg.id}}</b-link> <br/>
                             <span style="font-size:xx-small">{{msg.date_of_msg}}</span>
                            </span>
                            <span>Retries: {{msg.deliver_counts}}  </span>
                            <span>Last deliver: {{msg.time_since_deliver}}  </span>
                        </b-list-group-item>
                    </b-list-group>
                    </div>
                    </transition>

                  </div>
                      
              </b-alert>
            </b-card>
        </div>
    </b-row>
       
   </b-card>

   <b-card>
       <p>
       This section list the informations about the streams, looking for "pending messages."
       </p>
       <p>
       The logic will be to have a service that check the status and retry or fail.
       </p>
       <p>
       For the demonstration, a  Redis HASH contains the "retry" configuration for each group. (app:retry:[group_name])
       This to allow only one of the consumer to claim/retry/ack the pending messages, and define the process.
       (This is only for demonstration purpose and help understand the flow)
       </p>
   </b-card>


<div>

  <b-modal v-if=streamMessage.header id="message-viewer" title="Redis Streams Message">
      <h5> {{streamMessage.header.id}} </h5>
      <div class="small"> 
          Date: {{streamMessage.header.date_of_msg}}
      </div>
      <hr/>
      <div>
          Body:
        <ul>
            <li 
                v-for="(value, propertyName) in streamMessage.body"
                :key="propertyName"
                >
            {{ propertyName }}: {{ value }} 
            </li>
        </ul>
      </div>
  </b-modal>
</div>



  </div>



</template>

<script>
import { StreamInfo } from './../lib/StreamInfo'

export default {
  data() {
    return {
      streamInfo : null,
      consumers : [],
      show : true,
      showPending : false,
      streamMessage : {}
    };
  },
  created () {
    this.fetch();
    setInterval(function (){
      this.fetch();
    }.bind(this), 3000);
  },
  methods : {
    async fetch() {
      const { data } = await StreamInfo.getStreamInfo();
      console.log(data)
      this.streamInfo = data;
    },
    showPendingMessages(evt){
        const consumerGroup = evt.target.id;
        console.log(consumerGroup);
        this.showPending = !this.showPending;
    },
    async showMessage(id){

      const { data } = await StreamInfo.getMessage(id);
        this.streamMessage = data;

        this.$bvModal.show('message-viewer');
    }
  }
}
</script>

<style>
.fade-enter-active, .fade-leave-active {
  transition: opacity .8s;
}
.fade-enter, .fade-leave-to /* .fade-leave-active below version 2.1.8 */ {
  opacity: 0;
}
</style>