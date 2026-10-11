import js from '@eslint/js'
import tseslint from 'typescript-eslint'
import boundaries from 'eslint-plugin-boundaries'
import reactHooks from 'eslint-plugin-react-hooks'
import globals from 'globals'

export default tseslint.config(
  {
    ignores: ['dist/**', 'node_modules/**'],
  },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  {
    files: ['src/**/*.{ts,tsx}'],
    plugins: { 'react-hooks': reactHooks },
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'module',
      globals: {
        ...globals.browser,
        ...globals.es2022,
      },
    },
    rules: {
      'react-hooks/rules-of-hooks': 'error',
      'react-hooks/exhaustive-deps': 'warn',
    },
  },
  // Frontera de modulos entre features (espejo frontend de
  // ArquitecturaDeModulosTest.java en el backend): el codigo de una
  // feature solo puede importar de OTRA feature a traves de su indice
  // publico (features/X o features/X/index), nunca de un archivo interno
  // (features/X/algo/interno.ts). Importar los propios archivos internos
  // de la misma feature sigue permitido, igual que importar desde
  // comun/, api/ o apps/, que no estan sujetos a esta regla.
  {
    files: ['src/features/**/*.{ts,tsx}'],
    plugins: { boundaries },
    settings: {
      'boundaries/elements': [
        {
          type: 'feature',
          pattern: 'src/features/*',
        },
      ],
      'import/resolver': {
        typescript: { alwaysTryTypes: true },
      },
    },
    rules: {
      'boundaries/dependencies': [
        'error',
        {
          default: 'allow',
          policies: [
            {
              // Por defecto, ninguna feature puede importar archivos de
              // otra feature. Las importaciones dentro de la misma
              // feature son "internal" y el plugin las ignora sin
              // necesidad de una regla explicita.
              from: { element: { type: 'feature' } },
              disallow: { to: { element: { type: 'feature' } } },
              message:
                'No importes archivos internos de otra feature. Importa solo desde su indice publico (features/<nombre> o su index.ts).',
            },
            {
              // Unica excepcion: el indice publico de la feature, el
              // punto de entrada que decide que se expone al resto del
              // sistema.
              from: { element: { type: 'feature' } },
              allow: {
                to: { element: { type: 'feature', fileInternalPath: 'index.ts' } },
              },
            },
          ],
        },
      ],
    },
  },
)
