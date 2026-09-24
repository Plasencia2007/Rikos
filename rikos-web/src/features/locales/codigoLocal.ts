/** Mismo formato que valida el backend (CrearLocal.java): minusculas, numeros y guiones. */
const FORMATO_CODIGO = /^[a-z0-9]+(-[a-z0-9]+)*$/

export function esCodigoValido(codigo: string): boolean {
  return FORMATO_CODIGO.test(codigo)
}

const MAPA_TILDES: Record<string, string> = {
  á: 'a', é: 'e', í: 'i', ó: 'o', ú: 'u', ñ: 'n', ü: 'u',
}

/** Sugiere un código a partir del nombre: "San Isidro" -> "san-isidro". */
export function sugerirCodigo(nombre: string): string {
  const sinTildes = nombre
    .toLowerCase()
    .split('')
    .map((c) => MAPA_TILDES[c] ?? c)
    .join('')
  return sinTildes
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
}
