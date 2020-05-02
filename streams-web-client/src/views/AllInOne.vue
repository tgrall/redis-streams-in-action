<template>
  <div class="allInOne">
    <CalculatorForm/>

<hr/>
    <StreamViewer/>
    
  </div>
</template>

<script>
import StreamViewer from "../components/StreamViewer";
import CalculatorForm from "../components/CalculatorForm";

export default {
  name: 'Home',
  components: {
    StreamViewer,
    CalculatorForm
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


