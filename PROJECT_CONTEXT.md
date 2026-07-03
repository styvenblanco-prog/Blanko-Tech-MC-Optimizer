# Blanko-Tech MC Optimizer - Contexto del Proyecto

## 📋 Descripción General

**Blanko-Tech MC Optimizer** es una aplicación Android de optimización y control de rendimiento de dispositivos móviles. La app está construida con tecnologías modernas como **Kotlin**, **Jetpack Compose**, y **Shizuku** para proporcionar funcionalidades avanzadas de gestión de aplicaciones y optimización del sistema.

**App ID**: `com.aistudio.mcoptimizer.fkvzqp`  
**Versión**: 1.0  
**Nivel mínimo de API**: 24 (Android 7.0+)  
**API compilada**: 36

---

## 🏗️ Stack Tecnológico

### Lenguaje y Framework
- **Kotlin** 2.2.10
- **Jetpack Compose** (UI moderna)
- **Android Gradle Plugin**: 9.1.1

### Librerías Principales
- **Shizuku**: Control de aplicaciones en background y congelación de apps
- **Jetpack Navigation**: Compose Navigation 2.8.9
- **Room Database**: 2.7.0 (Persistencia de datos)
- **Retrofit + Moshi**: Para peticiones HTTP y serialización JSON
- **Firebase**: Integración de servicios en la nube
- **Datastore Preferences**: Almacenamiento de preferencias
- **Coroutines**: Programación asíncrona (1.10.2)
- **Coil**: Carga y manejo de imágenes

### Herramientas de Desarrollo
- **KSP (Kotlin Symbol Processing)**: 2.3.5
- **Roborazzi**: Testing UI
- **Robolectric**: 4.16.1 (Testing local)
- **Espresso**: Testing instrumentado

---

## 📁 Estructura del Proyecto

```
/workspaces/Blanko-Tech-MC-Optimizer/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt              # Actividad principal
│   │   │   │   ├── OptimizationEngine.kt        # Motor de optimización
│   │   │   │   ├── OptimizerViewModel.kt        # ViewModel central
│   │   │   │   ├── OverlayService.kt            # Servicio de overlay
│   │   │   │   └── [Otros componentes]
│   │   │   └── res/                             # Recursos (layouts, drawables, etc.)
│   │   ├── test/                                # Tests unitarios
│   │   └── androidTest/                         # Tests instrumentados
│   ├── build.gradle.kts                         # Configuración del build
│   └── proguard-rules.pro                       # Reglas de ofuscación
├── gradle/
│   └── libs.versions.toml                       # Versiones centralizadas
├── build.gradle.kts                             # Build principal
├── settings.gradle.kts                          # Configuración de módulos
├── local.properties                             # Propiedades locales
└── debug.apk                                    # APK compilada (entrega final)
```

---

## 🎯 Implementaciones Principales

### 1. **Widget de Congelación de Apps con Shizuku**
- Permite congelar/descongelar aplicaciones directamente desde la app
- Requiere permiso especial de Shizuku (debe estar instalado en el dispositivo)
- Implementado en: `ShizukuAppFreezerWidget`, `OptimizationEngine.kt`

### 2. **Motor de Optimización (OptimizationEngine.kt)**
- Análisis de rendimiento del sistema
- Gestión de permisos de aplicaciones
- Control de apps en background
- Manejo de memoria y batería

### 3. **OverlayService.kt**
- Servicio que proporciona funcionalidades de overlay
- Permite mostrar información flotante en pantalla

### 4. **OptimizerViewModel.kt**
- ViewModel centralizado con 214+ líneas de lógica
- Gestiona el estado de la aplicación
- Comunica datos entre UI y el modelo

### 5. **Integración Firebase**
- Telemetría y análisis de uso
- Autenticación (si aplica)

### 6. **Iconos del Launcher Actualizados**
- Recursos visuales mejorados en múltiples densidades

