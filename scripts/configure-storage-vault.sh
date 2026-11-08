#!/usr/bin/env bash
# Configuración única del proyecto hosted para 20261109330000_storage_por_api (ítem 18): guarda en Vault la URL
# del proyecto y la service role para que los triggers llamen por pg_net a la Storage API (borrar fotos con su
# binario) y a la Edge Function verify-image (magic bytes). Los valores salen del entorno, nunca del repo.
# Uso: source /c/temp/huellas-audit/.env.remote && scripts/configure-storage-vault.sh
# Requiere además desplegada la función: supabase functions deploy verify-image --use-api --project-ref $SUPABASE_PROJECT_REF
set -euo pipefail
cd "$(dirname "$0")/.."
: "${SUPABASE_URL:?define SUPABASE_URL}"
: "${SUPABASE_SERVICE_ROLE_KEY:?define SUPABASE_SERVICE_ROLE_KEY}"
q() { python -c 'import sys;print(sys.argv[1].replace("\x27","\x27\x27"))' "$1"; }
scripts/supabase-remote-sql.sh "DO \$\$
DECLARE s RECORD;
BEGIN
    FOR s IN SELECT * FROM (VALUES ('huellas_project_url', '$(q "$SUPABASE_URL")'),
                                   ('huellas_service_role_key', '$(q "$SUPABASE_SERVICE_ROLE_KEY")')) v(n, val) LOOP
        IF EXISTS (SELECT 1 FROM vault.secrets WHERE name = s.n) THEN
            PERFORM vault.update_secret((SELECT id FROM vault.secrets WHERE name = s.n), s.val);
        ELSE
            PERFORM vault.create_secret(s.val, s.n);
        END IF;
    END LOOP;
END \$\$;
SELECT name FROM vault.secrets WHERE name LIKE 'huellas_%' ORDER BY 1"
