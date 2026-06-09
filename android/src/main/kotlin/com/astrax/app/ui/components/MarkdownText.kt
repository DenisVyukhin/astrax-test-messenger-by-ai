package com.astrax.app.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current
) {
    Text(
        text = parseMarkdown(text),
        modifier = modifier,
        color = color,
        style = style
    )
}

fun parseMarkdown(input: String): AnnotatedString = buildAnnotatedString {
    var index = 0
    while (index < input.length) {
        when {
            input.startsWith("**", index) -> {
                val end = input.indexOf("**", index + 2)
                if (end > index) appendStyled(input.substring(index + 2, end), SpanStyle(fontWeight = FontWeight.Bold)).also { index = end + 2 } else append(input[index++])
            }
            input.startsWith("__", index) -> {
                val end = input.indexOf("__", index + 2)
                if (end > index) appendStyled(input.substring(index + 2, end), SpanStyle(textDecoration = TextDecoration.Underline)).also { index = end + 2 } else append(input[index++])
            }
            input.startsWith("~~", index) -> {
                val end = input.indexOf("~~", index + 2)
                if (end > index) appendStyled(input.substring(index + 2, end), SpanStyle(textDecoration = TextDecoration.LineThrough)).also { index = end + 2 } else append(input[index++])
            }
            input[index] == '*' -> {
                val end = input.indexOf("*", index + 1)
                if (end > index) appendStyled(input.substring(index + 1, end), SpanStyle(fontStyle = FontStyle.Italic)).also { index = end + 1 } else append(input[index++])
            }
            else -> append(input[index++])
        }
    }
}

private fun AnnotatedString.Builder.appendStyled(value: String, style: SpanStyle) {
    val start = length
    append(value)
    addStyle(style, start, length)
}
