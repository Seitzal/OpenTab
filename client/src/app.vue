<template>
  <v-app>
    <v-app-bar ref="navbar" color="primary darken-2" dark app>
      <v-app-bar-nav-icon class="d-lg-none" @click.stop="drawer = !drawer">
      </v-app-bar-nav-icon>
      <router-link to="/" class="headline nav-link mr-5">OpenTab</router-link>
      <v-toolbar-items class="d-none d-lg-block wsblock" v-if="tab != undefined">
        <v-btn :to="'/tab/' + tab.id" depressed dark>{{tab.name}}</v-btn>
        <v-btn :to="'/tab/' + tab.id + '/teams'" text>Teams</v-btn>
        <v-btn :to="'/tab/' + tab.id + '/speakers'" text>Speakers</v-btn>
        <v-btn :to="'/tab/' + tab.id + '/judges'" text>Judges</v-btn>
      </v-toolbar-items>
      <v-spacer></v-spacer>
      <div class="d-none d-lg-block">
        <v-btn v-if="!signedIn" class="ma-2" color="secondary" @click="signin">
          <v-icon left>mdi-account-circle</v-icon>Sign In
        </v-btn>
        <v-chip v-if="signedIn" class="ma-2" label light>
          <v-icon left>mdi-account-circle</v-icon>{{ username() }}
        </v-chip>
        <v-btn v-if="signedIn" class="ma-2" color="secondary" @click="signout">
          <v-icon left>mdi-logout</v-icon>Sign Out
        </v-btn>
      </div>
    </v-app-bar>
    <v-navigation-drawer ref="navdrawer" temporary app v-model="drawer" id="drawer">
      <v-list dense nav>
        <v-list-item two-line v-if="signedIn">
          <v-list-item-avatar><v-icon>mdi-account</v-icon></v-list-item-avatar>
          <v-list-item-content>
            <v-list-item-title>{{ username() }}</v-list-item-title>
            <v-list-item-subtitle>Account settings</v-list-item-subtitle>
          </v-list-item-content>
        </v-list-item>
        <v-list-item v-if="!signedIn" @click="signin">
          <v-list-item-icon><v-icon>mdi-account</v-icon></v-list-item-icon>
          <v-list-item-content>
            <v-list-item-title>Sign in</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-divider></v-divider>
        <v-list-item v-if="signedIn" @click="signout">
          <v-list-item-icon><v-icon>mdi-logout</v-icon></v-list-item-icon>
          <v-list-item-content>
            <v-list-item-title>Sign out</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-divider class="mt-2 mb-2"></v-divider>
        <v-list-group v-if="tab != undefined" prepend-icon="mdi-table-large" active-class="mobile_nav_tweak" v-model="drawerTabLinks">
          <template v-slot:activator>
            <v-list-item-content>
              <v-list-item-title v-text="tab.name"></v-list-item-title>
            </v-list-item-content>
          </template>
          <v-list-item :to="'/tab/' + tab.id + '/dashboard'" active-class="mobile_nav_tweak">
            <v-list-item-content>
              <v-list-item-title>Dashboard</v-list-item-title>
            </v-list-item-content>
          </v-list-item>
          <v-list-item :to="'/tab/' + tab.id + '/teams'" active-class="mobile_nav_tweak">
            <v-list-item-content>
              <v-list-item-title>Teams</v-list-item-title>
            </v-list-item-content>
          </v-list-item>
          <v-list-item :to="'/tab/' + tab.id + '/speakers'" active-class="mobile_nav_tweak">
            <v-list-item-content>
              <v-list-item-title>Speakers</v-list-item-title>
            </v-list-item-content>
          </v-list-item>
          <v-list-item :to="'/tab/' + tab.id + '/judges'" active-class="mobile_nav_tweak">
            <v-list-item-content>
              <v-list-item-title>Judges</v-list-item-title>
            </v-list-item-content>
          </v-list-item>
        </v-list-group>
        <v-spacer vertical></v-spacer>
      </v-list>
    </v-navigation-drawer>
    <v-content>
      <router-view ref="rview">
      </router-view>
    </v-content>
    <login-dialog ref="login_dialog" :show_login_dialog.sync="show_login_dialog"></login-dialog>
  </v-app>
</template>

<script>

import loginDialog from './components/login.vue';
import ajax from './ajax.js';
import Cookies from 'js-cookie';

export default {
  name: 'App',
  data: () => ({
    drawer: null,
    drawerTabLinks: true,
    show_login_dialog: false,
  }),
  methods: {
    username: function() {
      return Cookies.get("username")
    },
    signout: function() {
      this.drawer = false
      ajax.signOut(function(){})
    },
    signin: function() {
      this.drawer = false
      this.show_login_dialog = true
    }
  },
  computed: {
    signedIn: function() { return this.$store.getters.signedIn },
    tab: function() { return this.$store.getters.tab }
  },
  watch: {
    signedIn: function(val) {
      ajax.loadTabs()
      ajax.loadPermissions()
    }
  },
  mounted: function() {
    ajax.loadTabs()
    ajax.loadPermissions()
  },
  components: {
    'login-dialog': loginDialog
  }
}
</script>
