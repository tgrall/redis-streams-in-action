<template>
  <div class="home">

  <b-container>


<div>
  <b-tabs content-class="mt-3">
    <b-tab title="Intro" active>
        <img src="/imgs/s-001.png">
    </b-tab>

    <b-tab title="Consumer & Consumer Groups" >
        <img src="/imgs/s-002.png">
    </b-tab>

    <b-tab title="Error & Pending Messages" >
        <img src="/imgs/s-003.png">
    </b-tab>

    <b-tab title="Scaling" >
        <img src="/imgs/s-004.png">
    </b-tab>

  </b-tabs>
</div>


  </b-container>
  </div>
</template>

<script>


export default {
  name: 'Slides',
  components: {
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


