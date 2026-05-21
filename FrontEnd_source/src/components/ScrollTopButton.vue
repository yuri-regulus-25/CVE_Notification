<template>
  <v-fab-transition>
    <v-btn
      v-show="visible"
      fixed
      bottom
      right
      fab
      small
      color="primary"
      class="scroll-top-button"
      aria-label="上に戻る"
      @click="$emit('scroll-top')"
    >
      <v-icon>mdi-arrow-up</v-icon>
    </v-btn>
  </v-fab-transition>
</template>

<script>
export default {
  name: "ScrollTopButton",

  props: {
    visible: {
      type: Boolean,
      default: false,
    },
  },
};
</script>

<style scoped>
.scroll-top-button {
  bottom: 24px !important;
  right: 16px !important;
  z-index: 8;
}
</style>
