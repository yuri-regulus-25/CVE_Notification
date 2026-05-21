import Vue from 'vue'
import Vuetify from 'vuetify'
import 'vuetify/dist/vuetify.min.css'
import '@mdi/font/css/materialdesignicons.css'
import App from './App.vue'
import _ from 'lodash'

Vue.prototype.$_ = _;

Vue.use(Vuetify)

const prefersDark = window.matchMedia &&
  window.matchMedia('(prefers-color-scheme: dark)').matches

new Vue({
  vuetify: new Vuetify({
    theme: {
      dark: prefersDark
    },
    icons: {
      iconfont: 'mdi'
    }
  }),
  render: h => h(App)
}).$mount('#app')