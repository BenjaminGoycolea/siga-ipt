import Keycloak from 'keycloak-js'

const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8081',
  realm: import.meta.env.VITE_KEYCLOAK_REALM || 'siga-ipt',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'siga-ipt-web',
})

let initialization

export function initializeKeycloak() {
  if (!initialization) {
    initialization = keycloak.init({
      onLoad: 'check-sso',
      pkceMethod: 'S256',
      checkLoginIframe: false,
      silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
      silentCheckSsoFallback: false,
    })
  }
  return initialization
}

export default keycloak