---

## 🔨 Cómo Compilar la APK

### Requisitos Previos
1. **Gradle**: Debe estar instalado en el sistema
2. **JDK**: Se recomienda OpenJDK 17 o superior
3. **Android SDK**: Variables de entorno configuradas (opcional en algunos sistemas)

### Pasos para Compilar

```bash
cd /workspaces/Blanko-Tech-MC-Optimizer

# Compilación limpia (recomendado para asegurar inclusión de todos los cambios)
gradle clean assembleDebug

# Copiar APK a la raíz del proyecto
cp app/build/outputs/apk/debug/app-debug.apk ./debug.apk

# Verificar que el APK se generó correctamente
ls -lh ./debug.apk
```

### Ubicaciones del APK
- **Después de compilar**: `app/build/outputs/apk/debug/app-debug.apk`
- **Entrega final**: `./debug.apk` (en la raíz del proyecto)

---

## 📦 Cómo Entregar el APK

### Formato de Entrega
1. **Ubicación**: El archivo debe estar disponible como `debug.apk` en la **raíz del proyecto**
2. **Verificación**: Confirmar que la fecha de modificación del archivo es reciente
3. **Integridad**: Verificar hash SHA-256 para confirmar que no fue alterado

### Comando de Verificación
```bash
sha256sum ./debug.apk
```

### Ejemplo de Resultado Esperado
```
e80d9387ca87fb2ea811d08ff5cca3f9f853a3b1c7347a2f14717c08d512df71  ./debug.apk
```

---

## ⚠️ Notas Importantes

### Advertencias durante la Compilación
- **`google-services.json` faltante**: Es normal si la integración de Firebase no está configurada localmente. La app compilará correctamente sin este archivo.
- **Librerías nativas sin strip**: Las librerías `libandroidx.graphics.path.so` y `libdatastore_shared_counter.so` se incluyen tal cual. Esto es esperado.

### Deprecaciones Conocidas
```kotlin
// En OptimizationEngine.kt (líneas 23, 29)
'fun unsafeCheckOpNoThrow(...)'  // Deprecated
'fun noteOpNoThrow(...)'         // Deprecated

// En OptimizerViewModel.kt (línea 156)
'static field FLAG_IS_GAME: Int' // Deprecated
```
Estas son advertencias de la API de Android y no afectan el funcionamiento de la app.

---

## 🔄 Actualización de APK

Cada vez que se realicen cambios en el código:

1. **Verificar cambios**:
   ```bash
   git status --short
   git log --oneline -5
   ```

2. **Compilar**:
   ```bash
   gradle clean assembleDebug
   ```

3. **Copiar a raíz**:
   ```bash
   cp app/build/outputs/apk/debug/app-debug.apk ./debug.apk
   ```

4. **Validar**:
   ```bash
   sha256sum ./debug.apk app/build/outputs/apk/debug/app-debug.apk
   ```

Los hashes SHA-256 de ambos archivos deben ser idénticos.

---

## 🌳 Rama de Trabajo

- **Rama actual**: `main`
- **Rama remota**: `origin/main`
- **Commits recientes**:
  - `feat: add Shizuku app freezer widget`
  - `icon debug.apk`
  - `refactor: update app launcher icons`
  - `feat: integrate Shizuku for background app control`

---

## 📞 Información de Contacto para Agentes

Si trabajas con este proyecto en otro entorno:

1. Asegúrate de tener **Gradle** disponible en la terminal
2. No necesitas crear archivos adicionales para compilar
3. La compilación automáticamente incluirá todos los recursos en `app/src/`
4. Siempre usa `gradle clean assembleDebug` para compilaciones críticas
5. Verifica que el APK esté en la raíz con el nombre exacto: `debug.apk`

---

**Última actualización**: 2026-07-03  
**Estado**: ✅ Compilación exitosa  
**APK disponible**: Sí (`./debug.apk`)
