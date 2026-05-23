import React, { memo } from 'react';
import { useStore, useSelectedCity, useSidebarOpen } from '../store';
import { useIsMobile } from '../hooks/useMediaQuery';

interface Props {
  lastUpdated: Date;
  onRefresh:   () => void;
}

export const Header = memo(({ lastUpdated, onRefresh }: Props) => {
  const selectedCity = useSelectedCity();
  const sidebarOpen  = useSidebarOpen();
  const setSidebar   = useStore(s => s.setSidebar);
  const isMobile     = useIsMobile();

  return (
    <header style={{
      display:        'flex',
      alignItems:     'center',
      justifyContent: 'space-between',
      padding:        isMobile ? '8px 12px' : '10px 20px',
      background:     '#1a1d2e',
      borderBottom:   '1px solid #2d3748',
      flexShrink:     0,
      zIndex:         100,
      minHeight:      isMobile ? '48px' : '52px',
    }}>
      {/* Left — Brand */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
        <span style={{ fontSize: isMobile ? '16px' : '19px', fontWeight: 700, color: '#f97316' }}>
          🌡️ HeatShield
        </span>
        {!isMobile && (
          <span style={{ fontSize: '11px', color: '#718096' }}>
            Global Urban Heat Intelligence
          </span>
        )}
      </div>

      {/* Right — Controls */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
        <span style={{ fontSize: '11px', color: '#4a5568' }}>
          {lastUpdated.toLocaleTimeString()}
        </span>

        {/* Mobile — toggle bottom sheet */}
        {isMobile && selectedCity && (
          <button
            onClick={() => setSidebar(!sidebarOpen)}
            style={btnStyle}
          >
            {sidebarOpen ? '✕' : `📊 ${selectedCity.name}`}
          </button>
        )}

        <button onClick={onRefresh} style={btnStyle}>
          ↻{!isMobile && ' Refresh'}
        </button>
      </div>
    </header>
  );
});

const btnStyle: React.CSSProperties = {
  padding:      '5px 12px',
  borderRadius: '6px',
  border:       '1px solid #4a5568',
  background:   '#2d3748',
  color:        '#e2e8f0',
  cursor:       'pointer',
  fontSize:     '12px',
  whiteSpace:   'nowrap',
};
