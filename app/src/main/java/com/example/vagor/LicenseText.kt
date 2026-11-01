package com.example.vagor

// License text is wrapped for about 80 columns, which a phone's narrower dialog would wrap again,
// leaving a short line after nearly every line. This joins each paragraph (lines between blank
// lines) into one line, so the text wraps to the screen.
fun reflowParagraphs(text: String): String = text.trim()
    .split(Regex("\\n\\s*\\n"))
    .joinToString("\n\n") { paragraph -> paragraph.lines().joinToString(" ") { it.trim() } }
