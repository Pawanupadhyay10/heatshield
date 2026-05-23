import React, { memo } from 'react';
import type { RiskTier } from '../types';
import { TIER_COLOR, TIER_BG, TIER_LABEL } from '../constants';

interface Props {
  tier:  RiskTier;
  size?: 'sm' | 'md' | 'lg';
}

export const TierBadge = memo(({ tier, size = 'sm' }: Props) => {
  const fontSize  = size === 'lg' ? '13px' : size === 'md' ? '11px' : '10px';
  const padding   = size === 'lg' ? '4px 12px' : size === 'md' ? '3px 10px' : '2px 8px';
  return (
    <span style={{
      fontSize, fontWeight: 600, padding,
      borderRadius: '20px',
      color:        TIER_COLOR[tier],
      background:   TIER_BG[tier],
      letterSpacing: '0.03em',
      whiteSpace:   'nowrap',
    }}>
      {TIER_LABEL[tier]}
    </span>
  );
});
