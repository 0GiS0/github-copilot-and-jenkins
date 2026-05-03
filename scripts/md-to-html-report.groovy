/**
 * Converts a Markdown report to a styled HTML page for Jenkins publishHTML.
 *
 * Usage from a Declarative Pipeline:
 *   script {
 *       def report = load 'scripts/md-to-html-report.groovy'
 *       report.convert('analysis/report.md', 'analysis/report.html', 'Project Analysis Report')
 *   }
 */

@NonCPS
static String escapeHtml(String value) {
    value.replace('&', '&amp;')
         .replace('<', '&lt;')
         .replace('>', '&gt;')
         .replace('"', '&quot;')
         .replace("'", '&#x27;')
}

@NonCPS
static String renderInline(String text) {
    def result = new StringBuilder()
    int i = 0
    while (i < text.length()) {
        if (text[i] == '`') {
            int end = text.indexOf('`', i + 1)
            if (end != -1) {
                result.append("<code>${escapeHtml(text.substring(i + 1, end))}</code>")
                i = end + 1
                continue
            }
            result.append('`')
            i++
            continue
        }
        if (text.substring(i).startsWith('**')) {
            int end = text.indexOf('**', i + 2)
            if (end != -1) {
                result.append("<strong>${renderInline(text.substring(i + 2, end))}</strong>")
                i = end + 2
                continue
            }
            result.append('**')
            i += 2
            continue
        }
        int nextCode = text.indexOf('`', i)
        int nextBold = text.indexOf('**', i)
        def candidates = ([nextCode, nextBold] as List).findAll { it != -1 }
        int nextMarker = candidates ? candidates.min() : text.length()
        if (nextMarker == i) {
            result.append(escapeHtml(text[i] as String))
            i++
            continue
        }
        result.append(escapeHtml(text.substring(i, nextMarker)))
        i = nextMarker
    }
    result.toString()
}

@NonCPS
static String renderMarkdown(String source) {
    def lines = source.split('\n', -1) as List
    def output = []
    boolean inUl = false
    boolean inCode = false
    boolean inTable = false
    boolean tableHasHeader = false
    boolean inSection = false

    def closeBlocks = {
        if (inUl) { output << '</ul>'; inUl = false }
        if (inTable) { output << '</tbody></table></div>'; inTable = false; tableHasHeader = false }
    }

    def closeSection = {
        if (inSection) { closeBlocks(); output << '</div></details>'; inSection = false }
    }

    for (line in lines) {
        String stripped = line.trim()

        // Fenced code blocks
        if (stripped.startsWith('```')) {
            if (inCode) {
                output << '</code></pre>'
                inCode = false
            } else {
                closeBlocks()
                String lang = stripped.substring(3).trim()
                String cls = lang ? " class=\"lang-${escapeHtml(lang)}\"" : ''
                output << "<pre><code${cls}>"
                inCode = true
            }
            continue
        }
        if (inCode) {
            output << escapeHtml(line)
            continue
        }

        if (!stripped) { closeBlocks(); continue }
        if (stripped == '---') { closeBlocks(); output << '<hr>'; continue }

        // Tables
        if (stripped.startsWith('|') && stripped.endsWith('|')) {
            def cells = stripped[1..-2].split('\\|').collect { renderInline(it.trim()) }
            if (cells.every { it.replaceAll('[-:]', '').trim().isEmpty() }) { continue }
            if (!inTable) {
                closeBlocks()
                output << '<div class="table-wrap"><table><tbody>'
                inTable = true
                tableHasHeader = false
            }
            String tag = !tableHasHeader ? 'th' : 'td'
            tableHasHeader = true
            output << '<tr>' + cells.collect { "<${tag}>${it}</${tag}>" }.join('') + '</tr>'
            continue
        }

        // Unordered lists
        if (stripped.startsWith('- ')) {
            if (inTable) { output << '</tbody></table></div>'; inTable = false; tableHasHeader = false }
            if (!inUl) { output << '<ul>'; inUl = true }
            output << "<li>${renderInline(stripped.substring(2))}</li>"
            continue
        }

        closeBlocks()

        // Headings
        if (stripped.startsWith('#### ')) {
            output << "<h4>${renderInline(stripped.substring(5))}</h4>"
        } else if (stripped.startsWith('### ')) {
            output << "<h3>${renderInline(stripped.substring(4))}</h3>"
        } else if (stripped.startsWith('## ')) {
            closeSection()
            output << "<details class=\"report-section\" open><summary>${renderInline(stripped.substring(3))}</summary><div class=\"section-body\">"
            inSection = true
        } else if (stripped.startsWith('# ')) {
            output << "<h1>${renderInline(stripped.substring(2))}</h1>"
        } else {
            output << "<p>${renderInline(stripped)}</p>"
        }
    }
    if (inCode) { output << '</code></pre>' }
    closeBlocks()
    closeSection()
    output.join('\n')
}

private static final String CSS_PATH = 'scripts/report.css'

/**
 * Converts a Markdown file to a styled HTML report.
 *
 * @param mdPath   Workspace-relative path to the Markdown source
 * @param htmlPath Workspace-relative path for the HTML output
 * @param title    Report title shown in the header and page title
 */
void convert(String mdPath, String htmlPath, String title) {
    String markdown = readFile(file: mdPath, encoding: 'UTF-8')
    String css = readFile(file: CSS_PATH, encoding: 'UTF-8')
    String body = renderMarkdown(markdown)
    String safeTitle = escapeHtml(title)

    String cssDir = htmlPath.contains('/') ? htmlPath.substring(0, htmlPath.lastIndexOf('/')) : '.'
    String cssPath = "${cssDir}/report.css"

    String html = """<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${safeTitle}</title>
    <link rel="stylesheet" href="report.css">
</head>
<body>
    <main class="report-shell">
        <a class="back-link" href="../">&larr; Back to Jenkins build</a>
        <article class="report-card">
            <header class="report-header">
                <h1>${safeTitle}</h1>
                <div class="report-meta">
                    <span class="meta-pill">Generated by GitHub Copilot CLI</span>
                    <span class="meta-pill">Jenkins build report</span>
                </div>
            </header>
            <section class="report-content">
                ${body}
            </section>
        </article>
    </main>
</body>
</html>
"""
    writeFile(file: cssPath, text: css, encoding: 'UTF-8')
    writeFile(file: htmlPath, text: html, encoding: 'UTF-8')
    echo "Generated ${htmlPath} and ${cssPath}"
}

return this
