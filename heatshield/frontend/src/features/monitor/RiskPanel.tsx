import React, { memo } from 'react';
import type { HeatState } from '../../types';
import { TIER_COLOR } from '../../constants';

interface Props { state: HeatState; }

export const RiskPanel = memo(({ state }: Props) => {
  const color = TIER_COLOR[state.riskTier] || '#718096';
  const vuln  = state.vulnerabilityScore || 0;

  const stats = [
    { label: 'Heat Index', value: `${state.heatIndex?.toFixed(1)}°C`, color },
    { label: 'Risk Tier',  value: state.riskTier, color },
    { label: 'At Risk',    value: `${((state.estimatedAtRisk || 0) / 1000).toFixed(0)}K`, color: '#e2e8f0' },
    {
      label: 'Trend',
      value: state.trend === 'RISING' ? '↑ Rising'
           : state.trend === 'FALLING' ? '↓ Falling'
           : '→ Stable',
      color: state.trend === 'RISING' ? '#ef4444'
           : state.trend === 'FALLING' ? '#22c55e'
           : '#718096',
    },
  ];

  return (
    <div style={{ padding: '4px 0' }}>
      {/* Stat grid */}
      <div style={{
        display:             'grid',
        gridTemplateColumns: '1fr 1fr',
        gap:                 '8px',
        marginBottom:        '14px',
      }}>
        {stats.map(s => (
          <div key={s.label} style={{
            background:   '#242736',
            borderRadius: '8px',
            padding:      '10px 12px',
          }}>
            <div style={{ fontSize: '10px', color: '#718096', marginBottom: '4px', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
              {s.label}
            </div>
            <div style={{ fontSize: '18px', fontWeight: 700, color: s.color }}>
              {s.value}
            </div>
          </div>
        ))}
      </div>

      {/* Vulnerability bar */}
      <div style={{ fontSize: '11px', color: '#718096', marginBottom: '6px' }}>
        Vulnerability Score — {vuln.toFixed(0)}/100
      </div>
      <div style={{ height: '6px', background: '#2d3748', borderRadius: '3px', overflow: 'hidden' }}>
        <div style={{
          height:     '100%',
          width:      `${vuln}%`,
          background: color,
          borderRadius: '3px',
          transition: 'width 0.5s ease',
        }} />
      </div>

      {/* Updated at */}
      {state.updatedAt && (
        <div style={{ fontSize: '10px', color: '#4a5568', marginTop: '10px' }}>
          Last updated: {new Date(state.updatedAt).toLocaleTimeString()}
        </div>
      )}
    </div>
  );
});
