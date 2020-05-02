<template>
  <div class="hello">
    
    <b-container v-if="streamInfo">
      <b-row>
        <b-col>
          <h2>Stream Info <span style="font-family:Consolas">"{{streamInfo.key}}"</span></h2>
        </b-col>          
      </b-row>
      <b-row>
        <b-col class="text-left">          
          <b-list-group>
            <b-list-group-item class="d-flex justify-content-between align-items-center">
              Number of messages
              <b-badge variant="info" pill>{{streamInfo.length}}</b-badge> 
            </b-list-group-item>
            <b-list-group-item class="d-flex justify-content-between align-items-center">
              Number of Groups
               <b-badge pill variant="info" >{{streamInfo.groups.length}}</b-badge>
            </b-list-group-item>
          </b-list-group>
        </b-col>
      <b-col>
          <b-list-group>
            <b-list-group-item class="d-flex justify-content-between align-items-center">
                First Message
               <b-badge  variant="info">{{streamInfo.first_entry.id}}</b-badge>
               <b-badge  variant="info">{{streamInfo.first_entry.msg_date}}</b-badge>
            </b-list-group-item>

            <b-list-group-item class="d-flex justify-content-between align-items-center">
              Last Message
               <b-badge  variant="info">{{streamInfo.last_entry.id}}</b-badge>
               <b-badge  variant="info">{{streamInfo.last_entry.msg_date}}</b-badge>
            </b-list-group-item>

          </b-list-group>

          
        </b-col>

      </b-row>

      <b-row class="mt-5">
        <b-col>
          <h2>Consumer Groups</h2>
        </b-col>          
      </b-row>
      <b-row>
    <div 
          v-for="group in streamInfo.groups"
          :key="group.name"
          class="col-md-6 col-3 my-1">

            <b-card>
              <b-card-title >{{ group.name }}</b-card-title>
              <b-card-sub-title class="mb-2"><hr/></b-card-sub-title>

              <b-list-group>
                <b-list-group-item class="d-flex justify-content-between align-items-center" variant="dark">
                Pending messages
                <b-badge  v-if="group.pending == 0" pill variant="info">{{group.pending}}</b-badge>
                <b-badge v-else  pill variant=danger>{{group.pending}}</b-badge>
                </b-list-group-item>
                <b-list-group-item class="d-row ">
                  <div class="mb-3">
                  <b>Consumers  </b><b-badge  pill >{{group.consumers.length}}</b-badge>
                  </div>
                              
                  <b-list-group
                  v-for="consumer in group.consumers"
                  :key="consumer.name" >
                    <transition name="bounce">
                    <b-list-group-item v-if="show" class="d-flex justify-content-between align-items-center mb-2" 
                      :variant="(consumer.pending==0)?'info':'danger'"
                      :id="'consumers_'+ group.name +'_'+ consumer.name"
                      >
                      <span>Name: {{consumer.name}}  </span>
                      <span>idle: {{consumer.idle}} </span>
                      <span>Pending: {{consumer.pending}} </span>                      
                    </b-list-group-item>
                    </transition>
                  </b-list-group>

                </b-list-group-item>
              </b-list-group>



            </b-card>
        </div>
      </b-row>
    </b-container>
   
    
  </div>
</template>

<script>
import { StreamInfo } from './../lib/StreamInfo'

export default {
  name: 'StreamViewer',
  props: {
    msg: String
  },
  data() {
    return {
      streamInfo : null,
      consumers : [],
      show : true
    };
  },
  created () {
    this.fetch();
    setInterval(function (){
      this.fetch();
    }.bind(this), 1000);
  },
  methods : {
    async fetch() {
      const { data } = await StreamInfo.getStreamInfo();
      console.log(data)
      this.streamInfo = data;
    }
  }  
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
h3 {
  margin: 40px 0 0;
}
ul {
  list-style-type: none;
  padding: 0;
}
li {
  display: inline-block;
  margin: 0 10px;
}
a {
  color: #42b983;
}

.bounce-enter-active {
  animation: bounce-in .5s;
}
.bounce-leave-active {
  animation: bounce-in .5s reverse;
}
@keyframes bounce-in {
  0% {
    transform: scale(0);
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
  }
}

</style>


