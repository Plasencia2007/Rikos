import type { Local } from '../../api/cliente'
import { PREFIJOS_PAIS, separarTelefono, unirTelefono } from './prefijosPais'

/** Todo lo editable de un local. El PUT reemplaza el local entero, así que los 4 pasos comparten esta copia. */
export interface FormularioLocal {
  codigo: string
  nombre: string
  direccion: string
  latitud: number | null
  longitud: number | null
  prefijo: string
  numero: string
  correo: string
  facebookUrl: string
  instagramUrl: string
  tiktokUrl: string
  deliveryHabilitado: boolean
  recojoHabilitado: boolean
  tiempoMinEntregaMin: string
  tiempoMaxEntregaMin: string
  logoUrl: string
  colorPrincipal: string
  portadaUrl: string
}

export const COLOR_POR_DEFECTO = '#2B7A68'

export const FORMULARIO_VACIO: FormularioLocal = {
  codigo: '', nombre: '', direccion: '', latitud: null, longitud: null,
  prefijo: PREFIJOS_PAIS[0].prefijo, numero: '', correo: '',
  facebookUrl: '', instagramUrl: '', tiktokUrl: '',
  deliveryHabilitado: true, recojoHabilitado: true, tiempoMinEntregaMin: '', tiempoMaxEntregaMin: '',
  logoUrl: '', colorPrincipal: COLOR_POR_DEFECTO, portadaUrl: '',
}

export function formularioDesdeLocal(l: Local): FormularioLocal {
  const { prefijo, numero } = separarTelefono(l.telefono)
  return {
    codigo: l.codigo, nombre: l.nombre, direccion: l.direccion,
    latitud: l.latitud ?? null, longitud: l.longitud ?? null,
    prefijo, numero, correo: l.correo ?? '',
    facebookUrl: l.facebookUrl ?? '', instagramUrl: l.instagramUrl ?? '', tiktokUrl: l.tiktokUrl ?? '',
    deliveryHabilitado: l.deliveryHabilitado, recojoHabilitado: l.recojoHabilitado,
    tiempoMinEntregaMin: l.tiempoMinEntregaMin?.toString() ?? '',
    tiempoMaxEntregaMin: l.tiempoMaxEntregaMin?.toString() ?? '',
    logoUrl: l.logoUrl ?? '', colorPrincipal: l.colorPrincipal ?? COLOR_POR_DEFECTO, portadaUrl: l.portadaUrl ?? '',
  }
}

export function datosParaGuardar(f: FormularioLocal) {
  return {
    nombre: f.nombre,
    direccion: f.direccion,
    telefono: unirTelefono(f.prefijo, f.numero) || null,
    correo: f.correo || null,
    latitud: f.latitud,
    longitud: f.longitud,
    facebookUrl: f.facebookUrl || null,
    instagramUrl: f.instagramUrl || null,
    tiktokUrl: f.tiktokUrl || null,
    logoUrl: f.logoUrl || null,
    colorPrincipal: f.colorPrincipal || null,
    portadaUrl: f.portadaUrl || null,
    deliveryHabilitado: f.deliveryHabilitado,
    recojoHabilitado: f.recojoHabilitado,
    tiempoMinEntregaMin: f.tiempoMinEntregaMin ? Number(f.tiempoMinEntregaMin) : null,
    tiempoMaxEntregaMin: f.tiempoMaxEntregaMin ? Number(f.tiempoMaxEntregaMin) : null,
  }
}

const CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/** Teléfono y correo son obligatorios (el backend también los exige en el PUT). */
export function erroresDeContacto(f: FormularioLocal): { numero?: string; correo?: string } {
  const errores: { numero?: string; correo?: string } = {}
  const digitos = f.numero.replace(/\D/g, '')
  if (!f.numero.trim()) errores.numero = 'Ingresa el teléfono del local.'
  else if (digitos.length < 6) errores.numero = 'El teléfono parece incompleto.'
  if (!f.correo.trim()) errores.correo = 'Ingresa el correo del local.'
  else if (!CORREO.test(f.correo.trim())) errores.correo = 'Escribe un correo válido, por ejemplo contacto@tu-restaurante.pe.'
  return errores
}

export const COLOR_HEX = /^#[0-9A-Fa-f]{6}$/
