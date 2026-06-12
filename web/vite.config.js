import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import fs from 'fs'
import styleImport from 'vite-plugin-style-import' //按需加载样式

import SvgIconsPlugin from 'vite-plugin-svg-icons' // 打包生成svg雪碧图

import DC from '@dvgis/vite-plugin-dc'
// import ViteComponents, { AntDesignVueResolver } from 'vite-plugin-components'
//  import Components from 'unplugin-vue-components/vite'
//  import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers'

// https://vitejs.dev/config/
const vditorPackagePath = path.resolve(__dirname, 'node_modules/vditor')

function vditorStaticAssets() {
  return {
    name: 'vditor-static-assets',
    configureServer(server) {
      server.middlewares.use('/vditor', (req, res, next) => {
        const requestPath = decodeURIComponent((req.url || '').split('?')[0])
        const filePath = path.resolve(vditorPackagePath, `.${requestPath}`)
        if (!filePath.startsWith(vditorPackagePath)) {
          res.statusCode = 403
          res.end('Forbidden')
          return
        }
        fs.stat(filePath, (error, stats) => {
          if (error || !stats.isFile()) {
            next()
            return
          }
          res.setHeader('Content-Type', getStaticContentType(filePath))
          fs.createReadStream(filePath).pipe(res)
        })
      })
    },
    closeBundle() {
      const targetPath = path.resolve(__dirname, 'dist/vditor')
      copyDirectory(vditorPackagePath, targetPath)
    },
  }
}

function copyDirectory(source, target) {
  if (!fs.existsSync(source)) {
    return
  }
  fs.mkdirSync(target, { recursive: true })
  fs.readdirSync(source, { withFileTypes: true }).forEach((entry) => {
    const sourcePath = path.join(source, entry.name)
    const targetPath = path.join(target, entry.name)
    if (entry.isDirectory()) {
      copyDirectory(sourcePath, targetPath)
      return
    }
    if (entry.isFile()) {
      fs.copyFileSync(sourcePath, targetPath)
    }
  })
}

function getStaticContentType(filePath) {
  const ext = path.extname(filePath).toLowerCase()
  const contentTypes = {
    '.css': 'text/css; charset=utf-8',
    '.js': 'application/javascript; charset=utf-8',
    '.json': 'application/json; charset=utf-8',
    '.svg': 'image/svg+xml',
    '.png': 'image/png',
    '.jpg': 'image/jpeg',
    '.jpeg': 'image/jpeg',
    '.gif': 'image/gif',
    '.woff': 'font/woff',
    '.woff2': 'font/woff2',
    '.ttf': 'font/ttf',
  }
  return contentTypes[ext] || 'application/octet-stream'
}

export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, __dirname); // 获取全局变量
  const isBuild = command === 'build';
  return {
    base: env.VITE_BASE_PATH,
    css: {
      preprocessorOptions: {
        less: {
          modifyVars: {},
          javascriptEnabled: true,
        },
      },
    },
    assetsInclude: ['svg'],
    plugins: [
      vue(),

      styleImport({
        libs: [
          {
            libraryName: 'ant-design-vue',
            esModule: true,
            resolveStyle: (name) => {
              return `ant-design-vue/es/${name}/style/index`
            },
          },
        ],
      }),
      SvgIconsPlugin({
        iconDirs: [path.resolve(process.cwd(), 'src/assets/icon')],
        svgoOptions: isBuild,
        // default
        symbolId: 'icon-[dir]-[name]',
      }),
      DC(),
      vditorStaticAssets(),
    ],
    build: {
      rollupOptions: {
        output: {
          manualChunks: {
            echarts: ['echarts'],
          },
        },
      },
      terserOptions: {
        ecma: undefined,
        compress: {
          drop_console: true,
          drop_debugger: true,
          pure_funcs: ['console.log'],
        },
        // ecma: undefined,
        // warnings: false,
        // parse: {},
        // compress: {
        //   drop_console: true, // 禁用console.* 功能
        //   drop_debugger: false, //移除debugger语句
        //   pure_funcs: ['console.log'], // 移除被禁用的console.log语句
        // },
      },
    },
    server: {
      //本地服务
      host: '0.0.0.0',
      port: 3001, //端口号
      open: true, //启动时是否自动打开
      proxy: {
        '/vue-manage': {
          target: env.VITE_BASE_URL,
          changeOrigin: true,
        },
        '/local-test': {
          target: env.VITE_LOCAL_TEST_URL,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/local-test/, ''),
        },
      },
    },
    clearScreen: false, // vite清屏不清除控制台打印的信息
    // logLevel:'error',
    resolve: {
      alias: {
        vue: 'vue/dist/vue.esm-bundler.js',
        '@': path.resolve(__dirname, 'src'),
      },
    },
    optimizeDeps: {
      include: ['vue-i18n'],
    },
    logLevel: 'info',
  }
})
