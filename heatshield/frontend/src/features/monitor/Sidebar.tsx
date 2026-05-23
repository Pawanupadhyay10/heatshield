import React, { memo, Suspense } from 'react';
import { useStore, useHeatData, useSelectedCity, useActiveTab, useSidebarOpen } from '../../store';
import { CITIES, TIER_COLOR } from '../../constants';
import { CityCard } from './CityCard';
import { RiskPanel } from './RiskPanel';
import { ShelterPanel } from '../shelters/ShelterPanel';
import { AlertsPanel } from '../alerts/AlertsPanel';
import { TierBadge } from '../../components/TierBadge';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { ErrorBoundary } from '../../components/ErrorBoundary';
import { useIsMobile } from '../../hooks/useMediaQuery';
import { agentApi } from '../../services/api';
import toast from 'react-hot-toast';

const TABS = [
  { key: 'risk'     as const, label: '🌡️ Risk'     },
  { key: 'shelters' as const, label: '🏥 Shelters'  },
  { key: 'alerts'   as const, label: '🔔 Alerts'    },
];

export const Sidebar = memo(() => {
  const heatData     = useHeatData();
  const selectedCity = useSelectedCity();
  const activeTab    = useActiveTab();
  const sidebarOpen  = useSidebarOpen();
  const selectCity   = useStore(s => s.selectCity);
  const setTab       = useStore(s => s.setTab);
  const setSidebar   = useStore(s => s.setSidebar);
  const isMobile     = useIsMobile();

  const handleAgent = async () => {
    if (!selectedCity) return;
    try {
      await agentApi.trigger(selectedCity.id, selectedCity.name);
      toast.success(`🤖 Agent triggered for ${selectedCity.name}`);
    } catch {
      toast.error('Agent trigger failed');
    }
  };

  // ── Mobile: bottom sheet ───────────────────────────────────
  const mobileStyle: React.CSSProperties = {
    position:             'fixed',
    bottom:               0, left: 0, right: 0,
    height:               sidebarOpen ? '72vh' : '0',
    background:           '#1a1d2e',
    borderTopLeftRadius:  '16px',
    borderTopRightRadius: '16px',
    borderTop:            sidebarOpen ? '1px solid #2d3748' : 'none',
    overflow:             'hidden',
    transition:           'height 0.3s cubic-bezier(0.4,0,0.2,1)',
    zIndex:               200,
    display:              'flex',
    flexDirection:        'column',
  };

  // ── Desktop: right panel ───────────────────────────────────
  const desktopStyle: React.CSSProperties = {
    width:         '340px',
    flexShrink:    0,
    background:    '#1a1d2e',
    borderLeft:    '1px solid #2d3748',
    display:       'flex',
    flexDirection: 'column',
    overflowY:     'auto',
  };

  const state = selectedCity ? heatData[selectedCity.id] : null;

  return (
    <div style={isMobile ? mobileStyle : desktopStyle}>

      {/* Mobile drag handle */}
      {isMobile && (
        <div
          style={{ padding: '10px', display: 'flex', justifyContent: 'center', cursor: 'pointer', flexShrink: 0 }}
          onClick={() => setSidebar(!sidebarOpen)}
        >
          <div style={{ width: '36px', height: '4px', borderRadius: '2px', background: '#4a5568' }} />
        </div>
      )}

      <div style={{ flex: 1, overflowY: 'auto', padding: '0 8px 16px' }}>

        {/* ── City list (no city selected) ─────────────────── */}
        {!selectedCity && (
          <>
            <div style={{
              fontSize: '11px', fontWeight: 600, color: '#718096',
              textTransform: 'uppercase', letterSpacing: '0.05em',
              padding: '14px 8px 8px',
            }}>
              🌍 Live Heat Monitor
            </div>
            {CITIES.map(city => (
              <CityCard
                key={city.id}
                city={city}
                selected={false}
              />
            ))}
          </>
        )}

        {/* ── City detail panel ────────────────────────────── */}
        {selectedCity && (
          <>
            {/* Detail header */}
            <div style={{
              display:        'flex',
              alignItems:     'center',
              justifyContent: 'space-between',
              padding:        '12px 4px',
              borderBottom:   '1px solid #2d3748',
              marginBottom:   '10px',
              gap:            '8px',
            }}>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{
                  fontSize: '15px', fontWeight: 600,
                  color: '#e2e8f0', marginBottom: '2px',
                  display: 'flex', alignItems: 'center', gap: '8px',
                }}>
                  {selectedCity.name}
                  {state && <TierBadge tier={state.riskTier} size="sm" />}
                </div>
                <button
                  onClick={() => selectCity(null)}
                  style={{
                    fontSize: '11px', color: '#718096',
                    background: 'none', border: 'none',
                    cursor: 'pointer', padding: 0,
                  }}
                >
                  ← All cities
                </button>
              </div>

              {/* Agent trigger button */}
              <button onClick={handleAgent} style={{
                padding:      '6px 12px',
                borderRadius: '6px',
                border:       'none',
                background:   '#7c3aed',
                color:        '#fff',
                cursor:       'pointer',
                fontSize:     '11px',
                fontWeight:   600,
                flexShrink:   0,
                whiteSpace:   'nowrap',
              }}>
                🤖 Agent
              </button>
            </div>

            {/* Tabs */}
            <div style={{
              display:      'flex',
              borderBottom: '1px solid #2d3748',
              marginBottom: '12px',
            }}>
              {TABS.map(t => (
                <button
                  key={t.key}
                  onClick={() => setTab(t.key)}
                  style={{
                    flex:         1,
                    padding:      '8px 4px',
                    border:       'none',
                    background:   'none',
                    color:        activeTab === t.key ? '#f97316' : '#718096',
                    borderBottom: activeTab === t.key
                      ? '2px solid #f97316'
                      : '2px solid transparent',
                    cursor:     'pointer',
                    fontSize:   '11px',
                    fontWeight: 500,
                    transition: 'color 0.15s',
                  }}
                >
                  {t.label}
                </button>
              ))}
            </div>

            {/* Tab content */}
            <ErrorBoundary name="RiskPanel">
              {activeTab === 'risk' && state && <RiskPanel state={state} />}
              {activeTab === 'risk' && !state && (
                <div style={{ color: '#718096', textAlign: 'center', padding: '20px', fontSize: '13px' }}>
                  No heat data yet — make sure MS-2 is running
                </div>
              )}
            </ErrorBoundary>

            <ErrorBoundary name="ShelterPanel">
              <Suspense fallback={<LoadingSpinner text="Loading shelters..." />}>
                {activeTab === 'shelters' && <ShelterPanel city={selectedCity} />}
              </Suspense>
            </ErrorBoundary>

            <ErrorBoundary name="AlertsPanel">
              {activeTab === 'alerts' && <AlertsPanel />}
            </ErrorBoundary>
          </>
        )}
      </div>
    </div>
  );
});
