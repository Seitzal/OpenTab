import './util.js'
import Vue from 'vue'
import app from './app.vue'
import router from './router.js'
import store from './store.js'
import vuetify from './plugins/vuetify'

Vue.config.productionTip = false

const _app = new Vue({
  router,
  store,
  vuetify,
  render: h => h(app)
}).$mount('#app')
