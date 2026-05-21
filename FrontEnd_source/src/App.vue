<template>
  <v-app>
    <v-main>
      <v-container class="app-shell pa-3">
        <ActionPanel
          :loading="loading"
          :exist-ignore-item="existIgnoreItem"
          @clear-ignored-alerts="clearIgnoredAlerts"
        />

        <SummaryPanel
          :status="status"
          :generated-at-jst="generatedAtJst"
          :show-suspected-malfunction="showSuspectedMalfunction"
          :last-checked-at-jst="lastCheckedAtJst"
          :display-count="displayCount"
          :notification-count="notificationCount"
          :ignored-count="ignoredCount"
          :error-str="errorStr"
        />

        <FilterSettings
          :filter-status="filterStatus"
          :filter-source="filterSource"
          :filter-severity="filterSeverity"
          :filter-products="filterProducts"
          :filter-freshness="filterFreshness"
          :keyword="keyword"
          :status-filters="statusFilters"
          :source-filters="sourceFilters"
          :severity-filters="severityFilters"
          :product-filters="productFilters"
          :freshness-filters="freshnessFilters"
          @change-filter-status="filterStatus = $event"
          @change-filter-source="filterSource = $event"
          @change-filter-severity="filterSeverity = $event"
          @change-filter-products="filterProducts = $event"
          @change-filter-freshness="filterFreshness = $event"
          @select-all-products="selectAllProducts"
          @clear-products="filterProducts = []"
          @change-keyword="keyword = $event"
        />

        <AlertsList
          :alerts="filteredAlerts"
          :loading="loading"
          :expanded-descriptions="expandedDescriptions"
          @toggle-description="toggleDescription"
          @copy-description="copyDescription"
          @open-url="openUrl"
          @ignore-alert="ignoreAlert"
          @unignore-alert="unignoreAlert"
        />
      </v-container>

      <ScrollTopButton :visible="showScrollTop" @scroll-top="scrollToTop" />

      <v-snackbar v-model="snackbar.show" :color="snackbar.color" :timeout="2500">
        <v-icon color="white">{{ snackbar.icon }}</v-icon>
        {{ snackbar.text }}

        <template v-slot:action="{ attrs }">
          <v-btn text v-bind="attrs" @click="snackbar.show = false" icon> <v-icon white>mdi-close</v-icon> </v-btn>
        </template>
      </v-snackbar>
    </v-main>
  </v-app>
</template>

<script>
import ActionPanel from './components/Action.vue';
import AlertsList from './components/AlertsList.vue';
import FilterSettings from './components/FilterSettings.vue';
import _ from 'lodash';
import ScrollTopButton from './components/ScrollTopButton.vue';
import SummaryPanel from './components/Summary.vue';

