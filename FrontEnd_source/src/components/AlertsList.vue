<template>
  <div>
    <v-alert v-if="!loading && alerts.length === 0" type="info" outlined dense>
      表示対象のCVEはありません
    </v-alert>

    <AlertCard
      v-for="alert in alerts"
      :key="alert.alert_id"
      :alert="alert"
      @open-detail="$emit('open-detail', $event)"
      @pin-alert="$emit('pin-alert', $event)"
      @unpin-alert="$emit('unpin-alert', $event)"
      @hide-alert="$emit('hide-alert', $event)"
      @show-alert="$emit('show-alert', $event)"
    />
  </div>
</template>

<script>
import AlertCard from "./AlertCard.vue";

export default {
  components: {
    AlertCard,
  },

  props: {
    alerts: {
      type: Array,
      default: () => [],
    },
    loading: {
      type: Boolean,
      default: false,
    },
  },
};
</script>
