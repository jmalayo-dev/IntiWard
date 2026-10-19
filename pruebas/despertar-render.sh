#!/bin/bash
# Despierta los 6 servicios de Render (plan gratuito: se duermen tras 15 min sin tráfico) y espera a que respondan.
# Correrlo unos 2 minutos antes de una demo:  bash pruebas/despertar-render.sh
SERVICIOS="gateway auth catalogo inventario pedidos comprobantes"
for s in $SERVICIOS; do curl -s -o /dev/null --max-time 5 "https://intiguard-$s.onrender.com/actuator/health" & done
wait
for s in $SERVICIOS; do
  printf "%-14s" "$s"
  for i in $(seq 1 40); do
    if curl -s --max-time 10 "https://intiguard-$s.onrender.com/actuator/health" | grep -q '"UP"'; then echo "listo"; continue 2; fi
    sleep 5
  done
  echo "no respondió en 3 min: revisa sus logs en Render"
done