export default {
  components: {
    ActionPanel,
    AlertsList,
    FilterSettings,
    ScrollTopButton,
    SummaryPanel,
  },
  data() {
    return {
      loading: false,
      state: {
        ok: true,
        status: "未取得",
        generated_at_jst: "-",
        last_checked_at_jst: "-",
        stale: false,
        display_count: 0,
        notification_count: 0,
        ignored_count: 0,
        alerts: [],
      },
      filterStatus: "all",
      filterSource: "ALL",
      filterSeverity: "ALL",
      filterProducts: [],
      filterFreshness: "all",
      keyword: "",
      statusFilters: [
        { label: "全て", value: "all" },
        { label: "通知対象のみ", value: "active" },
        { label: "非通知設定のみ", value: "ignored" },
      ],
      sourceFilters: [
        { label: "全て", value: "ALL" },
        { label: "NVD", value: "NVD" },
        { label: "JVN", value: "JVN" },
      ],
      severityFilters: [
        { label: "全て", value: "ALL" },
        { label: "緊急", value: "CRITICAL" },
        { label: "重要", value: "HIGH" },
        { label: "警告", value: "MEDIUM" },
        { label: "注意", value: "LOW" },
        { label: "なし/情報なし", value: "UNKNOWN" }
      ],
      freshnessFilters: [
        { label: "全件", value: "all" },
        { label: "新規登録された情報のみ", value: "new" },
        { label: "最近更新された情報のみ", value: "updated" },
      ],
      expandedDescriptions: {},
      showScrollTop: false,
      snackbar: {
        show: false,
        text: "",
        color: "success",
      },
    };
  },

  computed: {
    // CVE情報検索
    filteredAlerts() {
      const keyword = (this.keyword || "").toLowerCase();

      const alerts = (this.state.alerts || []).filter((alert) => {
        if (this.filterStatus === "active" && alert.ignored) return false;
        if (this.filterStatus === "ignored" && !alert.ignored) return false;
        if (this.filterSource !== "ALL" && alert.source !== this.filterSource) return false;
        if (this.filterSeverity !== "ALL" && alert.severity !== this.filterSeverity) return false;
        if (this.filterProducts.length > 0 && !this.filterProducts.includes(alert.matched)) return false;
        if (this.filterFreshness === "new" && !alert.is_new) return false;
        if (this.filterFreshness === "updated" && !alert.is_updated) return false;

        if (!keyword) return true;

        const haystack = [
          alert.alert_id,
          alert.title,
          alert.cve_id,
          alert.source,
          alert.category,
          alert.severity,
          alert.priority,
          alert.description,
        ]
          .join(" ")
          .toLowerCase();

        return haystack.includes(keyword);
      });

      return alerts.sort((a, b) => this.alertTimestamp(b) - this.alertTimestamp(a));
    },
    productFilters() {
      const products = new Set();

      (this.state.alerts || []).forEach((alert) => {
        if (alert.matched) {
          products.add(alert.matched);
        }
      });

      return Array.from(products)
        .sort((a, b) => a.localeCompare(b))
        .map((value) => ({
          label: value,
          value,
        }));
    },
    // 状態情報(JSON取り込み情報)
    // 非通知件数
    ignoredCount() {
      return _.get(this.state, 'ignored_count', 0);
    },
    // 存在チェック
    existIgnoreItem() {
      return this.ignoredCount !== 0;
    },
    // 状態
    status() {
      return _.get(this.state, 'status', '');
    },
    // 最終更新日時
    generatedAtJst() {
      return _.get(this.state, 'generated_at_jst', '-');
    },
    // 最終更新1日以上経過Chip表示制御
    showSuspectedMalfunction() {
      const targetDate = new Date(this.generatedAtJst.replace(/\//g, '-')); // Date型に変換
      const now = new Date(); // 現在時刻を取得

      targetDate.setDate(targetDate.getDate() + 1);// 対象日時に1日プラスする

      // 1日経過していたらtrue
      return targetDate <= now
    },
    // 最終取得日時
    lastCheckedAtJst() {
      return _.get(this.state, 'last_checked_at_jst', '-');
    },
    // 全体件数
    displayCount() {
      return _.get(this.state, 'display_count', 0);
    },
    // 通知対象件数
    notificationCount() {
      return _.get(this.state, 'notification_count', 0);
    },
    // エラー情報
    errorStr() {
      return _.get(this.state, 'last_error', null);
    }
  },

  mounted() {
    this.loadAppState();

    // カラーテーマ設定
    if (window.matchMedia) {
      window.matchMedia("(prefers-color-scheme: dark)").addEventListener("change", (event) => {
        this.$vuetify.theme.dark = event.matches;
      });
    }
    window.addEventListener("scroll", this.handleScroll, { passive: true });
    this.handleScroll();

    // 更新
    window.refreshFromAndroid = () => {
      this.fetchAlerts();
    };
  },

  beforeDestroy() {
    window.removeEventListener("scroll", this.handleScroll);
  },

  methods: {
    callBridge(method, arg) {
      if (!window.AndroidBridge || typeof window.AndroidBridge[method] !== "function") {
        return JSON.stringify({
          ok: false,
          status: "AndroidBridge未接続",
          message: method,
        });
      }

      if (typeof arg === "undefined") {
        return window.AndroidBridge[method]();
      }

      return window.AndroidBridge[method](arg);
    },

    applyState(jsonText) {
      const parsed = JSON.parse(jsonText);

      if (!parsed.ok) {
        this.state.status = parsed.status || "エラー";
        this.state.last_error = parsed.message || "";
        return;
      }

      this.state = parsed;
    },

    loadAppState() {
      try {
        this.applyState(this.callBridge("getAppState"));
      } catch (e) {
        this.state.status = "状態取得エラー";
        this.state.last_error = String(e);
      }
    },

    fetchAlerts() {
      this.loading = true;

      this.$nextTick(() => {
        try {
          this.applyState(this.callBridge("fetchAlerts"));
          this.showToast("脆弱性情報を更新しました");
        } catch (e) {
          this.state.status = "取得エラー";
          this.showToast("エラーが発生しました", "error");
          this.state.last_error = String(e);
        } finally {
          this.loading = false;
        }
      });
    },

    ignoreAlert(alertId) {
      this.applyState(this.callBridge("ignoreAlert", alertId));
      this.showToast("非通知対象に設定しました");
    },

    unignoreAlert(alertId) {
      this.applyState(this.callBridge("unignoreAlert", alertId));
      this.showToast("通知対象に設定しました");
    },

    clearIgnoredAlerts() {
      this.applyState(this.callBridge("clearIgnoredAlerts"));
      this.showToast("全ての脆弱性情報を通知対象に設定しました");
    },

    openUrl(url) {
      this.callBridge("openUrl", url || "");
    },

    toggleDescription(alertId) {
      this.$set(this.expandedDescriptions, alertId, !this.expandedDescriptions[alertId]);
    },
    handleScroll() {
      const scrollTop = window.pageYOffset || document.documentElement.scrollTop || 0;
      this.showScrollTop = scrollTop > 240;
    },
    scrollToTop() {
      window.scrollTo({
        top: 0,
        behavior: "smooth",
      });
    },
    selectAllProducts() {
      this.filterProducts = this.productFilters.map((item) => item.value);
    },
    alertTimestamp(alert) {
      const dateText = alert.last_modified || alert.published || "";
      const timestamp = Date.parse(dateText);

      return Number.isNaN(timestamp) ? 0 : timestamp;
    },
    showToast(text, color = "success") {
      this.snackbar.icon = color == "success" ? "mdi-check-circle-outline" : "mdi-alert-circle-outline";
      this.snackbar.text = text;
      this.snackbar.color = color;
      this.snackbar.show = true;
    },
    copyDescription(text) {
      navigator.clipboard.writeText(text);
      this.showToast("説明をコピーしました");
    },
  },
};
</script>

<style>
:root {
  --app-bg-color: #f6f7f9;
  --app-card-bg: #ffffff;
  --app-text-main: #111111;
  --app-text-sub: #555555;
  --app-border-color: #e0e0e0;
  --app-ignored-bg: #eeeeee;
}

@media (prefers-color-scheme: dark) {
  :root {
    --app-bg-color: #121212;
    --app-card-bg: #1e1e1e;
    --app-text-main: #f5f5f5;
    --app-text-sub: #b0b0b0;
    --app-border-color: #333333;
    --app-ignored-bg: #2a2a2a;
  }
}

html,
body,
#app {
  margin: 0;
  min-height: 100%;
  background: var(--app-bg-color);
  color: var(--app-text-main);
  font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
}

.v-application {
  background: var(--app-bg-color) !important;
  color: var(--app-text-main) !important;
}

.app-shell {
  max-width: 960px;
  margin: 0 auto;
}

.app-card {
  background: var(--app-card-bg) !important;
  color: var(--app-text-main) !important;
  border-color: var(--app-border-color) !important;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  margin: 18px 0 8px;
  color: var(--app-text-main);
}

.label-text {
  color: var(--app-text-sub);
}

.value-text {
  color: var(--app-text-main);
  word-break: break-word;
}

</style>
