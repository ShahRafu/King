#!/usr/bin/env bash
set -euo pipefail

# scripts/quantize-model.sh
# Usage: ./quantize-model.sh /path/to/orig_model.bin /path/to/output_qmodel.bin q4_0
# This script assumes you built the quantize tool from llama.cpp on desktop (not Android).

SRC_MODEL=${1:-}
OUT_MODEL=${2:-}
QUANT=${3:-q4_0}

if [ -z "$SRC_MODEL" ] || [ -z "$OUT_MODEL" ]; then
  echo "Usage: $0 /path/to/orig_model.bin /path/to/out_model.bin [q4_0|q4_k_m|..]"
  exit 2
fi

if [ ! -f "$SRC_MODEL" ]; then
  echo "Source model not found: $SRC_MODEL"
  exit 2
fi

# Try to find quantize binary in scripts/llama.cpp/build/quantize
if [ -x "scripts/llama.cpp/build/quantize" ]; then
  QUANT_BIN="scripts/llama.cpp/build/quantize"
elif [ -x "scripts/llama.cpp/quantize" ]; then
  QUANT_BIN="scripts/llama.cpp/quantize"
else
  echo "Could not find quantize binary. Build llama.cpp on desktop first (see scripts/README_BUILD_LLM.md)"
  exit 2
fi

echo "Quantizing with $QUANT_BIN -> mode $QUANT"
$QUANT_BIN "$SRC_MODEL" "$OUT_MODEL" "$QUANT"

echo "Quantized model written to $OUT_MODEL"
