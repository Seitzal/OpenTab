<template>
  <v-container>
    <p>Placeholder: {{ viewName }}</p>
    <p v-if="$root.signedIn">Signed in as: {{ $root.username() }}</p>
  </v-container>
</template>

<script>
module.exports = {
  name: "index",
  data: function() { return {
    viewName: "Landing Page"
  }}
}
</script>

<style>
</style>