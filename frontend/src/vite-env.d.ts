/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_DEBUG_INIT_DATA?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
