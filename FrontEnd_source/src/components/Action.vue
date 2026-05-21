<template>
  <div>
    <div class="section-title">操作</div>
    <v-card outlined class="pa-3 app-card">
      <v-btn
        block
        outlined
        class="mt-3"
        :disabled="loading || existIgnoreItem"
        @click="clearIgnoredAlerts"
      >
        全ての非通知設定を解除
      </v-btn>
    </v-card>
  </div>
</template>

<script>
export default {
  name: "ActionPanel",

  props: {
    existIgnoreItem: {
      type: Boolean,
      default: false,
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  methods: {
    clearIgnoredAlerts() {
      this.$emit("clear-ignored-alerts");
    }
  }
}
</script>

<style>

</style>
