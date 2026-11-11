package com.example.qahelper.ui.components

import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.ext.latex.JLatexMathTheme
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin

/**
 * 把用户/AI 常见写法统一成 Markwon 认识的分隔符：
 *
 * - 行内：$...$、\(...\)  -> $$...$$
 * - 块级：$$...$$、\[...\] -> 单独两行 $$ 包起来
 *
 * Markdown 代码块和行内代码中的 $ 不会被处理。
 */
internal object MarkdownLatexNormalizer {

    private val fencedCode = Regex("""(?ms)^(`{3,}|~{3,})[^\n]*\n.*?^\1[ \t]*$""")
    private val inlineCode = Regex("`[^`\\n]*`")
    private val math = Regex(
        """\\\[(.+?)\\\]|\\\((.+?)\\\)|\$\$(.+?)\$\$|(?<!\\)(?<!\$)\$(?!\$)([^$\n]+?)\$(?!\$)""",
        setOf(RegexOption.DOT_MATCHES_ALL)
    )

    fun normalize(source: String): String {
        if (!source.contains('$') &&
            !source.contains("\\(") &&
            !source.contains("\\[")
        ) {
            return source
        }

        val protected = mutableListOf<String>()
        var text = protect(source, fencedCode, protected)
        text = protect(text, inlineCode, protected)

        val mathBlocks = mutableListOf<String>()
        text = math.replace(text) { match ->
            val groups = match.groups
            val latex = when {
                groups[1] != null -> groups[1]!!.value
                groups[2] != null -> groups[2]!!.value
                groups[3] != null -> groups[3]!!.value
                groups[4] != null -> groups[4]!!.value
                else -> return@replace match.value
            }.trim()

            if (latex.isEmpty()) {
                return@replace match.value
            }

            val isBlock = groups[1] != null || groups[3] != null
            val replacement = if (isBlock) {
                "\n\n$$\n$latex\n$$\n\n"
            } else {
                "$$" + latex + "$$"
            }
            val token = "\u0001M${mathBlocks.size}\u0001"
            mathBlocks.add(replacement)
            token
        }

        mathBlocks.forEachIndexed { index, value ->
            text = text.replace("\u0001M$index\u0001", value)
        }
        protected.forEachIndexed { index, value ->
            text = text.replace("\u0001P$index\u0001", value)
        }
        return text
    }

    private fun protect(
        source: String,
        regex: Regex,
        store: MutableList<String>
    ): String = regex.replace(source) { match ->
        val token = "\u0001P${store.size}\u0001"
        store.add(match.value)
        token
    }
}

/**
 * 详情页答案渲染：Markdown + LaTeX，跟随 Material3 主题的颜色和字号。
 */
@Composable
fun RichMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext
    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current

    val textColor = colorScheme.onSurface.toArgb()
    val linkColor = colorScheme.primary.toArgb()
    val codeBlockBackground = colorScheme.surfaceVariant.toArgb()
    val inlineCodeBackground = colorScheme.surfaceVariant.copy(alpha = 0.75f).toArgb()
    val quoteColor = colorScheme.outline.toArgb()
    val thematicColor = colorScheme.outline.copy(alpha = 0.6f).toArgb()
    val headingBreakColor = colorScheme.outline.copy(alpha = 0.5f).toArgb()
    val selectionColor = colorScheme.primary.copy(alpha = 0.28f).toArgb()

    val bodySize = MaterialTheme.typography.bodyLarge.fontSize.value
    val bodySizePx = with(density) { bodySize.sp.toPx() }
    val blockPaddingPx = with(density) { 8.dp.roundToPx() }
    val quoteWidthPx = with(density) { 3.dp.roundToPx() }

    val processed = remember(markdown) { MarkdownLatexNormalizer.normalize(markdown) }

    val markwon = remember(
        context,
        textColor,
        linkColor,
        codeBlockBackground,
        inlineCodeBackground,
        quoteColor,
        thematicColor,
        headingBreakColor,
        bodySizePx,
        blockPaddingPx,
        quoteWidthPx
    ) {
        val themePlugin = object : AbstractMarkwonPlugin() {
            override fun configureTheme(builder: MarkwonTheme.Builder) {
                builder
                    .linkColor(linkColor)
                    .isLinkUnderlined(true)
                    .blockQuoteColor(quoteColor)
                    .blockQuoteWidth(quoteWidthPx)
                    .codeTextColor(textColor)
                    .codeBackgroundColor(inlineCodeBackground)
                    .codeBlockTextColor(textColor)
                    .codeBlockBackgroundColor(codeBlockBackground)
                    .codeBlockMargin(blockPaddingPx)
                    .headingBreakColor(headingBreakColor)
                    .thematicBreakColor(thematicColor)
            }
        }

        val latexPlugin = JLatexMathPlugin.create(bodySizePx) { builder ->
            builder.inlinesEnabled(true)
            builder.blocksEnabled(true)
            builder.theme()
                .textColor(textColor)
                .inlineTextColor(textColor)
                .blockTextColor(textColor)
                .inlineBackgroundProvider {
                    ColorDrawable(AndroidColor.TRANSPARENT)
                }
                .blockBackgroundProvider {
                    ColorDrawable(codeBlockBackground)
                }
                .blockPadding(JLatexMathTheme.Padding.all(blockPaddingPx))
                .blockHorizontalAlignment(Gravity.CENTER_HORIZONTAL)
                .blockFitCanvas(true)
        }

        Markwon.builder(context)
            .usePlugin(MarkwonInlineParserPlugin.create())
            .usePlugin(latexPlugin)
            .usePlugin(themePlugin)
            .build()
    }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            TextView(ctx).apply {
                setTextIsSelectable(true)
                includeFontPadding = false
                setPadding(0, 0, 0, 0)
                setBackgroundColor(AndroidColor.TRANSPARENT)
                setLineSpacing(0f, 1.28f)
                textSize = bodySize
                setTextColor(textColor)
                setLinkTextColor(linkColor)
                highlightColor = selectionColor
            }
        },
        update = { view ->
            view.setTextColor(textColor)
            view.setLinkTextColor(linkColor)
            view.textSize = bodySize
            view.highlightColor = selectionColor

            val renderKey = "${processed.hashCode()}:${markwon.hashCode()}"
            if (view.tag != renderKey) {
                view.tag = renderKey
                markwon.setMarkdown(view, processed)
            }
        }
    )
}
