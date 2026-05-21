<template>
  <div>
    <div class="section-title">フィルター設定</div>
    <v-card outlined class="pa-3 app-card">
      <v-select
        v-model="filterStatusProxy"
        :items="statusFilters"
        item-text="label"
        item-value="value"
        dense
        outlined
        hide-details
        label="表示対象"
      />

      <v-select
        v-model="filterSourceProxy"
        :items="sourceFilters"
        item-text="label"
        item-value="value"
        dense
        outlined
        hide-details
        class="mt-3"
        label="ソース"
      />

      <v-select
        v-model="filterSeverityProxy"
        :items="severityFilters"
        item-text="label"
        item-value="value"
        dense
        outlined
        hide-details
        class="mt-3"
        label="レベル"
      />

      <v-text-field
        v-model="keywordProxy"
        dense
        outlined
        hide-details
        clearable
        class="mt-3"
        label="キーワード (CVE番号 / カテゴリ / 一致プロダクト名 etc...)"
      />
    </v-card>
  </div>
</template>

<script>
export default {
  props: {
    filterStatus: {
      type: String,
      required: true,
    },
    filterSource: {
      type: String,
      required: true,
    },
    filterSeverity: {
      type: String,
      required: true,
    },
    keyword: {
      type: String,
      default: "",
    },
    statusFilters: {
      type: Array,
      required: true,
    },
    sourceFilters: {
      type: Array,
      required: true,
    },
    severityFilters: {
      type: Array,
      required: true,
    },
  },

  computed: {
    filterStatusProxy: {
      get() {
        return this.filterStatus;
      },
      set(value) {
        this.$emit("change-filter-status", value);
      },
    },
    filterSourceProxy: {
      get() {
        return this.filterSource;
      },
      set(value) {
        this.$emit("change-filter-source", value);
      },
    },
    filterSeverityProxy: {
      get() {
        return this.filterSeverity;
      },
      set(value) {
        this.$emit("change-filter-severity", value);
      },
    },
    keywordProxy: {
      get() {
        return this.keyword;
      },
      set(value) {
        this.$emit("change-keyword", value || "");
      },
    },
  },
};
</script>
