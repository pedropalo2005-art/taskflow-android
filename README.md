# 📱 TaskFlow Pro (App Móvil para Android con Supabase & Dictado por Voz)

Aplicación móvil nativa diseñada para gestión rápida de tareas mediante **Tablero Kanban**, **Dictado por Voz**, **Recordatorios con Alarma** y **Sincronización en la Nube con Supabase**.

---

## ⚡ Características Principales

1. **🎙️ Dictado por Voz (Speech-to-Text):**
   * Presiona el botón de micrófono y dicta directamente en español (ej: *"Visitar tienda Blv. Morazán mañana a las 3 de la tarde"* o *"Hacer reporte de Pedidos Ya"*).
   * La app transcribe automáticamente el audio y autodetecta la categoría adecuada.

2. **📋 Tablero Kanban Ágil:**
   * **Por Hacer:** Tareas pendientes de iniciar.
   * **En Curso:** Tareas que se están realizando activamente.
   * **Hecho:** Tareas terminadas (con felicitación visual y cancelación de alarma).
   * Contadores en tiempo real en cada pestaña.

3. **⏰ Recordatorios y Alarmas Exactas:**
   * Selector integrado de Fecha y Hora.
   * Notificación programada en el teléfono con sonido y vibración a la hora indicada.

4. **☁️ Sincronización con Supabase + Modo Offline:**
   * Guarda y actualiza las tareas en la nube en tiempo real.
   * Cuenta con almacenamiento local inmediato: si la jefa entra a un sótano o a una tienda sin señal, la app sigue funcionando al 100% y se sincroniza al recuperar internet.

5. **🚀 Compilación Automática en GitHub Actions:**
   * No necesitas instalar Android Studio en tu computadora. GitHub compila el archivo `.apk` automáticamente en la nube.

---

## 🛠️ Paso 1: Configurar Supabase (1 Minuto)

1. Entra a tu proyecto en [Supabase](https://supabase.com).
2. Ve a la sección **SQL Editor** (en el menú lateral izquierdo).
3. Abre el archivo `supabase_schema.sql` que está en la raíz de este proyecto, copia todo su contenido y pégalo en el editor de Supabase.
4. Presiona el botón verde **Run**.
5. ¡Listo! La tabla `tasks` y sus permisos quedarán creados con 3 tareas de ejemplo.

---

## 🚀 Paso 2: Subir a GitHub y Generar el APK

Para compilar el archivo `.apk` automáticamente en GitHub:

1. Crea un repositorio nuevo en tu cuenta de GitHub (por ejemplo, con el nombre `TaskFlowApp`).
2. Desde la terminal en esta carpeta (`C:\Users\DELL\Desktop\TaskFlowApp`), ejecuta:

```bash
git init
git add .
git commit -m "feat: version inicial de TaskFlow Pro con voz y Supabase"
git branch -M main
git remote add origin https://github.com/TU-USUARIO/TaskFlowApp.git
git push -u origin main
```

3. Ve a tu repositorio en GitHub y haz clic en la pestaña **Actions**.
4. Verás el flujo de trabajo **"Compilar TaskFlow APK"** ejecutándose (tarda aproximadamente 2-3 minutos).
5. Al finalizar:
   * Puedes descargar el APK desde los **Artifacts** de la ejecución, o
   * Ir a la sección **Releases** en la derecha de tu repositorio en GitHub, donde encontrarás el archivo `app-debug.apk` listo para descargar.

---

## 📲 Paso 3: Instalación en el Teléfono de la Jefa

1. Envía el archivo `.apk` al WhatsApp o Telegram de la jefa, o pídele que lo descargue desde el enlace de Releases de GitHub.
2. Al abrir el archivo en su teléfono Android:
   * Si el sistema pregunta, selecciona **"Permitir instalar desde esta fuente"**.
   * Si aparece la advertencia de Google Play Protect (común en aplicaciones en desarrollo no firmadas comercialmente), presiona **"Más detalles"** -> **"Instalar de todas formas"**.
3. Al abrir la app por primera vez, concédele permiso de **Notificaciones** para que puedan sonar los recordatorios de sus tareas.
