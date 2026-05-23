import React, { memo } from 'react';

interface Props { text?: string; size?: number; }

export const LoadingSpinner = memo(({ text = 'Loading...', size = 24 }: Props) => (
  <div style={{
    display: 'flex', flexDirection: 'column',
    alignItems: 'center', justifyContent: 'center',
    padding: '24px', gap: '10px',
  }}>
    <div style={{
      width: `${size}px`, height: `${size}px`,
      border: '3px solid #2d3748',
      borderTopColor: '#f97316',
      borderRadius: '50%',
      animation: 'hs-spin 0.8s linear infinite',
    }} />
    {text && <span style={{ fontSize: '12px', color: '#718096' }}>{text}</span>}
    <style>{`@keyframes hs-spin { to { transform: rotate(360deg); } }`}</style>
  </div>
));
