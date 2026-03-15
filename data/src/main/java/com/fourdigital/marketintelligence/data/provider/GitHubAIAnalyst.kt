package com.fourdigital.marketintelligence.data.provider

import com.fourdigital.marketintelligence.analytics.ai.AIAnalysisResult
import com.fourdigital.marketintelligence.analytics.ai.NewsItem
import com.fourdigital.marketintelligence.core.network.api.ChatCompletionRequest
import com.fourdigital.marketintelligence.core.network.api.ChatMessage
import com.fourdigital.marketintelligence.core.network.api.GitHubModelsApi
import com.fourdigital.marketintelligence.core.network.api.ModelInfo
import com.fourdigital.marketintelligence.core.network.api.OpenAIModelsApi
import com.fourdigital.marketintelligence.domain.model.AIProviderMode
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI Market Analyst powered by GitHub Models API.
 * Supports multiple AI models: GPT-4o, GPT-4o-mini, o3-mini, etc.
 * Uses the user's GitHub PAT for authentication.
 * Provides deep market analysis, multi-timeframe predictions, and interactive Q&A.
 */
@Singleton
class GitHubAIAnalyst @Inject constructor(
    private val gitHubModelsApi: GitHubModelsApi,
    private val openAIModelsApi: OpenAIModelsApi,
    private val apiKeyManager: ApiKeyManager
) {
    companion object {
        const val GITHUB_KEY = "github"
        const val OPENAI_KEY = ApiKeyManager.OPENAI
        const val MODEL_KEY = "ai_model"

        // Available models ranked by capability
        val AVAILABLE_MODELS = listOf(
            AIModelOption("gpt-4o", "GPT-4o", "Most capable — deep analysis & complex reasoning", 4096),
            AIModelOption("gpt-4o-mini", "GPT-4o Mini", "Fast & efficient — good for quick analysis", 2048),
            AIModelOption("o3-mini", "o3-mini", "Advanced reasoning — best for predictions & strategy", 4096),
            AIModelOption("o1-mini", "o1-mini", "Strong reasoning — excellent for financial analysis", 4096),
            AIModelOption("Phi-4", "Phi-4", "Microsoft — lightweight & fast analysis", 2048),
            AIModelOption("Mistral-large-2411", "Mistral Large", "Open-source — great multilingual analysis", 2048),
            AIModelOption("DeepSeek-R1", "DeepSeek R1", "Deep reasoning — strong mathematical analysis", 4096)
        )
    }

    data class AIModelOption(
        val id: String,
        val displayName: String,
        val description: String,
        val maxTokens: Int
    )

    private var selectedModel: String = "gpt-4o-mini"
    private var providerMode: AIProviderMode = AIProviderMode.BOTH
    private var cachedModels: List<ModelInfo> = emptyList()

    // Models available on OpenAI's API
    private val openAICompatibleModels = setOf("gpt-4o", "gpt-4o-mini", "o3-mini", "o1-mini")

    /** Map selected model to one that works on OpenAI. GitHub-only models fall back to gpt-4o-mini. */
    private fun openAIModel(): String =
        if (selectedModel in openAICompatibleModels) selectedModel else "gpt-4o-mini"

    suspend fun isAvailable(): Boolean {
        val hasGitHub = !apiKeyManager.getKey(GITHUB_KEY).isNullOrBlank()
        val hasOpenAI = !apiKeyManager.getKey(OPENAI_KEY).isNullOrBlank()
        return when (providerMode) {
            AIProviderMode.GITHUB -> hasGitHub
            AIProviderMode.OPENAI -> hasOpenAI
            AIProviderMode.BOTH -> hasGitHub || hasOpenAI
        }
    }

    fun getSelectedModel(): String = selectedModel
    fun getProviderMode(): AIProviderMode = providerMode

    fun setSelectedModel(modelId: String) {
        selectedModel = modelId
    }

    fun setProviderMode(mode: AIProviderMode) {
        providerMode = mode
        cachedModels = emptyList()
    }

    suspend fun listAvailableModels(): List<ModelInfo> {
        if (cachedModels.isNotEmpty()) return cachedModels
        val githubToken = apiKeyManager.getKey(GITHUB_KEY)
        val openAiToken = apiKeyManager.getKey(OPENAI_KEY)

        return try {
            val models = mutableListOf<ModelInfo>()
            if (!githubToken.isNullOrBlank() && providerMode != AIProviderMode.OPENAI) {
                models += gitHubModelsApi.listModels("Bearer $githubToken").data
            }
            if (!openAiToken.isNullOrBlank() && providerMode != AIProviderMode.GITHUB) {
                models += openAIModelsApi.listModels("Bearer $openAiToken").data
            }
            cachedModels = models.distinctBy { it.id }
            cachedModels
        } catch (e: Exception) {
            Timber.w(e, "Failed to list models")
            emptyList()
        }
    }

    /**
     * Generate a comprehensive AI market analysis for a single asset.
     */
    suspend fun analyzeAsset(
        analysis: AIAnalysisResult,
        recentNews: List<NewsItem>
    ): String {
        if (!isAvailable()) {
            return "No AI account configured. Add GitHub/OpenAI key in Settings > API Configuration."
        }

        val prompt = buildAssetPrompt(analysis, recentNews)
        return callAI(prompt, temperature = 0.5)
    }

    /**
     * Generate a portfolio-wide market outlook with actionable recommendations.
     */
    suspend fun generateMarketOutlook(
        analyses: List<AIAnalysisResult>,
        news: List<NewsItem>
    ): String {
        if (!isAvailable()) {
            return "No AI account configured. Add GitHub/OpenAI key in Settings > API Configuration."
        }

        val prompt = buildMarketOutlookPrompt(analyses, news)
        return callAI(prompt, temperature = 0.6, maxTokens = 3000)
    }

    /**
     * Generate multi-horizon predictions (week, month, quarter, year).
     */
    suspend fun generatePredictions(
        analyses: List<AIAnalysisResult>,
        news: List<NewsItem>,
        targetReturn: Double = 10.0
    ): String {
        if (!isAvailable()) {
            return "No AI account configured. Add GitHub/OpenAI key in Settings > API Configuration."
        }

        val prompt = buildPredictionPrompt(analyses, news, targetReturn)
        return callAI(prompt, temperature = 0.4, maxTokens = 3500)
    }

    /**
     * Answer a specific question about the market using current data.
     */
    suspend fun askQuestion(
        question: String,
        analyses: List<AIAnalysisResult>,
        news: List<NewsItem>
    ): String {
        if (!isAvailable()) {
            return "No AI account configured. Add GitHub/OpenAI key in Settings > API Configuration."
        }

        val prompt = buildQuestionPrompt(question, analyses, news)
        return callAI(prompt)
    }

    private suspend fun callAI(
        userPrompt: String,
        temperature: Double = 0.6,
        maxTokens: Int? = null
    ): String {
        val modelOption = AVAILABLE_MODELS.find { it.id == selectedModel }
        val effectiveMaxTokens = maxTokens ?: modelOption?.maxTokens ?: 2048
        val githubToken = apiKeyManager.getKey(GITHUB_KEY)
        val openAiToken = apiKeyManager.getKey(OPENAI_KEY)
        val systemPrompt = """You are 4D Market Intelligence AI, a market analyst assistant.
            |
            |Follow these rules strictly:
            |- Use only the data provided by the user prompt.
            |- If a value is missing, write "insufficient data".
            |- Never guarantee profits or certainty.
            |- Always include risk and invalidation conditions.
            |- Keep responses concise, structured, and practical.
            |
            |Output format:
            |## Summary
            |## Evidence
            |## Trade Plan (if applicable)
            |## Risks
            |## Confidence (0-100)
            |
            |Use symbols: ▲ bullish, ▼ bearish, ▬ neutral, ⚠ risk.
            |This content is informational only, not financial advice.
            |
            |Model: $selectedModel""".trimMargin()

        suspend fun runGitHub(): String {
            val token = githubToken
                ?: return "GitHub account not configured."
            return callProvider(
                providerName = "GitHub",
                token = token,
                model = selectedModel,
                userPrompt = userPrompt,
                systemPrompt = systemPrompt,
                temperature = temperature,
                maxTokens = effectiveMaxTokens,
                call = { auth, request -> gitHubModelsApi.chatCompletion(auth, request) }
            )
        }

        suspend fun runOpenAI(): String {
            val token = openAiToken
                ?: return "OpenAI account not configured."
            return callProvider(
                providerName = "OpenAI",
                token = token,
                model = openAIModel(),
                userPrompt = userPrompt,
                systemPrompt = systemPrompt,
                temperature = temperature,
                maxTokens = effectiveMaxTokens,
                call = { auth, request -> openAIModelsApi.chatCompletion(auth, request) }
            )
        }

        return when (providerMode) {
            AIProviderMode.GITHUB -> runGitHub()
            AIProviderMode.OPENAI -> runOpenAI()
            AIProviderMode.BOTH -> supervisorScope {
                val githubResult = async { runGitHub() }
                val openAiResult = async { runOpenAI() }
                val gh = githubResult.await()
                val oa = openAiResult.await()
                """## Combined AI Analysis

### GitHub Models
$gh

### OpenAI
$oa""".trimIndent()
            }
        }
    }

    private suspend fun callProvider(
        providerName: String,
        token: String,
        model: String,
        userPrompt: String,
        systemPrompt: String,
        temperature: Double,
        maxTokens: Int,
        call: suspend (String, ChatCompletionRequest) -> com.fourdigital.marketintelligence.core.network.api.ChatCompletionResponse
    ): String {
        return try {
            val request = ChatCompletionRequest(
                model = model,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = userPrompt)
                ),
                temperature = temperature,
                maxTokens = maxTokens
            )
            val response = call("Bearer $token", request)
            response.choices.firstOrNull()?.message?.content
                ?: "$providerName: no response from model."
        } catch (e: retrofit2.HttpException) {
            Timber.w(e, "$providerName AI failed (HTTP ${e.code()}) with model $model")
            when (e.code()) {
                401, 403 -> "$providerName authentication failed. Check API key/token."
                429 -> "$providerName rate limit reached. Try again soon."
                404 -> "$providerName model '$model' not available."
                else -> "$providerName AI error (HTTP ${e.code()}): ${e.message()}"
            }
        } catch (e: java.net.SocketTimeoutException) {
            Timber.w(e, "$providerName AI timed out with model $model")
            "$providerName request timed out. The model may be overloaded — try again."
        } catch (e: Exception) {
            Timber.w(e, "$providerName AI analysis failed with model $model")
            "$providerName AI error: ${(e.message ?: "Unknown error").take(150)}"
        }
    }

    private fun buildAssetPrompt(analysis: AIAnalysisResult, news: List<NewsItem>): String {
        val newsSection = if (news.isNotEmpty()) {
            val topNews = news.take(6).joinToString("\n") { n -> "- ${n.headline} (${n.source})" }
            "\n\nRECENT NEWS:\n$topNews"
        } else ""

        val tfSection = analysis.timeframes.joinToString("\n") { tf ->
            "  ${tf.timeframe}: ${tf.direction} ${tf.estimatedChange}% (confidence: ${tf.confidence}%, factor: ${tf.keyFactor})"
        }

        val predSection = analysis.prediction?.let { pred ->
            """
TRADE SETUP:
- Entry: ${"%.2f".format(pred.entryPrice)}
- Target: ${"%.2f".format(pred.targetPrice)} (${"%.1f".format((pred.targetPrice - pred.entryPrice) / pred.entryPrice * 100)}%)
- Stop-Loss: ${"%.2f".format(pred.stopLoss)} (${"%.1f".format((pred.stopLoss - pred.entryPrice) / pred.entryPrice * 100)}%)
- R:R Ratio: ${"%.1f".format(pred.riskRewardRatio)}:1
- Time Horizon: ${pred.timeHorizon}"""
        } ?: ""

        val levelsSection = analysis.keyLevels.joinToString("\n") { level ->
            "- ${level.label}: ${"%.2f".format(level.price)} (${level.type})"
        }

        return """Analyze this asset and produce a disciplined trading assessment.

ASSET: ${analysis.symbol} (${analysis.assetName})
Asset Class: ${analysis.assetClass}
Current Price: ${"%.4f".format(analysis.currentPrice)}

TECHNICAL SIGNALS:
- Market Regime: ${analysis.signal.regime}
- Directional Bias: ${analysis.signal.directionalBias}
- Momentum Score: ${"%.1f".format(analysis.signal.momentumScore)}/100
- Volatility: ${analysis.signal.volatilityRegime}
- Risk Score: ${"%.1f".format(analysis.signal.riskScore)}/100
- Confidence: ${analysis.signal.confidenceScore}%

AI COMPOSITE SCORE: ${"%.2f".format(analysis.compositeScore)} (${analysis.action})
Overall Confidence: ${"%.0f".format(analysis.confidence)}%
Risk Level: ${analysis.riskLevel}

TIMEFRAME FORECASTS:
$tfSection
$predSection

KEY PRICE LEVELS:
$levelsSection
$newsSection

Provide output with these sections:
1. ## Summary
2. ## Evidence
3. ## Trade Plan (entry, target, stop, invalidation)
4. ## Multi-Timeframe Outlook (week, month, quarter)
5. ## Risks
6. ## Confidence (0-100)

For each directional statement, include a probability estimate.
If data is missing, explicitly state it."""
    }

    private fun buildMarketOutlookPrompt(
        analyses: List<AIAnalysisResult>,
        news: List<NewsItem>
    ): String {
        val summary = analyses.take(20).joinToString("\n") { a ->
            "  ${a.symbol} (${a.assetClass}): Price=${a.currentPrice}, AI=${a.action}(Score=${"%+.0f".format(a.compositeScore * 100)}%), " +
                    "Regime=${a.signal.regime}, Momentum=${"%.0f".format(a.signal.momentumScore)}, Risk=${"%.0f".format(a.signal.riskScore)}"
        }

        val newsSection = if (news.isNotEmpty()) {
            val topNews = news.take(10).joinToString("\n") { n -> "- ${n.headline} (${n.source})" }
            "\n\nLATEST MARKET NEWS:\n$topNews"
        } else ""

        val bullCount = analyses.count { it.compositeScore > 0.1 }
        val bearCount = analyses.count { it.compositeScore < -0.1 }
        val strongBuy = analyses.count { it.compositeScore > 0.4 }
        val strongSell = analyses.count { it.compositeScore < -0.4 }

        val assetClassBreakdown = analyses.groupBy { it.assetClass }.map { (cls, items) ->
            val avgScore = items.map { it.compositeScore }.average()
            "  $cls: ${items.size} assets, avg score=${"%+.0f".format(avgScore * 100)}%"
        }.joinToString("\n")

        return """Generate a market outlook for this portfolio with pragmatic positioning guidance.

PORTFOLIO OVERVIEW (${analyses.size} assets analyzed):
- Strong Buy signals: $strongBuy
- Buy signals: ${bullCount - strongBuy}
- Neutral: ${analyses.size - bullCount - bearCount}
- Sell signals: ${bearCount - strongSell}
- Strong Sell signals: $strongSell

ASSET CLASS BREAKDOWN:
$assetClassBreakdown

INDIVIDUAL ASSET SIGNALS:
$summary
$newsSection

Provide output with:
1. ## Market Regime
2. ## Asset-Class Strength/Weakness
3. ## Top Opportunities (max 5)
4. ## Top Risks (max 3)
5. ## Prediction Table
   | Asset | 1-Week | 1-Month | 3-Month | Direction | Confidence |
6. ## Portfolio Strategy (positioning + hedge ideas)
7. ## Action Checklist (now, this week, and monitor)

Use concise bullets and include uncertainty where relevant."""
    }

    private fun buildPredictionPrompt(
        analyses: List<AIAnalysisResult>,
        news: List<NewsItem>,
        targetReturn: Double
    ): String {
        val assetData = analyses.take(20).joinToString("\n") { a ->
            "  ${a.symbol}: Price=${a.currentPrice}, Score=${"%+.0f".format(a.compositeScore * 100)}%, " +
                "Bias=${a.signal.directionalBias}, Momentum=${"%.0f".format(a.signal.momentumScore)}, " +
                "Vol=${a.signal.volatilityRegime}, Risk=${"%.0f".format(a.signal.riskScore)}"
        }

        val newsContext = if (news.isNotEmpty()) {
            "\nKEY NEWS:\n" + news.take(8).joinToString("\n") { "- ${it.headline}" }
        } else ""

        return """Generate scenario-based predictions for the portfolio.
Target return: ${targetReturn}% over various timeframes.

PORTFOLIO DATA:
$assetData
$newsContext

For each asset, provide predictions in this format and include a clear BUY / HOLD / SELL action:

## PREDICTIONS BY TIMEFRAME

### 1 DAY OUTLOOK
| Asset | Current | Target | Action | Direction | Confidence | Trigger |

### 1 WEEK OUTLOOK
| Asset | Current | Target | Action | Direction | Confidence | Trigger |

### 1 MONTH OUTLOOK
| Asset | Current | Target | Action | Direction | Confidence | Trigger |

### 1 QUARTER (3 MONTHS) OUTLOOK
| Asset | Current | Target | Action | Direction | Confidence | Trigger |

### 1 YEAR OUTLOOK
| Asset | Current | Target | Action | Direction | Confidence | Trigger |

## TOP PICKS TOWARD ${targetReturn}%
- **Short-term (1-4 weeks)**: Best assets for quick gains
- **Swing trades (1-3 months)**: Best risk/reward setups
- **Long-term holds (3-12 months)**: Strategic positions

## RISK SCENARIOS
- **Bull case** (30% probability): What happens if markets rally
- **Base case** (50% probability): Most likely scenario
- **Bear case** (20% probability): What to watch for protection

## TRADE EXECUTION CHECKLIST
- Entry zone, invalidation level, and take-profit for each top pick
- Maximum risk per trade suggestion
- Which signals must confirm before buy/sell

Include specific price targets and stop-losses for each recommendation.
Clearly separate high-confidence vs low-confidence ideas."""
    }

    private fun buildQuestionPrompt(
        question: String,
        analyses: List<AIAnalysisResult>,
        news: List<NewsItem>
    ): String {
        val context = analyses.take(15).joinToString("\n") { a ->
            "  ${a.symbol} (${a.assetClass}): ${"%.4f".format(a.currentPrice)}, ${a.action}, " +
                "Score=${"%+.0f".format(a.compositeScore * 100)}%, Regime=${a.signal.regime}, " +
                "Momentum=${"%.0f".format(a.signal.momentumScore)}, Vol=${a.signal.volatilityRegime}"
        }

        val newsContext = if (news.isNotEmpty()) {
            "\n\nRecent headlines:\n" + news.take(8).joinToString("\n") { n -> "- ${n.headline} (${n.source})" }
        } else ""

        return """Answer this market question using only the provided market data below.

QUESTION: $question

CURRENT MARKET DATA (${analyses.size} assets):
$context
$newsContext

Provide a data-driven answer with:
1) direct answer,
2) supporting evidence,
3) risks/uncertainty,
4) optional trade plan if question asks for buy/sell.

Reference concrete values from the data whenever possible."""
    }
}
