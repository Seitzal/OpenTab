const global = {
  theme: {
    themes: {
      light: {
        primary: "#009688"
      }
    }
  }
};

const rc = routes.opentab.controllers.RESTController;
const ac = routes.opentab.controllers.AuthController;

function url_get(key) {
  let url = "" +  window.location;
  let urlsplit = url.split('?');
  if (urlsplit.length == 2) {
    let entries = urlsplit[1].split('&');
    for (i = 0; i < entries.length; i++) {
      var tuple = entries[i].split('=');
      if (decodeURI(tuple[0]) == key) {
        return decodeURI(tuple[1]);
      }
    }
    return undefined;
  } else {
    return undefined;
  }
}

function timestamp() {
  return (new Date()).getTime()
}

function retrieveSession() {
  const exp = Cookies.get("api_key.exp")
  if (exp != undefined && timestamp() > exp) {
    return Cookies.get("api_key")
  } else {
    return undefined
  }
}

function signIn(username, password, success, failure) {
  let rq = ac.signIn()
  rq.headers = {"Content-Type": "application/json"}
  rq.data = JSON.stringify({username: username, password: password});
  rq.success = (data) => {
    app.api_key = data[0]
    Cookies.set("api_key", data[0])
    Cookies.set("api_key.exp", data[1].expires)
    Cookies.set("username", username)
    success()
  }
  rq.error = failure
  $.ajax(rq)
}

function signOut(then) {
  let rq = ac.signOut()
  rq.headers = {Authorization: app.api_key}
  rq.success = () => {
    app.api_key = undefined;
    Cookies.remove("api_key")
    then()
  }
  rq.error = (xhr) => xhr.responseText
  $.ajax(rq)
}

function ajaxFailure(jqXHR, textStatus, errorThrown) {
  // TODO: Write default handling for expected API failure sources, such as expired tokens
  if (jqXHR.status == 401 && jqXHR.responseText == "Invalid API key") {
    alert("Credential rejected by server. Please sign in again.")
    app.signout()
  } else {
    console.error("AJAX failure: " + jqXHR.responseText + "( " + jqXHR.status + ")")
    alert("An unexpected error occured. Check browser console for details.")
  }
}

const router = new VueRouter({
  routes: vueRoutes
})

router.afterEach((to, from) => {
  app.drawer = false
  if (app.$refs.rview == undefined || app.$refs.rview.tab == undefined) {
    app.tab = undefined
  }
})

const app = new Vue({
  router,
  vuetify: new Vuetify({theme: global.theme}),
  data: {
    drawer: null,
    drawerTabLinks: true,
    tabs: [],
    tab: undefined,
    tabsUpToDate: false,
    api_key: retrieveSession(),
    show_login_dialog: false 
  },
  computed: {
    signedIn: function() {
      return this.api_key != undefined;
    }
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
      this.tabsUpToDate = false
      let rq = rc.getAllTabs()
      rq.headers = this.signedIn ? {Authorization: this.api_key} : {}
      rq.success = (data) => {this.tabs = data; this.tabsUpToDate = true}
      rq.error = ajaxFailure
      $.ajax(rq)
    }
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

