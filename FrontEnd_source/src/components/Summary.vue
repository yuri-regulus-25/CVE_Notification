<template>
  <div>
    <div class="section-title">サマリ</div>
    <v-card
      outlined
      class="pa-3 app-card"
    >
      <div class="summary-row">
        <div class="label-text">状態</div>
        <div class="value-text">{{ status }}</div>
      </div>

      <div class="summary-row" v-if="errorStr">
        <div class="label-text">エラー</div>
        <div class="value-text">{{ errorStr }}</div>
      </div>

      <div class="summary-row">
        <div class="label-text">最終更新</div>
        <div class="value-text">
          {{ generatedAtJst }}
          <v-chip v-if="showSuspectedMalfunction" x-small color="orange" text-color="white" class="ml-1"
            ><v-icon color="white" class="mr-2">mdi-alert</v-icon>最終更新1日以上経過</v-chip
          >
        </div>
      </div>

      <div class="summary-row">
        <div class="label-text">最終取得</div>
        <div class="value-text">{{ lastCheckedAtJst }}</div>
      </div>

      <div class="summary-row">
        <div class="label-text">件数</div>
        <div class="value-text">
          全体:　　　 {{ displayCount }}<br />
          通知対象:　 {{ notificationCount }}<br />
          非通知対象: {{ ignoredCount }}
        </div>
      </div>
    </v-card>
  </div>
</template>

<script>
export default {
  name: "SummaryPanel",

  props: {
    status: {
      type: String,
      default: null,
    },
    generatedAtJst: {
      type: String,
      default: null,
    },
    showSuspectedMalfunction: {
      type: Boolean,
      default: false
    },
    lastCheckedAtJst: {
      type: String,
      default: null,
    },
    displayCount: {
      type: Number,
      default: 0,
    },
    notificationCount: {
      type: Number,
      default: 0,
    },
    ignoredCount: {
      type: Number,
      default: 0,
    },
    errorStr: {
      type: String,
      default: null,
    }
  }
};
</script>

<style>
.summary-row {
  display: grid;
  grid-template-columns: 110px 1fr;
  gap: 8px;
  font-size: 13px;
  padding: 4px 0;
}

.label-text {
  color: var(--app-text-sub);
}

.value-text {
  color: var(--app-text-main);
  word-break: break-word;
}
</style>
