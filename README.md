# SkyWeather

<p align="center">
  <b>基于 Capacitor 的跨平台天气应用</b><br>
  <b>Cross-platform weather app built with Capacitor</b>
</p>

---

## 中文版

SkyWeather 是一款现代化的天气应用，使用 React + Vite + Capacitor 构建，支持 Web 和 Android 平台。

### 功能特性

- 实时天气查询与当前位置天气展示
- 未来 24 小时逐小时预报
- 未来 7 天天气趋势
- 降水概率预报
- 多城市收藏与管理
- 精美的动态天气背景
- 城市剪影视觉效果
- 响应式设计，适配移动端与桌面端

### 技术栈

| 类别 | 技术 |
|------|------|
| 前端框架 | React 19 + TypeScript |
| 构建工具 | Vite 7 |
| UI 组件 | Radix UI + Tailwind CSS |
| 跨平台 | Capacitor 8 |
| 后端 API | Hono + tRPC |
| 数据库 | Drizzle ORM + MySQL2 |
| 图表 | Chart.js / Recharts |

### 本地开发

```bash
# 进入应用目录
cd app

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build

# 同步到 Android 项目
npx cap sync android

# 打开 Android Studio
npx cap open android
```

### 构建 APK

本项目已配置 GitHub Actions，每次推送到 `main` 分支会自动构建 Debug APK。你也可以在仓库的 **Actions** 页面手动触发构建。

---

## English

SkyWeather is a modern weather application built with React, Vite, and Capacitor, supporting both Web and Android platforms.

### Features

- Real-time weather query and current location weather display
- Hourly forecast for the next 24 hours
- 7-day weather trend
- Precipitation probability forecast
- Multi-city favorites and management
- Beautiful dynamic weather backgrounds
- City silhouette visual effects
- Responsive design for mobile and desktop

### Tech Stack

| Category | Technology |
|----------|------------|
| Frontend | React 19 + TypeScript |
| Build Tool | Vite 7 |
| UI Components | Radix UI + Tailwind CSS |
| Cross-platform | Capacitor 8 |
| Backend API | Hono + tRPC |
| Database | Drizzle ORM + MySQL2 |
| Charts | Chart.js / Recharts |

### Local Development

```bash
# Enter the app directory
cd app

# Install dependencies
npm install

# Start dev server
npm run dev

# Build for production
npm run build

# Sync to Android project
npx cap sync android

# Open Android Studio
npx cap open android
```

### Build APK

This project is configured with GitHub Actions. A Debug APK is automatically built on every push to the `main` branch. You can also manually trigger a build from the **Actions** tab in the repository.

---

## 下载 / Download

| 版本 | 说明 | 下载 |
|------|------|------|
| v0.1.0 | 首个预览版本 | [Releases](../../releases) |

## 许可证 / License

MIT
