# 4D Market Intelligence

**Cross-Market Intelligence Cockpit for Android**

Developed by **Daniel Dorsch** — [4D Digital Solutions](https://4ddigital.solutions)

---

## Overview

4D Market Intelligence is a sophisticated Android application for monitoring and analyzing financial markets across German (DAX), Brazilian (B3), and global asset classes (crypto, commodities, FX). It is a **decision-support tool** — not a broker, not financial advice.

### Key Capabilities

- **Multi-Market Watchlists** — DAX 30, Ibovespa/B3, commodities (Gold, Silver, Oil), crypto (BTC, ETH)
- **AI Signal Center** — Rule-based technical analysis with explainable confidence scoring (RSI, MACD, SMA/EMA, ATR, Bollinger Bands)
- **Cross-Asset Correlation Engine** — Pearson & Spearman correlation, rolling windows, stability assessment, 11+ predefined cross-market pairs
- **World Market Clock** — Berlin / São Paulo / New York / UTC clocks with live exchange session status
- **Alert System** — Price, percentage, correlation, volatility, and signal-based alerts
- **Dark Terminal Aesthetic** — Professional dark UI optimized for Samsung Galaxy S24 Ultra

---

## Architecture

### Multi-Module Structure (16 modules)

```
├── app                     # Application entry point, Hilt setup
├── core-common             # DataResult, extensions, shared utilities
├── core-ui                 # Material 3 theme, colors, typography, common components
├── core-network            # Retrofit/OkHttp network layer (future)
├── core-database           # Room persistence (future)
├── domain                  # Domain models, provider interfaces, repository interfaces
├── data                    # Repository implementations, mock providers, DI
├── analytics               # Correlation engine, signal engine, DI
├── feature-watchlist       # Watchlist screens & ViewModel
├── feature-market-overview # Dashboard & cross-asset overview
├── feature-correlations    # Correlation matrix UI
├── feature-signals         # Signal Center with explainable AI
├── feature-alerts          # Alert rules & event history
├── feature-world-clock     # Multi-timezone market clock
├── feature-settings        # User preferences & configuration
└── feature-github-sync     # GitHub backup integration (future)
```

### Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.1.0, JVM 17 |
| UI | Jetpack Compose (BOM 2024.12.01), Material 3 |
| DI | Hilt 2.53.1 + KSP |
| Architecture | MVVM + Unidirectional Data Flow (StateFlow) |
| Async | Kotlin Coroutines 1.9.0 + Flow |
| Serialization | kotlinx-serialization 1.7.3 |
| Date/Time | kotlinx-datetime 0.6.1 |
| Network | Retrofit 2.11.0 + OkHttp 4.12.0 |
| Database | Room 2.6.1 |
| Build | Gradle 8.11.1, AGP 8.7.3, Version Catalog |
| Target | compileSdk 35, minSdk 28, targetSdk 35 |

### Design Patterns

- **Provider Abstraction** — All data sources behind interfaces (`MarketDataProvider`, `StreamingQuoteProvider`, `HistoricalDataProvider`, etc.) enabling vendor swapping without code changes
- **DataResult Sealed Class** — Type-safe success/error/loading states throughout
- **Repository Pattern** — Domain-level data access with in-memory caching
- **Explainable AI** — Every signal includes reasoning steps, feature contributions, and data quality warnings

---

## Getting Started

### Prerequisites

- Android Studio Ladybug (2024.2) or newer
- JDK 17+
- Android SDK 35

### Build & Run

```bash
# Clone the repository
git clone https://github.com/your-org/4d-market-intelligence.git
cd 4d-market-intelligence

# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Install on connected device
./gradlew installDebug
```

### Demo Mode

The app ships with a comprehensive **mock data provider** that generates realistic market data for all asset classes. No API keys are required for demo mode. The mock provider:

- Generates realistic price jitter (±2.5%) around real base prices
- Simulates proper bid/ask spreads per asset class
- Provides synthetic OHLCV historical data with trending patterns
- Computes real-time market hours for all exchanges (Xetra, B3, NYSE, etc.)

### Live Data (Optional)

To connect a real data provider, copy `.env.example` to `.env` and add your API keys. Then switch the provider type in Settings → Developer → Provider.

---

## Feature Modules

### Dashboard
Central command view with market regime indicator, key market tickers (DAX, BTC, Gold), navigation cards, and disclaimer.

### Watchlists
5 pre-seeded watchlists (DAX Core, B3 Core, Commodities Focus, Crypto Watch, Global Proxies). Real-time quotes with data quality badges, exchange indicators, and volume display.

### Correlation Engine
- **Pearson & Spearman** dual computation
- **Rolling correlation** with configurable windows (30/60/90/120 days)
- **Stability assessment** (Stable / Weakening / Breaking / Reverting)
- **11 predefined cross-market pairs**: DAX↔Gold, DAX↔BTC, Ibovespa↔Oil, Petrobras↔Oil, Vale↔Gold, BTC↔Gold, Germany↔Brazil, and more

### Signal Center
- **Regime Classification**: Risk-On, Risk-Off, Transitioning, Mixed
- **Directional Bias**: Strong Bullish → Strong Bearish (8 levels)
- **Technical Indicators**: SMA(20/50), EMA(12/26), RSI(14), MACD, ATR(14), Bollinger Bands
- **Price Zones**: ATR-based buy/sell zones, swing level support/resistance
- **Explainable AI**: Every signal includes reasoning steps, feature contributions, and a concise + advanced explanation
- **Confidence Scoring**: 0-100 with data quality penalties

### World Clock
Live clocks for Berlin, São Paulo, New York, and UTC. Exchange session cards for Xetra, B3, NYSE, NASDAQ, CME, COMEX, NYMEX, LSE, and 24/7 crypto markets.

### Alerts
Create rules for price targets, percentage moves, correlation thresholds, volatility levels, signal confidence, and market open/close events. Full event history with dismiss/snooze.

### Settings
Theme selection, default market, refresh interval, streaming toggle, analytics window configuration, risk profile, notification preferences, and developer mode with debug panel.

---

## Security

- **Network security config** with certificate pinning and cleartext traffic disabled
- **SecureReleaseTree** logging that redacts sensitive keywords in release builds
- **No hardcoded API keys** — all credentials via BuildConfig/encrypted storage
- **ProGuard/R8** code shrinking and obfuscation in release builds
- **OWASP-aligned** input validation at system boundaries

---

## Disclaimer

> **4D Market Intelligence is a decision-support and educational tool. It does NOT provide financial advice, investment recommendations, or trading signals. All analysis is based on historical data and statistical models — past performance does not guarantee future results. The developers assume no liability for financial decisions made using this application.**

---

## CI/CD

GitHub Actions workflow at `.github/workflows/ci.yml`:
- **Lint** → **Build Debug** → **Unit Tests** on every push/PR
- **Instrumented Tests** on Android emulator (API 34) for main branch pushes
- Build reports uploaded as artifacts (14-day retention)

---

## License

Proprietary — © 2024 Daniel Dorsch, 4D Digital Solutions. All rights reserved.

---

## Contact

- **Developer**: Daniel Dorsch
- **Company**: 4D Digital Solutions
- **Email**: contact@4ddigital.solutions
