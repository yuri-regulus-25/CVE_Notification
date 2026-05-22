<template>
  <v-app>
    <v-main>
      <v-container class="app-shell pa-3">
        <v-card outlined class="pa-3 mb-3 app-card">
          <div class="settings-header">
            <div class="section-title ma-0">設定</div>
            <div class="settings-actions">
              <v-btn outlined small color="primary" @click="openSearchDialog">
                <v-icon left small>mdi-filter-variant</v-icon>
                検索条件設定
              </v-btn>
              <v-btn outlined small color="primary" @click="openDisplayDialog">
                <v-icon left small>mdi-tune</v-icon>
                表示形式設定
              </v-btn>
            </div>
          </div>
        </v-card>

        <v-card outlined class="pa-3 mb-3 app-card">
          <div class="summary-main">全{{ totalCount }}件 / 絞込み後{{ filteredCount }}件</div>
          <div class="summary-sub">{{ displayRangeText }} / {{ totalPages }}ページ中{{ currentPage }}ページ</div>
          <div class="chip-row">
            <span class="chip-row-label">条件:</span>
            <v-chip
              v-for="chip in conditionChips"
              :key="chip"
              small
              outlined
              class="mr-1 mb-1"
            >
              {{ chip }}
            </v-chip>
          </div>
          <div class="chip-row">
            <span class="chip-row-label">並び:</span>
            <v-chip
              v-for="chip in sortChips"
              :key="chip"
              small
              outlined
              class="mr-1 mb-1"
            >
              {{ chip }}
            </v-chip>
          </div>
        </v-card>

        <AlertsList
          :alerts="pagedAlerts"
          :loading="loading"
          @open-detail="openDetailDialog"
          @pin-alert="pinAlert"
          @unpin-alert="unpinAlert"
          @hide-alert="hideAlert"
          @show-alert="showAlert"
        />

        <v-card outlined class="pa-3 mb-15 app-card pagination-card">
          <v-btn outlined :disabled="currentPage <= 1" @click="goToPreviousPage">前へ</v-btn>
          <div class="pagination-text">{{ currentPage }} / {{ totalPages }}</div>
          <v-btn outlined :disabled="currentPage >= totalPages" @click="goToNextPage">次へ</v-btn>
        </v-card>
      </v-container>

      <ScrollTopButton :visible="showScrollTop" @scroll-top="scrollToTop" />

      <v-overlay :value="loading" opacity="0.46" z-index="1000">
        <div class="loader-panel">
          <v-progress-circular indeterminate size="64" width="5" color="white" />
          <div class="loader-text">読み込み中...</div>
        </div>
      </v-overlay>

      <v-dialog
        v-model="searchDialog"
        attach="#app"
        content-class="app-dialog"
        max-width="720"
        scrollable
        :z-index="1200"
      >
        <v-card class="dialog-card">
          <v-card-title class="dialog-title">
            <v-btn icon @click="searchDialog = false">
              <v-icon>mdi-close</v-icon>
            </v-btn>
            <span>検索条件設定</span>
            <v-spacer />
            <v-btn text color="primary" @click="clearSearchDraft">クリア</v-btn>
            <v-btn color="primary" @click="applySearchSettings">適用</v-btn>
          </v-card-title>

          <v-card-text class="dialog-body">
            <div class="dialog-section-title">キーワード検索</div>
            <v-text-field
              v-model="searchDraft.keyword"
              outlined
              dense
              clearable
              hide-details
            />

            <div class="dialog-section-title product-title-row">
              <span>プロダクト</span>
              <div class="product-title-actions">
                <v-btn small text color="primary" @click="selectAllProducts">全選択</v-btn>
                <v-btn small text color="primary" @click="clearProductSelection">全解除</v-btn>
              </div>
            </div>
            <v-text-field
              v-model="productFilterText"
              outlined
              dense
              clearable
              hide-details
              placeholder="絞り込み"
            />
            <div class="product-group-list">
              <v-card
                v-for="group in filteredProductGroups"
                :key="group.category"
                outlined
                class="product-group-card"
              >
                <v-checkbox
                  :input-value="isProductGroupSelected(group)"
                  :indeterminate="isProductGroupIndeterminate(group)"
                  :label="group.category"
                  dense
                  hide-details
                  class="product-category-checkbox"
                  @change="toggleProductGroup(group, $event)"
                />
                <div class="product-option-grid">
                  <v-checkbox
                    v-for="item in group.items"
                    :key="item.value"
                    v-model="searchDraft.products"
                    :value="item.value"
                    :label="item.label"
                    dense
                    hide-details
                    class="product-option-checkbox"
                  />
                </div>
              </v-card>
            </div>

            <div class="dialog-section-title">深刻度</div>
            <v-row dense>
              <v-col v-for="item in severityOptions" :key="item.value" cols="12" sm="6">
                <v-checkbox
                  v-model="searchDraft.severities"
                  :value="item.value"
                  :label="item.label"
                  dense
                  hide-details
                />
              </v-col>
            </v-row>

            <div class="dialog-section-title">対応領域</div>
            <v-row dense>
              <v-col v-for="item in areaOptions" :key="item.value" cols="12" sm="6">
                <v-checkbox
                  v-model="searchDraft.areas"
                  :value="item.value"
                  :label="item.label"
                  dense
                  hide-details
                />
              </v-col>
            </v-row>

            <div class="dialog-section-title">状態</div>
            <v-row dense>
              <v-col v-for="item in stateOptions" :key="item.value" cols="12" sm="4">
                <v-checkbox
                  v-model="searchDraft.states"
                  :value="item.value"
                  :label="item.label"
                  dense
                  hide-details
                />
              </v-col>
            </v-row>

            <div class="dialog-section-title">期間</div>
            <v-row dense>
              <v-col v-for="item in periodOptions" :key="item.value" cols="12" sm="4">
                <v-checkbox
                  v-model="searchDraft.periods"
                  :value="item.value"
                  :label="item.label"
                  dense
                  hide-details
                />
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </v-dialog>

      <v-dialog
        v-model="displayDialog"
        attach="#app"
        content-class="app-dialog"
        max-width="620"
        scrollable
        :z-index="1200"
      >
        <v-card class="dialog-card">
          <v-card-title class="dialog-title">
            <v-btn icon @click="displayDialog = false">
              <v-icon>mdi-close</v-icon>
            </v-btn>
            <span>表示形式設定</span>
            <v-spacer />
            <v-btn color="primary" @click="applyDisplaySettings">適用</v-btn>
          </v-card-title>

          <v-card-text class="dialog-body">
            <div class="dialog-section-title">表示件数</div>
            <v-radio-group v-model="displayDraft.pageSize" row hide-details>
              <v-radio v-for="size in pageSizeOptions" :key="size" :label="String(size)" :value="size" />
            </v-radio-group>

            <div class="dialog-section-title">ソート項目</div>
            <v-radio-group v-model="displayDraft.sortKey" hide-details>
              <v-radio v-for="item in sortKeyOptions" :key="item.value" :label="item.label" :value="item.value" />
            </v-radio-group>

            <div class="dialog-section-title">ソート順</div>
            <v-radio-group v-model="displayDraft.sortOrder" row hide-details>
              <v-radio v-for="item in sortOrderOptions" :key="item.value" :label="item.label" :value="item.value" />
            </v-radio-group>
          </v-card-text>
        </v-card>
      </v-dialog>

      <v-dialog
        v-model="detailDialog"
        attach="#app"
        content-class="app-dialog"
        max-width="760"
        scrollable
        :z-index="1200"
      >
        <v-card v-if="selectedAlert" class="dialog-card">
          <v-card-title class="dialog-title">
            <v-btn icon @click="detailDialog = false">
              <v-icon>mdi-close</v-icon>
            </v-btn>
            <span>{{ selectedAlert.cve_id }}</span>
          </v-card-title>

          <v-card-text class="dialog-body">
            <div class="detail-grid">
              <div class="label-text">深刻度</div>
              <div class="value-text">{{ selectedAlert.severity }}</div>

              <div class="label-text">スコア(CVSS v3)</div>
              <div class="value-text">{{ displayScore(selectedAlert.score) }}</div>

              <div class="label-text">ソース</div>
              <div class="value-text">{{ selectedAlert.source || "-" }}</div>

              <div class="label-text">領域</div>
              <div class="value-text">{{ selectedAlert.area_label || areaLabel(selectedAlert.area) }}</div>

              <div class="label-text">プロダクト</div>
              <div class="value-text">{{ selectedAlert.matched || "-" }}</div>

              <div class="label-text">公開日時</div>
              <div class="value-text">{{ formatDate(selectedAlert.published) }}</div>

              <div class="label-text">最終更新日時</div>
              <div class="value-text">{{ formatDate(selectedAlert.last_modified) }}</div>
            </div>

            <div class="dialog-section-title">対応領域</div>
            <v-select
              v-model="detailAreaDraft"
              :items="areaOptions"
              item-text="label"
              item-value="value"
              outlined
              dense
              hide-details
              @change="updateSelectedArea"
            />

            <div class="dialog-section-title">Description</div>
            <div class="detail-description">{{ selectedAlert.description || "-" }}</div>

            <div class="dialog-section-title">URL</div>
            <button class="url-button" type="button" @click="openUrl(selectedAlert.url)">
              {{ selectedAlert.url || "-" }}
            </button>
          </v-card-text>

          <v-card-actions class="detail-actions">
            <v-btn outlined color="red darken-1" @click="togglePin(selectedAlert)">
              {{ isPinned(selectedAlert) ? "ピン留め解除" : "ピン留め" }}
            </v-btn>
            <v-btn outlined @click="toggleHidden(selectedAlert)">
              {{ isHidden(selectedAlert) ? "表示" : "非表示" }}
            </v-btn>
            <v-spacer />
            <v-btn text @click="detailDialog = false">閉じる</v-btn>
          </v-card-actions>
        </v-card>
      </v-dialog>

      <v-snackbar v-model="snackbar.show" :color="snackbar.color" :timeout="2500">
        <v-icon color="white">{{ snackbar.icon }}</v-icon>
        {{ snackbar.text }}

        <template v-slot:action="{ attrs }">
          <v-btn text v-bind="attrs" @click="snackbar.show = false" icon>
            <v-icon white>mdi-close</v-icon>
          </v-btn>
        </template>
      </v-snackbar>
    </v-main>
  </v-app>
