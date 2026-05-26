# 📚 StreamPay Android Client - Índice de Documentación

Bienvenido al cliente Android de StreamPay. Esta es tu guía para encontrar toda la información que necesitas.

---

## 🚀 Empezar Aquí

**¿Primera vez compilando un APK?**
1. Lee: [`CHECKLIST_COMPILACION.md`](./CHECKLIST_COMPILACION.md) ✅
2. Sigue: [`GUIA_RAPIDA_APK.md`](./GUIA_RAPIDA_APK.md) ⚡

**¿Ya tienes experiencia con Expo/EAS?**
- Directo a: [`README_CLIENT.md`](./README_CLIENT.md) 📖

---

## 📁 Archivos y su Propósito

### 📖 Documentación Principal

| Archivo | Propósito | Para Quién |
|---------|-----------|------------|
| [`CHECKLIST_COMPILACION.md`](./CHECKLIST_COMPILACION.md) | Guía paso a paso con checklist completo | Principiantes |
| [`GUIA_RAPIDA_APK.md`](./GUIA_RAPIDA_APK.md) | Comandos esenciales y pasos rápidos | Usuarios con experiencia |
| [`README_CLIENT.md`](./README_CLIENT.md) | Documentación técnica completa | Desarrolladores |
| [`RESUMEN_PROYECTO.md`](./RESUMEN_PROYECTO.md) | Overview del proyecto y características | Todos |

### 🔧 Integración y Deployment

| Archivo | Propósito | Para Quién |
|---------|-----------|------------|
| [`INTEGRACION_GITHUB.md`](./INTEGRACION_GITHUB.md) | Cómo subir a GitHub y configurar CI/CD | DevOps/Mantenedores |
| [`.github/workflows/build-apk.yml`](./.github/workflows/build-apk.yml) | Workflow de GitHub Actions | Automatización |

### 🛠️ Herramientas

| Archivo | Propósito | Para Quién |
|---------|-----------|------------|
| [`build-apk.sh`](./build-apk.sh) | Script interactivo de compilación | Usuarios de terminal |

### 💻 Código Fuente

| Directorio | Contenido |
|------------|-----------|
| [`frontend/`](./frontend/) | Aplicación Expo completa |
| [`frontend/app/`](./frontend/app/) | Screens de la aplicación |
| [`frontend/app.json`](./frontend/app.json) | Configuración Expo/Android |
| [`frontend/eas.json`](./frontend/eas.json) | Perfiles de compilación |

---

## 🎯 Casos de Uso Comunes

### "Quiero compilar mi primer APK"

```
1. CHECKLIST_COMPILACION.md (seguir todos los pasos)
2. Usar build-apk.sh
3. Instalar y probar en tu dispositivo
```

### "Necesito automatizar la compilación"

```
1. INTEGRACION_GITHUB.md (sección GitHub Actions)
2. Configurar EXPO_TOKEN en GitHub
3. Push al repositorio
```

### "Quiero entender el código"

```
1. RESUMEN_PROYECTO.md (estructura del proyecto)
2. README_CLIENT.md (documentación técnica)
3. Explorar frontend/app/
```

### "Quiero personalizar la app"

```
1. README_CLIENT.md (sección "Personalización")
2. Editar frontend/app.json (colores, nombre)
3. Modificar frontend/app/*.tsx (código)
```

### "Tengo un error al compilar"

```
1. README_CLIENT.md (sección "Solución de Problemas")
2. GUIA_RAPIDA_APK.md (problemas comunes)
3. CHECKLIST_COMPILACION.md (troubleshooting rápido)
```

---

## 📊 Flujo de Trabajo Recomendado

### Para Principiantes

```mermaid
CHECKLIST_COMPILACION.md
         ↓
   Crear cuenta Expo
         ↓
   Instalar EAS CLI
         ↓
      eas init
         ↓
     eas build
         ↓
   Descargar APK
         ↓
  Instalar y probar
```

### Para Desarrolladores

```mermaid
README_CLIENT.md
         ↓
Entender arquitectura
         ↓
 Personalizar código
         ↓
INTEGRACION_GITHUB.md
         ↓
Configurar CI/CD
         ↓
 Automatizar builds
```

---

## 🗂️ Organización por Tema

### 🎓 Aprendizaje

- **Nivel 1 (Básico)**: 
  - CHECKLIST_COMPILACION.md
  - GUIA_RAPIDA_APK.md

- **Nivel 2 (Intermedio)**: 
  - README_CLIENT.md
  - RESUMEN_PROYECTO.md

- **Nivel 3 (Avanzado)**: 
  - INTEGRACION_GITHUB.md
  - Código fuente en frontend/

### 🔨 Compilación

- **Local**: 
  - GUIA_RAPIDA_APK.md
  - build-apk.sh

- **Automática**: 
  - INTEGRACION_GITHUB.md
  - .github/workflows/build-apk.yml

### 🎨 Personalización

