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

      <v-select
        v-model="filterProductsProxy"
        :items="productFilters"
        item-text="label"
        item-value="value"
        dense
        outlined
        multiple
        chips
        small-chips
        deletable-chips
        hide-details
        class="mt-3"
        label="対象プロダクト"
      >
        <template v-slot:selection="{ item, index }">
          <v-chip v-if="index < 2" small>
            {{ item.label }}
          </v-chip>
          <span v-if="index === 2" class="selected-count">
            +{{ filterProducts.length - 2 }}
          </span>
        </template>
      </v-select>

      <div class="product-actions">
        <v-btn small text color="primary" @click="$emit('select-all-products')">
          全選択
        </v-btn>
        <v-btn small text color="primary" @click="$emit('clear-products')">
          全選択解除
        </v-btn>
      </div>

      <v-select
        v-model="filterFreshnessProxy"
        :items="freshnessFilters"
        item-text="label"
        item-value="value"
        dense
        outlined
        hide-details
        class="mt-3"
        label="登録/更新"
      />

      <v-text-field
        v-model="keywordProxy"
        dense
        outlined
        hide-details
        clearable
        class="mt-3"
        label="キーワード (CVE番号 / カテゴリ / 説明 etc...)"
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
    filterProducts: {
      type: Array,
      default: () => [],
    },
    filterFreshness: {
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
    productFilters: {
      type: Array,
      required: true,
    },
    freshnessFilters: {
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
    filterProductsProxy: {
      get() {
        return this.filterProducts;
      },
      set(value) {
        this.$emit("change-filter-products", value || []);
      },
    },
    filterFreshnessProxy: {
      get() {
        return this.filterFreshness;
      },
      set(value) {
        this.$emit("change-filter-freshness", value);
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

<style scoped>
.product-actions {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
  margin-top: 4px;
}

.selected-count {
  color: var(--app-text-sub);
  font-size: 12px;
  margin-left: 4px;
}
</style>
