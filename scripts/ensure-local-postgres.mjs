import { spawnSync } from 'node:child_process'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const repositoryRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const docker = process.platform === 'win32' ? 'docker.exe' : 'docker'

function runDocker(argumentsList, options = {}) {
  const result = spawnSync(docker, argumentsList, {
    cwd: repositoryRoot,
    encoding: 'utf8',
    stdio: options.capture ? 'pipe' : 'inherit',
  })

  if (result.error) {
    throw new Error(`Unable to run Docker: ${result.error.message}`)
  }

  if (result.status !== 0) {
    const detail = options.capture ? `: ${(result.stderr || result.stdout).trim()}` : ''
    throw new Error(`Docker command failed (${argumentsList.join(' ')})${detail}`)
  }

  return options.capture ? result.stdout.trim() : ''
}

runDocker(['volume', 'create', 'url-shortener_postgres_data'])
runDocker(['compose', 'up', '--detach', 'postgres'])

for (let attempt = 0; attempt < 30; attempt += 1) {
  const health = runDocker(
    ['inspect', '--format', '{{.State.Health.Status}}', 'postgres-db'],
    { capture: true },
  )

  if (health === 'healthy') {
    break
  }

  if (attempt === 29) {
    throw new Error('postgres-db did not become healthy within 30 seconds')
  }

  await new Promise((resolvePromise) => setTimeout(resolvePromise, 1000))
}

runDocker([
  'compose',
  'run',
  '--rm',
  '--no-deps',
  'auth-database-init',
])

console.log('Local PostgreSQL is ready: postgres-db/authserver')
