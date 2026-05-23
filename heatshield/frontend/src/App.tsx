import React, { Suspense, lazy, useState, useCallback } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';
import { Header } from './components/Header';
import { Sidebar } from './features/monitor/Sidebar';
import { ErrorBoundary } from './components/ErrorBoundary';
import { LoadingSpinner } from './components/LoadingSpinner';
import { useDataFetcher } from './hooks/useDataFetcher';
import { useIsMobile } from './hooks/useMediaQuery';

// ── Code split — Mapbox (500KB) loads only when needed ────────
const HeatMap = lazy(() =>
  import('./features/map/HeatMap').then(m => ({ default: m.HeatMap }))
);

// ── React Query client ────────────────────────────────────────
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry:                2,
      refetchOnWindowFocus: false,
      staleTime:            20_000,
    },
  },
});

// ── Inner app — inside QueryClientProvider ────────────────────
const AppInner: React.FC = () => {
  const isMobile              = useIsMobile();
  const [lastUpdated, setLastUpdated] = useState(new Date());
  const { refetch }           = useDataFetcher();

  const handleRefresh = useCallback(async () => {
    await refetch();
    setLastUpdated(new Date());
  }, [refetch]);

  return (
    <div style={{
      display:       'flex',
      flexDirection: 'column',
      height:        '100dvh',
      background:    '#0f1117',
      color:         '#e2e8f0',
      overflow:      'hidden',
      fontFamily:    '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    }}>
      <Toaster
        position="top-center"
        toastOptions={{
          duration: 3000,
          style: { background: '#1a1d2e', color: '#e2e8f0', border: '1px solid #2d3748' },
        }}
      />

      {/* ── Header ── */}
      <Header lastUpdated={lastUpdated} onRefresh={handleRefresh} />

      {/* ── Body ── */}
      <div style={{ display: 'flex', flex: 1, overflow: 'hidden', position: 'relative' }}>

        {/* Map — always full area */}
        <ErrorBoundary
          name="HeatMap"
          fallback={
            <div style={{
              flex: 1, display: 'flex', alignItems: 'center',
              justifyContent: 'center', flexDirection: 'column', gap: '12px',
            }}>
              <div style={{ color: '#ef4444', fontSize: '14px' }}>
                ⚠️ Map failed to load
              </div>
              <div style={{ color: '#718096', fontSize: '12px' }}>
                Check your VITE_MAPBOX_TOKEN in .env
              </div>
            </div>
          }
        >
          <Suspense fallback={<LoadingSpinner text="Loading map..." />}>
            <HeatMap />
          </Suspense>
        </ErrorBoundary>

        {/* Desktop sidebar — right panel */}
        {!isMobile && (
          <ErrorBoundary name="Sidebar">
            <Sidebar />
          </ErrorBoundary>
        )}
      </div>

      {/* Mobile sidebar — bottom sheet */}
      {isMobile && (
        <ErrorBoundary name="MobileSidebar">
          <Sidebar />
        </ErrorBoundary>
      )}
    </div>
  );
};

// ── Root export ───────────────────────────────────────────────
export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AppInner />
    </QueryClientProvider>
  );
}
