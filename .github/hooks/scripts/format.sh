#!/bin/bash
# ─────────────────────────────────────────────────────────────
# ✨ FORMAT — Formatea el código automáticamente después de
#    cada cambio
# 📌 Se ejecuta en: postToolUse (después de que el agente
#    escribe o modifica un archivo)
# 🛠️ Herramienta: Spotless (google-java-format)
# ─────────────────────────────────────────────────────────────

INPUT=$(cat)

TOOL_NAME=$(echo "$INPUT" | jq -r '.tool_name // empty')

# Solo formatear después de herramientas que escriben archivos
if ! [[ "$TOOL_NAME" =~ ^(replace_string_in_file|multi_replace_string_in_file|create_file)$ ]]; then
  echo "[format.sh] ⏭️  Skipped (read-only tool): $TOOL_NAME"
  exit 0
fi

# Extraer rutas de archivo según el tipo de herramienta
if [[ "$TOOL_NAME" == "multi_replace_string_in_file" ]]; then
  FILES=$(echo "$INPUT" | jq -r '.tool_input.replacements[].filePath // empty' | sort -u)
else
  FILES=$(echo "$INPUT" | jq -r '.tool_input.filePath // empty')
fi

if [ -z "$FILES" ]; then
  echo "[format.sh] ⚠️  No filePath found in input"
  exit 0
fi

# Formatear cada archivo
while IFS= read -r FILE; do
  [ -z "$FILE" ] && continue

  # Solo formatear archivos Java
  EXT="${FILE##*.}"
  if [[ "$EXT" != "java" ]]; then
    echo "[format.sh] ⏭️  Not a Java file: $FILE"
    continue
  fi

  # El archivo debe existir
  if [ ! -f "$FILE" ]; then
    echo "[format.sh] ⚠️  File not found: $FILE"
    continue
  fi

  # Encontrar el directorio del proyecto Maven
  PROJECT_DIR=""
  SEARCH_DIR=$(dirname "$FILE")
  while [ "$SEARCH_DIR" != "/" ]; do
    if [ -f "$SEARCH_DIR/pom.xml" ]; then
      PROJECT_DIR="$SEARCH_DIR"
      break
    fi
    SEARCH_DIR=$(dirname "$SEARCH_DIR")
  done

  if [ -z "$PROJECT_DIR" ]; then
    echo "[format.sh] ⏭️  No Maven project found for: $FILE"
    continue
  fi

  # Verificar que mvn está disponible
  if ! command -v mvn &>/dev/null; then
    echo "[format.sh] ⚠️  mvn not available — skipping format"
    exit 0
  fi

  # Ejecutar Spotless para formatear
  echo "[format.sh] 📝 Formatting: $FILE"
  
  cd "$PROJECT_DIR"
  
  # Obtener tamaño antes
  BEFORE=$(stat -c%s "$FILE" 2>/dev/null || stat -f%z "$FILE" 2>/dev/null)
  
  # Ejecutar spotless:apply
  FORMAT_OUTPUT=$(mvn spotless:apply -q 2>&1)
  EXIT_CODE=$?
  
  # Obtener tamaño después
  AFTER=$(stat -c%s "$FILE" 2>/dev/null || stat -f%z "$FILE" 2>/dev/null)

  if [ $EXIT_CODE -ne 0 ]; then
    echo "[format.sh] ⚠️  Spotless not configured or failed: $FORMAT_OUTPUT"
    # No es un error fatal - el plugin puede no tener spotless configurado
  elif [ "$BEFORE" = "$AFTER" ]; then
    echo "[format.sh] ✅ No changes needed"
  else
    echo "[format.sh] ✨ File formatted successfully"
  fi
done <<< "$FILES"
