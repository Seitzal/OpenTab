import Vue from 'vue'
import VueRouter from 'vue-router'
import Home from './views/index.vue'

Vue.use(VueRouter)

const routes = [
  {path: '/',
    component: Home},
  {path: '/tab/:tabid',
    component: () => import('./views/tab.vue')},
  {path: '/tab/:tabid/teams',
    component: () => import('./views/teams.vue')},
  {path: '/tab/:tabid/speakers',
    component: () => import('./views/speakers.vue')},
  {path: '/tab/:tabid/speakers/:teamid',
    component: () => import('./views/speakers.vue')},
  {path: '/tab/:tabid/judges',
    component: () => import('./views/judges.vue')},
]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
})

router.beforeEach((to, from, next) => {
  if (to.params.tabid != undefined) {
    store.commit("setTabId", to.params.tabid)
    next()
  } else {
    store.commit("setTabId", undefined)
    next()
  }
})

export default router