</template>

<script>
import AlertsList from './components/AlertsList.vue';
import ScrollTopButton from './components/ScrollTopButton.vue';

const STORAGE_KEY = "cve_notification_display_settings";

const DEFAULT_SETTINGS = {
  pageSize: 20,
  sortKey: "last_modified",
  sortOrder: "desc",
  filters: {
    keyword: "",
    products: [],
    severities: ["CRITICAL", "HIGH"],
    areas: [],
    states: ["visible", "pinned"],
    periods: [],
  },
};

const CLEARED_FILTERS = {
  keyword: "",
  products: [],
  severities: [],
  areas: [],
  states: ["visible", "pinned"],
  periods: [],
};

const PRODUCT_GROUPS = [
  {
    category: "Linux",
    items: [
      "AlmaLinux",
      "Red Hat Enterprise Linux",
      "RHEL",
    ],
  },
  {
    category: "Windows",
    items: [
      "Microsoft Windows",
      "Windows Server",
      "Windows 10",
      "Windows 11",
      "Windows Server 2012",
      "Windows Server 2016",
      "Windows Server 2019",
      "Windows Server 2022",
      "Windows Server 2025",
      "Windows Kernel",
      "Win32k",
      "Windows Installer",
      "Windows TCP/IP",
      "Windows Common Log File System",
      "CLFS",
      "BitLocker",
      "Hyper-V",
    ],
  },
  {
    category: "WEB",
    items: [
      "Apache",
      "Apache HTTP Server",
      "Apache Tomcat",
      "IIS",
      "Remote Desktop Services",
      "RDP",
      "SMB",
      "NTLM",
      "Kerberos",
      "Active Directory",
      "LDAP",
      "DNS Server",
      "DHCP Server",
      "Windows Print Spooler",
      "Windows Routing and Remote Access Service",
      "RRAS",
      "Microsoft Defender",
      "Windows Defender",
    ],
  },
  {
    category: "DB",
    items: [
      "PostgreSQL",
      "pgAdmin",
      "pgAdmin 4",
      "pgadmin4",
    ],
  },
  {
    category: "PHP",
    items: [
      "composer",
      "Composer",
      "Laravel",
      "Symfony",
      "symfony",
      "AuraSQL",
      "Aura SQL",
    ],
  },
  {
    category: "JS",
    items: [
      "npm",
      "Node.js",
      "Vue.js",
      "Vue",
      "Vuetify",
    ],
  },
].map((group) => ({
  ...group,
  items: group.items.map((item) => ({ label: item, value: item })),
}));

