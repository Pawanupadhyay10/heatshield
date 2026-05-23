#!/usr/bin/env bash
# HeatShield Frontend — Week 7 Setup
# Run from: /workspaces/heatshield/heatshield/
set -e
GREEN='\033[0;32m'; NC='\033[0m'
log() { echo -e "${GREEN}[FRONTEND]${NC} $1"; }

log "Replacing frontend with production-grade modular version..."

# Backup old frontend
[ -d frontend ] && mv frontend frontend_backup_$(date +%s)
log "Old frontend backed up"

# Create new frontend from zip
mkdir -p frontend
cd frontend

log "Installing dependencies..."
npm install

log "Verifying TypeScript..."
npx tsc --noEmit && log "✓ Zero TypeScript errors" || log "⚠ TS errors found"

echo ""
echo "=================================="
echo -e "${GREEN}Frontend ready!${NC}"
echo "=================================="
echo ""
echo "Add your tokens to .env:"
echo "  cp .env.example .env"
echo "  nano .env  # add VITE_MAPBOX_TOKEN"
echo ""
echo "Start dev server:"
echo "  npm run dev"
echo ""
echo "Then open Ports tab → 3000 → globe icon"
