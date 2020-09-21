<template>
  <div>
    <v-container v-if="checkReturned && valid">
      <p>Placeholder for personal page of judge #{{$route.params.judgeid}}</p>
    </v-container>
    <v-container  v-if="checkReturned && !valid">
      <h2 class="error">This is not a valid personal link :( </h2>
    </v-container>
  </div>
</template>

<script>

import api from '../api.js';

export default {

  name: "personal",

  data: function() { return {
    checkReturned: false,
    checkValid: false
  }},

  mounted: function() {
    api.verifyPersonalKey(this.$route.params.judgeid, this.$route.params.key, 
      (result) => {
        this.checkReturned = true;
        this.valid = result;
      }
    );
  }
}

</script>
