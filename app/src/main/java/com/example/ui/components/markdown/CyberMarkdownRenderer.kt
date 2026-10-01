/*
 * PeerLink - Offline Peer-to-Peer Communication Platform
 * File: CyberMarkdownRenderer.kt
 *
 * Commentary / Architectural Overview:
 * Rich, comprehensive Markdown rendering engine for Jetpack Compose:
 * - Code Blocks: Dedicated dark container with language badge, monospace font, horizontal scroll, and Copy button.
 * - Inline Code: Distinct background pill with accent color and monospace font.
 * - Headers: Hierarchical styling (# H1 down to ###### H6) with appropriate weights and accent accents.
 * - Text Formatting: Bold (**text**), Italic (*text*), Bold+Italic (***text***), and Strikethrough (~~text~~).
 * - Lists: Unordered lists (- or * or •) with custom bullet indicators; Ordered lists (1., 2.) with numeric badges.
 * - Blockquotes (> quote): Left accent border, tinted background, and italic styling.
 * - Tables: Formatted grid cells with header styling and borders.
 * - Horizontal Dividers (---): Subtle accented divider line.
 * - Links: Styled in primary cyan accent with underline.
 */

package com.example.ui.components.markdown

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberAccentAmber
import com.example.ui.theme.CyberAccentCyan
import com.example.ui.theme.CyberAccentGreen
import com.example.ui.theme.CyberAccentPurple
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCard
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun CyberMarkdownRenderer(
    markdown: String,
    modifier: Modifier = Modifier,
    baseTextColor: Color = CyberTextPrimary,
    baseFontSize: TextUnit = 13.sp,
    baseLineHeight: TextUnit = 19.sp
) {
    if (markdown.isBlank()) return

    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (block in blocks) {
            when (block) {
                is MarkdownBlock.Header -> {
                    HeaderBlock(block)
                }
                is MarkdownBlock.CodeBlock -> {
                    CodeBlock(block)
                }
                is MarkdownBlock.Blockquote -> {
                    BlockquoteBlock(block, baseFontSize, baseLineHeight)
                }
                is MarkdownBlock.BulletItem -> {
                    BulletBlock(block, baseTextColor, baseFontSize, baseLineHeight)
                }
                is MarkdownBlock.NumberedItem -> {
                    NumberedBlock(block, baseTextColor, baseFontSize, baseLineHeight)
                }
                is MarkdownBlock.HorizontalRule -> {
                    HorizontalDivider(
                        color = CyberBorder.copy(alpha = 0.6f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                is MarkdownBlock.Table -> {
                    TableBlock(block)
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = buildInlineMarkdown(block.text, baseTextColor),
                        fontSize = baseFontSize,
                        color = baseTextColor,
                        lineHeight = baseLineHeight
                    )
                }
            }
        }
    }
}

// ----------------- BLOCK COMPOSABLES -----------------

@Composable
private fun HeaderBlock(block: MarkdownBlock.Header) {
    val (fontSize, fontWeight, color) = when (block.level) {
        1 -> Triple(18.sp, FontWeight.ExtraBold, CyberTextPrimary)
        2 -> Triple(16.sp, FontWeight.Bold, CyberAccentCyan)
        3 -> Triple(14.sp, FontWeight.Bold, CyberAccentGreen)
        4 -> Triple(13.sp, FontWeight.SemiBold, CyberAccentPurple)
        else -> Triple(12.sp, FontWeight.Medium, CyberAccentAmber)
    }

    Text(
        text = buildInlineMarkdown(block.text, color),
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color,
        lineHeight = (fontSize.value + 6).sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun CodeBlock(block: MarkdownBlock.CodeBlock) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isCopied by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp)),
        color = Color(0xFF090D16),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
    ) {
        Column {
            // Header bar with language tag and Copy button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberSurface)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CyberAccentCyan.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = block.language.uppercase(Locale.ROOT),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberAccentCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            clipboardManager.setText(AnnotatedString(block.code))
                            isCopied = true
                            Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                            scope.launch {
                                delay(2000)
                                isCopied = false
                            }
                        }
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy Code",
                        tint = if (isCopied) CyberAccentGreen else CyberTextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCopied) "COPIED" else "COPY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCopied) CyberAccentGreen else CyberTextSecondary
                    )
                }
            }

            // Code content with horizontal scroll
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(10.dp)
            ) {
                Text(
                    text = block.code,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFFE2E8F0)
                )
            }
        }
    }
}

