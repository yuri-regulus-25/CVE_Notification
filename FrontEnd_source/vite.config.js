import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue2'
import { viteSingleFile } from 'vite-plugin-singlefile'

export default defineConfig({
  plugins: [
    vue(),
    viteSingleFile()
  ],
  base: './',
  build: {
    outDir: 'dist',
    assetsInlineLimit: 100000000,
    cssCodeSplit: false
  }
})