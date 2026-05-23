import React, { memo } from 'react';
import type { City } from '../../types';
import { useStore, useHeatData } from '../../store';
import { TierBadge } from '../../components/TierBadge';
import { TIER_COLOR } from '../../constants';

interface Props {
  city:     City;
  selected: boolean;
}

export const CityCard = memo(({ city, selected }: Props) => {
  const heatData   = useHeatData();
  const selectCity = useStore(s => s.selectCity);
  const state      = heatData[city.id];
  const tier       = state?.riskTier || 'SAFE';
  const color      = TIER_COLOR[tier];

  return (
    <div
      onClick={() => selectCity(city)}
      style={{
        padding:      '10px 12px',
        borderRadius: '8px',
        background:   selected ? '#2d3748' : '#242736',
        borderLeft:   `4px solid ${color}`,
        cursor:       'pointer',
        outline:      selected ? '1px solid #4a5568' : 'none',
        transition:   'background 0.15s',
        marginBottom: '4px',
      }}
    >
      {/* City name + tier */}
      <div style={{
        display:        'flex',
        justifyContent: 'space-between',
        alignItems:     'center',
        marginBottom:   '4px',
      }}>
        <span style={{ fontSize: '13px', fontWeight: 500, color: '#e2e8f0' }}>
          {city.name}
        </span>
        <TierBadge tier={tier} />
      </div>

      {/* Heat index + stats */}
      <div style={{ display: 'flex', alignItems: 'baseline', gap: '8px' }}>
        <span style={{ fontSize: '20px', fontWeight: 700, color }}>
          {state?.heatIndex?.toFixed(1) ?? '—'}°C
        </span>
        {state && (
          <span style={{ fontSize: '11px', color: '#718096' }}>
            👥 {((state.estimatedAtRisk || 0) / 1000).toFixed(0)}K
            {' '}
            {state.trend === 'RISING' ? '↑' : state.trend === 'FALLING' ? '↓' : '→'}
          </span>
        )}
      </div>
    </div>
  );
});
