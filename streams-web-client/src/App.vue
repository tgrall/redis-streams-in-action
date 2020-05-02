<template>
  <div id="app">
    <div style="margin-bottom:10px">
      <b-navbar toggleable="lg" type="dark" variant="dark">
        <b-navbar-brand style="margin-left:110px" to="/" > Redis Streams in Action</b-navbar-brand>
        <b-navbar-toggle target="nav-collapse"></b-navbar-toggle>
        <b-collapse id="nav-collapse" is-nav>
        <b-navbar-nav>
        <b-nav-item><img src="/imgs/redis-logo.svg" height="30" /></b-nav-item>  
        <b-nav-item  to="/" >   Home</b-nav-item>
        <b-nav-item  to="/calculator" >   Post Messages</b-nav-item>
        <b-nav-item  to="/all-in-one" >   Post & View</b-nav-item>
      </b-navbar-nav>
      <b-navbar-nav class="ml-auto">
        <img src="/imgs/redislabs.png" height="30" />
        </b-navbar-nav>      
        </b-collapse>
      </b-navbar>
    </div>
    <div class="mt-5">
      <router-view/>
    </div>
    <a href="https://github.com/tgrall/redis-streams-in-action"><img src="/imgs/forkme_left_red.svg" style="position:absolute;top:0;left:0;" alt="Fork me on GitHub"></a>

  </div>
</template>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
}

#nav {
  padding: 30px;
}

#nav a {
  font-weight: bold;
  color: #2c3e50;
}

#nav a.router-link-exact-active {
  color: #42b983;
}
</style>
