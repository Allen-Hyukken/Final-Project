/**
 * TEACHER DASHBOARD - SHARED JAVASCRIPT
 * Functions for teacher pages including code copy, file upload, and syntax highlighting
 */

// ============================================
// CLASS CODE COPY FUNCTIONALITY
// ============================================

/**
 * Copy class code to clipboard
 * @param {string} codeElementId - ID of element containing the code
 * @param {string} feedbackElementId - ID of feedback element to show confirmation
 */
function copyClassCode(codeElementId, feedbackElementId) {
    const codeElement = document.getElementById(codeElementId);
    const feedbackElement = document.getElementById(feedbackElementId);

    if (!codeElement) {
        console.error('Code element not found:', codeElementId);
        return;
    }

    const codeToCopy = codeElement.textContent.trim();

    if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(codeToCopy).then(() => {
            showFeedback(feedbackElement);
        }).catch(err => {
            console.error('Failed to copy text using Clipboard API:', err);
            fallbackCopy(codeElement, codeToCopy, feedbackElement);
        });
    } else {
        fallbackCopy(codeElement, codeToCopy, feedbackElement);
    }
}

/**
 * Fallback copy method using execCommand
 */
function fallbackCopy(codeElement, codeToCopy, feedbackElement) {
    try {
        const range = document.createRange();
        range.selectNodeContents(codeElement);
        const selection = window.getSelection();
        selection.removeAllRanges();
        selection.addRange(range);
        document.execCommand('copy');
        selection.removeAllRanges();

        if (feedbackElement) {
            feedbackElement.textContent = 'Copied!';
            showFeedback(feedbackElement);
        }
    } catch (fallbackErr) {
        console.error('Fallback copy failed: ', fallbackErr);
        alert(`Failed to copy code. Please manually copy: ${codeToCopy}`);
    }
}

/**
 * Show feedback message temporarily
 */
function showFeedback(feedbackElement) {
    if (feedbackElement) {
        feedbackElement.style.display = 'inline';
        setTimeout(() => {
            feedbackElement.style.display = 'none';
        }, 2000);
    }
}

// ============================================
// FILE UPLOAD DISPLAY NAME
// ============================================

/**
 * Initialize file input to display selected filename
 */
function initializeFileInput() {
    const fileInput = document.getElementById('classPic');
    if (fileInput) {
        fileInput.addEventListener('change', function(e) {
            const fileName = e.target.files[0] ? e.target.files[0].name : 'Upload Class Banner (Optional)';
            const label = e.target.nextElementSibling;
            if (label) {
                label.innerText = fileName;
            }
        });
    }
}

// ============================================
// SYNTAX HIGHLIGHTING FOR CODE
// ============================================

/**
 * Highlight code syntax with colors
 * @param {string} code - The code to highlight
 * @returns {string} HTML string with syntax highlighting
 */
function highlightSyntax(code) {
    const keywords = ['function', 'const', 'let', 'var', 'if', 'else', 'for', 'while', 'return',
        'class', 'new', 'this', 'import', 'export', 'from', 'async', 'await',
        'try', 'catch', 'throw', 'break', 'continue', 'switch', 'case', 'default',
        'typeof', 'instanceof', 'delete', 'void', 'yield', 'in', 'of', 'do'];

    let highlighted = code;

    // Escape HTML entities
    highlighted = highlighted.replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');

    // Highlight comments
    highlighted = highlighted.replace(/(\/\/.*$)/gm, '<span class="comment">$1</span>');
    highlighted = highlighted.replace(/(\/\*[\s\S]*?\*\/)/g, '<span class="comment">$1</span>');

    // Highlight strings
    highlighted = highlighted.replace(/("(?:[^"\\]|\\.)*")/g, '<span class="string">$1</span>');
    highlighted = highlighted.replace(/('(?:[^'\\]|\\.)*')/g, '<span class="string">$1</span>');
    highlighted = highlighted.replace(/((?:[^\\]|\\.)*)/g, '<span class="string">$1</span>');

    // Highlight numbers
    highlighted = highlighted.replace(/\b(\d+\.?\d*)\b/g, '<span class="number">$1</span>');

    // Highlight keywords
    keywords.forEach(keyword => {
        const regex = new RegExp('\\b(' + keyword + ')\\b', 'g');
        highlighted = highlighted.replace(regex, '<span class="keyword">$1</span>');
    });

    // Highlight function names
    highlighted = highlighted.replace(/\b([a-zA-Z_$][a-zA-Z0-9_$]*)\s*(?=\()/g, '<span class="function">$1</span>');

    // Highlight properties
    highlighted = highlighted.replace(/\.([a-zA-Z_$][a-zA-Z0-9_$]*)/g, '.<span class="property">$1</span>');

    return highlighted;
}

/**
 * Update line numbers for code editor
 * @param {HTMLTextAreaElement} textarea - The textarea element
 * @param {HTMLElement} lineNumbersDiv - The div showing line numbers
 */
function updateLineNumbers(textarea, lineNumbersDiv) {
    const lines = textarea.value.split('\n').length;
    let numbers = '';
    for (let i = 1; i <= lines; i++) {
        numbers += i + '\n';
    }
    lineNumbersDiv.textContent = numbers;
}

/**
 * Initialize code editor with syntax highlighting
 */
function initializeCodeEditor() {
    const modalEditor = document.getElementById('modal-code-editor');
    const modalHighlight = document.getElementById('modal-code-highlight');
    const modalLineNumbers = document.getElementById('modal-line-numbers');

    if (modalEditor) {
        // Handle Tab key in code editor
        modalEditor.addEventListener('keydown', function(e) {
            if (e.key === 'Tab') {
                e.preventDefault();
                const start = this.selectionStart;
                const end = this.selectionEnd;
                this.value = this.value.substring(0, start) + '    ' + this.value.substring(end);
                this.selectionStart = this.selectionEnd = start + 4;
            }
        });

        // Update syntax highlighting
        function updateHighlight() {
            if (modalHighlight) {
                modalHighlight.innerHTML = highlightSyntax(modalEditor.value);
            }
            if (modalLineNumbers) {
                updateLineNumbers(modalEditor, modalLineNumbers);
            }
        }

        // Initialize and attach event listeners
        updateHighlight();
        modalEditor.addEventListener('input', updateHighlight);

        // Sync scrolling
        modalEditor.addEventListener('scroll', function() {
            if (modalHighlight) {
                modalHighlight.scrollTop = this.scrollTop;
                modalHighlight.scrollLeft = this.scrollLeft;
            }
            if (modalLineNumbers) {
                modalLineNumbers.scrollTop = this.scrollTop;
            }
        });
    }

    // Syntax highlight existing code displays
    document.querySelectorAll('.code-display').forEach(function(display) {
        const code = display.getAttribute('data-code');
        if (code) {
            display.innerHTML = highlightSyntax(code);
        }
    });
}

// ============================================
// INITIALIZATION
// ============================================

/**
 * Initialize all dashboard functionality when DOM is ready
 */
document.addEventListener('DOMContentLoaded', function() {
    // Initialize file input
    initializeFileInput();

    // Initialize code editor
    initializeCodeEditor();

    // Add any other initializations here
    console.log('Teacher Dashboard initialized');
});