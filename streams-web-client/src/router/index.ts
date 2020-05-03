import Vue from 'vue'
import VueRouter, { RouteConfig } from 'vue-router'
import Home from '../views/Home.vue'

Vue.use(VueRouter)

  const routes: Array<RouteConfig> = [
  {
    path: '/',
    name: 'Home',
    component: Home
  },
  {
    path: '/calculator',
    name: 'Calculator',
    component: () => import( '../views/Calculator.vue')
  },
  {
    path: '/all-in-one',
    name: 'AllInOne',
    component: () => import('../views/AllInOne.vue')
  },
  {
    path: '/doc',
    name: 'Slides',
    component: () => import('../views/Slides.vue')
  }]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
})

export default router
