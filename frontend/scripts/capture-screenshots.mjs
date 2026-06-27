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
const largeViewport = { width: 1920, height: 1200 }

const showcaseTargets = [
  { name: 'dashboard', route: 'dashboard', selector: '[data-screenshot="dashboard"]' },
  { name: 'ticket-detail', route: 'ticket-detail', selector: '[data-screenshot="ticket-detail"]' },
  { name: 'knowledge-base', route: 'knowledge-base', selector: '[data-screenshot="knowledge-base"]' },
  { name: 'trace-evidence', route: 'trace-evidence', selector: '[data-screenshot="trace-evidence"]' },
  { name: 'human-review', route: 'human-review', selector: '[data-screenshot="human-review"]' }
]

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

function targetUrl(route) {
  const url = new URL(baseUrl)
  url.hash = route
  return url.toString()
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

async function openShowcase(page, target, viewport = standardViewport) {
  await page.setViewportSize(viewport)
  await page.goto(targetUrl(target.route), { waitUntil: 'networkidle' })
  await page.waitForSelector(target.selector)
  await page.evaluate(() => window.scrollTo(0, 0))
  await page.waitForTimeout(150)
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

  for (const target of showcaseTargets) {
    await openShowcase(page, target)
    await captureViewport(page, target.name)
    await captureLarge(page, target.name)
    await openShowcase(page, target, { width: 1366, height: 900 })
    await assertNoHorizontalOverflow(page, { width: 1366, height: 900 }, `${target.name} 1366 desktop`)
    await openShowcase(page, target, { width: 390, height: 844 })
    await assertNoHorizontalOverflow(page, { width: 390, height: 844 }, `${target.name} 390 mobile`)
  }

  await browser.close()
  console.log(`Screenshots saved to ${outputRoot}`)
} finally {
  stopServer(server)
}
