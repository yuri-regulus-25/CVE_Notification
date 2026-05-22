<template>
  <v-card outlined class="pa-3 mb-3 app-card cve-card" :class="{ 'hidden-card': isHidden }">
    <div class="card-header">
      <div class="card-title-block">
        <div class="cve-id">{{ alert.cve_id || alert.alert_id }}</div>
        <div class="card-meta">
          <v-chip x-small :color="severityColor(alert.severity)" :text-color="severityTextColor">
            {{ severityText(alert.severity || "UNKNOWN") }}
          </v-chip>
          <v-chip x-small>
            スコア: {{ displayScore(alert.score) }}
          </v-chip>
          <v-chip x-small>
            領域: {{ alert.area_label || areaLabel(alert.area) }}
          </v-chip>
        </div>
      </div>

      <div class="status-icons">
        <v-icon v-if="isPinned" color="red darken-1" title="pinned">mdi-pin-outline</v-icon>
        <v-icon v-if="alert.is_new" color="blue darken-2" title="new">mdi-alpha-n-circle-outline</v-icon>
        <v-icon v-else-if="alert.is_updated" color="green darken-2" title="updated">mdi-alpha-u-circle-outline</v-icon>
      </div>
    </div>

    <div class="matched-keyword">{{ alert.matched || "-" }}</div>

    <div class="date-grid">
      <span class="label-text">公開日</span>
      <span class="value-text">{{ formatDate(alert.published) }}</span>
      <span class="label-text">最終更新</span>
      <span class="value-text">{{ formatDate(alert.last_modified) }}</span>
    </div>

    <div class="description">
      {{ alert.description || "-" }}
    </div>

    <v-row dense class="card-actions">
      <v-col cols="12" sm="4">
        <v-btn block outlined color="primary" @click="$emit('open-detail', alert)">詳細</v-btn>
      </v-col>
      <v-col cols="12" sm="4">
        <v-btn block outlined color="red darken-1" @click="togglePin">
          {{ isPinned ? "ピン留め解除" : "ピン留め" }}
        </v-btn>
      </v-col>
      <v-col cols="12" sm="4">
        <v-btn block outlined @click="toggleHidden">
          {{ isHidden ? "表示" : "非表示" }}
        </v-btn>
      </v-col>
    </v-row>
  </v-card>
</template>

<script>
export default {
  props: {
    alert: {
      type: Object,
      required: true,
    },
  },

  computed: {
    isPinned() {
      return this.alert.display_state === "pinned";
    },
    isHidden() {
      return this.alert.display_state === "hidden";
    },
    severityTextColor() {
      return "white";
    },
  },

  methods: {
    togglePin() {
      this.$emit(this.isPinned ? "unpin-alert" : "pin-alert", this.alert);
    },
    toggleHidden() {
      this.$emit(this.isHidden ? "show-alert" : "hide-alert", this.alert);
    },
    displayScore(score) {
      if (score === "null" || typeof score === "undefined" || String(score).trim() === "") {
        return "評価情報無し";
      }

      return score;
    },
    formatDate(dateText) {
      const timestamp = Date.parse(dateText || "");
      if (Number.isNaN(timestamp)) return "-";

      const date = new Date(timestamp);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, "0");
      const day = String(date.getDate()).padStart(2, "0");
      return `${year}-${month}-${day}`;
    },
    areaLabel(area) {
      if (area === "infra") return "インフラ";
      if (area === "dev") return "開発";
      if (area === "common") return "共通";
      if (area === "out_of_scope") return "領域外";
      return "未分類";
    },
    severityText(severity) {
      if (severity === "CRITICAL") return "緊急 / CRITICAL";
      if (severity === "HIGH") return "重要 / HIGH";
      if (severity === "MEDIUM") return "警告 / MEDIUM";
      if (severity === "LOW") return "注意 / LOW";
      return "無し, 情報無し / UNKNOWN";
    },
    severityColor(severity) {
      if (severity === "CRITICAL") return "red";
      if (severity === "HIGH") return "orange darken-4";
      if (severity === "MEDIUM") return "light-green darken-3";
      if (severity === "LOW") return "blue-grey darken-2";
      return "grey darken-2";
    },
  },
};
</script>

<style scoped>
.cve-card {
  transition: opacity 0.16s ease;
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.card-title-block {
  min-width: 0;
}

.cve-id {
  font-size: 18px;
  line-height: 1.35;
  font-weight: 700;
  color: var(--app-text-main);
  word-break: break-word;
}

.card-meta,
.status-icons {
  display: flex;
  align-items: center;
}

.card-meta {
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 6px;
  font-size: 13px;
  color: var(--app-text-sub);
}

.status-icons {
  flex-shrink: 0;
  gap: 4px;
}

.matched-keyword {
  margin-top: 12px;
  font-size: 15px;
  font-weight: 600;
  color: var(--app-text-main);
  word-break: break-word;
}

.date-grid {
  display: grid;
  grid-template-columns: 72px 1fr;
  gap: 6px 10px;
  margin-top: 10px;
  font-size: 13px;
}

.description {
  display: -webkit-box;
  margin-top: 12px;
  overflow: hidden;
  color: var(--app-text-main);
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.card-actions {
  border-top: 1px solid var(--app-border-color);
  margin-top: 12px;
  padding-top: 12px;
}

.hidden-card {
  background: var(--app-hidden-bg) !important;
  opacity: 0.78;
}
</style>
