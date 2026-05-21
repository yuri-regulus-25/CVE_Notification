<template>
  <div>
    <div class="section-title">CVE情報</div>

    <v-alert v-if="!loading && alerts.length === 0" type="info" outlined dense>
      表示対象のCVEはありません
    </v-alert>

    <AlertCard
      v-for="alert in alerts"
      :key="alert.alert_id"
      :alert="alert"
      :description-expanded="isDescriptionExpanded(alert.alert_id)"
      @toggle-description="$emit('toggle-description', $event)"
      @copy-description="$emit('copy-description', $event)"
      @open-url="$emit('open-url', $event)"
      @ignore-alert="$emit('ignore-alert', $event)"
      @unignore-alert="$emit('unignore-alert', $event)"
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
    expandedDescriptions: {
      type: Object,
      default: () => ({}),
    },
  },

  methods: {
    isDescriptionExpanded(alertId) {
      return !!this.expandedDescriptions[alertId];
    },
  },
};
</script>
