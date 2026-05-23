import React, { memo } from 'react';
import { useAlerts } from '../../store';
import { TIER_COLOR } from '../../constants';

export const AlertsPanel = memo(() => {
  const alerts = useAlerts();

  if (!alerts.length) return (
    <div style={{
      textAlign: 'center', color: '#718096',
      padding: '20px', fontSize: '13px',
    }}>
      🔔 No active alerts
    </div>
  );

  return (
    <div>
      {alerts.slice(0, 10).map((a, i) => {
        const color = TIER_COLOR[a.severity] || '#eab308';
        return (
          <div key={i} style={{
            background:   '#242736',
            borderRadius: '8px',
            padding:      '10px 12px',
            marginBottom: '8px',
            borderLeft:   `3px solid ${color}`,
          }}>
            <div style={{
              display:        'flex',
              justifyContent: 'space-between',
              alignItems:     'center',
              marginBottom:   '4px',
            }}>
              <span style={{ fontSize: '13px', fontWeight: 500, color: '#e2e8f0' }}>
                {a.cityName}
              </span>
              <span style={{ fontSize: '11px', fontWeight: 700, color }}>
                {a.severity}
              </span>
            </div>
            <div style={{ fontSize: '11px', color: '#a0aec0', lineHeight: 1.5 }}>
              {a.message}
            </div>
            <div style={{ fontSize: '10px', color: '#4a5568', marginTop: '4px' }}>
              {new Date(a.timestamp).toLocaleTimeString()}
            </div>
          </div>
        );
      })}
    </div>
  );
});
