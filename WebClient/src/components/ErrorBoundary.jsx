import { Component } from 'react';

export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, info) {
    console.error('UI napaka:', error, info);
  }

  reset = () => this.setState({ hasError: false, error: null });

  render() {
    if (!this.state.hasError) return this.props.children;
    return (
      <div className="container">
        <div className="card" style={{ padding: 40, textAlign: 'center' }}>
          <h1 style={{ marginTop: 0 }}>Nekaj je šlo narobe.</h1>
          <p style={{ color: '#6b7280' }}>
            {this.state.error?.message || 'Nepričakovana napaka v vmesniku.'}
          </p>
          <div style={{ display: 'flex', gap: 8, justifyContent: 'center', marginTop: 16 }}>
            <button onClick={this.reset}>Poskusi znova</button>
            <button className="secondary" onClick={() => window.location.assign('/')}>
              Nazaj na začetno
            </button>
          </div>
        </div>
      </div>
    );
  }
}
