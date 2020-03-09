const vueRoutes = [
  { path: '/',
    component: httpVueLoader("static/vue/index.vue")},

  { path: '/tab/:tabid',
    component: httpVueLoader("static/vue/tab.vue")},

  { path: '/tab/:tabid/dashboard',
    component: httpVueLoader("static/vue/tab.vue")},

  { path: '/tab/:tabid/teams',
    component: httpVueLoader("static/vue/teams.vue")},

  { path: '/tab/:tabid/speakers',
    component: httpVueLoader("static/vue/speakers.vue")},

  { path: '/tab/:tabid/speakers/:teamid',
    component: httpVueLoader("static/vue/speakers.vue")},

  { path: '/tab/:tabid/judges',
    component: httpVueLoader("static/vue/judges.vue")},

  { path: '/tab404',
    component: httpVueLoader("static/vue/tab404.vue")}
]
