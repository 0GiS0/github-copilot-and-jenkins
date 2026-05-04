#!/bin/bash
# ─────────────────────────────────────────────────────────────
# 🔍 LINT — Verifica la calidad del código Java antes de escribir
# 📌 Se ejecuta en: preToolUse (antes de que el agente
#    escriba o modifique un archivo)
# 🛠️ Herramienta: Maven compile / Checkstyle
# ─────────────────────────────────────────────────────────────

# ── Helpers para responder al hook ──────────────────────────

allow() {
  jq -n --arg reason "$1" '{
    hookSpecificOutput: {
      hookEventName: "PreToolUse",
      permissionDecision: "allow",
      permissionDecisionReason: $reason
    }
  }'
  exit 0
}

deny() {
  jq -n --arg reason "$1" --arg context "$2" '{
    hookSpecificOutput: {
      hookEventName: "PreToolUse",
      permissionDecision: "deny",
      permissionDecisionReason: $reason,
      additionalContext: $context
    }
  }'
  exit 1
}

# ── Lógica principal ────────────────────────────────────────

INPUT=$(cat)

TOOL_NAME=$(echo "$INPUT" | jq -r '.tool_name // empty')
FILE=$(echo "$INPUT" | jq -r '.tool_input.filePath // empty')

# Solo lint en herramientas que escriben archivos
[[ "$TOOL_NAME" =~ ^(replace_string_in_file|multi_replace_string_in_file|create_file)$ ]] \
  || allow "⏭️ Read-only tool, no lint needed"

# Necesitamos una ruta de archivo
[ -z "$FILE" ] && allow "⏭️ No filePath in input"

# Solo lint en archivos Java
EXT="${FILE##*.}"
[[ "$EXT" == "java" ]] || allow "⏭️ Not a Java file"

# El archivo debe existir (para replace, no para create)
if [[ "$TOOL_NAME" != "create_file" ]] && [ ! -f "$FILE" ]; then
  deny "⚠️ File not found: $FILE" ""
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

[ -z "$PROJECT_DIR" ] && allow "⏭️ No Maven project found"

# Verificar que mvn está disponible
command -v mvn &> /dev/null || allow "⚠️ mvn not available — skipping lint"

# Ejecutar compilación para verificar sintaxis
cd "$PROJECT_DIR"
if COMPILE_OUTPUT=$(mvn compile -q 2>&1); then
  allow "✅ Compilation passed"
else
  deny "❌ Compilation failed" "$COMPILE_OUTPUT"
fi
