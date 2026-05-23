import React, { memo } from 'react';
import { useQuery } from '@tanstack/react-query';
import type { City } from '../../types';
import { routingApi } from '../../services/api';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { SHELTER_ICON } from '../../constants';

interface Props { city: City; }

export const ShelterPanel = memo(({ city }: Props) => {
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey:  ['shelters', city.id],
    queryFn:   () => routingApi.getShelters(city.lat, city.lng, city.id),
    staleTime: 600_000,  // 10 min — shelters don't change often
    retry:     1,
  });

  if (isLoading) return <LoadingSpinner text="Finding cool shelters..." />;

  if (isError) return (
    <div style={{ textAlign: 'center', padding: '20px' }}>
      <div style={{ color: '#ef4444', fontSize: '12px', marginBottom: '8px' }}>
        Could not load shelters
      </div>
      <button onClick={() => refetch()} style={{
        padding: '4px 12px', borderRadius: '6px',
        border: '1px solid #4a5568', background: '#2d3748',
        color: '#e2e8f0', cursor: 'pointer', fontSize: '11px',
      }}>
        Retry
      </button>
    </div>
  );

  const shelters = data?.shelters || [];

  if (!shelters.length) return (
    <div style={{ textAlign: 'center', color: '#718096', padding: '20px', fontSize: '13px' }}>
      No shelters found nearby
    </div>
  );

  return (
    <div>
      {data?.generalAdvice && (
        <div style={{
          background: 'rgba(249,115,22,0.1)', borderRadius: '8px',
          padding: '10px 12px', marginBottom: '12px',
          fontSize: '12px', color: '#fb923c', lineHeight: 1.5,
          borderLeft: '3px solid #f97316',
        }}>
          {data.generalAdvice}
        </div>
      )}

      {shelters.map((s, i) => (
        <div key={s.id || i} style={{
          background: '#242736', borderRadius: '8px',
          padding: '12px', marginBottom: '8px',
          border: i === 0 ? '1px solid #f97316' : '1px solid transparent',
        }}>
          {/* Header */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
            <span style={{
              width: '22px', height: '22px', borderRadius: '50%',
              background: i === 0 ? '#f97316' : '#4a5568',
              color: '#fff', display: 'flex',
              alignItems: 'center', justifyContent: 'center',
              fontSize: '11px', fontWeight: 700, flexShrink: 0,
            }}>
              {i + 1}
            </span>
            <span style={{ fontSize: '13px', fontWeight: 500, color: '#e2e8f0', flex: 1 }}>
              {SHELTER_ICON[s.shelterType] || '📍'} {s.name}
            </span>
            {i === 0 && (
              <span style={{
                fontSize: '10px', padding: '2px 6px', borderRadius: '10px',
                background: 'rgba(249,115,22,0.2)', color: '#f97316',
              }}>
                Best
              </span>
            )}
          </div>

          {/* Stats row */}
          <div style={{
            display: 'flex', gap: '14px',
            fontSize: '11px', color: '#718096', marginBottom: '8px',
          }}>
            <span>📍 {s.distanceKm?.toFixed(2)}km</span>
            <span>🌡️ Exposure: {s.exposureScore?.toFixed(1)}</span>
            {s.acConfirmed && <span style={{ color: '#22c55e' }}>❄️ AC</span>}
          </div>

          {/* Route advice */}
          <div style={{
            fontSize: '11px', color: '#a0aec0',
            lineHeight: 1.5,
            background: 'rgba(255,255,255,0.03)',
            borderRadius: '6px', padding: '8px',
          }}>
            {s.routeAdvice}
          </div>
        </div>
      ))}
    </div>
  );
});
