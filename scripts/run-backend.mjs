import { spawn } from 'node:child_process'
import { existsSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { loadEnvFile } from 'node:process'
import { fileURLToPath } from 'node:url'

const repositoryRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const environmentFile = resolve(repositoryRoot, '.env')
const backendDirectory = resolve(repositoryRoot, 'backend')

if (existsSync(environmentFile)) {
  loadEnvFile(environmentFile)
}

const wrapper = process.platform === 'win32' ? 'mvnw.cmd' : './mvnw'
const backend = spawn(
  wrapper,
  ['spring-boot:run', '-Dspring-boot.run.profiles=local'],
  {
    cwd: backendDirectory,
    env: process.env,
    shell: process.platform === 'win32',
    stdio: 'inherit',
  },
)

backend.on('error', (error) => {
  console.error(`Unable to start the backend: ${error.message}`)
  process.exitCode = 1
})

backend.on('exit', (code, signal) => {
  if (signal) {
    process.kill(process.pid, signal)
    return
  }

  process.exitCode = code ?? 1
})
