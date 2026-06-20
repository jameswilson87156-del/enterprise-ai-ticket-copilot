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
    return spawn(`${npmCommand} run dev:demo -- --host 127.0.0.1 --port 5173`, {
      cwd: frontendRoot,
      env: { ...env, BROWSER: 'none' },
      shell: true,
      stdio: ['ignore', 'pipe', 'pipe']
    })
  }
  return spawn(npmCommand, ['run', 'dev:demo', '--', '--host', '127.0.0.1', '--port', '5173'], {
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

async function waitForServer(url, timeoutMs = 30000) {
  const started = Date.now()
  while (Date.now() - started < timeoutMs) {
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

async function captureViewport(page, name) {
  await page.screenshot({
    path: resolve(outputRoot, `${name}.png`),
    animations: 'disabled'
  })
}

async function captureLarge(page, name) {
  await page.setViewportSize({ width: 1920, height: 1200 })
  await page.screenshot({
    path: resolve(largeRoot, `${name}.png`),
    animations: 'disabled'
  })
  await page.setViewportSize({ width: 1440, height: 1050 })
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
  await waitForServer(baseUrl)
  const executablePath = findBrowser()
  if (!executablePath) {
    throw new Error('No Chrome or Edge executable found. Set CHROME_PATH to a Chromium-based browser.')
  }

  const browser = await chromium.launch({
    executablePath,
    headless: true
  })
  const page = await browser.newPage({ viewport: { width: 1440, height: 1050 }, deviceScaleFactor: 1 })
  await page.goto(baseUrl, { waitUntil: 'networkidle' })
  await page.waitForSelector('[data-screenshot="dashboard"]')

  await captureViewport(page, 'dashboard')
  await captureLarge(page, 'dashboard')

  await page.getByRole('button', { name: /DEMO-0005/ }).click()
  await page.waitForTimeout(300)
  await scrollTo(page, '[data-screenshot="ticket-detail"]')
  await captureViewport(page, 'ticket-detail')
  await captureLarge(page, 'ticket-detail')

  await scrollTo(page, '[data-screenshot="ai-analysis"]')
  await captureViewport(page, 'ai-analysis')
  await captureLarge(page, 'ai-analysis')

  await page.getByRole('button', { name: /DEMO-0008/ }).click()
  await page.waitForTimeout(300)
  await scrollTo(page, '[data-screenshot="knowledge-base"]')
  await captureViewport(page, 'knowledge-base')
  await captureLarge(page, 'knowledge-base')

  await browser.close()
  console.log(`Screenshots saved to ${outputRoot}`)
} finally {
  stopServer(server)
}