- **UI/UX**: 
  - README_CLIENT.md → Personalización
  - frontend/app/*.tsx

- **Configuración**: 
  - frontend/app.json
  - frontend/eas.json

### 🐛 Debugging

- **Errores de compilación**: 
  - GUIA_RAPIDA_APK.md → Problemas Comunes
  - README_CLIENT.md → Solución de Problemas

- **Errores de ejecución**: 
  - CHECKLIST_COMPILACION.md → Troubleshooting
  - README_CLIENT.md → Requisitos del Servidor

---

## 📞 Recursos Externos

### Documentación Oficial

- [Expo Docs](https://docs.expo.dev)
- [EAS Build](https://docs.expo.dev/build/introduction)
- [React Native](https://reactnative.dev)
- [React Native WebView](https://github.com/react-native-webview/react-native-webview)

### Comunidad

- [Expo Forums](https://forums.expo.dev)
- [Discord Expo](https://chat.expo.dev)
- [Stack Overflow](https://stackoverflow.com/questions/tagged/expo)

### Herramientas

- [Expo Snack](https://snack.expo.dev) - Playground online
- [Expo Orbit](https://expo.dev/orbit) - Desktop app para gestionar builds
- [Expo Go](https://expo.dev/client) - App para testing

---

## 🔄 Actualizaciones

### Control de Versiones

```
v1.0.0 - Febrero 2025
- ✅ WebView inicial
- ✅ Configuración de IP
- ✅ Soporte HTTP
- ✅ Tema dark
- ✅ Documentación completa
```

### Próximas Versiones

```
v1.1.0 (Planificado)
- [ ] Push notifications
- [ ] Deep linking
- [ ] Modo offline
- [ ] Biometric auth
```

---

## 📋 Checklist de Documentos

Antes de distribuir, asegúrate de que:

- [ ] Todos los archivos .md están presentes
- [ ] Los links entre documentos funcionan
- [ ] El script build-apk.sh tiene permisos de ejecución
- [ ] El workflow de GitHub Actions está configurado
- [ ] Los ejemplos de código funcionan
- [ ] Las rutas de archivos son correctas
- [ ] No hay información sensible (tokens, passwords)

---

## 🎯 Quick Links

**Compilación Rápida:**
```bash
./build-apk.sh
```

**Compilación Manual:**
```bash
cd frontend
eas build --platform android --profile preview
```

**Ver Documentación Completa:**
```bash
cat README_CLIENT.md
```

**Ver Guía Rápida:**
```bash
cat GUIA_RAPIDA_APK.md
```

---

## 💡 Tips

1. **Guarda tus credenciales**: Anota tu usuario, projectId y tokens en un lugar seguro
2. **Haz backups**: Guarda copias de los APKs compilados
3. **Versionado**: Incrementa la versión en app.json con cada build
4. **Testing**: Prueba cada build antes de distribuir
5. **Logs**: Revisa los logs de EAS Build si algo falla

---

## 🌟 Destacados

**✨ Mejores Prácticas:**
- Usa el perfil `preview` para testing
- Usa el perfil `production` para distribución final
- Mantén actualizada la documentación con cada cambio
- Usa GitHub Releases para distribuir versiones estables

**🚀 Optimizaciones:**
- El WebView usa aceleración hardware
- Cache habilitado para mejor rendimiento
- Cleartext traffic solo para desarrollo (usar HTTPS en producción)
- AsyncStorage para persistencia local eficiente

---

## 📮 Contacto y Soporte

**Reportar Issues:**
- GitHub Issues del proyecto StreamPay
- Foros de Expo para problemas de compilación

**Contribuir:**
- Fork el repositorio
- Crea una rama feature
- Submit Pull Request

---

## ✅ Estado del Proyecto

| Componente | Estado | Notas |
|------------|--------|-------|
| Aplicación Base | ✅ Completo | WebView funcional |
| Configuración IP | ✅ Completo | AsyncStorage implementado |
| Tema Dark | ✅ Completo | Colores StreamPay |
| Documentación | ✅ Completo | 6 guías disponibles |
| CI/CD | ✅ Completo | GitHub Actions configurado |
| Testing | ⚠️ Manual | Automatizar en futuro |
| Play Store | 📋 Pendiente | Preparado para publicación |

---

## 🎉 ¡Comienza Ahora!

**Elige tu ruta:**

👶 **Principiante:** → [`CHECKLIST_COMPILACION.md`](./CHECKLIST_COMPILACION.md)

⚡ **Rápido:** → [`GUIA_RAPIDA_APK.md`](./GUIA_RAPIDA_APK.md)

🔧 **Técnico:** → [`README_CLIENT.md`](./README_CLIENT.md)

📦 **GitHub:** → [`INTEGRACION_GITHUB.md`](./INTEGRACION_GITHUB.md)

---

**🎬 ¡Bienvenido a StreamPay Android!**

_Tu plataforma de streaming, ahora en tu bolsillo._
