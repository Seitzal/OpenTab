global= {
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

const router = new VueRouter({
  routes: [
    { path: '/', component: httpVueLoader("static/vue/index.vue")}
  ]
})

const app = new Vue({
  router,
  vuetify: new Vuetify({theme: global.theme}),
  data: {
    drawer: null,
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
    }
  },
  components: {
    'login-dialog': httpVueLoader("static/vue/login.vue")
  }
}).$mount('#app')

