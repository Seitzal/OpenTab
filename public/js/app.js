const global = {
  theme: {
    themes: {
      light: {
        primary: "#009688"
      }
    }
  }
};

const router = new VueRouter({
  routes: vueRoutes
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

const app = new Vue({
  router,
  vuetify: new Vuetify({theme: global.theme}),
  store,
  data: {
    drawer: null,
    drawerTabLinks: true,
    show_login_dialog: false,
  },
  methods: {
    username: function() {
      return Cookies.get("username")
    },
    signout: function() {
      this.drawer = false
      signOut(function(){ router.push("/")})
    },
    signin: function() {
      this.drawer = false
      this.show_login_dialog = true
    },
    loadTabs: function() {
      loadTabs()
    }
  },
  computed: {
    signedIn: function() { return this.$store.getters.signedIn },
    tab: function() { return this.$store.getters.tab }
  },
  watch: {
    signedIn: function(val) {
      this.loadTabs()
    }
  },
  mounted: function() {
    this.loadTabs()
  },
  components: {
    'login-dialog': httpVueLoader("static/vue/login.vue")
  }
}).$mount('#app')

