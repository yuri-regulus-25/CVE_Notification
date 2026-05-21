<template>
  <v-card outlined class="pa-3 mb-3 app-card" :class="{ 'ignored-card': alert.ignored }">
    <div class="d-flex align-start justify-space-between">
      <div class="cve-title">
        {{ alert.title || alert.cve_id }}
      </div>

      <v-chip v-if="alert.ignored" small color="grey lighten-1" text-color="white"> 通知非通知 </v-chip>
    </div>

    <div class="detail-grid">
      <div class="label-text">ソース</div>
      <div class="value-text">
        <v-chip x-small :color="sourceColor(alert.source)" class="mr-1">
          {{ alert.source || "UNKNOWN" }}
        </v-chip>
      </div>

      <div class="label-text">CVE番号</div>
      <div class="value-text">{{ alert.cve_id }}</div>

      <div class="label-text">カテゴリ</div>
      <div class="value-text">{{ alert.category || "-" }}</div>

      <div class="label-text">一致プロダクト名</div>
      <div class="value-text">{{ alert.matched || "-" }}</div>

      <div class="label-text">レベル</div>
      <div class="value-text">
        <v-chip x-small :color="severityColor(alert.severity)" :text-color="severityTextColor">
          {{ severityText(alert.severity || "UNKNOWN") }}
        </v-chip>
      </div>

      <div class="label-text">CVSS v3</div>
      <div class="value-text">{{ displayScore(alert.score) }}</div>
    </div>

    <div v-if="alert.description" class="description" :class="{ collapsed: !descriptionExpanded }">
      {{ alert.description }}
    </div>

    <v-btn
      v-if="alert.description"
      text
      small
      color="primary"
      class="mt-1 px-0"
      @click="$emit('toggle-description', alert.alert_id)"
    >
      <v-icon color="primary" left class="mr-2 description-toggle-icon" :class="{ open: descriptionExpanded }">
        mdi-chevron-down
      </v-icon>
      {{ descriptionExpanded ? "説明を閉じる" : "説明を表示" }}
    </v-btn>

    <v-spacer />

    <v-btn
      v-if="alert.description"
      text
      small
      color="primary"
      class="mt-1 px-0"
      @click="$emit('copy-description', alert.description)"
    >
      <v-icon left>mdi-clipboard-multiple-outline</v-icon>
      説明テキストコピー
    </v-btn>

    <v-row dense class="card-actions">
      <v-col cols="12" sm="6">
        <v-btn block outlined @click="$emit('open-url', alert.url)">詳細表示</v-btn>
      </v-col>

      <v-col cols="12" sm="6">
        <v-btn v-if="!alert.ignored" block outlined color="red darken-1" @click="$emit('ignore-alert', alert.alert_id)">
          通知解除
        </v-btn>

        <v-btn v-else block outlined color="primary" @click="$emit('unignore-alert', alert.alert_id)">
          非通知設定解除
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
    descriptionExpanded: {
      type: Boolean,
      default: false,
    },
  },

  computed: {
    severityTextColor() {
      return "white";
    },
  },

  methods: {
    displayScore(score) {
      if (score === "null" || typeof score === "undefined" || String(score).trim() === "") {
        return "評価情報無し";
      }

      return score;
    },

    sourceColor(source) {
      if (source === "JVN") return "pink darken-3";
      if (source === "NVD") return "purple darken-2";
      return "blue lighten-4";
    },

    severityText(severity) {
      if (severity === "CRITICAL") return "緊急 / CRITICAL";
      if (severity === "HIGH") return "重要 / HIGH";
      if (severity === "MEDIUM") return "警告 / MEDIUM";
      if (severity === "LOW") return "注意 / LOW";
      return "なし,情報なし / UNKNOWN";
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
.cve-title {
  font-size: 18px;
  line-height: 1.35;
  font-weight: 600;
  word-break: break-word;
  color: var(--app-text-main);
}

.detail-grid {
  display: grid;
  grid-template-columns: 130px 1fr;
  gap: 8px;
  font-size: 13px;
  margin-top: 12px;
}

.description {
  margin-top: 12px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--app-text-main);
  white-space: pre-wrap;
  word-break: break-word;
}

.description.collapsed {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.description-toggle-icon {
  transition: transform 0.16s ease;
}

.description-toggle-icon.open {
  transform: rotate(180deg);
}

.card-actions {
  border-top: 1px solid var(--app-border-color);
  margin-top: 12px;
  padding-top: 12px;
}

.ignored-card {
  background: var(--app-ignored-bg) !important;
  opacity: 0.78;
}
</style>
