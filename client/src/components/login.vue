<template>
  <v-dialog v-model="show_login_dialog" @click:outside="close()" persistent max-width="500px"
    @keydown.enter="submit()" @keydown.esc="close()">
    <v-card>
      <v-card-title style="background-color: #00897B; color: #ffffff">
        <span class="headline">Sign in</span>
      </v-card-title>
      <v-card-text class="pb-0">
        <v-container>
          <v-form>
            <v-text-field v-model="username" label="Username" prepend-icon="mdi-account" autofocus></v-text-field>
            <v-text-field v-model="password" label="Password" prepend-icon="mdi-lock" type="password"></v-text-field>
            <p class="error--text" v-if="failure">{{ errmsg }}</p>
          </v-form>
        </v-container>
      </v-card-text>
      <v-card-actions>
        <v-container v-if="!waiting" class="pt-0">
          <v-row>
            <v-col cols="6">
              <v-btn block @click="submit()" color="primary">Sign in</v-btn>
            </v-col>
            <v-col cols="6">
              <v-btn block @click="close()" color="error">Cancel</v-btn>
            </v-col>
          </v-row>
        </v-container>
        <v-container class="text-center pt-0" v-if="waiting">
           <v-progress-circular indeterminate color="primary"></v-progress-circular>
        </v-container>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>

import api from '../api.js';

export default {
  name: "login-dialog",
  props: {
    show_login_dialog: {
      default: false
    }
  },
  data: function() { return {
    username: "",
    password: "",
    errmsg: "",
    failure: true,
    waiting: false
  }},
  methods: {
    close() {
      this.waiting = false
      this.username = ""
      this.password = ""
      this.failure = false
      this.$emit('update:show_login_dialog', false)
    },
    submit() {
      this.waiting = true
      api.signIn(
        this.username,
        this.password, 
        this.close,
        (xhr) => {
          this.errmsg = xhr.responseText,
          this.failure = true,
          this.waiting = false
        })
    }
  },
}
</script>
