import { Component, type ReactNode } from "react";
import { Link } from "react-router-dom";

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
}

export default class AppErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  render() {
    if (this.state.hasError) {
      return (
        <main className="recovery-page">
          <section className="state-card state-error">
            <div>
              <strong>We hit an unexpected problem.</strong>
              <span>Try returning to the show listing or reload the page.</span>
            </div>
            <Link className="button button-primary button-small" to="/shows">Browse shows</Link>
          </section>
        </main>
      );
    }
    return this.props.children;
  }
}