@Composable
private fun BlockquoteBlock(
    block: MarkdownBlock.Blockquote,
    baseFontSize: TextUnit,
    baseLineHeight: TextUnit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(CyberSurface.copy(alpha = 0.5f))
            .padding(vertical = 4.dp)
    ) {
        // Left accent bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .background(CyberAccentCyan)
                .padding(vertical = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = buildInlineMarkdown(block.text, CyberTextSecondary),
            fontSize = baseFontSize,
            color = CyberTextSecondary,
            fontStyle = FontStyle.Italic,
            lineHeight = baseLineHeight,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}

@Composable
private fun BulletBlock(
    block: MarkdownBlock.BulletItem,
    textColor: Color,
    baseFontSize: TextUnit,
    baseLineHeight: TextUnit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (block.indent * 12).dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(CyberAccentCyan)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = buildInlineMarkdown(block.text, textColor),
            fontSize = baseFontSize,
            color = textColor,
            lineHeight = baseLineHeight,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun NumberedBlock(
    block: MarkdownBlock.NumberedItem,
    textColor: Color,
    baseFontSize: TextUnit,
    baseLineHeight: TextUnit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (block.indent * 12).dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "${block.number}.",
            fontSize = baseFontSize,
            fontWeight = FontWeight.Bold,
            color = CyberAccentCyan,
            modifier = Modifier.width(22.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = buildInlineMarkdown(block.text, textColor),
            fontSize = baseFontSize,
            color = textColor,
            lineHeight = baseLineHeight,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TableBlock(block: MarkdownBlock.Table) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        color = CyberSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
    ) {
        Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Header row
                if (block.headers.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .background(CyberCard)
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                    ) {
                        for (cell in block.headers) {
                            Text(
                                text = cell.trim(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberAccentCyan,
                                modifier = Modifier
                                    .width(110.dp)
                                    .padding(horizontal = 4.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = CyberBorder, thickness = 1.dp)
                }

                // Data rows
                for ((index, row) in block.rows.withIndex()) {
                    Row(
                        modifier = Modifier
                            .background(if (index % 2 == 1) CyberCard.copy(alpha = 0.5f) else Color.Transparent)
                            .padding(vertical = 5.dp, horizontal = 4.dp)
                    ) {
                        for (cell in row) {
                            Text(
                                text = buildInlineMarkdown(cell.trim(), CyberTextPrimary),
                                fontSize = 11.sp,
                                color = CyberTextPrimary,
                                modifier = Modifier
                                    .width(110.dp)
                                    .padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ----------------- PARSERS & DATA TYPES -----------------

private sealed class MarkdownBlock {
    data class Header(val level: Int, val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class Blockquote(val text: String) : MarkdownBlock()
    data class BulletItem(val indent: Int, val text: String) : MarkdownBlock()
    data class NumberedItem(val indent: Int, val number: String, val text: String) : MarkdownBlock()
    data object HorizontalRule : MarkdownBlock()
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
}

/**
 * Splits raw markdown text into structural blocks.
 */
private fun parseMarkdownBlocks(raw: String): List<MarkdownBlock> {
    val lines = raw.lines()
    val blocks = mutableListOf<MarkdownBlock>()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        // 1. Code block fence
        if (trimmed.startsWith("```")) {
            val lang = trimmed.removePrefix("```").trim().ifEmpty { "code" }
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            blocks.add(MarkdownBlock.CodeBlock(lang, codeLines.joinToString("\n")))
            i++
            continue
        }

        // 2. Horizontal divider
        if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
            blocks.add(MarkdownBlock.HorizontalRule)
            i++
            continue
        }

        // 3. Headers
        if (trimmed.startsWith("#")) {
            val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 6)
            val headerText = trimmed.drop(level).trim()
            if (headerText.isNotEmpty()) {
                blocks.add(MarkdownBlock.Header(level, headerText))
                i++
                continue
            }
        }

        // 4. Blockquotes
        if (trimmed.startsWith(">")) {
            val quoteLines = mutableListOf<String>()
            while (i < lines.size && lines[i].trim().startsWith(">")) {
                quoteLines.add(lines[i].trim().removePrefix(">").trim())
                i++
            }
            blocks.add(MarkdownBlock.Blockquote(quoteLines.joinToString(" ")))
            continue
        }

        // 5. Tables
        if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
            val tableRows = mutableListOf<List<String>>()
            var headers = emptyList<String>()

            while (i < lines.size && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                val rowCells = lines[i].trim()
                    .removePrefix("|")
                    .removeSuffix("|")
                    .split("|")
                    .map { it.trim() }

                // Check if this is the separator row: |---|---|
                val isSeparator = rowCells.all { cell -> cell.all { it == '-' || it == ':' || it == ' ' } }
                if (isSeparator) {
                    // separator line, skip
                } else if (headers.isEmpty()) {
                    headers = rowCells
                } else {
                    tableRows.add(rowCells)
                }
                i++
            }

            if (headers.isNotEmpty()) {
                blocks.add(MarkdownBlock.Table(headers, tableRows))
                continue
            }
        }

        // 6. Unordered lists
        val bulletMatch = Regex("^(\\s*)([-*•+] )\\s*(.*)").find(line)
        if (bulletMatch != null) {
            val indentSpaces = bulletMatch.groupValues[1].length
            val indent = (indentSpaces / 2).coerceIn(0, 4)
            val text = bulletMatch.groupValues[3]
            blocks.add(MarkdownBlock.BulletItem(indent, text))
            i++
            continue
        }

        // 7. Numbered lists
        val numberMatch = Regex("^(\\s*)(\\d+)\\.\\s*(.*)").find(line)
        if (numberMatch != null) {
            val indentSpaces = numberMatch.groupValues[1].length
            val indent = (indentSpaces / 2).coerceIn(0, 4)
            val num = numberMatch.groupValues[2]
            val text = numberMatch.groupValues[3]
            blocks.add(MarkdownBlock.NumberedItem(indent, num, text))
            i++
            continue
        }

        // 8. Paragraphs
        if (trimmed.isNotEmpty()) {
            val paragraphLines = mutableListOf<String>()
            while (i < lines.size) {
                val nextTrimmed = lines[i].trim()
                if (nextTrimmed.isEmpty() ||
                    nextTrimmed.startsWith("```") ||
                    nextTrimmed.startsWith("#") ||
                    nextTrimmed.startsWith(">") ||
                    nextTrimmed == "---" ||
                    Regex("^(\\s*)([-*•+] )").containsMatchIn(lines[i]) ||
                    Regex("^(\\s*)(\\d+)\\.").containsMatchIn(lines[i])
                ) {
                    break
                }
                paragraphLines.add(lines[i].trim())
                i++
            }
            blocks.add(MarkdownBlock.Paragraph(paragraphLines.joinToString("\n")))
            continue
        }

        i++
    }

    return blocks
}

/**
 * Parses inline markdown: bold, italic, bold-italic, inline-code, strikethrough, links.
 */
fun buildInlineMarkdown(text: String, defaultColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val length = text.length

        while (cursor < length) {
            // 1. Inline code: `code`
            if (text[cursor] == '`') {
                val end = text.indexOf('`', cursor + 1)
                if (end != -1) {
                    val codeContent = text.substring(cursor + 1, end)
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            color = CyberAccentCyan,
                            background = CyberSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append(" $codeContent ")
                    }
                    cursor = end + 1
                    continue
                }
            }

            // 2. Bold + Italic: ***text***
            if (text.startsWith("***", cursor)) {
                val end = text.indexOf("***", cursor + 3)
                if (end != -1) {
                    val content = text.substring(cursor + 3, end)
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.ExtraBold,
                            fontStyle = FontStyle.Italic,
                            color = CyberAccentCyan
                        )
                    ) {
                        append(content)
                    }
                    cursor = end + 3
                    continue
                }
            }

            // 3. Bold: **text** or __text__
            if (text.startsWith("**", cursor) || text.startsWith("__", cursor)) {
                val delimiter = text.substring(cursor, cursor + 2)
                val end = text.indexOf(delimiter, cursor + 2)
                if (end != -1) {
                    val content = text.substring(cursor + 2, end)
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFF1F5F9)
                        )
                    ) {
                        append(content)
                    }
                    cursor = end + 2
                    continue
                }
            }

            // 4. Strikethrough: ~~text~~
            if (text.startsWith("~~", cursor)) {
                val end = text.indexOf("~~", cursor + 2)
                if (end != -1) {
                    val content = text.substring(cursor + 2, end)
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = CyberTextMuted)) {
                        append(content)
                    }
                    cursor = end + 2
                    continue
                }
            }

            // 5. Italic: *text* or _text_
            if ((text[cursor] == '*' || text[cursor] == '_') && (cursor == 0 || text[cursor - 1] != text[cursor])) {
                val marker = text[cursor]
                val end = text.indexOf(marker, cursor + 1)
                if (end != -1 && end > cursor + 1 && (end == length - 1 || text[end + 1] != marker)) {
                    val content = text.substring(cursor + 1, end)
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = defaultColor)) {
                        append(content)
                    }
                    cursor = end + 1
                    continue
                }
            }

            // 6. Markdown Link: [text](url)
            if (text[cursor] == '[') {
                val closeBracket = text.indexOf(']', cursor + 1)
                if (closeBracket != -1 && closeBracket + 1 < length && text[closeBracket + 1] == '(') {
                    val closeParen = text.indexOf(')', closeBracket + 2)
                    if (closeParen != -1) {
                        val label = text.substring(cursor + 1, closeBracket)
                        withStyle(
                            SpanStyle(
                                color = CyberAccentCyan,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.SemiBold
                            )
                        ) {
                            append(label)
                        }
                        cursor = closeParen + 1
                        continue
                    }
                }
            }

            // Default character
            append(text[cursor])
            cursor++
        }
    }
}