const clone = (value) => JSON.parse(JSON.stringify(value));

export default {
  components: {
    AlertsList,
    ScrollTopButton,
  },
  data() {
    return {
      loading: true,
      activeBridgeRequests: 0,
      bridgeRequestSeq: 0,
      bridgeRequests: {},
      bridgeTimeoutMs: 60000,
      currentPage: 1,
      settings: clone(DEFAULT_SETTINGS),
      searchDialog: false,
      displayDialog: false,
      detailDialog: false,
      searchDraft: clone(DEFAULT_SETTINGS.filters),
      productFilterText: "",
      displayDraft: {
        pageSize: DEFAULT_SETTINGS.pageSize,
        sortKey: DEFAULT_SETTINGS.sortKey,
        sortOrder: DEFAULT_SETTINGS.sortOrder,
      },
      selectedAlertId: "",
      detailAreaDraft: "uncategorized",
      state: {
        ok: true,
        status: "未取得",
        generated_at_jst: "-",
        last_checked_at_jst: "-",
        stale: false,
        display_count: 0,
        notification_count: 0,
        hidden_count: 0,
        pinned_count: 0,
        alerts: [],
      },
      severityOptions: [
        { label: "緊急 / CRITICAL", value: "CRITICAL" },
        { label: "重要 / HIGH", value: "HIGH" },
        { label: "警告 / MEDIUM", value: "MEDIUM" },
        { label: "注意 / LOW", value: "LOW" },
        { label: "無し, 情報無し / UNKNOWN", value: "UNKNOWN" },
      ],
      areaOptions: [
        { label: "インフラ", value: "infra" },
        { label: "開発", value: "dev" },
        { label: "共通", value: "common" },
        { label: "未分類", value: "uncategorized" },
        { label: "領域外", value: "out_of_scope" },
      ],
      stateOptions: [
        { label: "表示のみ", value: "visible" },
        { label: "ピン止めのみ", value: "pinned" },
        { label: "非表示のみ", value: "hidden" },
      ],
      periodOptions: [
        { label: "今日更新", value: "today" },
        { label: "3日以内", value: "3days" },
        { label: "7日以内", value: "7days" },
      ],
      pageSizeOptions: [10, 20, 50, 100],
      sortKeyOptions: [
        { label: "最終更新日", value: "last_modified" },
        { label: "公開日", value: "published" },
        { label: "新規検知日", value: "detected_date" },
        { label: "更新検知日", value: "updated_date" },
        { label: "CVSS", value: "score" },
        { label: "深刻度", value: "severity" },
        { label: "CVE ID", value: "cve_id" },
      ],
      sortOrderOptions: [
        { label: "降順", value: "desc" },
        { label: "昇順", value: "asc" },
      ],
      showScrollTop: false,
      snackbar: {
        show: false,
        text: "",
        color: "success",
        icon: "mdi-check-circle-outline",
      },
    };
  },

  computed: {
    allAlerts() {
      return this.state.alerts || [];
    },
    productGroups() {
      return PRODUCT_GROUPS;
    },
    productOptions() {
      return this.productGroups.flatMap((group) => group.items);
    },
    filteredProductGroups() {
      const filterText = (this.productFilterText || "").trim().toLowerCase();

      if (!filterText) return this.productGroups;

      return this.productGroups
        .map((group) => {
          const groupMatched = group.category.toLowerCase().includes(filterText);
          const items = groupMatched
            ? group.items
            : group.items.filter((item) => item.label.toLowerCase().includes(filterText));

          return {
            ...group,
            items,
          };
        })
        .filter((group) => group.items.length > 0);
    },
    totalCount() {
      return this.allAlerts.length;
    },
    filteredAlerts() {
      const filters = this.settings.filters;
      const keyword = (filters.keyword || "").trim().toLowerCase();

      return this.allAlerts.filter((alert) => {
        if (filters.severities.length > 0 && !filters.severities.includes(alert.severity || "UNKNOWN")) return false;
        if (filters.products.length > 0 && !filters.products.some((product) => this.alertMatchesProduct(alert, product))) return false;
        if (filters.areas.length > 0 && !filters.areas.includes(alert.area || "uncategorized")) return false;
        if (filters.states.length > 0 && !filters.states.includes(alert.display_state || "visible")) return false;
        if (filters.periods.length > 0 && !filters.periods.some((period) => this.matchesPeriod(alert, period))) return false;

        if (!keyword) return true;

        const haystack = [
          alert.alert_id,
          alert.cve_id,
          alert.title,
          alert.source,
          alert.severity,
          alert.area_label,
          alert.category,
          alert.product_key,
          alert.matched,
          alert.description,
        ].join(" ").toLowerCase();

        return haystack.includes(keyword);
      });
    },
    sortedAlerts() {
      return [...this.filteredAlerts].sort(this.compareAlerts);
    },
    pagedAlerts() {
      const start = (this.currentPage - 1) * this.settings.pageSize;
      return this.sortedAlerts.slice(start, start + this.settings.pageSize);
    },
    filteredCount() {
      return this.filteredAlerts.length;
    },
    totalPages() {
      return Math.max(1, Math.ceil(this.filteredCount / this.settings.pageSize));
    },
    pageStart() {
      if (this.filteredCount === 0) return 0;
      return (this.currentPage - 1) * this.settings.pageSize + 1;
    },
    pageEnd() {
      return Math.min(this.currentPage * this.settings.pageSize, this.filteredCount);
    },
    displayRangeText() {
      if (this.filteredCount === 0) return "0件表示";
      return `${this.pageStart}〜${this.pageEnd}件表示`;
    },
    conditionChips() {
      const filters = this.settings.filters;
      const chips = [];

      if (filters.keyword) chips.push(`キーワード: ${filters.keyword}`);
      if (filters.products.length > 0) chips.push(`プロダクト: ${filters.products.length}項目選択中`);
      if (filters.severities.length > 0) chips.push(`深刻度: ${filters.severities.map(this.severityShortLabel).join(", ")}`);
      if (filters.states.length > 0) chips.push(`状態: ${this.stateFilterLabel(filters.states)}`);
      if (filters.areas.length > 0) chips.push(`対応領域: ${filters.areas.map(this.areaLabel).join(", ")}`);
      if (filters.periods.length > 0) chips.push(`期間: ${filters.periods.map(this.periodLabel).join(", ")}`);

      return chips.length > 0 ? chips : ["全条件"];
    },
    sortChips() {
      return [
        this.sortKeyLabel(this.settings.sortKey),
        this.sortOrderLabel(this.settings.sortOrder),
      ];
    },
    selectedAlert() {
      if (!this.selectedAlertId) return null;
      return this.allAlerts.find((alert) => alert.alert_id === this.selectedAlertId) || null;
    },
    isDialogOpen() {
      return this.searchDialog || this.displayDialog || this.detailDialog;
    },
  },

  watch: {
    currentPage() {
      this.ensureCurrentPage();
    },
    isDialogOpen(open) {
      this.setPullRefreshEnabled(!open);
    },
  },

  mounted() {
    this.loadSettings();

    if (window.matchMedia) {
      window.matchMedia("(prefers-color-scheme: dark)").addEventListener("change", (event) => {
        this.$vuetify.theme.dark = event.matches;
      });
    }

    window.addEventListener("scroll", this.handleScroll, { passive: true });
    this.handleScroll();

    window.onAndroidBridgeResult = (requestId, jsonText) => {
      this.handleBridgeResult(requestId, jsonText);
    };

    window.refreshFromAndroid = () => {
      this.fetchAlerts();
    };

    this.setPullRefreshEnabled(!this.isDialogOpen);
    this.loadAppState();
  },

  beforeDestroy() {
    window.removeEventListener("scroll", this.handleScroll);
    window.onAndroidBridgeResult = null;
    window.refreshFromAndroid = null;
    this.setPullRefreshEnabled(true);

    Object.values(this.bridgeRequests).forEach((request) => {
      if (request.timeoutId) clearTimeout(request.timeoutId);
    });
  },

  methods: {
    requestBridge(method, args = [], options = {}) {
      const requestId = `${Date.now()}-${++this.bridgeRequestSeq}`;
      const loading = options.loading !== false;

      if (loading) this.beginLoading();

      const timeoutId = window.setTimeout(() => {
        if (!this.bridgeRequests[requestId]) return;
        this.handleBridgeResult(requestId, JSON.stringify({
          ok: false,
          status: options.errorStatus || "操作タイムアウト",
          message: "AndroidBridge callback timeout",
        }));
      }, this.bridgeTimeoutMs);

      this.$set(this.bridgeRequests, requestId, {
        loading,
        timeoutId,
        successMessage: options.successMessage || "",
        errorStatus: options.errorStatus || "操作エラー",
        showErrorToast: options.showErrorToast !== false,
      });

      if (!window.AndroidBridge || typeof window.AndroidBridge[method] !== "function") {
        this.handleBridgeResult(requestId, JSON.stringify({
          ok: false,
          status: "AndroidBridge未接続",
          message: method,
        }));
        return;
      }

      try {
        window.AndroidBridge[method](requestId, ...args);
      } catch (e) {
        this.handleBridgeResult(requestId, JSON.stringify({
          ok: false,
          status: options.errorStatus || "操作エラー",
          message: String(e),
        }));
      }
    },
    beginLoading() {
      this.activeBridgeRequests += 1;
      this.loading = true;
    },
    endLoading() {
      this.activeBridgeRequests = Math.max(0, this.activeBridgeRequests - 1);
      this.loading = this.activeBridgeRequests > 0;
    },
    applyState(jsonText) {
      const parsed = typeof jsonText === "string" ? JSON.parse(jsonText) : jsonText;

      if (!parsed.ok) {
        this.state.status = parsed.status || "エラー";
        this.state.last_error = parsed.message || "";
        return false;
      }

      this.state = parsed;
      this.ensureCurrentPage();
      return true;
    },
    handleBridgeResult(requestId, jsonText) {
      const request = this.bridgeRequests[requestId];
      if (!request) return;

      if (request.timeoutId) clearTimeout(request.timeoutId);
      this.$delete(this.bridgeRequests, requestId);

      try {
        const ok = this.applyState(jsonText);

        if (ok && request.successMessage) {
          this.showToast(request.successMessage);
        } else if (!ok && request.showErrorToast) {
          this.showToast("エラーが発生しました", "error");
        }
      } catch (e) {
        this.state.status = request.errorStatus;
        this.state.last_error = String(e);

        if (request.showErrorToast) this.showToast("エラーが発生しました", "error");
      } finally {
        if (request.loading) this.endLoading();
      }
    },
    loadAppState() {
      this.requestBridge("getAppState", [], {
        errorStatus: "状態取得エラー",
        showErrorToast: false,
      });
    },
    fetchAlerts() {
      this.requestBridge("fetchAlerts", [], {
        successMessage: "脆弱性情報を更新しました",
        errorStatus: "取得エラー",
      });
    },
    pinAlert(alert) {
      this.requestBridge("pinAlert", [alert.alert_id], {
        successMessage: "ピン留めしました",
        errorStatus: "ピン留め設定エラー",
      });
    },
    unpinAlert(alert) {
      this.requestBridge("unpinAlert", [alert.alert_id], {
        successMessage: "ピン留めを解除しました",
        errorStatus: "ピン留め解除エラー",
      });
    },
    hideAlert(alert) {
      this.requestBridge("hideAlert", [alert.alert_id], {
        successMessage: "非表示にしました",
        errorStatus: "非表示設定エラー",
      });
    },
    showAlert(alert) {
      this.requestBridge("showAlert", [alert.alert_id], {
        successMessage: "表示に戻しました",
        errorStatus: "表示設定エラー",
      });
    },
    togglePin(alert) {
      if (this.isPinned(alert)) {
        this.unpinAlert(alert);
      } else {
        this.pinAlert(alert);
      }
    },
    toggleHidden(alert) {
      if (this.isHidden(alert)) {
        this.showAlert(alert);
      } else {
        this.hideAlert(alert);
      }
    },
    updateSelectedArea() {
      if (!this.selectedAlert) return;

      this.requestBridge("setAlertArea", [this.selectedAlert.alert_id, this.detailAreaDraft], {
        successMessage: "対応領域を更新しました",
        errorStatus: "対応領域更新エラー",
      });
    },
    openUrl(url) {
      if (window.AndroidBridge && typeof window.AndroidBridge.openUrl === "function") {
        window.AndroidBridge.openUrl(url || "");
      }
    },
    setPullRefreshEnabled(enabled) {
      if (window.AndroidBridge && typeof window.AndroidBridge.setPullRefreshEnabled === "function") {
        window.AndroidBridge.setPullRefreshEnabled(Boolean(enabled));
      }
    },
    openDetailDialog(alert) {
      this.selectedAlertId = alert.alert_id;
      this.detailAreaDraft = alert.area || "uncategorized";
      this.detailDialog = true;
    },
    openSearchDialog() {
      this.searchDraft = clone(this.settings.filters);
      this.productFilterText = "";
      this.searchDialog = true;
    },
    clearSearchDraft() {
      this.searchDraft = clone(CLEARED_FILTERS);
      this.productFilterText = "";
    },
    applySearchSettings() {
      this.settings = {
        ...this.settings,
        filters: this.normalizeFilters(this.searchDraft),
      };
      this.currentPage = 1;
      this.saveSettings();
      this.searchDialog = false;
    },
    openDisplayDialog() {
      this.displayDraft = {
        pageSize: this.settings.pageSize,
        sortKey: this.settings.sortKey,
        sortOrder: this.settings.sortOrder,
      };
      this.displayDialog = true;
    },
    applyDisplaySettings() {
      this.settings = this.normalizeSettings({
        ...this.settings,
        pageSize: this.displayDraft.pageSize,
        sortKey: this.displayDraft.sortKey,
        sortOrder: this.displayDraft.sortOrder,
      });
      this.currentPage = 1;
      this.saveSettings();
      this.displayDialog = false;
    },
    loadSettings() {
      try {
        const stored = JSON.parse(localStorage.getItem(STORAGE_KEY) || "{}");
        this.settings = this.normalizeSettings(stored);
      } catch (e) {
        this.settings = clone(DEFAULT_SETTINGS);
      }
    },
    saveSettings() {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(this.settings));
    },
    normalizeSettings(value) {
      const merged = {
        ...clone(DEFAULT_SETTINGS),
        ...(value || {}),
        filters: this.normalizeFilters((value && value.filters) || {}),
      };

      if (!this.pageSizeOptions.includes(Number(merged.pageSize))) merged.pageSize = DEFAULT_SETTINGS.pageSize;
      if (!this.sortKeyOptions.some((item) => item.value === merged.sortKey)) merged.sortKey = DEFAULT_SETTINGS.sortKey;
      if (!this.sortOrderOptions.some((item) => item.value === merged.sortOrder)) merged.sortOrder = DEFAULT_SETTINGS.sortOrder;

      merged.pageSize = Number(merged.pageSize);
      return merged;
    },
    normalizeFilters(filters) {
      const source = {
        ...clone(DEFAULT_SETTINGS.filters),
        ...(filters || {}),
      };

      return {
        ...source,
        keyword: String(source.keyword || "").trim(),
        products: this.pickAllowed(source.products, this.productOptions),
        severities: this.pickAllowed(source.severities, this.severityOptions),
        areas: this.pickAllowed(source.areas, this.areaOptions),
        states: this.pickAllowed(source.states, this.stateOptions),
        periods: this.pickAllowed(source.periods, this.periodOptions),
      };
    },
    pickAllowed(values, options) {
      const allowed = options.map((item) => item.value);
      return Array.isArray(values) ? values.filter((value) => allowed.includes(value)) : [];
    },
    ensureProductDraft() {
      if (!Array.isArray(this.searchDraft.products)) {
        this.$set(this.searchDraft, "products", []);
      }
    },
    selectAllProducts() {
      this.$set(this.searchDraft, "products", this.productOptions.map((item) => item.value));
    },
    clearProductSelection() {
      this.$set(this.searchDraft, "products", []);
    },
    isProductGroupSelected(group) {
      const selected = new Set(this.searchDraft.products || []);
      return group.items.length > 0 && group.items.every((item) => selected.has(item.value));
    },
    isProductGroupIndeterminate(group) {
      const selected = new Set(this.searchDraft.products || []);
      const selectedCount = group.items.filter((item) => selected.has(item.value)).length;
      return selectedCount > 0 && selectedCount < group.items.length;
    },
    toggleProductGroup(group, checked) {
      this.ensureProductDraft();

      const selected = new Set(this.searchDraft.products);
      const groupValues = group.items.map((item) => item.value);

      if (checked) {
        groupValues.forEach((value) => selected.add(value));
      } else {
        groupValues.forEach((value) => selected.delete(value));
      }

      this.$set(this.searchDraft, "products", this.productOptions
        .map((item) => item.value)
        .filter((value) => selected.has(value)));
    },
    alertMatchesProduct(alert, product) {
      const target = String(product || "").trim().toLowerCase();
      if (!target) return false;

      const haystack = [
        alert.product_key,
        alert.matched,
        alert.category,
        alert.title,
        alert.description,
      ].join(" ").toLowerCase();

      if (haystack.includes(target)) return true;

      const looseTarget = target.replace(/[^a-z0-9]+/g, "");
      const looseHaystack = haystack.replace(/[^a-z0-9]+/g, "");

      return looseTarget.length > 1 && looseHaystack.includes(looseTarget);
    },
    compareAlerts(a, b) {
      const pinnedCompare = Number(this.isPinned(b)) - Number(this.isPinned(a));
      if (pinnedCompare !== 0) return pinnedCompare;

      const direction = this.settings.sortOrder === "asc" ? 1 : -1;
      const aValue = this.sortValue(a, this.settings.sortKey);
      const bValue = this.sortValue(b, this.settings.sortKey);

      if (aValue < bValue) return -1 * direction;
      if (aValue > bValue) return 1 * direction;
      return String(a.cve_id || "").localeCompare(String(b.cve_id || ""));
    },
    sortValue(alert, key) {
      if (key === "published") return this.dateValue(alert.published);
      if (key === "detected_date") return this.dateValue(alert.detected_date);
      if (key === "updated_date") return this.dateValue(alert.updated_date);
      if (key === "score") return Number.parseFloat(alert.score) || 0;
      if (key === "severity") return this.severityRank(alert.severity);
      if (key === "cve_id") return String(alert.cve_id || "");
      return this.dateValue(alert.last_modified);
    },
    matchesPeriod(alert, period) {
      const timestamp = this.dateValue(alert.last_modified);
      if (!timestamp) return false;

      const now = new Date();
      const target = new Date(timestamp);

      if (period === "today") {
        return now.getFullYear() === target.getFullYear() &&
          now.getMonth() === target.getMonth() &&
          now.getDate() === target.getDate();
      }

      const days = period === "3days" ? 3 : 7;
      return now.getTime() - timestamp <= days * 24 * 60 * 60 * 1000;
    },
    dateValue(dateText) {
      const timestamp = Date.parse(dateText || "");
      return Number.isNaN(timestamp) ? 0 : timestamp;
    },
    severityRank(severity) {
      return {
        CRITICAL: 4,
        HIGH: 3,
        MEDIUM: 2,
        LOW: 1,
        UNKNOWN: 0,
      }[severity || "UNKNOWN"] || 0;
    },
    severityShortLabel(severity) {
      const option = this.severityOptions.find((item) => item.value === severity);
      return option ? option.label.split(" / ")[0] : severity;
    },
    stateLabel(state) {
      const option = this.stateOptions.find((item) => item.value === state);
      return option ? option.label : state;
    },
    stateFilterLabel(states) {
      const stateSet = new Set(states);

      if (stateSet.has("visible") && stateSet.has("pinned") && !stateSet.has("hidden")) {
        return "表示のみ";
      }

      return states.map(this.stateLabel).join(", ");
    },
    areaLabel(area) {
      const option = this.areaOptions.find((item) => item.value === area);
      return option ? option.label : "未分類";
    },
    periodLabel(period) {
      const option = this.periodOptions.find((item) => item.value === period);
      return option ? option.label : period;
    },
    sortKeyLabel(sortKey) {
      const option = this.sortKeyOptions.find((item) => item.value === sortKey);
      return option ? option.label : sortKey;
    },
    sortOrderLabel(sortOrder) {
      const option = this.sortOrderOptions.find((item) => item.value === sortOrder);
      return option ? option.label : sortOrder;
    },
    isPinned(alert) {
      return alert && alert.display_state === "pinned";
    },
    isHidden(alert) {
      return alert && alert.display_state === "hidden";
    },
    displayScore(score) {
      if (score === "null" || typeof score === "undefined" || String(score).trim() === "") {
        return "評価情報無し";
      }

      return score;
    },
    formatDate(dateText) {
      const timestamp = this.dateValue(dateText);
      if (!timestamp) return "-";

      const date = new Date(timestamp);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, "0");
      const day = String(date.getDate()).padStart(2, "0");
      return `${year}-${month}-${day}`;
    },
    ensureCurrentPage() {
      if (this.currentPage > this.totalPages) this.currentPage = this.totalPages;
      if (this.currentPage < 1) this.currentPage = 1;
    },
    goToPreviousPage() {
      if (this.currentPage > 1) this.currentPage -= 1;
    },
    goToNextPage() {
      if (this.currentPage < this.totalPages) this.currentPage += 1;
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
    showToast(text, color = "success") {
      this.snackbar.icon = color == "success" ? "mdi-check-circle-outline" : "mdi-alert-circle-outline";
      this.snackbar.text = text;
      this.snackbar.color = color;
      this.snackbar.show = true;
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
  --app-hidden-bg: #eeeeee;
}

@media (prefers-color-scheme: dark) {
  :root {
    --app-bg-color: #121212;
    --app-card-bg: #1e1e1e;
    --app-text-main: #f5f5f5;
    --app-text-sub: #b0b0b0;
    --app-border-color: #333333;
    --app-hidden-bg: #2a2a2a;
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
  padding-bottom: 16px;
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

.settings-header,
.settings-actions,
.chip-row,
.pagination-card {
  display: flex;
  align-items: center;
}

.settings-header {
  justify-content: space-between;
  gap: 12px;
}

.settings-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.summary-main {
  font-size: 16px;
  font-weight: 700;
}

.summary-sub {
  margin-top: 4px;
  color: var(--app-text-sub);
  font-size: 13px;
}

.chip-row {
  flex-wrap: wrap;
  margin-top: 8px;
}

.chip-row-label {
  font-size: 13px;
  font-weight: 700;
  margin-right: 8px;
  color: var(--app-text-sub);
}

.pagination-card {
  justify-content: space-between;
}

.pagination-text {
  font-weight: 700;
  color: var(--app-text-main);
}

.label-text {
  color: var(--app-text-sub);
}

.value-text {
  color: var(--app-text-main);
  word-break: break-word;
}

.app-dialog {
  align-self: center;
  margin: 16px;
  max-height: calc(100vh - 32px);
}

.dialog-card {
  background: var(--app-card-bg) !important;
  color: var(--app-text-main) !important;
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 32px);
  overflow: hidden;
}

.dialog-body {
  overflow-y: auto;
}

.dialog-title {
  flex: 0 0 auto;
  position: sticky;
  top: 0;
  z-index: 3;
  gap: 8px;
  background: var(--app-card-bg);
  border-bottom: 1px solid var(--app-border-color);
  word-break: break-word;
}

.dialog-section-title {
  margin: 18px 0 8px;
  font-size: 14px;
  font-weight: 700;
  color: var(--app-text-main);
}

.product-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.product-title-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 4px;
}

.product-group-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
}

