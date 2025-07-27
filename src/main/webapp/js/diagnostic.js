/**
 * Pipeline Doctor Plugin JavaScript
 * Provides interactive functionality for diagnostic results display
 */

(function() {
    'use strict';

    // Wait for DOM to be ready
    document.addEventListener('DOMContentLoaded', function() {
        initializeDiagnosticUI();
    });

    /**
     * Initialize the diagnostic UI components
     */
    function initializeDiagnosticUI() {
        initializeFilters();
        initializeExpandCollapse();
        initializeCopyButtons();
        initializeKeyboardNavigation();
    }

    /**
     * Initialize severity filter buttons
     */
    function initializeFilters() {
        const filterButtons = document.querySelectorAll('.filter-btn');
        const diagnosticItems = document.querySelectorAll('.diagnostic-item');

        filterButtons.forEach(button => {
            button.addEventListener('click', function() {
                const severity = this.getAttribute('data-severity');
                
                // Update active button
                filterButtons.forEach(btn => btn.classList.remove('active'));
                this.classList.add('active');
                
                // Filter diagnostic items
                filterDiagnosticItems(diagnosticItems, severity);
                
                // Update URL hash for bookmarking
                if (severity !== 'all') {
                    window.location.hash = 'severity-' + severity.toLowerCase();
                } else {
                    window.location.hash = '';
                }
            });
        });

        // Apply filter from URL hash on load
        const hash = window.location.hash;
        if (hash.startsWith('#severity-')) {
            const severity = hash.replace('#severity-', '').toUpperCase();
            const button = document.querySelector(`[data-severity="${severity}"]`);
            if (button) {
                button.click();
            }
        }
    }

    /**
     * Filter diagnostic items by severity
     */
    function filterDiagnosticItems(items, severity) {
        items.forEach(item => {
            if (severity === 'all' || item.getAttribute('data-severity') === severity) {
                item.classList.remove('filtered-out');
                item.style.display = '';
            } else {
                item.classList.add('filtered-out');
                item.style.display = 'none';
            }
        });

        // Update result count display
        updateFilteredResultCount(items, severity);
    }

    /**
     * Update the displayed result count based on filter
     */
    function updateFilteredResultCount(items, severity) {
        const countElement = document.querySelector('.result-count');
        if (!countElement) return;

        if (severity === 'all') {
            const totalCount = items.length;
            countElement.textContent = `Found ${totalCount} issue(s)`;
        } else {
            const filteredCount = Array.from(items).filter(item => 
                item.getAttribute('data-severity') === severity
            ).length;
            countElement.textContent = `Found ${filteredCount} ${severity.toLowerCase()} issue(s)`;
        }
    }

    /**
     * Initialize expand/collapse functionality
     */
    function initializeExpandCollapse() {
        const diagnosticHeaders = document.querySelectorAll('.diagnostic-header');
        
        diagnosticHeaders.forEach(header => {
            header.addEventListener('click', function() {
                const item = this.closest('.diagnostic-item');
                const details = item.querySelector('.diagnostic-details');
                const expandBtn = this.querySelector('.expand-btn img');
                
                if (details.style.display === 'none' || !details.style.display) {
                    // Expand
                    details.style.display = 'block';
                    item.classList.add('expanded');
                    item.classList.remove('collapsed');
                    if (expandBtn) {
                        expandBtn.style.transform = 'rotate(90deg)';
                    }
                } else {
                    // Collapse
                    details.style.display = 'none';
                    item.classList.add('collapsed');
                    item.classList.remove('expanded');
                    if (expandBtn) {
                        expandBtn.style.transform = 'rotate(0deg)';
                    }
                }
            });
        });

        // Add keyboard support for expand/collapse
        diagnosticHeaders.forEach(header => {
            header.setAttribute('tabindex', '0');
            header.setAttribute('role', 'button');
            header.setAttribute('aria-expanded', 'false');
            
            header.addEventListener('keydown', function(event) {
                if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault();
                    this.click();
                    
                    // Update aria-expanded
                    const expanded = this.closest('.diagnostic-item').classList.contains('expanded');
                    this.setAttribute('aria-expanded', expanded.toString());
                }
            });
        });
    }

    /**
     * Initialize copy-to-clipboard functionality
     */
    function initializeCopyButtons() {
        const copyButtons = document.querySelectorAll('.copy-btn');
        
        copyButtons.forEach(button => {
            button.addEventListener('click', function(event) {
                event.stopPropagation(); // Prevent triggering expand/collapse
                
                const commandBlock = this.closest('.command-block');
                const code = commandBlock.querySelector('code');
                const command = code.textContent;
                
                copyToClipboard(command).then(() => {
                    showCopyFeedback(this, 'Copied!');
                }).catch(() => {
                    showCopyFeedback(this, 'Failed to copy');
                });
            });
        });
    }

    /**
     * Copy text to clipboard
     */
    function copyToClipboard(text) {
        if (navigator.clipboard && window.isSecureContext) {
            return navigator.clipboard.writeText(text);
        } else {
            // Fallback for older browsers
            return new Promise((resolve, reject) => {
                const textArea = document.createElement('textarea');
                textArea.value = text;
                textArea.style.position = 'fixed';
                textArea.style.left = '-999999px';
                textArea.style.top = '-999999px';
                document.body.appendChild(textArea);
                textArea.focus();
                textArea.select();
                
                try {
                    document.execCommand('copy');
                    textArea.remove();
                    resolve();
                } catch (error) {
                    textArea.remove();
                    reject(error);
                }
            });
        }
    }

    /**
     * Show visual feedback for copy action
     */
    function showCopyFeedback(button, message) {
        const originalText = button.textContent;
        button.textContent = message;
        button.disabled = true;
        
        setTimeout(() => {
            button.textContent = originalText;
            button.disabled = false;
        }, 1500);
    }

    /**
     * Initialize keyboard navigation
     */
    function initializeKeyboardNavigation() {
        document.addEventListener('keydown', function(event) {
            // Handle Escape key to collapse all items
            if (event.key === 'Escape') {
                collapseAllItems();
            }
            
            // Handle Ctrl+F to focus filter buttons
            if (event.ctrlKey && event.key === 'f') {
                event.preventDefault();
                const firstFilterBtn = document.querySelector('.filter-btn');
                if (firstFilterBtn) {
                    firstFilterBtn.focus();
                }
            }
        });
    }

    /**
     * Collapse all diagnostic items
     */
    function collapseAllItems() {
        const diagnosticItems = document.querySelectorAll('.diagnostic-item');
        diagnosticItems.forEach(item => {
            const details = item.querySelector('.diagnostic-details');
            const header = item.querySelector('.diagnostic-header');
            const expandBtn = header.querySelector('.expand-btn img');
            
            details.style.display = 'none';
            item.classList.add('collapsed');
            item.classList.remove('expanded');
            
            if (expandBtn) {
                expandBtn.style.transform = 'rotate(0deg)';
            }
            
            header.setAttribute('aria-expanded', 'false');
        });
    }

    /**
     * Global function to toggle specific diagnostic item (called from Jelly template)
     */
    window.toggleDiagnostic = function(resultId) {
        const details = document.getElementById('details-' + resultId);
        const item = details.closest('.diagnostic-item');
        const header = item.querySelector('.diagnostic-header');
        
        if (header) {
            header.click();
        }
    };

    /**
     * Global function to copy command (called from Jelly template)
     */
    window.copyCommand = function(command) {
        copyToClipboard(command).then(() => {
            // Show a toast notification
            showToast('Command copied to clipboard!');
        }).catch(() => {
            showToast('Failed to copy command');
        });
    };

    /**
     * Show a toast notification
     */
    function showToast(message) {
        // Remove existing toast
        const existingToast = document.querySelector('.diagnostic-toast');
        if (existingToast) {
            existingToast.remove();
        }
        
        // Create new toast
        const toast = document.createElement('div');
        toast.className = 'diagnostic-toast';
        toast.textContent = message;
        toast.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            background: #28a745;
            color: white;
            padding: 12px 16px;
            border-radius: 4px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.2);
            z-index: 10000;
            font-size: 14px;
            transition: all 0.3s ease;
        `;
        
        document.body.appendChild(toast);
        
        // Auto-remove after 3 seconds
        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateY(-10px)';
            setTimeout(() => {
                if (toast.parentNode) {
                    toast.remove();
                }
            }, 300);
        }, 3000);
    }

    /**
     * Initialize tooltips for better UX
     */
    function initializeTooltips() {
        const elements = document.querySelectorAll('[data-tooltip]');
        
        elements.forEach(element => {
            element.addEventListener('mouseenter', function() {
                showTooltip(this, this.getAttribute('data-tooltip'));
            });
            
            element.addEventListener('mouseleave', function() {
                hideTooltip();
            });
        });
    }

    /**
     * Show tooltip
     */
    function showTooltip(element, text) {
        const tooltip = document.createElement('div');
        tooltip.className = 'diagnostic-tooltip';
        tooltip.textContent = text;
        tooltip.style.cssText = `
            position: absolute;
            background: #333;
            color: white;
            padding: 6px 8px;
            border-radius: 4px;
            font-size: 12px;
            z-index: 1000;
            pointer-events: none;
            white-space: nowrap;
        `;
        
        document.body.appendChild(tooltip);
        
        const rect = element.getBoundingClientRect();
        tooltip.style.left = rect.left + (rect.width / 2) - (tooltip.offsetWidth / 2) + 'px';
        tooltip.style.top = rect.top - tooltip.offsetHeight - 5 + 'px';
    }

    /**
     * Hide tooltip
     */
    function hideTooltip() {
        const tooltip = document.querySelector('.diagnostic-tooltip');
        if (tooltip) {
            tooltip.remove();
        }
    }

})();