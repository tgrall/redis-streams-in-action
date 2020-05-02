<template>
  <div class="about">
    <b-container>
    <CalculatorForm/>

    <CalculatorServiceState/>
    </b-container>
  </div>
</template>

<script>
import CalculatorServiceState from "../components/CalculatorServiceState";
import CalculatorForm from "../components/CalculatorForm";

  export default {
    name: "Calculator",
    components :{
      CalculatorServiceState,
      CalculatorForm
    }
  }
</script>