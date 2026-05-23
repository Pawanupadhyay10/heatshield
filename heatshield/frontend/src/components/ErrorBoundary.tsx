import React, { Component, ReactNode } from 'react';

interface Props { children: ReactNode; fallback?: ReactNode; name?: string; }
interface State { hasError: boolean; error?: Error; }

export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error) {
    console.error(`[ErrorBoundary:${this.props.name}]`, error);
  }

  render() {
    if (this.state.hasError) {
      return this.props.fallback ?? (
        <div style={{
          padding: '16px', textAlign: 'center',
          color: '#ef4444', fontSize: '12px',
          background: 'rgba(239,68,68,0.1)',
          borderRadius: '8px', margin: '8px',
        }}>
          ⚠️ {this.props.name || 'Component'} failed to load.
          <button
            onClick={() => this.setState({ hasError: false })}
            style={{
              display: 'block', margin: '8px auto 0',
              padding: '4px 12px', borderRadius: '6px',
              border: '1px solid #ef4444',
              background: 'none', color: '#ef4444',
              cursor: 'pointer', fontSize: '11px',
            }}
          >
            Retry
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}
