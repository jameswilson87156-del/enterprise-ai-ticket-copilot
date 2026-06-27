import { spawn, spawnSync } from 'node:child_process'
import { existsSync, mkdirSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { chromium } from 'playwright-core'

const __dirname = dirname(fileURLToPath(import.meta.url))
const frontendRoot = resolve(__dirname, '..')
const repoRoot = resolve(frontendRoot, '..')
const outputRoot = resolve(repoRoot, 'docs', 'images')
const largeRoot = resolve(outputRoot, 'large')
const baseUrl = process.env.SCREENSHOT_URL || 'http://127.0.0.1:5173'
const shouldStartServer = !process.env.SCREENSHOT_URL
const standardViewport = { width: 1440, height: 960 }
const focusedViewport = { width: 1100, height: 960 }
const largeViewport = { width: 1920, height: 1200 }

const browserCandidates = [
  process.env.CHROME_PATH,
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe',
  'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe',
  'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'
].filter(Boolean)

function findBrowser() {
  return browserCandidates.find((candidate) => existsSync(candidate))
}

function startServer() {
  const npmCommand = process.platform === 'win32' ? 'npm.cmd' : 'npm'
  const env = Object.fromEntries(Object.entries(process.env).filter(([key, value]) => key && !key.startsWith('=') && value !== undefined))
  if (process.platform === 'win32') {
    return spawn(`${npmCommand} run dev:demo -- --host 127.0.0.1 --port 5173 --strictPort`, {
      cwd: frontendRoot,
      env: { ...env, BROWSER: 'none' },
      shell: true,
      stdio: ['ignore', 'pipe', 'pipe']
    })
  }
  return spawn(npmCommand, ['run', 'dev:demo', '--', '--host', '127.0.0.1', '--port', '5173', '--strictPort'], {
    cwd: frontendRoot,
    env: { ...env, BROWSER: 'none' },
    stdio: ['ignore', 'pipe', 'pipe']
  })
}

function stopServer(serverProcess) {
  if (!serverProcess) {
    return
  }
  if (process.platform === 'win32') {
    spawnSync('taskkill', ['/pid', String(serverProcess.pid), '/T', '/F'], { stdio: 'ignore' })
    return
  }
  serverProcess.kill('SIGTERM')
}

async function waitForServer(url, serverProcess, timeoutMs = 30000) {
  const started = Date.now()
  await new Promise((resolve) => setTimeout(resolve, 250))
  while (Date.now() - started < timeoutMs) {
    if (serverProcess && serverProcess.exitCode !== null) {
      throw new Error(`Demo server exited before becoming ready (exit code ${serverProcess.exitCode}).`)
    }
    try {
      const response = await fetch(url)
      if (response.ok) {
        return
      }
    } catch {
      // Keep polling while Vite starts.
    }
    await new Promise((resolve) => setTimeout(resolve, 500))
  }
  throw new Error(`Timed out waiting for ${url}`)
}

async function assertNoHorizontalOverflow(page, viewport, label) {
  await page.setViewportSize(viewport)
  await page.evaluate(() => window.scrollTo(0, 0))
  await page.waitForTimeout(150)
  const dimensions = await page.evaluate(() => ({
    viewportWidth: document.documentElement.clientWidth,
    documentWidth: Math.max(document.documentElement.scrollWidth, document.body.scrollWidth)
  }))
  if (dimensions.documentWidth > dimensions.viewportWidth + 1) {
    throw new Error(`${label} viewport has horizontal overflow: ${dimensions.documentWidth}px > ${dimensions.viewportWidth}px.`)
  }
}

async function verifyDemoInteractions(page) {
  await page.setViewportSize(standardViewport)
  await page.evaluate(() => window.scrollTo(0, 0))

  const workbench = page.locator('[data-screenshot="ticket-detail"]')
  const search = workbench.getByRole('searchbox', { name: '搜索', exact: true })
  await search.fill('KB-REDIS-CONN')
  await page.waitForTimeout(100)
  if (await page.getByRole('button', { name: /DEMO-0003/ }).count() !== 1) {
    throw new Error('Knowledge keyword search did not return DEMO-0003.')
  }

  await search.fill('no-such-ticket-keyword')
  await page.getByText('暂无匹配工单', { exact: true }).waitFor()
  await search.fill('')

  await workbench.getByRole('button', { name: '已沉淀', exact: true }).click()
  if (await page.getByRole('button', { name: /DEMO-0008/ }).count() !== 1) {
    throw new Error('Knowledge-based filter did not return DEMO-0008.')
  }
  await workbench.getByRole('button', { name: '全部', exact: true }).click()

  await page.getByRole('button', { name: /DEMO-0007/ }).click()
  await page.waitForTimeout(300)
  await page.getByRole('button', { name: '生成知识草稿', exact: true }).click()
  await page.getByText('知识草稿已生成，发布前仍需人工审核。', { exact: true }).waitFor()
  await page.getByRole('button', { name: '人工确认入库', exact: true }).click()
  await page.getByText('知识草稿已由人工确认并完成沉淀。', { exact: true }).waitFor()
}

async function captureViewport(page, name, viewport = standardViewport) {
  await page.setViewportSize(viewport)
  await page.screenshot({
    path: resolve(outputRoot, `${name}.png`),
    animations: 'disabled'
  })
}

async function captureLarge(page, name) {
  await page.setViewportSize(largeViewport)
  await page.screenshot({
    path: resolve(largeRoot, `${name}.png`),
    animations: 'disabled'
  })
  await page.setViewportSize(standardViewport)
}

async function scrollTo(page, selector) {
  await page.locator(selector).scrollIntoViewIfNeeded()
  await page.waitForTimeout(200)
}

mkdirSync(outputRoot, { recursive: true })
mkdirSync(largeRoot, { recursive: true })

const server = shouldStartServer ? startServer() : null

if (server) {
  server.stdout.on('data', (chunk) => process.stdout.write(chunk))
  server.stderr.on('data', (chunk) => process.stderr.write(chunk))
}

try {
  await waitForServer(baseUrl, server)
  const executablePath = findBrowser()
  if (!executablePath) {
    throw new Error('No Chrome or Edge executable found. Set CHROME_PATH to a Chromium-based browser.')
  }

  const browser = await chromium.launch({
    executablePath,
    headless: true
  })
  const page = await browser.newPage({ viewport: standardViewport, deviceScaleFactor: 1 })
  await page.goto(baseUrl, { waitUntil: 'networkidle' })
  await page.waitForSelector('[data-screenshot="dashboard"]')
  await page.waitForSelector('[data-screenshot="ticket-detail"] .showcase-metadata-grid')
  await page.waitForSelector('[data-screenshot="ai-analysis"] .showcase-signal-grid')

  await captureViewport(page, 'dashboard')
  await captureLarge(page, 'dashboard')

  await page.getByRole('button', { name: /DEMO-0005/ }).click()
  await page.waitForSelector('[data-screenshot="ticket-detail"] .showcase-detail-header h1')
  await page.waitForSelector('[data-screenshot="ticket-detail"] .showcase-metadata-grid')
  await page.waitForSelector('[data-screenshot="ai-analysis"] .showcase-signal-grid')
  await page.waitForTimeout(240)
  await captureViewport(page, 'ticket-detail', standardViewport)
  await captureLarge(page, 'ticket-detail')

  await page.waitForSelector('[data-screenshot="ai-analysis"] .showcase-signal-grid')
  await page.setViewportSize(focusedViewport)
  await scrollTo(page, '[data-screenshot="ai-analysis"]')
  await captureViewport(page, 'ai-analysis', focusedViewport)
  await captureLarge(page, 'ai-analysis')

  await page.getByRole('button', { name: /DEMO-0008/ }).click()
  await page.waitForTimeout(300)
  await page.setViewportSize(focusedViewport)
  await scrollTo(page, '[data-screenshot="knowledge-base"]')
  await captureViewport(page, 'knowledge-base', focusedViewport)
  await captureLarge(page, 'knowledge-base')

  await verifyDemoInteractions(page)
  await assertNoHorizontalOverflow(page, { width: 1366, height: 900 }, '1366 desktop')
  await assertNoHorizontalOverflow(page, { width: 390, height: 844 }, '390 mobile')

  await browser.close()
  console.log(`Screenshots saved to ${outputRoot}`)
} finally {
  stopServer(server)
}