.product-group-card {
  padding: 10px 12px 12px;
  background: transparent !important;
}

.product-category-checkbox {
  margin-top: 0;
}

.product-category-checkbox .v-label {
  color: var(--app-text-main);
  font-weight: 700;
}

.product-option-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(145px, 1fr));
  column-gap: 10px;
  padding-left: 20px;
}

.product-option-checkbox {
  margin-top: 0;
  min-width: 0;
}

.product-option-checkbox .v-label {
  color: var(--app-text-main);
  font-size: 13px;
  line-height: 1.35;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.detail-grid {
  display: grid;
  grid-template-columns: 120px 1fr;
  gap: 8px;
  font-size: 13px;
}

.detail-description {
  font-size: 13px;
  line-height: 1.65;
  color: var(--app-text-main);
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-actions {
  flex: 0 0 auto;
  flex-wrap: wrap;
  gap: 8px;
}

.url-button {
  display: block;
  width: 100%;
  padding: 0;
  color: #1565c0;
  text-align: left;
  word-break: break-all;
  cursor: pointer;
}

.loader-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 180px;
  min-height: 120px;
}

.loader-text {
  margin-top: 14px;
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
}

.loader-panel .v-progress-circular--indeterminate > svg,
.loader-panel .v-progress-circular--indeterminate .v-progress-circular__overlay {
  animation-play-state: running !important;
}

@media (max-width: 520px) {
  .settings-header {
    align-items: stretch;
    flex-direction: column;
  }

  .settings-actions {
    justify-content: stretch;
  }

  .settings-actions .v-btn {
    flex: 1 1 100%;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
